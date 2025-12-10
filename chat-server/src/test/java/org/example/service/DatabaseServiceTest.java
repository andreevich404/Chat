package org.example.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для {@link DatabaseService}, проверяющие корректность загрузки конфигурации.
 *
 * <p>Проверяются следующие сценарии:</p>
 * <ul>
 *     <li>успешная загрузка при корректных параметрах;</li>
 *     <li>генерация исключения при отсутствии обязательного параметра jdbcUrl;</li>
 * </ul>
 *
 * <p>В тестах временно переопределяются значения конфигурации,
 * после выполнения тестов состояние очищается.</p>
 */
class DatabaseServiceTest {

    @AfterEach
    void clearOverrides() {
        TestConfigLoader.setProperty("jdbcUrl", null);
        TestConfigLoader.setEnv("DB_USERNAME", null);
        TestConfigLoader.setEnv("DB_PASSWORD", null);
        ConfigLoaderService.setTestOverrideProperties(null);
        ConfigLoaderService.setTestOverrideEnv(null);
    }

    /**
     * Проверяет, что конфигурация корректно загружается при наличии всех параметров.
     */
    @Test
    void loadShouldReturnValidDatabaseService() {
        TestConfigLoader.setProperty("jdbcUrl", "jdbc:h2:./testdb");
        TestConfigLoader.setEnv("DB_USERNAME", "user");
        TestConfigLoader.setEnv("DB_PASSWORD", "pass");

        DatabaseService db = DatabaseService.load();

        assertEquals("jdbc:h2:./testdb", db.getJdbcUrl());
        assertEquals("user", db.getUsername());
        assertEquals("pass", db.getPassword());
    }

    /**
     * Проверяет, что при отсутствии обязательного параметра jdbcUrl
     * генерируется исключение {@link IllegalStateException}.
     */
    @Test
    void loadShouldThrowIfJdbcUrlMissing() {
        TestConfigLoader.setProperty("jdbcUrl", null);

        assertThrows(IllegalStateException.class, DatabaseService::load);
    }
}