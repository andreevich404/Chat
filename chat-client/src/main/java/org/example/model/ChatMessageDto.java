package org.example.model;

import java.time.LocalDateTime;

/**
 * Унифицированный формат сообщений, отправляемых сервером клиенту.
 *
 * <p>ROOM-сообщение:</p>
 * <ul>
 *   <li>{@code room} задан</li>
 *   <li>{@code to} отсутствует или пустой</li>
 * </ul>
 *
 * <p>DM-сообщение:</p>
 * <ul>
 *   <li>{@code room} отсутствует или пустой</li>
 *   <li>{@code to} задан</li>
 * </ul>
 */
public class ChatMessageDto {

    private String room;
    private String from;
    private String to;
    private String content;
    private LocalDateTime sentAt;

    /**
     * Конструктор по умолчанию для JSON-десериализации.
     */
    public ChatMessageDto() {
    }

    /**
     * Возвращает комнату для ROOM-сообщений.
     *
     * @return имя комнаты или {@code null}
     */
    public String getRoom() {
        return room;
    }

    /**
     * Возвращает отправителя сообщения.
     *
     * @return имя отправителя или {@code null}
     */
    public String getFrom() {
        return from;
    }

    /**
     * Возвращает получателя для DM-сообщений.
     *
     * @return имя получателя или {@code null}
     */
    public String getTo() {
        return to;
    }

    /**
     * Возвращает текст сообщения.
     *
     * @return текст сообщения или {@code null}
     */
    public String getContent() {
        return content;
    }

    /**
     * Возвращает время отправки.
     *
     * @return время отправки или {@code null}
     */
    public LocalDateTime getSentAt() {
        return sentAt;
    }
}