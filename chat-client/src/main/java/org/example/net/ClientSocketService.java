package org.example.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.example.model.AuthResponse;
import org.example.model.ChatHistoryResponse;
import org.example.model.ChatMessageDto;
import org.example.net.events.Events;
import org.example.service.ConfigLoader;
import org.example.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Клиентский сокет-сервис обмена сообщениями по JSON-протоколу.
 *
 * <p>Чтение выполняется в отдельном потоке. Отправка потокобезопасна на уровне публичных
 * {@code synchronized} методов.</p>
 *
 * <p>Сервис кэширует последний снимок пользователей онлайн и воспроизводит его при установке
 * listener-а, чтобы исключить потерю снапшота при переключении экранов (auth → chat).</p>
 */
public final class ClientSocketService implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(ClientSocketService.class);

    private static final String KEY_HOST = "client.server.host";
    private static final String KEY_PORT = "client.server.port";
    private static final String KEY_CONNECT_TIMEOUT_MS = "client.server.connectTimeoutMs";

    private static final String IN_CHAT_MESSAGE = "CHAT_MESSAGE";
    private static final String IN_DIRECT_MESSAGE = "DIRECT_MESSAGE";
    private static final String IN_HISTORY_RESPONSE = "HISTORY_RESPONSE";

    private static final String IN_USER_PRESENCE = "USER_PRESENCE";
    private static final String IN_USER_JOINED = "USER_JOINED";
    private static final String IN_USER_LEFT = "USER_LEFT";
    private static final String IN_USERS_LIST = "USERS_LIST";
    private static final String IN_USERS = "USERS";

    private static final String IN_ERROR = "ERROR";
    private static final String IN_AUTH_RESPONSE = "AUTH_RESPONSE";
    private static final String IN_AUTH_FAILED = "AUTH_FAILED";

    private final AtomicBoolean running = new AtomicBoolean(false);

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private Thread readerThread;

    private volatile Consumer<Events.Event> listener;
    private volatile List<String> lastUsersSnapshot;

    private ClientAuthCallbacks authCallbacks;

    /**
     * Устанавливает обработчик событий.
     *
     * <p>Если к моменту установки уже известен снимок пользователей онлайн, он будет
     * отправлен в listener немедленно (реплей), чтобы UI не терял снапшот.</p>
     *
     * @param listener обработчик событий; null отключает обработку
     */
    public void setListener(Consumer<Events.Event> listener) {
        this.listener = listener;

        if (listener == null) return;

        if (isConnected()) {
            safeEmit(new Events.Connected());
        }

        List<String> snap = this.lastUsersSnapshot;
        if (snap != null) {
            safeEmit(new Events.UsersSnapshot(snap));
        }
    }

    public void setAuthCallbacks(ClientAuthCallbacks cb) {
        this.authCallbacks = cb;
    }

    public boolean isConnected() {
        Socket s = this.socket;
        return running.get() && s != null && s.isConnected() && !s.isClosed();
    }

    public synchronized void connect() {
        requireNotRunning();

        String host = ConfigLoader.getString(KEY_HOST);
        int port = ConfigLoader.getInt(KEY_PORT);

        if (host == null || host.isBlank()) throw new IllegalStateException("Missing property: " + KEY_HOST);
        if (port <= 0) throw new IllegalStateException("Invalid property: " + KEY_PORT + "=" + port);

        int timeoutMs = ConfigLoader.getInt(KEY_CONNECT_TIMEOUT_MS);
        if (timeoutMs <= 0) timeoutMs = 2500;

        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(host, port), timeoutMs);
            s.setTcpNoDelay(true);

            this.socket = s;
            this.in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            this.out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));

            running.set(true);

            this.readerThread = new Thread(this::readLoopSafe, "client-socket-reader");
            this.readerThread.setDaemon(true);
            this.readerThread.start();

            safeEmit(new Events.Connected());
            log.info("Connected to {}:{}", host, port);
        } catch (IOException e) {
            safeEmit(new Events.Error("Ошибка подключения", e));
            safeStop();
        }
    }

    public void disconnect() {
        safeStop();
    }

    public synchronized void sendChat(String room, String from, String content) {
        ensureRunning();
        requireNotBlank(room, "room");
        requireNotBlank(from, "from");
        requireNotBlank(content, "content");

        JsonObject data = new JsonObject();
        data.addProperty("room", room);
        data.addProperty("from", from);
        data.addProperty("content", content);

        JsonObject root = new JsonObject();
        root.addProperty("type", "CHAT_MESSAGE");
        root.add("data", data);

        writeLine(root.toString());
    }

    public synchronized void sendDirectMessage(String to, String content) {
        ensureRunning();
        requireNotBlank(to, "to");
        requireNotBlank(content, "content");

        JsonObject data = new JsonObject();
        data.addProperty("to", to);
        data.addProperty("content", content);

        JsonObject root = new JsonObject();
        root.addProperty("type", "DIRECT_MESSAGE");
        root.add("data", data);

        writeLine(root.toString());
    }

    public synchronized void requestRoomHistory(String room, int limit) {
        ensureRunning();
        requireNotBlank(room, "room");

        JsonObject data = new JsonObject();
        data.addProperty("scope", "ROOM");
        data.addProperty("room", room);
        data.addProperty("limit", normalizeLimit(limit));

        JsonObject root = new JsonObject();
        root.addProperty("type", "HISTORY_REQUEST");
        root.add("data", data);

        writeLine(root.toString());
    }

    public synchronized void requestDirectHistory(String peer, int limit) {
        ensureRunning();
        requireNotBlank(peer, "peer");

        JsonObject data = new JsonObject();
        data.addProperty("scope", "DM");
        data.addProperty("peer", peer);
        data.addProperty("limit", normalizeLimit(limit));

        JsonObject root = new JsonObject();
        root.addProperty("type", "HISTORY_REQUEST");
        root.add("data", data);

        writeLine(root.toString());
    }

    public synchronized void sendAuth(String action, String username, String password) {
        ensureRunning();
        requireNotBlank(action, "action");

        JsonObject data = new JsonObject();
        data.addProperty("action", action);
        data.addProperty("username", username);
        data.addProperty("password", password);

        JsonObject root = new JsonObject();
        root.addProperty("type", "AUTH_REQUEST");
        root.add("data", data);

        writeLine(root.toString());
    }

    private void writeLine(String json) {
        try {
            out.write(json);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            safeEmit(new Events.Error("Ошибка отправки", e));
            safeStop();
        }
    }

    private void readLoopSafe() {
        try {
            readLoop();
        } catch (Exception e) {
            safeEmit(new Events.Error("Ошибка чтения", e));
        } finally {
            safeStop();
        }
    }

    private void readLoop() throws IOException {
        String line;
        while (running.get() && (line = in.readLine()) != null) {
            String json = line.trim();
            if (!json.isEmpty()) {
                dispatchIncoming(json);
            }
        }
    }

    private void dispatchIncoming(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        String type = requiredString(obj, "type").toUpperCase(Locale.ROOT);
        JsonElement data = obj.get("data");

        switch (type) {
            case IN_CHAT_MESSAGE, IN_DIRECT_MESSAGE -> emitMessageDto(data);
            case IN_HISTORY_RESPONSE -> emitHistory(data);

            case IN_USER_PRESENCE, IN_USER_JOINED, IN_USER_LEFT -> emitPresence(type, data);
            case IN_USERS_LIST, IN_USERS -> emitUsersSnapshot(data);

            case IN_AUTH_RESPONSE -> handleAuthResponse(data);
            case IN_AUTH_FAILED -> handleAuthFailed(data);

            case IN_ERROR -> handleServerError(data);
            default -> log.warn("Unknown event: {}", type);
        }
    }

    private void emitMessageDto(JsonElement data) {
        ChatMessageDto dto = JsonUtil.fromJson(data, ChatMessageDto.class);
        if (dto != null) {
            safeEmit(new Events.Message(dto));
        }
    }

    private void emitHistory(JsonElement data) {
        ChatHistoryResponse resp = JsonUtil.fromJson(data, ChatHistoryResponse.class);
        if (resp != null) {
            safeEmit(new Events.History(resp));
        }
    }

    private void emitPresence(String type, JsonElement data) {
        if (data == null || data.isJsonNull() || !data.isJsonObject()) return;

        JsonObject d = data.getAsJsonObject();
        String username = optString(d, "username");
        int count = optInt(d, "onlineCount", optInt(d, "count", -1));

        if (IN_USER_JOINED.equals(type)) {
            safeEmit(new Events.UserJoined(username, count));
            return;
        }
        if (IN_USER_LEFT.equals(type)) {
            safeEmit(new Events.UserLeft(username, count));
            return;
        }

        // USER_PRESENCE: event/action
        String action = optString(d, "action");
        if (action.isBlank()) action = optString(d, "event");

        String a = action.toLowerCase(Locale.ROOT);
        if (a.contains("join")) safeEmit(new Events.UserJoined(username, count));
        else if (a.contains("left") || a.contains("leave")) safeEmit(new Events.UserLeft(username, count));
    }

    private void emitUsersSnapshot(JsonElement data) {
        List<String> users = parseUsersListNormalized(data);
        this.lastUsersSnapshot = users;
        safeEmit(new Events.UsersSnapshot(users));
    }

    private void handleAuthResponse(JsonElement data) {
        AuthResponse resp = JsonUtil.fromJson(data, AuthResponse.class);
        String username = (resp == null) ? "" : safeTrim(resp.getUsername());

        ClientAuthCallbacks cb = this.authCallbacks;
        if (cb == null) {
            log.warn("AUTH_RESPONSE received but authCallbacks is null");
            return;
        }

        if (!username.isBlank()) cb.onAuthSuccess(username);
        else cb.onAuthFailed("Пустой ответ авторизации");
    }

    private void handleAuthFailed(JsonElement data) {
        String reason = extractReasonFromAny(data);
        ClientAuthCallbacks cb = this.authCallbacks;
        if (cb == null) {
            log.warn("AUTH_FAILED received but authCallbacks is null");
            return;
        }
        cb.onAuthFailed(reason.isBlank() ? "Ошибка авторизации" : reason);
    }

    private void handleServerError(JsonElement data) {
        String reason = extractReasonFromAny(data);
        if (reason.isBlank()) reason = "Ошибка от сервера";
        safeEmit(new Events.Error(reason, null));
    }

    private void safeEmit(Events.Event event) {
        Consumer<Events.Event> l = this.listener;
        if (l == null || event == null) return;
        try {
            l.accept(event);
        } catch (RuntimeException e) {
            log.warn("Listener error: {}", e.getMessage(), e);
        }
    }

    private void requireNotRunning() {
        if (running.get()) throw new IllegalStateException("ClientSocketService is already running");
    }

    private void ensureRunning() {
        if (!isConnected()) throw new IllegalStateException("Not connected");
    }

    private synchronized void safeStop() {
        if (!running.getAndSet(false)) return;

        this.lastUsersSnapshot = List.of();

        safeEmit(new Events.UsersSnapshot(List.of()));
        safeEmit(new Events.Disconnected());

        Thread t = readerThread;
        readerThread = null;
        if (t != null && t != Thread.currentThread()) {
            try { t.interrupt(); } catch (Exception ignored) { }
        }

        Socket s = socket;
        socket = null;
        if (s != null) {
            try { s.close(); } catch (IOException ignored) { }
        }
        closeQuietly(in);
        closeQuietly(out);
    }

    @Override
    public void close() {
        safeStop();
    }

    private static void closeQuietly(Closeable c) {
        if (c == null) return;
        try { c.close(); } catch (IOException ignored) { }
    }

    private static int normalizeLimit(int limit) {
        return limit <= 0 ? 150 : limit;
    }

    private static void requireNotBlank(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is blank");
    }

    private static String requiredString(JsonObject obj, String field) {
        JsonElement el = obj.get(field);
        if (el == null || el.isJsonNull()) throw new JsonParseException("Missing field: " + field);
        String v = el.getAsString();
        if (v == null || v.isBlank()) throw new JsonParseException("Blank field: " + field);
        return v;
    }

    private static String optString(JsonObject obj, String field) {
        JsonElement el = obj.get(field);
        if (el == null || el.isJsonNull()) return "";
        try { return el.getAsString(); } catch (Exception ignored) { return ""; }
    }

    private static int optInt(JsonObject obj, String field, int def) {
        JsonElement el = obj.get(field);
        if (el == null || el.isJsonNull()) return def;
        try { return el.getAsInt(); } catch (Exception ignored) { return def; }
    }

    /**
     * Нормализованный парсер USERS_LIST/USERS:
     * - data может быть массивом: ["a","b"]
     * - data может быть объектом: {"users":[...]} или {"usernames":[...]}
     * - а также: {"<любой_ключ>":[...]} (для совместимости с сервером)
     */
    private static List<String> parseUsersListNormalized(JsonElement data) {
        if (data == null || data.isJsonNull()) return List.of();

        List<String> raw = new ArrayList<>();

        if (data.isJsonArray()) {
            for (JsonElement el : data.getAsJsonArray()) {
                String u = safeTrim((el == null || el.isJsonNull()) ? "" : el.getAsString());
                if (!u.isBlank()) raw.add(u);
            }
            return normalizeUsers(raw);
        }

        if (data.isJsonObject()) {
            JsonObject o = data.getAsJsonObject();

            // 1) сначала ищем стандартные ключи
            JsonElement arr = firstArray(o, "users", "usernames");

            // 2) если не нашли — берём первый попавшийся JsonArray
            if (arr == null) arr = firstAnyArray(o);

            if (arr != null) {
                for (JsonElement el : arr.getAsJsonArray()) {
                    String u = safeTrim((el == null || el.isJsonNull()) ? "" : el.getAsString());
                    if (!u.isBlank()) raw.add(u);
                }
            }

            return normalizeUsers(raw);
        }

        return List.of();
    }

    private static JsonElement firstArray(JsonObject o, String... keys) {
        for (String k : keys) {
            JsonElement el = o.get(k);
            if (el != null && el.isJsonArray()) return el;
        }
        return null;
    }

    /**
     * На случай, если сервер отдает массив пользователей под другим ключом.
     */
    private static JsonElement firstAnyArray(JsonObject o) {
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            JsonElement v = e.getValue();
            if (v != null && v.isJsonArray()) return v;
        }
        return null;
    }

    /**
     * Убираем пустые, дубликаты (без учёта регистра), сортируем для стабильного UI.
     */
    private static List<String> normalizeUsers(List<String> users) {
        if (users == null || users.isEmpty()) return List.of();

        // key = lower-case, value = оригинал (последний)
        Map<String, String> uniq = new HashMap<>();
        for (String u : users) {
            String t = safeTrim(u);
            if (t.isBlank()) continue;
            uniq.put(t.toLowerCase(Locale.ROOT), t);
        }

        List<String> out = new ArrayList<>(uniq.values());
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return List.copyOf(out);
    }

    private static String extractReasonFromAny(JsonElement data) {
        if (data == null || data.isJsonNull()) return "";
        try {
            if (data.isJsonPrimitive()) return safeTrim(data.getAsString());
            if (data.isJsonObject()) {
                JsonObject o = data.getAsJsonObject();
                String m = optString(o, "message");
                if (!m.isBlank()) return safeTrim(m);
                String c = optString(o, "code");
                return safeTrim(c);
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static String safeTrim(String s) {
        return (s == null) ? "" : s.trim();
    }
}