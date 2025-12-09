package org.example.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Вспомогательный класс для подмены значений ConfigLoaderService в тестах.
 * Используется только в модульных тестах.
 */
public final class TestConfigLoader {

    private static final Map<String, String> properties = new HashMap<>();
    private static final Map<String, String> env = new HashMap<>();

    private TestConfigLoader() {}

    public static void setProperty(String key, String value) {
        if (value == null) properties.remove(key);
        else properties.put(key, value);

        // Имитация ConfigLoaderService.getString()
        ConfigLoaderService.setTestOverrideProperties(properties);
    }

    public static void setEnv(String key, String value) {
        if (value == null) env.remove(key);
        else env.put(key, value);

        // Имитация ConfigLoaderService.getEnv()
        ConfigLoaderService.setTestOverrideEnv(env);
    }
}