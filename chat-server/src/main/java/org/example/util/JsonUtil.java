package org.example.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.time.LocalDateTime;

/**
 * Утилитный класс для работы с JSON-сериализацией и десериализацией.
 *
 * <p>Использует библиотеку {@link Gson} для преобразования объектов
 * Java в JSON-строку и обратно.</p>
 *
 * <p>Особенности реализации:</p>
 * <ul>
 *     <li>Настроен кастомный сериализатор для {@link LocalDateTime};</li>
 *     <li>{@link LocalDateTime} сериализуется в строковый ISO-формат
 *         через {@link LocalDateTime#toString()};</li>
 *     <li>Экземпляр {@link Gson} создаётся один раз и переиспользуется;</li>
 *     <li>Класс является утилитным и не предполагает создание экземпляров.</li>
 * </ul>
 *
 * <p>Используется в серверной логике для формирования и парсинга JSON-сообщений,
 * включая ответы API ({@code ApiResponse}, {@code ErrorResponse} и т.д.).</p>
 */
public final class JsonUtil {

    /**
     * Настроенный экземпляр {@link Gson}.
     *
     * <p>Содержит пользовательский сериализатор для {@link LocalDateTime}.</p>
     */
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                            src == null ? null : new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> {
                        if (json == null || json.isJsonNull()) {
                            return null;
                        }
                        try {
                            return LocalDateTime.parse(json.getAsString());
                        }
                        catch (RuntimeException e) {
                            throw new JsonParseException("Неверный формат LocalDateTime: " + json, e);
                        }
                    })
            .create();

    /**
     * Приватный конструктор предотвращает создание экземпляров утилитного класса.
     */
    private JsonUtil() {
    }

    /**
     * Сериализует объект в JSON-строку.
     *
     * @param obj объект для сериализации
     * @return JSON-представление объекта
     */
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    /**
     * Десериализует JSON-строку в объект указанного класса.
     *
     * @param json  JSON-строка
     * @param clazz класс целевого объекта
     * @param <T>   тип целевого объекта
     * @return объект, созданный из JSON
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
}