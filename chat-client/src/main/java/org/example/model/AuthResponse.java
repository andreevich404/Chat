package org.example.model;

/**
 * Ответ сервера на успешную авторизацию (LOGIN/REGISTER).
 *
 * <p>Используется клиентом для подтверждения входа и получения имени пользователя,
 * с которым установлена сессия.</p>
 */
public class AuthResponse {

    private String username;

    /**
     * Конструктор по умолчанию для JSON-десериализации.
     */
    public AuthResponse() {
    }

    /**
     * Создаёт ответ авторизации.
     *
     * @param username имя пользователя
     */
    public AuthResponse(String username) {
        this.username = username;
    }

    /**
     * Возвращает имя пользователя.
     *
     * @return имя пользователя
     */
    public String getUsername() {
        return username;
    }
}