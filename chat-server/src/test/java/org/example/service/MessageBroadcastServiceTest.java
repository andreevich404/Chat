package org.example.service;

import org.example.model.ServerEvent;
import org.example.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Юнит-тест MessageBroadcastService.
 *
 * Проверяет:
 * - broadcastExcept не пишет исключённому клиенту
 */
class MessageBroadcastServiceTest {

    @Test
    void broadcastExceptShouldNotSendToExcludedClient() throws Exception {
        MessageBroadcastService svc = new MessageBroadcastService();

        StringWriter w1 = new StringWriter();
        StringWriter w2 = new StringWriter();

        BufferedWriter out1 = new BufferedWriter(w1);
        BufferedWriter out2 = new BufferedWriter(w2);

        svc.addClient(1L, out1);
        svc.addClient(2L, out2);

        ServerEvent evt = ServerEvent.of("TEST", "hello");
        svc.broadcastExcept(1L, evt);

        out1.flush();
        out2.flush();

        String s1 = w1.toString();
        String s2 = w2.toString();

        assertTrue(s1.isBlank(), "Исключённый клиент не должен получать сообщение");

        assertFalse(s2.isBlank(), "Ожидалось сообщение для второго клиента");

        ServerEvent parsed = JsonUtil.fromJson(s2.trim(), ServerEvent.class);
        assertEquals("TEST", parsed.getType());
    }
}