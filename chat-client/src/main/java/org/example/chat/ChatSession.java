package org.example.chat;

import java.util.Objects;

/**
 * Состояние текущей пользовательской сессии чата.
 */
public final class ChatSession {

    private String username;
    private ChatMode mode = ChatMode.ROOM;
    private String currentPeer;

    public String getUsername() {
        return username;
    }

    public ChatMode getMode() {
        return mode;
    }

    public String getCurrentPeer() {
        return currentPeer;
    }

    public void setUsername(String username) {
        this.username = safeTrim(username);
    }

    public void backToRoom() {
        this.mode = ChatMode.ROOM;
        this.currentPeer = null;
    }

    public void openDirect(String peer) {
        Objects.requireNonNull(peer, "peer");
        this.mode = ChatMode.DM;
        this.currentPeer = safeTrim(peer);
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}