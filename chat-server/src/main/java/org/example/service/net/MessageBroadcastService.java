package org.example.service.net;

import org.example.model.protocol.ServerEvent;
import org.example.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис рассылки событий всем подключённым клиентам.
 *
 * <p>Потокобезопасность:</p>
 * <ul>
 *     <li>клиенты хранятся в {@link ConcurrentHashMap};</li>
 *     <li>запись в {@link BufferedWriter} синхронизирована на объекте writer.</li>
 * </ul>
 */
public class MessageBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(MessageBroadcastService.class);

    private final Map<Long, ClientConnection> clients = new ConcurrentHashMap<>();

    /**
     * Добавляет клиента в рассылку.
     *
     * @param clientId id клиента
     * @param out writer сокета клиента
     */
    public void addClient(long clientId, BufferedWriter out) {
        clients.put(clientId, new ClientConnection(clientId, out));
        log.info("Клиент добавлен в рассылку: id={} onlineCount={}", clientId, getOnlineCount());
    }

    /**
     * Удаляет клиента из рассылки.
     *
     * @param clientId id клиента
     */
    public void removeClient(long clientId) {
        ClientConnection removed = clients.remove(clientId);
        if (removed != null) {
            log.info("Клиент удален из рассылки: id={} onlineCount={}", clientId, getOnlineCount());
        }
    }

    /**
     * Привязывает username к соединению.
     *
     * @param clientId id клиента
     * @param username имя пользователя
     */
    public void bindUsername(long clientId, String username) {
        ClientConnection conn = clients.get(clientId);
        if (conn != null) {
            conn.username = username;
            log.info("Username привязан к клиенту: id={} username={}", clientId, username);
        }
    }

    /**
     * Количество онлайн-пользователей = количество соединений с username.
     *
     * @return count онлайн пользователей
     */
    public int getOnlineCount() {
        int count = 0;
        for (ClientConnection c : clients.values()) {
            String u = c.username;
            if (u != null && !u.isBlank()) count++;
        }
        return count;
    }

    /**
     * Снимок списка пользователей онлайн (уникальный, отсортированный).
     */
    public List<String> getOnlineUsersSnapshot() {
        Map<String, String> uniq = new HashMap<>();
        for (ClientConnection c : clients.values()) {
            String u = c.username;
            if (u == null) continue;
            String t = u.trim();
            if (t.isBlank()) continue;
            uniq.put(t.toLowerCase(Locale.ROOT), t);
        }
        List<String> out = new ArrayList<>(uniq.values());
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return List.copyOf(out);
    }

    /**
     * Рассылка события всем клиентам.
     *
     * @param event событие
     */
    public void broadcast(ServerEvent event) {
        String json = JsonUtil.toJson(event);
        for (ClientConnection conn : clients.values()) {
            if (!trySendOrRemove(conn, json, "broadcast")) {
            }
        }
    }

    /**
     * Рассылка события всем, кроме исключённого клиента.
     *
     * @param excludeClientId исключаемый clientId
     * @param event событие
     */
    public void broadcastExcept(long excludeClientId, ServerEvent event) {
        String json = JsonUtil.toJson(event);
        for (ClientConnection conn : clients.values()) {
            if (conn.clientId == excludeClientId) continue;
            if (!trySendOrRemove(conn, json, "broadcastExcept")) {
            }
        }
    }

    /**
     * Отправляет событие конкретному clientId.
     *
     * @param clientId id клиента
     * @param event событие
     * @return true если отправлено, false если клиента нет/удален
     */
    public boolean sendToClient(long clientId, ServerEvent event) {
        ClientConnection conn = clients.get(clientId);
        if (conn == null) return false;

        String json = JsonUtil.toJson(event);
        return trySendOrRemove(conn, json, "sendToClient");
    }

    /**
     * Отправляет событие пользователю по username (DM).
     *
     * @param username username
     * @param event событие
     * @return true если пользователь онлайн и сообщение доставлено
     */
    public boolean sendToUser(String username, ServerEvent event) {
        if (username == null || username.isBlank()) return false;

        ClientConnection target = null;
        for (ClientConnection conn : clients.values()) {
            String u = conn.username;
            if (u != null && u.equalsIgnoreCase(username)) {
                target = conn;
                break;
            }
        }
        if (target == null) return false;

        String json = JsonUtil.toJson(event);
        return trySendOrRemove(target, json, "sendToUser");
    }

    private boolean trySendOrRemove(ClientConnection conn, String json, String op) {
        try {
            sendRaw(conn, json);
            return true;
        }
        catch (IOException e) {
            log.warn("{} не удался. Удаляем клиента: id={} username={} message={}",
                    op, conn.clientId, conn.username, e.getMessage());
            clients.remove(conn.clientId);
            return false;
        }
    }

    private void sendRaw(ClientConnection conn, String json) throws IOException {
        synchronized (conn.out) {
            conn.out.write(json);
            conn.out.newLine();
            conn.out.flush();
        }
    }

    private static final class ClientConnection {
        private final long clientId;
        private final BufferedWriter out;
        private volatile String username;

        private ClientConnection(long clientId, BufferedWriter out) {
            this.clientId = clientId;
            this.out = out;
        }
    }
}