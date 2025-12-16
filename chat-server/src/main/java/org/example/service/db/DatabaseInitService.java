package org.example.service.db;

import org.example.repository.DatabaseException;
import org.h2.tools.RunScript;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Сервис инициализации базы данных.
 */
public class DatabaseInitService {

    private final ConnectionFactoryService connectionFactory;

    private final String initMode;
    private final boolean devMode;


    public DatabaseInitService(ConnectionFactoryService connectionFactory) {
        this.connectionFactory = connectionFactory;

        String env = ConfigLoaderService.getString("app.env");
        this.devMode = env != null && env.equalsIgnoreCase("dev");

        String mode = ConfigLoaderService.getString("db.init.mode");
        this.initMode = (mode == null || mode.isBlank()) ? "never" : mode.trim();
    }

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
    }

    public void runSchema() {
        String path = devMode ? "sql/schema-dev.sql" : "sql/schema.sql";
        runScriptFromClasspath(path);
    }

    private void runScriptFromClasspath(String resourcePath) {
        try (Connection conn = connectionFactory.getConnection();
             InputStream is = DatabaseInitService.class.getClassLoader().getResourceAsStream(resourcePath)) {

            if (is == null) {
                throw new IllegalStateException("SQL ресурс не найден по пути: " + resourcePath);
            }

            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                RunScript.execute(conn, reader);
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL скрипта: " + resourcePath, e);
        }
        catch (IOException e) {
            throw new IllegalStateException("Ошибка чтения SQL ресурса: " + resourcePath, e);
        }
        catch (RuntimeException e) {
            throw e;
        }
    }
}