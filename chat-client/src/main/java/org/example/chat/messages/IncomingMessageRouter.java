package org.example.chat.messages;

import org.example.chat.ChatMode;
import org.example.chat.ChatSession;
import org.example.model.ChatMessageDto;

/**
 * Определяет, должно ли входящее сообщение
 * отображаться в текущем состоянии клиента.
 */
public final class IncomingMessageRouter {

    private final String roomName;

    public IncomingMessageRouter(String roomName) {
        this.roomName = roomName;
    }

    public boolean shouldDisplay(ChatMessageDto msg, ChatSession session) {
        if (msg == null || session == null) return false;

        if (msg.getTo() == null || msg.getTo().isBlank()) {
            return session.getMode() == ChatMode.ROOM &&
                   roomName.equalsIgnoreCase(msg.getRoom());
        }

        if (session.getMode() != ChatMode.DM) return false;

        String peer = session.getCurrentPeer();
        String me = session.getUsername();

        return peer != null &&
               me != null &&
               (
                   msg.getFrom().equalsIgnoreCase(peer) ||
                   msg.getTo().equalsIgnoreCase(peer)
               );
    }
}