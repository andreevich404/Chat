package org.example.model.protocol;

/**
 * DTO событий сервера для клиента.
 * Единый формат всех сообщений сервера:
 * {
 *   "type": "AUTH_RESPONSE|CHAT_MESSAGE|ERROR",
 *   "data": { ... }
 * }
 */
public class ServerEvent {

    private String type;
    private Object data;

    public ServerEvent() {
    }

    public ServerEvent(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public static ServerEvent of(String type, Object data) {
        return new ServerEvent(type, data);
    }

    public static ServerEvent error(String code, String message) {
        return new ServerEvent("ERROR", new ErrorResponse(code, message));
    }

    public String getType() {
        return type;
    }

    public Object getData() {
        return data;
    }
}