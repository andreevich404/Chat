package org.example.repository.jdbc;

import org.example.model.domain.ChatMessage;
import org.example.repository.DatabaseException;
import org.example.repository.MessageRepository;
import org.example.service.db.ConnectionFactoryService;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JDBC-реализация {@link MessageRepository}.
 */
public class JdbcMessageRepository implements MessageRepository {

    private final ConnectionFactoryService connectionFactory;

    /**
     * Создаёт репозиторий.
     *
     * @param connectionFactory фабрика соединений
     */
    public JdbcMessageRepository(ConnectionFactoryService connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
    }

    @Override
    public long saveMessage(long roomId, long senderId, String content, LocalDateTime sentAt) {
        validateIds(roomId, senderId);
        String text = requireContent(content);
        Objects.requireNonNull(sentAt, "sentAt");

        final String sql = """
                INSERT INTO message (chat_room_id, sender_id, content, sent_at)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection c = connectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, roomId);
            ps.setLong(2, senderId);
            ps.setString(3, text);
            ps.setTimestamp(4, Timestamp.valueOf(sentAt));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new DatabaseException("Не удалось получить id созданного сообщения", null);
            }

        }
        catch (SQLException e) {
            throw new DatabaseException("Ошибка сохранения сообщения roomId=" + roomId + " senderId=" + senderId, e);
        }
    }

    @Override
    public List<ChatMessage> loadHistory(long roomId, int limit) {
        if (roomId <= 0) throw new IllegalArgumentException("roomId должен быть > 0");
        int safeLimit = Math.max(1, limit);

        final String sql = """
                SELECT u.username AS from_username,
                       m.content,
                       m.sent_at
                FROM message m
                JOIN users u ON u.id = m.sender_id
                WHERE m.chat_room_id = ?
                ORDER BY m.sent_at ASC
                LIMIT ?
                """;

        List<ChatMessage> list = new ArrayList<>();

        try (Connection c = connectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, roomId);
            ps.setInt(2, safeLimit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String from = rs.getString("from_username");
                    String text = rs.getString("content");

                    Timestamp ts = rs.getTimestamp("sent_at");
                    LocalDateTime at = ts == null ? null : ts.toLocalDateTime();

                    list.add(new ChatMessage(null, from, null, text, at));
                }
            }

            return list;

        }
        catch (SQLException e) {
            throw new DatabaseException("Ошибка загрузки истории roomId=" + roomId, e);
        }
    }

    private static void validateIds(long roomId, long senderId) {
        if (roomId <= 0) throw new IllegalArgumentException("roomId должен быть > 0");
        if (senderId <= 0) throw new IllegalArgumentException("senderId должен быть > 0");
    }

    private static String requireContent(String content) {
        String s = content == null ? "" : content.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("content не должен быть пустым");
        }
        if (s.length() > 1000) {
            throw new IllegalArgumentException("content превышает максимальную длину 1000");
        }
        return s;
    }
}