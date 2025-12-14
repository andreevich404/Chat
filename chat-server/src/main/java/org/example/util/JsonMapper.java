package org.example.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDateTime;

/**
 * JsonMapper для единого формата обмена (envelope: type + data).
 *
 * Требования:
 * - camelCase поля (используются напрямую как имена полей Java)
 * - обработка неверного JSON
 * - обработка неизвестного типа сообщения
 */
public final class JsonMapper {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                            src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString())
            )
            .registerTypeAdapter(
                    LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                            (json == null || json.isJsonNull())
                                    ? null
                                    : LocalDateTime.parse(json.getAsString())
            )
            .create();

    private static final Type JSON_OBJECT_TYPE = new TypeToken<JsonObject>() {}.getType();

    private JsonMapper() {
    }

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    /**
     * Парсит входящее сообщение и возвращает envelope (type + raw data as JsonElement).
     *
     * @throws JsonParseException если JSON невалидный или отсутствует обязательное поле type
     */
    public static ParsedEnvelope parseEnvelope(String json) {
        JsonObject obj = GSON.fromJson(json, JSON_OBJECT_TYPE);
        if (obj == null) {
            throw new JsonParseException("JSON пустой или некорректный");
        }

        JsonElement typeEl = obj.get("type");
        if (typeEl == null || typeEl.isJsonNull() || typeEl.getAsString().isBlank()) {
            throw new JsonParseException("Отсутствует поле type");
        }

        JsonElement dataEl = obj.get("data");
        return new ParsedEnvelope(typeEl.getAsString(), dataEl);
    }

    /**
     * Десериализация поля data в конкретный DTO.
     */
    public static <T> T parseData(JsonElement data, Class<T> clazz) {
        if (data == null || data.isJsonNull()) {
            return null;
        }
        return GSON.fromJson(data, clazz);
    }

    /**
     * Результат разбора envelope.
     */
    public record ParsedEnvelope(String type, JsonElement data) {
    }
}