package org.example.model.protocol;

import java.util.List;

/**
 * Снапшот списка пользователей онлайн.
 */
public class UsersListEvent {

    private List<String> users;

    public UsersListEvent() {
    }

    public UsersListEvent(List<String> users) {
        this.users = users;
    }

    public List<String> getUsers() {
        return users;
    }
}