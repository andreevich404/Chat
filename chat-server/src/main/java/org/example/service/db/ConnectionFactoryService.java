package org.example.service.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Фабрика JDBC-подключений.
 *
 * <p>Назначение:</p>
 * <ul>
 *   <li>централизованное создание JDBC {@link Connection};</li>
 *   <li>единое логирование SQL-ошибок;</li>
 *   <li>использование общей конфигурации {@link DatabaseService}.</li>
 * </ul>
 *
 * <p>Реализована как Singleton, так как:</p>
 * <ul>
 *   <li>использует единую конфигурацию БД;</li>
 *   <li>не хранит состояния подключения;</li>
 *   <li>должна быть общей для всех репозиториев.</li>
 * </ul>
 */
public final class ConnectionFactoryService {

    private static final Logger log = LoggerFactory.getLogger(ConnectionFactoryService.class);

    private final DatabaseService config;

    /**
     * Приватный конструктор.
     * Загружает конфигурацию БД.
     */
    private ConnectionFactoryService() {
        this.config = DatabaseService.load();
        log.info("Конфигурация БД загружена: jdbcUrl={}", config.getJdbcUrl());
    }

    /**
     * Lazy-инициализация singleton без синхронизации.
     */
    private static final class Holder {
        private static final ConnectionFactoryService INSTANCE = new ConnectionFactoryService();
    }

    public static ConnectionFactoryService getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Создаёт новое JDBC-подключение.
     *
     * @throws SQLException если соединение не удалось
     */
    public Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(
                    config.getJdbcUrl(),
                    config.getUsername(),
                    config.getPassword()
            );
        }
        catch (SQLException e) {
            logSqlError("Ошибка при создании подключения к БД", e);
            throw e;
        }
    }

    /**
     * Проверяет доступность базы данных.
     *
     * @return true — если соединение и тестовый запрос успешны
     */
    public boolean testConnection() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {

            boolean ok = rs.next();
            if (ok) {
                log.info("Тест соединения с БД прошёл успешно");
            }
            else {
                log.warn("Тест соединения с БД вернул пустой результат");
            }
            return ok;

        }
        catch (SQLException e) {
            logSqlError("Ошибка при тесте соединения с БД", e);
            return false;
        }
    }

    private void logSqlError(String message, SQLException e) {
        log.error(
                "{} | SQLState={} | ErrorCode={} | Message={}",
                message,
                e.getSQLState(),
                e.getErrorCode(),
                e.getMessage()
        );
    }
}