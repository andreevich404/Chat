package org.example.model.protocol;

import java.time.LocalDateTime;

/**
 * Унифицированное представление сообщения для истории/доставки в UI.
 *
 * <p>room != null -> общее сообщение</p>
 * <p>to != null -> личное сообщение</p>
 */
public class ChatMessageDto {

    private String room;
    private String from;
    private String to;
    private String content;
    private LocalDateTime sentAt;

    public ChatMessageDto() { }

    public ChatMessageDto(String room, String from, String to, String content, LocalDateTime sentAt) {
        this.room = room;
        this.from = from;
        this.to = to;
        this.content = content;
        this.sentAt = sentAt;
    }

    public String getRoom() { return room; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public String getContent() { return content; }
    public LocalDateTime getSentAt() { return sentAt; }
}