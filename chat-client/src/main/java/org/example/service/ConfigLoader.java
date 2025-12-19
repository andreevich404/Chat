package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static final Properties properties = new Properties();

    private ConfigLoader() {
    }

    static {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.error("Не удалось найти application.properties по пути");
            }
            else {
                properties.load(input);
            }
        }
        catch (IOException ex) {
            logger.error("Ошибка загрузки конфигурации: {}", ex.getMessage(), ex);
        }
    }

    public static String getString(String key) {
        String v = properties.getProperty(key);
        return v == null ? null : v.trim();
    }

    public static int getInt(String key) {
        String v = getString(key);
        if (v == null || v.isBlank()) return 0;
        try {
            return Integer.parseInt(v);
        }
        catch (NumberFormatException e) {
            logger.warn("Неверное значение int свойства: {}='{}'", key, v);
            return 0;
        }
    }

    public static boolean getBoolean(String key) {
        String v = getString(key);
        if (v == null) return false;
        return Boolean.parseBoolean(v);
    }
}