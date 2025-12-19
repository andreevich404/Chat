package org.example.net;

/**
 * Коллбеки результата авторизации клиента.
 */
public interface ClientAuthCallbacks {

    /**
     * Вызывается при успешной авторизации.
     *
     * @param username имя пользователя, подтверждённое сервером
     */
    void onAuthSuccess(String username);

    /**
     * Вызывается при отказе в авторизации.
     *
     * @param reason причина отказа
     */
    void onAuthFailed(String reason);
}