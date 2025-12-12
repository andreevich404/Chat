package org.example.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Вспомогательный утилитный класс для подмены конфигурационных значений
 * {@link ConfigLoaderService} в модульных тестах.
 *
 * <p>Класс предназначен <b>исключительно для тестирования</b> и не должен
 * использоваться в production-коде.</p>
 *
 * <p>Позволяет имитировать:</p>
 * <ul>
 *     <li>значения из {@code application.properties};</li>
 *     <li>переменные окружения ({@code .env}).</li>
 * </ul>
 *
 * <p>Реализация основана на передаче override-карт в
 * {@link ConfigLoaderService#setTestOverrideProperties(Map)} и
 * {@link ConfigLoaderService#setTestOverrideEnv(Map)}.</p>
 *
 * <p>Состояние хранится статически, поэтому после каждого теста
 * рекомендуется очищать подмены (см. {@code @AfterEach} в тестах).</p>
 */
public final class TestConfigLoader {

    /**
     * Карта подменённых свойств, имитирующая {@code application.properties}.
     */
    private static final Map<String, String> properties = new HashMap<>();

    /**
     * Карта подменённых переменных окружения, имитирующая {@code .env}.
     */
    private static final Map<String, String> env = new HashMap<>();

    /**
     * Приватный конструктор предотвращает создание экземпляров утилитного класса.
     */
    private TestConfigLoader() {
    }

    /**
     * Устанавливает или удаляет подменённое значение свойства.
     *
     * <p>Если {@code value == null}, ключ удаляется из карты подмен.</p>
     *
     * @param key   имя свойства
     * @param value значение свойства или {@code null} для удаления
     */
    public static void setProperty(String key, String value) {
        if (value == null) {
            properties.remove(key);
        } else {
            properties.put(key, value);
        }

        ConfigLoaderService.setTestOverrideProperties(properties);
    }

    /**
     * Устанавливает или удаляет подменённую переменную окружения.
     *
     * <p>Если {@code value == null}, переменная удаляется из карты подмен.</p>
     *
     * @param key   имя переменной окружения
     * @param value значение переменной или {@code null} для удаления
     */
    public static void setEnv(String key, String value) {
        if (value == null) {
            env.remove(key);
        }
        else {
            env.put(key, value);
        }
        ConfigLoaderService.setTestOverrideEnv(env);
    }
}