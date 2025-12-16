package org.example.model.protocol;

import java.time.LocalDateTime;

/**
 * Личное сообщение (direct message).
 *
 * <p>Используется для адресной доставки: отправитель + получатель.</p>
 */
public class DirectMessage {

    private String from;
    private String to;
    private String content;
    private LocalDateTime sentAt;

    public DirectMessage() {
    }

    public DirectMessage(String from, String to, String content, LocalDateTime sentAt) {
        this.from = from;
        this.to = to;
        this.content = content;
        this.sentAt = sentAt;
    }

    public String getFrom() { return from; }

    public String getTo() { return to; }

    public String getContent() { return content; }

    public LocalDateTime getSentAt() { return sentAt; }
}