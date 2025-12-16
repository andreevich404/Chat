package org.example.chat.online;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * Состояние онлайн-пользователей клиента.
 */
public final class OnlineUsersState {

    private final ObservableList<String> users =
            FXCollections.observableArrayList();

    public ObservableList<String> getUsers() {
        return users;
    }

    public int getOnlineCount() {
        return users.size();
    }

    void replaceAll(List<String> snapshot) {
        users.setAll(snapshot);
    }

    void add(String username) {
        if (users.stream().noneMatch(u -> u.equalsIgnoreCase(username))) {
            users.add(username);
        }
    }

    void remove(String username) {
        users.removeIf(u -> u.equalsIgnoreCase(username));
    }

    void clear() {
        users.clear();
    }
}