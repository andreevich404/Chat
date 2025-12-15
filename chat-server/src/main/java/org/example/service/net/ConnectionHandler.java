package org.example.service.net;

import org.example.model.domain.ChatMessage;
import org.example.model.protocol.*;
import org.example.service.auth.AuthService;
import org.example.service.chat.ChatMessagingService;
import org.example.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Обработчик одного клиентского socket-соединения (transport layer).
 *
 * <p>Ответственность:</p>
 * <ul>
 *     <li>читать строковые JSON-сообщения из сокета;</li>
 *     <li>десериализовать {@link ServerEvent};</li>
 *     <li>маршрутизировать сообщения по типам (AUTH/CHAT/DM/HISTORY/LOGOUT);</li>
 *     <li>отправлять ответы и события клиентам;</li>
 *     <li>триггерить presence-события.</li>
 * </ul>
 *
 * <p>Не отвечает за бизнес-логику хранения/истории сообщений — это делегируется в
 * {@link ChatMessagingService}.</p>
 */
public class ConnectionHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ConnectionHandler.class);

    private final long clientId;
    private final Socket socket;

    private final AuthService authService;
    private final ChatMessagingService chatService;
    private final MessageBroadcastService broadcastService;

    private volatile String username;

    private final Map<String, EventProcessor> processors = Map.of(
            Protocol.AUTH_REQUEST, this::onAuthRequest,
            Protocol.CHAT_MESSAGE, this::onChatMessage,
            Protocol.DIRECT_MESSAGE, this::onDirectMessage,
            Protocol.HISTORY_REQUEST, this::onHistoryRequest,
            Protocol.LOGOUT, this::onLogout
    );

    /**
     * Создаёт обработчик клиентского подключения.
     *
     * @param clientId id клиента
     * @param socket сокет клиента
     * @param authService сервис авторизации
     * @param chatService use-case сервис чата
     * @param broadcastService сервис рассылки
     */
    public ConnectionHandler(long clientId,
                             Socket socket,
                             AuthService authService,
                             ChatMessagingService chatService,
                             MessageBroadcastService broadcastService) {
        this.clientId = clientId;
        this.socket = Objects.requireNonNull(socket, "socket");
        this.authService = Objects.requireNonNull(authService, "authService");
        this.chatService = Objects.requireNonNull(chatService, "chatService");
        this.broadcastService = Objects.requireNonNull(broadcastService, "broadcastService");
    }

    @Override
    public void run() {
        String remote = String.valueOf(socket.getRemoteSocketAddress());

        try {
            configureSocketSafely();

            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

                log.info("Обработчик запущен: id={} remote={}", clientId, remote);

                broadcastService.addClient(clientId, out);

                readLoop(in, out);

                log.info("Клиент отключился: id={} remote={}", clientId, remote);
            }

        } catch (IOException e) {
            log.warn("Ошибка I/O клиента: id={} remote={} message={}", clientId, remote, e.getMessage());
            log.debug("Детали I/O: id={} remote={}", clientId, remote, e);

        } catch (RuntimeException e) {
            log.error("Неожиданная ошибка обработчика: id={} remote={}", clientId, remote, e);

        } finally {
            cleanupAndBroadcastLeft();
            closeQuietly();
            log.info("Обработчик остановлен: id={} remote={}", clientId, remote);
        }
    }

    private void readLoop(BufferedReader in, BufferedWriter out) throws IOException {
        while (!socket.isClosed()) {
            String line = readLineWithTimeout(in);
            if (line == null) break;

            String json = line.trim();
            if (json.isEmpty()) continue;

            handleIncoming(json, out);
        }
    }

    private String readLineWithTimeout(BufferedReader in) throws IOException {
        try {
            return in.readLine();
        } catch (SocketTimeoutException timeout) {
            // периодически просыпаемся, чтобы заметить закрытие
            return socket.isClosed() ? null : "";
        }
    }

    private void handleIncoming(String json, BufferedWriter out) throws IOException {
        ServerEvent event = parseEventOrSendError(json, out);
        if (event == null) return;

        String type = normalizeType(event.getType());
        EventProcessor processor = processors.get(type);

        if (processor == null) {
            send(out, ServerEvent.error(Protocol.UNKNOWN_TYPE, "Неизвестный тип сообщения: " + event.getType()));
            return;
        }

        processor.process(event, out);
    }

    private ServerEvent parseEventOrSendError(String json, BufferedWriter out) throws IOException {
        try {
            ServerEvent event = JsonUtil.fromJson(json, ServerEvent.class);
            if (event == null || event.getType() == null || event.getType().isBlank()) {
                send(out, ServerEvent.error(Protocol.INVALID_REQUEST, "Отсутствует поле type"));
                return null;
            }
            return event;
        } catch (RuntimeException e) {
            log.warn("Неверный JSON от клиента id={} message={}", clientId, e.getMessage());
            send(out, ServerEvent.error(Protocol.INVALID_JSON, "Неверный JSON"));
            return null;
        }
    }

    // ------------------- processors -------------------

    private void onAuthRequest(ServerEvent event, BufferedWriter out) throws IOException {
        AuthRequest req = parseData(event, AuthRequest.class, out);
        if (req == null) return;

        String action = safeTrim(req.getAction()).toUpperCase(Locale.ROOT);
        if (action.isBlank()) {
            send(out, ServerEvent.error(Protocol.VALIDATION_ERROR, "Поле action обязательно (LOGIN|REGISTER)"));
            return;
        }

        ApiResponse<AuthResponse> response;
        switch (action) {
            case "REGISTER" -> response = authService.register(req.getUsername(), req.getPassword());
            case "LOGIN" -> response = authService.login(req.getUsername(), req.getPassword());
            default -> {
                send(out, ServerEvent.error(Protocol.UNKNOWN_ACTION, "Неизвестное значение action: " + req.getAction()));
                return;
            }
        }

        if (!response.isSuccess()) {
            send(out, ServerEvent.of(Protocol.ERROR, response.getError()));
            return;
        }

        AuthResponse auth = response.getData();
        this.username = auth.getUsername();
        broadcastService.bindUsername(clientId, this.username);

        // 1) auth ok
        send(out, ServerEvent.of(Protocol.AUTH_RESPONSE, auth));

        // 2) room history
        List<ChatMessageDto> history = chatService.getRoomHistory(Protocol.DEFAULT_ROOM, Protocol.DEFAULT_HISTORY_LIMIT);
        send(out, ServerEvent.of(Protocol.HISTORY_RESPONSE,
                new ChatHistoryResponse("ROOM", Protocol.DEFAULT_ROOM, null, history)
        ));

        // 3) presence
        broadcastPresence("userJoined", this.username);
    }

    private void onHistoryRequest(ServerEvent event, BufferedWriter out) throws IOException {
        if (!requireAuthed(out)) return;

        ChatHistoryRequest req = parseData(event, ChatHistoryRequest.class, out);
        if (req == null) return;

        String scope = safeTrim(req.getScope()).toUpperCase(Locale.ROOT);
        int limit = req.getLimit() <= 0 ? Protocol.DEFAULT_HISTORY_LIMIT : req.getLimit();

        if ("ROOM".equals(scope)) {
            String room = safeTrim(req.getRoom());
            if (room.isBlank()) {
                send(out, ServerEvent.error(Protocol.VALIDATION_ERROR, "room обязателен для scope=ROOM"));
                return;
            }
            List<ChatMessageDto> history = chatService.getRoomHistory(room, limit);
            send(out, ServerEvent.of(Protocol.HISTORY_RESPONSE, new ChatHistoryResponse("ROOM", room, null, history)));
            return;
        }

        if ("DM".equals(scope)) {
            String peer = safeTrim(req.getPeer());
            if (peer.isBlank()) {
                send(out, ServerEvent.error(Protocol.VALIDATION_ERROR, "peer обязателен для scope=DM"));
                return;
            }
            List<ChatMessageDto> history = chatService.getDirectHistory(username, peer, limit);
            send(out, ServerEvent.of(Protocol.HISTORY_RESPONSE, new ChatHistoryResponse("DM", null, peer, history)));
            return;
        }

        send(out, ServerEvent.error(Protocol.UNKNOWN_SCOPE, "Неизвестный scope: " + req.getScope()));
    }

    private void onChatMessage(ServerEvent event, BufferedWriter out) throws IOException {
        if (!requireAuthed(out)) return;

        ChatMessage msg = parseData(event, ChatMessage.class, out);
        if (msg == null) return;

        String room = safeTrim(msg.getRoom());
        if (room.isBlank()) room = Protocol.DEFAULT_ROOM;

        String content = normalizeContentOrSendError(msg.getContent(), out);
        if (content == null) return;

        LocalDateTime sentAt = msg.getSentAt() == null ? LocalDateTime.now() : msg.getSentAt();

        // persist
        chatService.postToRoom(room, username, content, sentAt);

        // broadcast
        ChatMessageDto dto = new ChatMessageDto(room, username, null, content, sentAt);
        broadcastService.broadcast(ServerEvent.of(Protocol.CHAT_MESSAGE, dto));
    }

    private void onDirectMessage(ServerEvent event, BufferedWriter out) throws IOException {
        if (!requireAuthed(out)) return;

        DirectMessage dm = parseData(event, DirectMessage.class, out);
        if (dm == null) return;

        String to = safeTrim(dm.getTo());
        if (to.isBlank()) {
            send(out, ServerEvent.error(Protocol.VALIDATION_ERROR, "to обязателен"));
            return;
        }

        String content = normalizeContentOrSendError(dm.getContent(), out);
        if (content == null) return;

        LocalDateTime sentAt = dm.getSentAt() == null ? LocalDateTime.now() : dm.getSentAt();

        // persist
        chatService.postDirect(username, to, content, sentAt);

        // deliver
        ChatMessageDto dto = new ChatMessageDto(null, username, to, content, sentAt);
        ServerEvent outEvent = ServerEvent.of(Protocol.DIRECT_MESSAGE, dto);

        boolean delivered = broadcastService.sendToUser(to, outEvent);
        if (!delivered) {
            send(out, ServerEvent.error(Protocol.USER_OFFLINE, "Пользователь не в сети: " + to));
        }

        // echo sender
        broadcastService.sendToClient(clientId, outEvent);
    }

    private void onLogout(ServerEvent event, BufferedWriter out) throws IOException {
        if (!requireAuthed(out)) return;

        String left = this.username;
        this.username = null;

        // remove first to ensure onlineCount is "after"
        broadcastService.removeClient(clientId);
        broadcastPresence("userLeft", left);

        closeQuietly();
    }

    // ------------------- helpers -------------------

    private <T> T parseData(ServerEvent event, Class<T> clazz, BufferedWriter out) throws IOException {
        try {
            Object raw = event.getData();
            T parsed = JsonUtil.fromJson(JsonUtil.toJson(raw), clazz);
            if (parsed == null) {
                send(out, ServerEvent.error(Protocol.INVALID_REQUEST, "Поле data обязательно"));
            }
            return parsed;
        } catch (RuntimeException e) {
            send(out, ServerEvent.error(Protocol.INVALID_REQUEST, "Поле data имеет неверный формат"));
            return null;
        }
    }

    private boolean requireAuthed(BufferedWriter out) throws IOException {
        if (username != null && !username.isBlank()) return true;
        send(out, ServerEvent.error(Protocol.UNAUTHORIZED, "Сначала выполните вход"));
        return false;
    }

    private String normalizeContentOrSendError(String content, BufferedWriter out) throws IOException {
        if (content == null || content.isBlank()) {
            send(out, ServerEvent.error(Protocol.VALIDATION_ERROR, "content не должен быть пустым"));
            return null;
        }
        String normalized = content.trim();
        if (normalized.length() > Protocol.MAX_MESSAGE_LENGTH) {
            send(out, ServerEvent.error(Protocol.VALIDATION_ERROR,
                    "content превышает максимальную длину " + Protocol.MAX_MESSAGE_LENGTH));
            return null;
        }
        return normalized;
    }

    private void broadcastPresence(String action, String user) {
        if (user == null || user.isBlank()) return;
        int online = broadcastService.getOnlineCount();
        broadcastService.broadcast(ServerEvent.of(Protocol.USER_PRESENCE, new UserPresenceEvent(action, user, online)));
    }

    private void cleanupAndBroadcastLeft() {
        broadcastService.removeClient(clientId);

        String leftUser = this.username;
        if (leftUser != null && !leftUser.isBlank()) {
            broadcastPresence("userLeft", leftUser);
        }
    }

    private void send(BufferedWriter out, ServerEvent event) throws IOException {
        out.write(JsonUtil.toJson(event));
        out.newLine();
        out.flush();
    }

    private static String normalizeType(String type) {
        return type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private void configureSocketSafely() {
        try {
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(2000);
        } catch (Exception ignored) {
            // intentionally ignored
        }
    }

    private void closeQuietly() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // intentionally ignored
        }
    }

    @FunctionalInterface
    private interface EventProcessor {
        void process(ServerEvent event, BufferedWriter out) throws IOException;
    }
}