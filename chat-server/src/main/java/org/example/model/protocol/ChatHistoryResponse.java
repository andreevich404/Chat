package org.example.model.protocol;

import java.util.List;

/**
 * Ответ с историей сообщений.
 */
public class ChatHistoryResponse {

    private String scope;     // ROOM | DM
    private String room;      // для ROOM
    private String peer;      // для DM
    private List<ChatMessageDto> messages;

    public ChatHistoryResponse() { }

    public ChatHistoryResponse(String scope, String room, String peer, List<ChatMessageDto> messages) {
        this.scope = scope;
        this.room = room;
        this.peer = peer;
        this.messages = messages;
    }

    public String getScope() { return scope; }
    public String getRoom() { return room; }
    public String getPeer() { return peer; }
    public List<ChatMessageDto> getMessages() { return messages; }
}