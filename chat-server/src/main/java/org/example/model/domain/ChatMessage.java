package org.example.model.domain;

import java.time.LocalDateTime;

/**
 * DTO сообщения чата.
 *
 * <p>Поддерживаем два режима:</p>
 * <ul>
 *   <li>Общий чат: {@code room="General"}, {@code to=null}</li>
 *   <li>Личный чат: {@code room="DM"}, {@code to="<username>"} (получатель)</li>
 * </ul>
 *
 * <p>JSON (data):</p>
 * <pre>
 * {
 *   "room": "General|DM",
 *   "from": "alice",
 *   "to": "bob",           // только для DM
 *   "content": "Hello",
 *   "sentAt": "2025-12-13T20:10:00"
 * }
 * </pre>
 */
public class ChatMessage {

    private String room;
    private String from;
    private String to;              // nullable, используется для DM
    private String content;
    private LocalDateTime sentAt;

    public ChatMessage() {
    }

    public ChatMessage(String room, String from, String to, String content, LocalDateTime sentAt) {
        this.room = room;
        this.from = from;
        this.to = to;
        this.content = content;
        this.sentAt = sentAt;
    }

    public String getRoom() {
        return room;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}