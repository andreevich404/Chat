package org.example.repository.jdbc;

import org.example.model.domain.User;
import org.example.repository.DatabaseException;
import org.example.repository.UserRepository;
import org.example.service.db.ConnectionFactoryService;

import java.sql.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC-реализация {@link UserRepository} для таблицы {@code users}.
 */
public class JdbcUserRepository implements UserRepository {

    private final ConnectionFactoryService connectionFactory;
    private final Clock clock;

    /**
     * Создаёт репозиторий.
     *
     * @param connectionFactory фабрика соединений
     */
    public JdbcUserRepository(ConnectionFactoryService connectionFactory) {
        this(connectionFactory, Clock.systemDefaultZone());
    }

    /**
     * Создаёт репозиторий с подменяемым источником времени.
     *
     * @param connectionFactory фабрика соединений
     * @param clock            источник времени
     */
    public JdbcUserRepository(ConnectionFactoryService connectionFactory, Clock clock) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String uname = normalizeLookupKey(username);
        if (uname.isEmpty()) {
            return Optional.empty();
        }

        final String sql = "SELECT id, username, password_hash, created_at FROM users WHERE username = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uname);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                long id = rs.getLong("id");
                String dbUsername = rs.getString("username");
                String passwordHash = rs.getString("password_hash");

                Timestamp createdTs = rs.getTimestamp("created_at");
                LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : null;

                return Optional.of(new User(id, dbUsername, passwordHash, createdAt));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Ошибка поиска пользователя по имени: " + uname, e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        String uname = normalizeLookupKey(username);
        if (uname.isEmpty()) {
            return false;
        }

        final String sql = "SELECT 1 FROM users WHERE username = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uname);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Ошибка проверки существования пользователя: " + uname, e);
        }
    }

    @Override
    public void save(User user) {
        Objects.requireNonNull(user, "user");

        String username = safeTrim(user.getUsername());
        if (username.isEmpty()) {
            throw new IllegalArgumentException("username не должен быть пустым");
        }

        String passwordHash = safeTrim(user.getPasswordHash());
        if (passwordHash.isEmpty()) {
            throw new IllegalArgumentException("passwordHash не должен быть пустым");
        }

        ensureCreatedAt(user);

        if (user.getId() == null) {
            insert(user);
        } else {
            update(user);
        }
    }

    private void insert(User user) {
        final String sql = "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setTimestamp(3, Timestamp.valueOf(user.getCreatedAt()));

            int affected = ps.executeUpdate();
            if (affected <= 0) {
                throw new DatabaseException("Не удалось добавить пользователя: " + user.getUsername(), null);
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("Не удалось получить id созданного пользователя: " + user.getUsername(), null);
                }
                user.setId(keys.getLong(1));
            }

        } catch (SQLException e) {
            throw new DatabaseException("Ошибка добавления пользователя: " + user.getUsername(), e);
        }
    }

    private void update(User user) {
        final String sql = "UPDATE users SET username = ?, password_hash = ?, created_at = ? WHERE id = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setTimestamp(3, Timestamp.valueOf(user.getCreatedAt()));
            ps.setLong(4, user.getId());

            int affected = ps.executeUpdate();
            if (affected <= 0) {
                throw new DatabaseException("Не удалось обновить пользователя id=" + user.getId(), null);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Ошибка обновления пользователя id=" + user.getId(), e);
        }
    }

    private void ensureCreatedAt(User user) {
        if (user.getCreatedAt() != null) return;
        user.setCreatedAt(LocalDateTime.now(clock));
    }

    /**
     * Нормализация ключа поиска по username (lookup).
     *
     * @param username входной username
     * @return нормализованный ключ
     */
    private static String normalizeLookupKey(String username) {
        return safeTrim(username).toLowerCase(Locale.ROOT);
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}