package org.example.net.events;

/**
 * Типы событий сокет-клиента.
 */
public enum ClientEventType {
    CONNECTED,
    DISCONNECTED,

    AUTH_SUCCESS,
    AUTH_FAILED,

    USERS_SNAPSHOT,
    USER_JOINED,
    USER_LEFT,

    MESSAGE,
    HISTORY,

    ERROR
}