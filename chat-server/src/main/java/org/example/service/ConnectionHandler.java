package org.example.service;

import org.example.model.*;
import org.example.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Обработчик клиентского подключения:
 * - принимает ServerEvent(type+data)
 * - поддерживает AUTH_REQUEST и CHAT_MESSAGE
 * - уведомляет о userJoined/userLeft
 * - broadcast сообщений всем онлайн
 */
public class ConnectionHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ConnectionHandler.class);

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final long clientId;
    private final Socket socket;
    private final AuthService authService;
    private final MessageBroadcastService broadcastService;

    private volatile String username;

    public ConnectionHandler(long clientId,
                             Socket socket,
                             AuthService authService,
                             MessageBroadcastService broadcastService) {
        this.clientId = clientId;
        this.socket = socket;
        this.authService = authService;
        this.broadcastService = broadcastService;
    }

    @Override
    public void run() {
        String remote = String.valueOf(socket.getRemoteSocketAddress());

        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );
                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
                )
        ) {
            log.info("Обработчик запущен: id={} remote={}", clientId, remote);

            broadcastService.addClient(clientId, out);

            String line;
            while ((line = in.readLine()) != null) {
                String json = line.trim();
                if (json.isEmpty()) {
                    continue;
                }
                handleIncoming(json, out);
            }
            log.info("Клиент отключился: id={} remote={}", clientId, remote);
        }
        catch (IOException e) {
            log.warn("Ошибка I/O клиента: id={} remote={} message={}", clientId, remote, e.getMessage());
            log.debug("Детали ошибки I/O клиента: id={} remote={}", clientId, remote, e);
        }
        catch (RuntimeException e) {
            log.error("Неожиданная ошибка обработчика: id={} remote={}", clientId, remote, e);
        }
        finally {
            broadcastService.removeClient(clientId);

            if (username != null && !username.isBlank()) {
                int online = broadcastService.getOnlineCount();
                broadcastService.broadcast(ServerEvent.of(
                        "USER_PRESENCE",
                        new UserPresenceEvent("userLeft", username, online)
                ));
            }
            closeQuietly();
            log.info("Обработчик остановлен: id={} remote={}", clientId, remote);
        }
    }

    private void handleIncoming(String json, BufferedWriter out) throws IOException {

        final ServerEvent event;
        try {
            event = JsonUtil.fromJson(json, ServerEvent.class);
        }
        catch (RuntimeException e) {
            log.warn("Неверный JSON от клиента id={} сообщение={}", clientId, e.getMessage());
            send(out, ServerEvent.error("INVALID_JSON", "Неверный JSON"));
            return;
        }

        if (event == null || event.getType() == null || event.getType().isBlank()) {
            send(out, ServerEvent.error("INVALID_REQUEST", "Отсутствует поле type"));
            return;
        }

        switch (event.getType()) {
            case "AUTH_REQUEST" -> handleAuthRequest(event, out);
            case "CHAT_MESSAGE" -> handleChatMessage(event, out);
            default -> send(out, ServerEvent.error("UNKNOWN_TYPE", "Неизвестный тип сообщения: " + event.getType()));
        }
    }

    private void handleAuthRequest(ServerEvent event, BufferedWriter out) throws IOException {

        final AuthRequest req;
        try {
            req = JsonUtil.fromJson(JsonUtil.toJson(event.getData()), AuthRequest.class);
        }
        catch (RuntimeException e) {
            send(out, ServerEvent.error("INVALID_REQUEST", "Поле data имеет неверный формат"));
            return;
        }

        if (req == null) {
            send(out, ServerEvent.error("INVALID_REQUEST", "Поле data обязательно"));
            return;
        }

        String action = req.getAction();
        if (action == null || action.isBlank()) {
            send(out, ServerEvent.error("VALIDATION_ERROR", "Поле action обязательно (LOGIN|REGISTER)"));
            return;
        }

        ApiResponse<AuthResponse> response;
        switch (action.toUpperCase()) {
            case "REGISTER" -> response = authService.register(req.getUsername(), req.getPassword());
            case "LOGIN" -> response = authService.login(req.getUsername(), req.getPassword());
            default -> {
                send(out, ServerEvent.error("UNKNOWN_ACTION", "Неизвестное значение action: " + action));
                return;
            }
        }

        if (response.isSuccess()) {
            AuthResponse auth = response.getData();
            this.username = auth.getUsername();

            broadcastService.bindUsername(clientId, this.username);

            send(out, ServerEvent.of("AUTH_RESPONSE", auth));

            int online = broadcastService.getOnlineCount();
            broadcastService.broadcast(ServerEvent.of(
                    "USER_PRESENCE",
                    new UserPresenceEvent("userJoined", this.username, online)
            ));

        }
        else {
            send(out, ServerEvent.of("ERROR", response.getError()));
        }
    }

        private void handleChatMessage(ServerEvent event, BufferedWriter out) throws IOException {

        if (username == null || username.isBlank()) {
            send(out, ServerEvent.error("UNAUTHORIZED", "Сначала выполните вход"));
            return;
        }

        final ChatMessage msg;
        try {
            msg = JsonUtil.fromJson(JsonUtil.toJson(event.getData()), ChatMessage.class);
        } catch (RuntimeException e) {
            send(out, ServerEvent.error("INVALID_REQUEST", "Поле data имеет неверный формат"));
            return;
        }

        if (msg == null) {
            send(out, ServerEvent.error("INVALID_REQUEST", "Поле data обязательно"));
            return;
        }

        String content = msg.getContent();
        if (content == null || content.isBlank()) {
            send(out, ServerEvent.error("VALIDATION_ERROR", "content не должен быть пустым"));
            return;
        }

        String normalizedContent = content.trim();
        if (normalizedContent.length() > MAX_MESSAGE_LENGTH) {
            send(out, ServerEvent.error(
                    "VALIDATION_ERROR",
                    "content превышает максимальную длину " + MAX_MESSAGE_LENGTH
            ));
            return;
        }

        ChatMessage normalized = new ChatMessage(msg.getRoom(), username, normalizedContent, msg.getSentAt());
        broadcastService.broadcastExcept(clientId, ServerEvent.of("CHAT_MESSAGE", normalized));
    }

    private void send(BufferedWriter out, ServerEvent event) throws IOException {
        out.write(JsonUtil.toJson(event));
        out.newLine();
        out.flush();
    }

    private void closeQuietly() {
        try {
            socket.close();
        }
        catch (IOException ignored) {
        }
    }
}