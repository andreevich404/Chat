package org.example.service;

import org.example.model.ServerEvent;
import org.example.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис рассылки сообщений всем подключённым клиентам.
 *
 * <p>Хранит список активных клиентов (онлайн) и предоставляет broadcast(...).</p>
 * <p>Потокобезопасен (ConcurrentHashMap).</p>
 */
public class MessageBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(MessageBroadcastService.class);

    private final Map<Long, ClientConnection> clients = new ConcurrentHashMap<>();

    public void addClient(long clientId, BufferedWriter out) {
        clients.put(clientId, new ClientConnection(clientId, out));
        log.info("Клиент добавлен в рассылку: id={} onlineCount={}", clientId, getOnlineCount());
    }

    public void removeClient(long clientId) {
        ClientConnection removed = clients.remove(clientId);
        if (removed != null) {
            log.info("Клиент удален из рассылки: id={} onlineCount={}", clientId, getOnlineCount());
        }
    }

    /**
     * Привязывает username к подключению (после успешного login/register).
     */
    public void bindUsername(long clientId, String username) {
        ClientConnection conn = clients.get(clientId);
        if (conn != null) {
            conn.username = username;
            log.info("Username привязан к клиенту: id={} username={}", clientId, username);
        }
    }

    public int getOnlineCount() {
        return clients.size();
    }

    /**
     * Рассылает событие всем клиентам.
     * Если клиент недоступен (IOException), он удаляется из списка.
     */
    public void broadcast(ServerEvent event) {
        String json = JsonUtil.toJson(event);

        for (ClientConnection conn : clients.values()) {
            try {
                sendRaw(conn, json);
            }
            catch (IOException e) {
                log.warn("Рассылка не удалась. Удаляем клиента: id={} username={} сообщение={}",
                        conn.clientId, conn.username, e.getMessage());
                clients.remove(conn.clientId);
            }
        }
    }

    /**
     * Рассылает событие всем, кроме отправителя (опционально полезно для чата).
     */
    public void broadcastExcept(long excludeClientId, ServerEvent event) {
        String json = JsonUtil.toJson(event);

        for (ClientConnection conn : clients.values()) {
            if (conn.clientId == excludeClientId) {
                continue;
            }
            try {
                sendRaw(conn, json);
            }
            catch (IOException e) {
                log.warn("Рассылка не удалась. Удаляем клиента: id={} username={} сообщение={}",
                        conn.clientId, conn.username, e.getMessage());
                clients.remove(conn.clientId);
            }
        }
    }

    private void sendRaw(ClientConnection conn, String json) throws IOException {
        conn.out.write(json);
        conn.out.newLine();
        conn.out.flush();
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