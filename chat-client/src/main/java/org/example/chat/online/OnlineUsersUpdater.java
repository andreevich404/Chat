package org.example.chat.online;

import java.util.List;

/**
 * Применяет события сервера к состоянию онлайн-пользователей.
 */
public final class OnlineUsersUpdater {

    private final OnlineUsersState state;

    public OnlineUsersUpdater(OnlineUsersState state) {
        this.state = state;
    }

    public void applySnapshot(List<String> users) {
        state.replaceAll(users);
    }

    public void userJoined(String username, int ignoredCount) {
        if (username != null && !username.isBlank()) {
            state.add(username.trim());
        }
    }

    public void userLeft(String username, int ignoredCount) {
        if (username != null && !username.isBlank()) {
            state.remove(username.trim());
        }
    }

    public void reset() {
        state.clear();
    }
}