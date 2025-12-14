package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Dev-сервис генерации тестовых чатов и сообщений.
 *
 * <p>Используется только в dev-окружении.</p>
 *
 * <p>Создаёт:</p>
 * <ul>
 *     <li>Общий чат (General);</li>
 *     <li>Сообщения от тестовых пользователей;</li>
 * </ul>
 */
public class DevMessageSeederService {

    private static final Logger log = LoggerFactory.getLogger(DevMessageSeederService.class);

    private final ConnectionFactoryService connectionFactory;

    public DevMessageSeederService(ConnectionFactoryService connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void seed() {
        try (Connection conn = connectionFactory.getConnection()) {

            long chatRoomId = ensureGeneralChat(conn);

            insertMessage(conn, chatRoomId, "alice", "Привет всем!");
            insertMessage(conn, chatRoomId, "bob", "Привет, Alice!");
            insertMessage(conn, chatRoomId, "ivan", "Как дела?");

            log.info("Тестовые сообщения успешно созданы");

        }
        catch (SQLException e) {
            log.error("Не удалось создать тестовые сообщения", e);
        }
    }

    private long ensureGeneralChat(Connection conn) throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM chat_room WHERE name = ?")) {

            ps.setString(1, "General");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO chat_room (name, created_at) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, "General");
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    log.info("Тестовая комната создана: General");
                    return keys.getLong(1);
                }
            }
        }

        throw new IllegalStateException("Не удалось создать тестовую комнату General");
    }

    private void insertMessage(Connection conn, long chatRoomId, String username, String content)
            throws SQLException {

        Long userId = findUserId(conn, username);
        if (userId == null) {
            log.warn("Тестовое сообщение пропущено, пользователь не найден: {}", username);
            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO message (chat_room_id, sender_id, content, sent_at) VALUES (?, ?, ?, ?)")) {

            ps.setLong(1, chatRoomId);
            ps.setLong(2, userId);
            ps.setString(3, content);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            log.info("Dev message inserted: [{}] {}", username, content);
        }
    }

    private Long findUserId(Connection conn, String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM users WHERE username = ?")) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }
}