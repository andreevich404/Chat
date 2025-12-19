package org.example.chat.actions;

import org.example.chat.ChatMode;
import org.example.chat.ChatSession;
import org.example.net.ClientSocketService;

import java.util.Objects;

/**
 * Use-case отправки сообщений.
 */
public final class ChatActions {

    private final String defaultRoom;

    public ChatActions(String defaultRoom) {
        this.defaultRoom = defaultRoom;
    }

    public void send(ChatSession session,
                     ClientSocketService socket,
                     String content) {

        Objects.requireNonNull(session);
        Objects.requireNonNull(socket);

        String text = content == null ? "" : content.trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException("Пустое сообщение");
        }

        if (session.getMode() == ChatMode.ROOM) {
            socket.sendChat(defaultRoom, session.getUsername(), text);
            return;
        }

        String peer = session.getCurrentPeer();
        if (peer == null || peer.isBlank()) {
            throw new IllegalStateException("Не выбран пользователь");
        }

        socket.sendDirectMessage(peer, text);
    }
}