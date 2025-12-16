package org.example.chat.messages;

import org.example.chat.ChatMode;
import org.example.chat.ChatSession;
import org.example.model.ChatHistoryResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Применение истории сообщений к текущему состоянию.
 */
public final class MessageHistoryApplier {

    private final String roomName;

    public MessageHistoryApplier(String roomName) {
        this.roomName = roomName;
    }

    public List<String> apply(ChatHistoryResponse resp,
                              ChatSession session,
                              MessageViewModel formatter) {

        if (resp == null || session == null) return List.of();

        boolean applicable =
                (session.getMode() == ChatMode.ROOM &&
                 "ROOM".equalsIgnoreCase(resp.getScope()) &&
                 roomName.equalsIgnoreCase(resp.getRoom()))
             ||
                (session.getMode() == ChatMode.DM &&
                 "DM".equalsIgnoreCase(resp.getScope()) &&
                 session.getCurrentPeer() != null &&
                 session.getCurrentPeer().equalsIgnoreCase(resp.getPeer()));

        if (!applicable || resp.getMessages() == null) {
            return List.of();
        }

        List<String> out = new ArrayList<>();
        resp.getMessages().forEach(m -> out.add(formatter.format(m)));
        return out;
    }
}