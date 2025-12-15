package org.example.repository.jdbc;

import org.example.repository.ChatRoomRepository;
import org.example.repository.DatabaseException;
import org.example.service.db.ConnectionFactoryService;

import java.sql.*;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * JDBC-реализация {@link ChatRoomRepository}.
 */
public class JdbcChatRoomRepository implements ChatRoomRepository {

    private static final String ROOM_TYPE_ROOM = "ROOM";
    private static final String ROOM_TYPE_DM = "DM";

    private final ConnectionFactoryService connectionFactory;

    /**
     * Создаёт репозиторий.
     *
     * @param connectionFactory фабрика соединений
     */
    public JdbcChatRoomRepository(ConnectionFactoryService connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
    }

    @Override
    public Optional<Long> findRoomIdByName(String roomName) {
        String name = safeTrim(roomName);
        if (name.isEmpty()) return Optional.empty();

        final String sql = """
                SELECT id
                FROM chat_room
                WHERE room_type = ? AND name = ?
                """;

        try (Connection c = connectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, ROOM_TYPE_ROOM);
            ps.setString(2, name);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска комнаты ROOM по имени: " + name, e);
        }
    }

    @Override
    public long createRoom(String roomName) {
        String name = safeTrim(roomName);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("roomName не должен быть пустым");
        }

        Optional<Long> existing = findRoomIdByName(name);
        if (existing.isPresent()) return existing.get();

        // Пытаемся создать. Если словим уникальный конфликт (гонка) — перечитаем id.
        final String insertSql = """
                INSERT INTO chat_room (name, room_type)
                VALUES (?, ?)
                """;

        try (Connection c = connectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, ROOM_TYPE_ROOM);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new DatabaseException("Не удалось получить id созданной ROOM: " + name, null);

        } catch (SQLException e) {
            Optional<Long> after = findRoomIdByName(name);
            if (after.isPresent()) return after.get();

            throw new DatabaseException("Ошибка создания ROOM: " + name, e);
        }
    }

    @Override
    public long createDirectRoom() {
        String technicalName = generateDmName();

        final String sql = """
                INSERT INTO chat_room (name, room_type)
                VALUES (?, ?)
                """;

        try (Connection c = connectionFactory.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, technicalName);
            ps.setString(2, ROOM_TYPE_DM);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new DatabaseException("Не удалось получить id созданной DM комнаты", null);

        } catch (SQLException e) {
            throw new DatabaseException("Ошибка создания DM комнаты", e);
        }
    }

    private static String generateDmName() {
        long n1 = System.nanoTime();
        int n2 = ThreadLocalRandom.current().nextInt(1_000_000);
        return "DM:TEMP:" + n1 + "-" + n2;
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}