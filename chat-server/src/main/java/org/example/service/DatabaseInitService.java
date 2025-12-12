package org.example.service;

import org.example.repository.DatabaseException;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Сервис инициализации базы данных.
 *
 * <p>Выполняет только DDL-скрипт схемы (prod/dev) в зависимости от конфигурации.</p>
 *
 * <p>Тестовые пользователи (dev) создаются через {@link DevUserSeederService} и {@link AuthService},
 * чтобы гарантировать корректные PBKDF2-хэши.</p>
 *
 * <p>Параметры управления (application.properties):</p>
 * <ul>
 *     <li>{@code app.env}: dev | prod</li>
 *     <li>{@code db.init.mode}: never | schema</li>
 * </ul>
 */
public class DatabaseInitService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitService.class);

    private final ConnectionFactoryService connectionFactory;

    private final String initMode;
    private final boolean devMode;

    private DevUserSeederService devUserSeeder;
    private DevMessageSeederService devMessageSeeder;

    public DatabaseInitService(ConnectionFactoryService connectionFactory) {
        this.connectionFactory = connectionFactory;

        String env = ConfigLoaderService.getString("app.env");
        this.devMode = env != null && env.equalsIgnoreCase("dev");

        String mode = ConfigLoaderService.getString("db.init.mode");
        this.initMode = (mode == null || mode.isBlank()) ? "never" : mode.trim();
    }

    /**
     * Подключает dev-seeder (создание тестовых пользователей через AuthService).
     * Вызывать только если AuthService/UserRepository уже созданы.
     *
     * @param devUserSeeder сервис генерации dev-пользователей
     */
    public void setDevUserSeeder(DevUserSeederService devUserSeeder) {
        this.devUserSeeder = devUserSeeder;
    }

    public void setDevMessageSeeder(DevMessageSeederService devMessageSeeder) {
        this.devMessageSeeder = devMessageSeeder;
    }

    /**
     * Выполняет инициализацию БД согласно конфигурации.
     */
    public void init() {
        switch (initMode.toLowerCase()) {
            case "schema":
                runSchema();
                break;
            case "never":
                break;
            default:
                throw new IllegalStateException("Неизвестное значение db.init.mode: " + initMode);
        }

        if (devMode) {
            if (devUserSeeder != null) {
                devUserSeeder.seed();
            }
            if (devMessageSeeder != null) {
                devMessageSeeder.seed();
            }
        }
    }

    /**
     * Выполняет DDL-скрипт схемы.
     * В dev-режиме используется {@code sql/schema-dev.sql}, в prod — {@code sql/schema.sql}.
     */
    public void runSchema() {
        runScriptFromClasspath(devMode ? "sql/schema-dev.sql" : "sql/schema.sql");
    }

    private void runScriptFromClasspath(String resourcePath) {
        try (Connection conn = connectionFactory.getConnection();
             InputStream is = DatabaseInitService.class.getClassLoader().getResourceAsStream(resourcePath)) {

            if (is == null) {
                throw new IllegalStateException("SQL ресурс не найден по пути: " + resourcePath);
            }

            RunScript.execute(conn, new InputStreamReader(is, StandardCharsets.UTF_8));
            log.info("SQL скрипт выполнен успешно: {}", resourcePath);

        } catch (SQLException e) {
            log.error("SQL ошибка при выполнении скрипта: {} | SQLState={} | ErrorCode={} | Message={}",
                    resourcePath, e.getSQLState(), e.getErrorCode(), e.getMessage());
            throw new DatabaseException("Ошибка выполнения SQL скрипта: " + resourcePath, e);

        } catch (RuntimeException e) {
            log.error("Неожиданная ошибка при выполнении скрипта: {}", resourcePath, e);
            throw e;

        } catch (Exception e) {
            log.error("Неожиданная ошибка при выполнении скрипта: {}", resourcePath, e);
            throw new IllegalStateException("Ошибка выполнения SQL скрипта: " + resourcePath, e);
        }
    }
}