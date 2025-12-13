package org.example.service;

import org.example.model.AuthRequest;
import org.example.model.ServerEvent;
import org.example.util.JsonUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты JSON-протокола (ServerEvent + payload).
 *
 * <p>Проверяет:</p>
 * <ul>
 *     <li>корректную сериализацию/десериализацию ServerEvent;</li>
 *     <li>вложенный payload (AuthRequest) через поле data;</li>
 *     <li>обработку неверного JSON (исключение при парсинге).</li>
 * </ul>
 */
class JsonProtocolTest {

    @Test
    void shouldSerializeAndDeserializeServerEventWithAuthRequestPayload() {
        AuthRequest payload = new AuthRequest("LOGIN", "ivan", "123");
        ServerEvent outEvent = ServerEvent.of("AUTH_REQUEST", payload);

        String json = JsonUtil.toJson(outEvent);
        assertNotNull(json);
        assertFalse(json.isBlank());

        ServerEvent parsedEvent = JsonUtil.fromJson(json, ServerEvent.class);
        assertNotNull(parsedEvent);
        assertEquals("AUTH_REQUEST", parsedEvent.getType());
        assertNotNull(parsedEvent.getData());

        // data -> JSON -> AuthRequest
        AuthRequest parsedPayload = JsonUtil.fromJson(JsonUtil.toJson(parsedEvent.getData()), AuthRequest.class);
        assertNotNull(parsedPayload);

        assertEquals("LOGIN", parsedPayload.getAction());
        assertEquals("ivan", parsedPayload.getUsername());
        assertEquals("123", parsedPayload.getPassword());
    }

    @Test
    void shouldSerializeAndDeserializeErrorEvent() {
        ServerEvent errorEvent = ServerEvent.error("INVALID_JSON", "Неверный JSON");

        String json = JsonUtil.toJson(errorEvent);
        assertNotNull(json);
        assertFalse(json.isBlank());

        ServerEvent parsedEvent = JsonUtil.fromJson(json, ServerEvent.class);
        assertNotNull(parsedEvent);
        assertEquals("ERROR", parsedEvent.getType());
        assertNotNull(parsedEvent.getData());

        org.example.model.ErrorResponse err =
                JsonUtil.fromJson(JsonUtil.toJson(parsedEvent.getData()), org.example.model.ErrorResponse.class);

        assertNotNull(err);
        assertEquals("INVALID_JSON", err.getCode());
        assertEquals("Неверный JSON", err.getMessage());
    }

    @Test
    void shouldThrowOnInvalidJson() {
        String invalidJson = "{ this is not json }";

        assertThrows(RuntimeException.class, () -> JsonUtil.fromJson(invalidJson, ServerEvent.class));
    }
}