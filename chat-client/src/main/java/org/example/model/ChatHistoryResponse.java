package org.example.model;

import java.util.List;

/**
 * Ответ сервера с историей сообщений.
 *
 * <p>Структура данных (data):</p>
 * <ul>
 *   <li>{@code scope}: {@code "ROOM"} или {@code "DM"}</li>
 *   <li>{@code room}: имя комнаты для {@code scope="ROOM"}</li>
 *   <li>{@code peer}: собеседник для {@code scope="DM"}</li>
 *   <li>{@code messages}: список сообщений</li>
 * </ul>
 */
public class ChatHistoryResponse {

    private String scope;
    private String room;
    private String peer;
    private List<ChatMessageDto> messages;

    /**
     * Конструктор по умолчанию для JSON-десериализации.
     */
    public ChatHistoryResponse() {
    }

    /**
     * Возвращает область истории.
     *
     * @return {@code "ROOM"} или {@code "DM"}
     */
    public String getScope() {
        return scope;
    }

    /**
     * Возвращает имя комнаты, если {@code scope="ROOM"}.
     *
     * @return имя комнаты или {@code null}
     */
    public String getRoom() {
        return room;
    }

    /**
     * Возвращает имя собеседника, если {@code scope="DM"}.
     *
     * @return имя собеседника или {@code null}
     */
    public String getPeer() {
        return peer;
    }

    /**
     * Возвращает список сообщений.
     *
     * @return список сообщений или {@code null}
     */
    public List<ChatMessageDto> getMessages() {
        return messages;
    }
}