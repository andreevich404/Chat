package org.example.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты для {@link ConfigLoaderService}.
 *
 * <p>В тестах используется вспомогательный класс {@link TestConfigLoader}
 * для подмены значений:</p>
 * <ul>
 *     <li>свойств из {@code application.properties};</li>
 *     <li>переменных окружения ({@code .env}).</li>
 * </ul>
 *
 * <p>Это позволяет изолировать тесты от реальной конфигурации среды
 * и проверять поведение {@link ConfigLoaderService} детерминированно.</p>
 *
 * <p>После каждого теста все подмены конфигураций очищаются,
 * чтобы избежать побочных эффектов между тестами.</p>
 */
class ConfigLoaderServiceTest {

    /**
     * Очищает тестовые подмены свойств и переменных окружения
     * после выполнения каждого теста.
     */
    @AfterEach
    void clearOverrides() {
        TestConfigLoader.setProperty("some.key.to.clear", null);
        TestConfigLoader.setEnv("SOME_ENV_TO_CLEAR", null);
        ConfigLoaderService.setTestOverrideProperties(null);
        ConfigLoaderService.setTestOverrideEnv(null);
    }

    /**
     * Проверяет, что {@link ConfigLoaderService#getString(String)}
     * возвращает значение, подменённое через {@link TestConfigLoader}.
     */
    @Test
    void getStringShouldReturnOverriddenValue() {
        TestConfigLoader.setProperty("jdbcUrl", "jdbc:h2:./testdb");

        String value = ConfigLoaderService.getString("jdbcUrl");

        assertEquals("jdbc:h2:./testdb", value);
    }

    /**
     * Проверяет, что {@link ConfigLoaderService#getEnv(String)}
     * возвращает значение переменной окружения,
     * подменённое в тесте.
     */
    @Test
    void getEnvShouldReturnOverriddenValue() {
        TestConfigLoader.setEnv("DB_USERNAME", "testUser");

        String value = ConfigLoaderService.getEnv("DB_USERNAME");

        assertEquals("testUser", value);
    }

    /**
     * Проверяет, что {@link ConfigLoaderService#getBoolean(String)}
     * возвращает {@code false}, если ключ отсутствует.
     */
    @Test
    void getBooleanShouldReturnDefaultFalseIfMissing() {
        boolean flag = ConfigLoaderService.getBoolean("MISSING_BOOL_KEY");
        assertFalse(flag);
    }

    /**
     * Проверяет, что {@link ConfigLoaderService#getInt(String)}
     * возвращает {@code 0}, если ключ отсутствует.
     */
    @Test
    void getIntShouldReturnDefaultZeroIfMissing() {
        int value = ConfigLoaderService.getInt("MISSING_INT_KEY");
        assertEquals(0, value);
    }
}