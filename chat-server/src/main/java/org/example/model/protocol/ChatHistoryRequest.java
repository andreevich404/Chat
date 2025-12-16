package org.example.model.protocol;

/**
 * Запрос истории сообщений.
 *
 * <p>scope:</p>
 * <ul>
 *   <li>ROOM — история общего чата (room обязателен)</li>
 *   <li>DM — история личного диалога (peer обязателен)</li>
 * </ul>
 */
public class ChatHistoryRequest {

    private String scope;  // ROOM | DM
    private String room;   // для ROOM
    private String peer;   // для DM (username собеседника)
    private int limit;     // сколько сообщений вернуть (по умолчанию 100)

    public ChatHistoryRequest() { }

    public ChatHistoryRequest(String scope, String room, String peer, int limit) {
        this.scope = scope;
        this.room = room;
        this.peer = peer;
        this.limit = limit;
    }

    public String getScope() { return scope; }
    public String getRoom() { return room; }
    public String getPeer() { return peer; }
    public int getLimit() { return limit; }
}