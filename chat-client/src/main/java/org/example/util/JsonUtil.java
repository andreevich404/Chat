package org.example.util;

import com.google.gson.*;
import java.time.LocalDateTime;

/**
 * Утилиты JSON (Gson) для сериализации/десериализации моделей клиента.
 */
public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, type, ctx) ->
                            src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, type, ctx) ->
                            (json == null || json.isJsonNull()) ? null : LocalDateTime.parse(json.getAsString()))
            .create();

    private JsonUtil() { }

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static JsonElement toJsonTree(Object obj) {
        return GSON.toJsonTree(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static <T> T fromJson(JsonElement el, Class<T> clazz) {
        if (el == null || el.isJsonNull()) return null;
        return GSON.fromJson(el, clazz);
    }
}