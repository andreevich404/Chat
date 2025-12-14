package org.example.model;

/**
 * Входящее/исходящее сообщение в едином формате.
 *
 * JSON:
 * {
 *   "type": "...",
 *   "data": { ... }
 * }
 */
public class MessageEnvelope {

    private String type;
    private Object data;

    public MessageEnvelope() {
    }

    public MessageEnvelope(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public Object getData() {
        return data;
    }
}