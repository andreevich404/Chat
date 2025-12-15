package org.example.service.net;

/**
 * Константы протокола обмена сообщениями между клиентом и сервером.
 */
public final class Protocol {

    private Protocol() {
    }

    // ---- Event types (server<->client) ----
    public static final String AUTH_REQUEST = "AUTH_REQUEST";
    public static final String AUTH_RESPONSE = "AUTH_RESPONSE";

    public static final String CHAT_MESSAGE = "CHAT_MESSAGE";
    public static final String DIRECT_MESSAGE = "DIRECT_MESSAGE";

    public static final String HISTORY_REQUEST = "HISTORY_REQUEST";
    public static final String HISTORY_RESPONSE = "HISTORY_RESPONSE";

    public static final String USER_PRESENCE = "USER_PRESENCE";

    public static final String ERROR = "ERROR";
    public static final String LOGOUT = "LOGOUT";

    // ---- Error codes ----
    public static final String INVALID_JSON = "INVALID_JSON";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String UNKNOWN_TYPE = "UNKNOWN_TYPE";
    public static final String UNKNOWN_ACTION = "UNKNOWN_ACTION";
    public static final String UNKNOWN_SCOPE = "UNKNOWN_SCOPE";
    public static final String USER_OFFLINE = "USER_OFFLINE";

    // ---- Domain defaults ----
    public static final String DEFAULT_ROOM = "General";
    public static final int DEFAULT_HISTORY_LIMIT = 150;
    public static final int MAX_MESSAGE_LENGTH = 1000;
}