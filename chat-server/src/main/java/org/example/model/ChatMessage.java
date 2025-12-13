package org.example.model;

import java.time.LocalDateTime;

/**
 * DTO сообщения чата.
 *
 * JSON формат:
 * {
 *   "type": "CHAT_MESSAGE",
 *   "data": {
 *     "room": "General",
 *     "from": "alice",
 *     "content": "Hello",
 *     "sentAt": "2025-12-13T20:10:00"
 *   }
 * }
 */
public class ChatMessage {

    private String room;
    private String from;
    private String content;
    private LocalDateTime sentAt;

    public ChatMessage() {
    }

    public ChatMessage(String room, String from, String content, LocalDateTime sentAt) {
        this.room = room;
        this.from = from;
        this.content = content;
        this.sentAt = sentAt;
    }

    public String getRoom() {
        return room;
    }

    public String getFrom() {
        return from;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}