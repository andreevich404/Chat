package org.example.model.protocol;

/**
 * DTO-класс успешного ответа авторизации или регистрации пользователя.
 *
 * <p>Используется в составе {@link ApiResponse} для передачи данных
 * клиенту при успешном выполнении операций входа или регистрации.</p>
 *
 */
public class AuthResponse {

    /**
     * Имя пользователя, прошедшего авторизацию или регистрацию.
     */
    private final String username;

    /**
     * Создаёт объект успешного ответа авторизации.
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