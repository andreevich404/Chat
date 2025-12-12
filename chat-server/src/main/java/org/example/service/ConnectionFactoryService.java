package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Фабрика подключений к базе данных (JDBC).
 */
public class ConnectionFactoryService {

    private static final Logger log = LoggerFactory.getLogger(ConnectionFactoryService.class);
    private static final ConnectionFactoryService INSTANCE = new ConnectionFactoryService();

    private final DatabaseService config;

    /**
     * Приватный конструктор:
     * - загружает JDBC-драйвер;
     * - инициализирует конфигурацию БД.
     */
    private ConnectionFactoryService() {
        this.config = DatabaseService.load();
        loadJdbcDriver();
        log.info("Конфигурация базы данных загружена: jdbcUrl={}", config.getJdbcUrl());
    }

    public static ConnectionFactoryService getInstance() {
        return INSTANCE;
    }

    /**
     * Явная загрузка JDBC-драйвера.
     * Обязательно для стабильной работы в CI и production.
     */
    private void loadJdbcDriver() {
        try {
            Class.forName("org.h2.Driver");
            log.info("JDBC драйвер загружен: org.h2.Driver");
        }
        catch (ClassNotFoundException e) {
            log.error("JDBC драйвер не найден в classpath", e);
            throw new IllegalStateException("JDBC драйвер не найден", e);
        }
    }

    /**
     * Создаёт новое JDBC-подключение.
     */
    public Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(config.getJdbcUrl(), config.getUsername(), config.getPassword());
        }
        catch (SQLException e) {
            logSqlError("Ошибка при создании подключения к базе данных", e);
            throw e;
        }
    }

    /**
     * Проверяет доступность БД.
     */
    public boolean testConnection() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                log.info("Тест соединения с БД успешен");
                return true;
            }
            return false;

        } catch (SQLException e) {
            logSqlError("Ошибка при тесте соединения с БД", e);
            return false;
        }
    }

    private void logSqlError(String msg, SQLException e) {
        log.error("{} | SQLState={} | Код ошибки={} | Сообщение={}", msg, e.getSQLState(), e.getErrorCode(), e.getMessage());
    }
}