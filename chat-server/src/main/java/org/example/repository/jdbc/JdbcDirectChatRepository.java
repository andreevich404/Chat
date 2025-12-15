package org.example.repository.jdbc;

import org.example.repository.DatabaseException;
import org.example.repository.DirectChatRepository;
import org.example.service.db.ConnectionFactoryService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC-реализация {@link DirectChatRepository}.
 */
public class JdbcDirectChatRepository implements DirectChatRepository {

    private final ConnectionFactoryService connectionFactory;

    /**
     * Создаёт репозиторий.
     *
     * @param connectionFactory фабрика соединений
     */
    public JdbcDirectChatRepository(ConnectionFactoryService connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
    }

    @Override
    public Optional<Long> findDmRoomId(long userAId, long userBId) {
        Pair p = Pair.of(userAId, userBId);

        final String sql = """
                SELECT chat_room_id
                FROM direct_chat
                WHERE user_low_id = ? AND user_high_id = ?
                """;

        try (Connection c = connectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, p.low());
            ps.setLong(2, p.high());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty();
            }

        }
        catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска DM комнаты для пары: " + p.low() + "-" + p.high(), e);
        }
    }

    @Override
    public long createDm(long userAId, long userBId, long chatRoomId) {
        Pair p = Pair.of(userAId, userBId);
        if (chatRoomId <= 0) {
            throw new IllegalArgumentException("chatRoomId должен быть > 0");
        }

        final String insertSql = """
                INSERT INTO direct_chat (user_low_id, user_high_id, chat_room_id)
                VALUES (?, ?, ?)
                """;

        try (Connection c = connectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(insertSql)) {

            ps.setLong(1, p.low());
            ps.setLong(2, p.high());
            ps.setLong(3, chatRoomId);

            ps.executeUpdate();
            return chatRoomId;

        }
        catch (SQLException e) {
            Optional<Long> existing = findDmRoomId(p.low(), p.high());
            if (existing.isPresent()) {
                long existingRoomId = existing.get();

                if (existingRoomId != chatRoomId) {
                    deleteChatRoomQuietly(chatRoomId);
                }
                return existingRoomId;
            }
            throw new DatabaseException("Ошибка создания DM комнаты для пары: " + p.low() + "-" + p.high(), e);
        }
    }

    private void deleteChatRoomQuietly(long chatRoomId) {
        final String sql = "DELETE FROM chat_room WHERE id = ? AND room_type = 'DM'";
        try (Connection c = connectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, chatRoomId);
            ps.executeUpdate();
        }
        catch (SQLException ignored) {
        }
    }

    private record Pair(long low, long high) {
        static Pair of(long a, long b) {
            if (a <= 0 || b <= 0) {
                throw new IllegalArgumentException("userId должен быть > 0");
            }
            long low = Math.min(a, b);
            long high = Math.max(a, b);
            if (low == high) {
                throw new IllegalArgumentException("Нельзя создать DM с самим собой");
            }
            return new Pair(low, high);
        }
    }
}