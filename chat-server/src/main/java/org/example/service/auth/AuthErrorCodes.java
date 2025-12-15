package org.example.service.auth;

/**
 * Коды ошибок авторизации/регистрации.
 *
 */
public final class AuthErrorCodes {

    private AuthErrorCodes() {
    }

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String USER_EXISTS = "USER_EXISTS";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String INVALID_PASSWORD = "INVALID_PASSWORD";
    public static final String DATABASE_ERROR = "DATABASE_ERROR";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
}