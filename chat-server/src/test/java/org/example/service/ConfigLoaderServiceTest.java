package org.example.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для ConfigLoaderService с использованием TestConfigLoader * для подмены значений в тестах.
 */
class ConfigLoaderServiceTest {

    @AfterEach
    void clearOverrides() {
        // Очистка подмен после каждого теста
        TestConfigLoader.setProperty("some.key.to.clear", null);
        TestConfigLoader.setEnv("SOME_ENV_TO_CLEAR", null);
        ConfigLoaderService.setTestOverrideProperties(null);
        ConfigLoaderService.setTestOverrideEnv(null);
    }

    @Test
    void getStringShouldReturnOverriddenValue() {
        TestConfigLoader.setProperty("jdbcUrl", "jdbc:h2:./testdb");

        String value = ConfigLoaderService.getString("jdbcUrl");

        assertEquals("jdbc:h2:./testdb", value);
    }

    @Test
    void getEnvShouldReturnOverriddenValue() {
        TestConfigLoader.setEnv("DB_USERNAME", "testUser");

        String value = ConfigLoaderService.getEnv("DB_USERNAME");

        assertEquals("testUser", value);
    }

    @Test
    void getBooleanShouldReturnDefaultFalseIfMissing() {
        boolean flag = ConfigLoaderService.getBoolean("MISSING_BOOL_KEY");
        assertFalse(flag);
    }

    @Test
    void getIntShouldReturnDefaultZeroIfMissing() {
        int value = ConfigLoaderService.getInt("MISSING_INT_KEY");
        assertEquals(0, value);
    }
}