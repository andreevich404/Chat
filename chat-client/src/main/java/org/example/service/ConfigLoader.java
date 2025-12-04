package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    private static final Properties properties = new Properties();
    private static final Dotenv dotenv = Dotenv.load();

    private ConfigLoader() {
    }

    static {
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.error("Unable to find application.properties");
            } else {
                properties.load(input);
            }
        } catch (IOException ex) {
            logger.error("Error loading configuration: {}", ex.getMessage());
        }
    }

    public static String getString(String key) {
        return properties.getProperty(key);
    }

    public static int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key, "0"));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key, "false"));
    }

    public static String getEnv(String key) {
        return dotenv.get(key);
    }
}