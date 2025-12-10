package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Фабрика подключений к базе данных (JDBC).
 *
 * <p>Отвечает за:</p>
 * <ul>
 *     <li>централизованное создание {@link Connection} на основе настроек {@link DatabaseService};</li>
 *     <li>логирование SQL-ошибок при установке соединения и выполнении тестового запроса;</li>
 *     <li>предоставление singleton-экземпляра для использования во всех репозиториях.</li>
 * </ul>
 *
 * <p>Использование отдельного сервиса позволяет:</p>
 * <ul>
 *     <li>избежать дублирования кода {@code DriverManager.getConnection(...)} в разных местах;</li>
 *     <li>концентрировать обработку и логирование SQL-ошибок в одном слое;</li>
 *     <li>упростить тестирование и конфигурирование подключения к БД.</li>
 * </ul>
 */
public class ConnectionFactoryService {
    private static final Logger log = LoggerFactory.getLogger(ConnectionFactoryService.class);
    private static final ConnectionFactoryService INSTANCE = new ConnectionFactoryService();

    private final DatabaseService config;

    /**
     * Приватный конструктор инициализирует конфигурацию подключения
     * и логирует используемый {@code jdbcUrl}.
     */
    private ConnectionFactoryService() {
        this.config = DatabaseService.load();
        log.info("Конфигурация базы данных загружена: jdbcUrl={}", config.getJdbcUrl());
    }

    /**
     * Возвращает singleton-экземпляр метода подключений.
     *
     * @return глобальный экземпляр {@code ConnectionFactoryService}
     */
    public static ConnectionFactoryService getInstance() {
        return INSTANCE;
    }

    /**
     * Создаёт новое подключение к базе данных на основе настроек {@link DatabaseService}.
     *
     * <p>При возникновении {@link SQLException} ошибка логируется с указанием
     * SQLState, кода ошибки и текста сообщения, после чего исключение пробрасывается дальше.</p>
     *
     * @return новое JDBC-подключение
     * @throws SQLException если не удалось установить соединение
     */
    public Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(config.getJdbcUrl(), config.getUsername(), config.getPassword());
        }
        catch (SQLException e) {
            logSqlError("Ошибка при создании с базой данных", e);
            throw e;
        }
    }

    /**
     * Выполняет простой тестовый запрос {@code SELECT 1} для проверки доступности БД.
     *
     * @return true, если запрос успешно выполнился и вернул хотя бы одну строку;
     *         false — в противном случае или при SQL-ошибке (ошибка будет залогирована)
     */
    public boolean testConnection() {
        String sql = "SELECT 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                log.info("Тест соединения с базой данных прошел успешно — результат = {}", rs.getInt(1));
                return true;
            }

            log.warn("Запрос теста вернул пустой результат");
            return false;
        }
        catch (SQLException e) {
            logSqlError("Ошибка при выполнении запроса теста", e);
            return false;
        }
    }

    /**
     * Логирует подробную информацию об SQL-ошибке.
     *
     * @param msg служебное сообщение для лога
     * @param e   исходное {@link SQLException}
     */
    private void logSqlError(String msg, SQLException e) {
        log.error("{} | SQLState={} | ErrorCode={} | Message={}", msg, e.getSQLState(), e.getErrorCode(), e.getMessage());
    }
}
