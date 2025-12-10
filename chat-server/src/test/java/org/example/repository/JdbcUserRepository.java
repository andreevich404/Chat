package org.example.repository;

import org.example.model.User;
import org.example.service.ConnectionFactoryService;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class JdbcUserRepository implements UserRepository {

    private final ConnectionFactoryService connectionFactory;

    public JdbcUserRepository(ConnectionFactoryService connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public JdbcUserRepository() {
        this(ConnectionFactoryService.getInstance());
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, created_at FROM users WHERE username = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong("id");
                    String uname = rs.getString("username");
                    String passwordHash = rs.getString("password_hash");
                    Timestamp createdTs = rs.getTimestamp("created_at");
                    LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : null;

                    User user = new User(id, uname, passwordHash, createdAt);
                    return Optional.of(user);
                }
                else {
                    return Optional.empty();
                }
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске пользователя по имени: " + username, e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Ошибка при проверке существования пользователя по имени: " + username, e);
        }
    }

    @Override
    public void save(User user) {
        if (user.getId() == null) {
            insert(user);
        }
        else {
            update(user);
        }
    }

    private void insert(User user) {
        String sql = "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, ?)";

        LocalDateTime createdAt = user.getCreatedAt();
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
            user.setCreatedAt(createdAt);
        }

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setTimestamp(3, Timestamp.valueOf(createdAt));

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Ошибка при добавлении пользователя, не было изменений", null);
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    user.setId(id);
                }
                else {
                    throw new DatabaseException("Ошибка при добавлении пользователя, не было получено ID", null);
                }
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Ошибка при добавлении пользователя с именем: " + user.getUsername(), e);
        }
    }

    private void update(User user) {
        String sql = "UPDATE users SET username = ?, password_hash = ?, created_at = ? WHERE id = ?";

        LocalDateTime createdAt = user.getCreatedAt();
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
            user.setCreatedAt(createdAt);
        }

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setTimestamp(3, Timestamp.valueOf(createdAt));
            ps.setLong(4, user.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Ошибка при обновлении пользователя, не было изменений (id=" + user.getId() + ")", null);
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Ошибка при обновлении пользователя с id: " + user.getId(), e);
        }
    }
}