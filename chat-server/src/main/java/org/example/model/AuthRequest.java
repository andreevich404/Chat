package org.example.model;

/**
 * DTO запроса авторизации/регистрации от клиента.
 *
 * JSON формат:
 * {
 *   "type": "AUTH_REQUEST",
 *   "data": { "action": "LOGIN|REGISTER", "username": "...", "password": "..." }
 * }
 */
public class AuthRequest {

    private String action;   // LOGIN | REGISTER
    private String username;
    private String password;

    public AuthRequest() {
    }

    public AuthRequest(String action, String username, String password) {
        this.action = action;
        this.username = username;
        this.password = password;
    }

    public String getAction() {
        return action;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}