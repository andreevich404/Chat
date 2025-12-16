package org.example.service.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Сервис загрузки конфигурации приложения.
 *
 * <p>Отвечает за чтение параметров из двух источников:</p>
 * <ul>
 *     <li><b>application.properties</b> — основные настройки приложения;</li>
 *     <li><b>.env</b> — переменные окружения (секреты, логины, пароли);</li>
 * </ul>
 *
 * <p>Особенности реализации:</p>
 * <ul>
 *     <li>файл {@code application.properties} загружается один раз при инициализации класса;</li>
 *     <li>если файл отсутствует, ошибка записывается в лог;</li>
 *     <li>значения из {@code .env} обрабатываются через библиотеку {@link Dotenv};</li>
 *     <li>для модульных тестов предусмотрены override-карты ({@code testOverrideProps}, {@code testOverrideEnv}),
 *         позволяющие подменять реальные конфигурации;</li>
 * </ul>
 *
 * <p>Класс является утилитным и не предполагает создание экземпляров.</p>
 */
public class ConfigLoaderService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoaderService.class);

    private static final Properties properties = new Properties();
    private static final Dotenv dotenv = Dotenv
            .configure()
            .ignoreIfMissing()
            .load();

    // Тестовые подмены конфигураций — используются только при тестировании
    private static Map<String, String> testOverrideProps = null;
    private static Map<String, String> testOverrideEnv = null;

    /**
     * Приватный конструктор предотвращает создание экземпляров утилитного класса.
     */
    private ConfigLoaderService() {
    }

    /*
     * Загрузка файла application.properties.
     * Выполняется ровно один раз при загрузке класса.
     */
    static {
        try (InputStream input = ConfigLoaderService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.error("Не удалось найти application.properties");
            }
            else {
                properties.load(input);
            }
        }
        catch (IOException ex) {
            logger.error("Ошибка загрузки конфигурации: {}", ex.getMessage());
        }
    }

    /**
     * Подменяет значения свойств для тестов.
     *
     * @param props карта ключ–значение, имитирующая содержимое файла application.properties
     */
    public static void setTestOverrideProperties(Map<String, String> props) {
        testOverrideProps = props;
    }

    /**
     * Подменяет переменные окружения для тестов.
     *
     * @param env карта ключ–значение, имитирующая содержимое .env-файла
     */
    public static void setTestOverrideEnv(Map<String, String> env) {
        testOverrideEnv = env;
    }

    /**
     * Возвращает строковое значение свойства.
     *
     * <p>Приоритеты:</p>
     * <ol>
     *     <li>значение в testOverrideProps (если установлено);</li>
     *     <li>значение в файле application.properties;</li>
     *     <li>null — если ключ не найден.</li>
     * </ol>
     *
     * @param key имя свойства
     * @return значение свойства или null
     */
    public static String getString(String key) {
        if (testOverrideProps != null && testOverrideProps.containsKey(key)) {
            return testOverrideProps.get(key);
        }
        return properties.getProperty(key);
    }

    /**
     * Возвращает целочисленное свойство.
     *
     * @param key имя свойства
     * @return число или 0, если ключ отсутствует
     * @throws NumberFormatException если значение невозможно преобразовать к числу
     */
    public static int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key, "0"));
    }

    /**
     * Возвращает булево свойство.
     *
     * @param key имя свойства
     * @return true, если значение равно "true" (без учёта регистра)
     */
    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key, "false"));
    }

    /**
     * Возвращает значение переменной окружения.
     *
     * <p>Приоритеты:</p>
     * <ol>
     *     <li>значение из testOverrideEnv;</li>
     *     <li>значение из dotenv;</li>
     *     <li>null — если ключ отсутствует.</li>
     * </ol>
     *
     * @param key имя переменной окружения
     * @return строковое значение или null
     */
    public static String getEnv(String key) {
        if (testOverrideEnv != null && testOverrideEnv.containsKey(key)) {
            return testOverrideEnv.get(key);
        }
        return dotenv.get(key);
    }
}