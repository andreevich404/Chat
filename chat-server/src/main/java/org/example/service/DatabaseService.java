package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Сервис для загрузки параметров подключения к базе данных.
 *
 * <p>Класс инкапсулирует три значения конфигурации:</p>
 * <ul>
 *     <li><b>jdbcUrl</b> — обязательный параметр, определяющий строку подключения к БД;</li>
 *     <li><b>username</b> — имя пользователя БД, может отсутствовать;</li>
 *     <li><b>password</b> — пароль пользователя БД, может отсутствовать;</li>
 * </ul>
 *
 * <p>Загрузка выполняется из двух источников:</p>
 * <ul>
 *     <li><b>application.properties</b> — параметр {@code jdbcUrl};</li>
 *     <li><b>.env</b> — переменные окружения {@code DB_USERNAME} и {@code DB_PASSWORD};</li>
 * </ul>
 *
 * <p>При отсутствии обязательной конфигурации генерируется исключение
 * {@link IllegalStateException}. Отсутствие username/password не является
 * критической ошибкой, но фиксируется в логах уровня WARN.</p>
 *
 * <p>Класс является immutable и потокобезопасным.</p>
 */
public final class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    private final String jdbcUrl;
    private final String username;
    private final String password;

    /**
     * Конструктор создаётся через статическую метод {@link #load()}.
     *
     * @param jdbcUrl  строка подключения к БД (не может быть null)
     * @param username имя пользователя (может быть null)
     * @param password пароль (может быть null)
     */
    private DatabaseService(String jdbcUrl, String username, String password) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl не может быть null");
        this.username = username;
        this.password = password;
    }

    /**
     * Загружает параметры подключения к базе данных из конфигурации проекта.
     *
     * <p>Источник данных:</p>
     * <ul>
     *     <li>{@code application.properties}: параметр {@code jdbcUrl};</li>
     *     <li>{@code .env}: переменные {@code DB_USERNAME} и {@code DB_PASSWORD};</li>
     * </ul>
     *
     * @return корректно проинициализированный экземпляр {@code DatabaseService}
     * @throws IllegalStateException если {@code jdbcUrl} отсутствует или пустой
     */
    public static DatabaseService load() {
        String jdbcUrl = ConfigLoaderService.getString("jdbcUrl");

        String username = ConfigLoaderService.getEnv("DB_USERNAME");
        String password = ConfigLoaderService.getEnv("DB_PASSWORD");

        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalStateException("Свойство 'jdbcUrl' должно быть установлено в application.properties");
        }

        if (username == null) {
            logger.warn("Переменная окружения DB_USERNAME не установлена (username будет null)");
        }
        if (password == null) {
            logger.warn("Переменная окружения DB_PASSWORD не установлена (password будет null)");
        }

        return new DatabaseService(jdbcUrl, username, password);
    }

    /** @return строка подключения JDBC */
    public String getJdbcUrl() {
        return jdbcUrl;
    }
    /** @return имя пользователя БД или null */
    public String getUsername() {
        return username;
    }
    /** @return пароль пользователя БД или null */
    public String getPassword() {
        return password;
    }
}