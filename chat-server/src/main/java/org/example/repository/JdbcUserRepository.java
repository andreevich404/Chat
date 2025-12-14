package org.example.repository;

import org.example.model.User;
import org.example.service.ConnectionFactoryService;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * JDBC-реализация {@link UserRepository} для работы с таблицей {@code users}.
 *
 * <p>Класс предоставляет методы доступа к данным пользователей через JDBC,
 * используя {@link ConnectionFactoryService} для получения соединений.</p>
 *
 * <p>Особенности реализации:</p>
 * <ul>
 *     <li>Используются {@link PreparedStatement} для защиты от SQL-инъекций;</li>
 *     <li>Результаты запросов преобразуются в доменную модель {@link User};</li>
 *     <li>При возникновении {@link SQLException} выбрасывается {@link DatabaseException};</li>
 *     <li>Метод {@link #save(User)} выбирает {@code INSERT} или {@code UPDATE}
 *         в зависимости от наличия {@code id} у пользователя.</li>
 * </ul>
 *
 * <p>Логирование SQL-ошибок рекомендуется выполнять в одном месте (в слое репозитория
 * или в фабрике соединений), чтобы исключения логировались ровно один раз.</p>
 */
public class JdbcUserRepository implements UserRepository {

    /**
     * Фабрика соединений с базой данных.
     */
    private final ConnectionFactoryService connectionFactory;

    /**
     * Создаёт репозиторий с указанной фабрикой соединений.
     *
     * @param connectionFactory фабрика соединений
     */
    public JdbcUserRepository(ConnectionFactoryService connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Создаёт репозиторий, используя singleton-экземпляр {@link ConnectionFactoryService}.
     */
    public JdbcUserRepository() {
        this(ConnectionFactoryService.getInstance());
    }

    /**
     * Ищет пользователя по имени.
     *
     * @param username имя пользователя
     * @return {@link Optional} с пользователем, если найден, иначе пустой {@link Optional}
     * @throws DatabaseException при ошибке обращения к базе данных
     */
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
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске пользователя по имени: " + username, e);
        }
    }

    /**
     * Проверяет существование пользователя по имени.
     *
     * @param username имя пользователя
     * @return {@code true}, если пользователь существует, иначе {@code false}
     * @throws DatabaseException при ошибке обращения к базе данных
     */
    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при проверке существования пользователя по имени: " + username, e);
        }
    }

    /**
     * Сохраняет пользователя в базе данных.
     *
     * <p>Если у пользователя отсутствует {@code id}, выполняется вставка (INSERT),
     * иначе выполняется обновление (UPDATE).</p>
     *
     * @param user пользователь для сохранения
     * @throws DatabaseException при ошибке обращения к базе данных
     */
    @Override
    public void save(User user) {
        if (user.getId() == null) {
            insert(user);
        } else {
            update(user);
        }
    }

    /**
     * Выполняет вставку нового пользователя в таблицу {@code users}.
     *
     * <p>Если {@code createdAt} не задан, он устанавливается в текущее время.</p>
     * <p>После успешной вставки идентификатор пользователя считывается через
     * {@link PreparedStatement#getGeneratedKeys()} и устанавливается в объект {@link User}.</p>
     *
     * @param user пользователь
     * @throws DatabaseException при ошибке вставки или отсутствии сгенерированного ID
     */
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
                } else {
                    throw new DatabaseException("Ошибка при добавлении пользователя, не было получено ID", null);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при добавлении пользователя с именем: " + user.getUsername(), e);
        }
    }

    /**
     * Выполняет обновление существующего пользователя в таблице {@code users}.
     *
     * <p>Если {@code createdAt} не задан, он устанавливается в текущее время.</p>
     *
     * @param user пользователь
     * @throws DatabaseException при ошибке обновления или отсутствии изменённых строк
     */
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
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при обновлении пользователя с id: " + user.getId(), e);
        }
    }
}