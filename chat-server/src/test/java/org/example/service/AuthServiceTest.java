package org.example.service;

import org.example.model.protocol.ApiResponse;
import org.example.model.protocol.AuthResponse;
import org.example.service.security.PasswordHasher;
import org.example.model.domain.User;
import org.example.repository.InMemoryUserRepository;
import org.example.repository.UserRepository;
import org.example.service.auth.AuthService;
import org.example.service.security.Pbkdf2PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты для {@link AuthService}.
 *
 * <p>Тесты проверяют бизнес-логику регистрации и авторизации:</p>
 * <ul>
 *     <li>успешная регистрация пользователя;</li>
 *     <li>ошибка при попытке регистрации существующего пользователя;</li>
 *     <li>успешный вход при корректном пароле;</li>
 *     <li>ошибка при неверном пароле;</li>
 *     <li>ошибка при входе несуществующего пользователя;</li>
 *     <li>валидация входных параметров.</li>
 * </ul>
 *
 * <p>Для изоляции тестов от внешних ресурсов используется:</p>
 * <ul>
 *     <li>{@link InMemoryUserRepository} — вместо реальной базы данных;</li>
 *     <li>{@link Pbkdf2PasswordHasher} — для реального безопасного хеширования паролей.</li>
 * </ul>
 */
class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordHasher passwordHasher;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        this.userRepository = new InMemoryUserRepository();
        this.passwordHasher = new Pbkdf2PasswordHasher();
        this.authService = new AuthService(userRepository, passwordHasher);
    }

    /**
     * Проверяет, что регистрация создаёт пользователя при условии,
     * что пользователь с таким именем ещё не существует.
     */
    @Test
    void registerShouldCreateUserWhenUserNotExists() {
        ApiResponse<AuthResponse> response = authService.register("ivan", "password123");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("ivan", response.getData().getUsername());
        assertNull(response.getError());

        assertTrue(userRepository.existsByUsername("ivan"));

        User stored = userRepository.findByUsername("ivan").orElseThrow();
        assertNotNull(stored.getPasswordHash());
        assertNotEquals("password123", stored.getPasswordHash(), "Пароль не должен храниться в открытом виде");
        assertNotNull(stored.getCreatedAt(), "Дата создания должна быть установлена при регистрации");
    }

    /**
     * Проверяет, что регистрация возвращает ошибку, если пользователь уже существует.
     */
    @Test
    void registerShouldFailWhenUserAlreadyExists() {
        authService.register("ivan", "password123");

        ApiResponse<AuthResponse> response = authService.register("ivan", "anotherPassword");

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertNull(response.getData());
        assertNotNull(response.getError());
        assertEquals("USER_EXISTS", response.getError().getCode());
    }

    /**
     * Проверяет, что login выполняется успешно при корректном пароле.
     */
    @Test
    void loginShouldSucceedWithCorrectPassword() {
        authService.register("ivan", "password123");

        ApiResponse<AuthResponse> response = authService.login("ivan", "password123");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("ivan", response.getData().getUsername());
        assertNull(response.getError());
    }

    /**
     * Проверяет, что login возвращает ошибку при неверном пароле.
     */
    @Test
    void loginShouldFailWithInvalidPassword() {
        authService.register("ivan", "password123");

        ApiResponse<AuthResponse> response = authService.login("ivan", "wrongPassword");

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertNull(response.getData());
        assertNotNull(response.getError());
        assertEquals("INVALID_PASSWORD", response.getError().getCode());
    }

    /**
     * Проверяет, что login возвращает ошибку, если пользователь не найден.
     */
    @Test
    void loginShouldFailWhenUserNotFound() {
        ApiResponse<AuthResponse> response = authService.login("unknown", "password123");

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertNull(response.getData());
        assertNotNull(response.getError());
        assertEquals("USER_NOT_FOUND", response.getError().getCode());
    }

    /**
     * Проверяет, что register возвращает ошибку при некорректных входных данных.
     */
    @Test
    void registerShouldFailWhenInputInvalid() {
        ApiResponse<AuthResponse> response1 = authService.register(null, "password123");
        assertFalse(response1.isSuccess());
        assertEquals("VALIDATION_ERROR", response1.getError().getCode());

        ApiResponse<AuthResponse> response2 = authService.register("ivan", null);
        assertFalse(response2.isSuccess());
        assertEquals("VALIDATION_ERROR", response2.getError().getCode());

        ApiResponse<AuthResponse> response3 = authService.register("   ", "password123");
        assertFalse(response3.isSuccess());
        assertEquals("VALIDATION_ERROR", response3.getError().getCode());

        ApiResponse<AuthResponse> response4 = authService.register("ivan", "   ");
        assertFalse(response4.isSuccess());
        assertEquals("VALIDATION_ERROR", response4.getError().getCode());
    }

    /**
     * Проверяет, что login возвращает ошибку при некорректных входных данных.
     */
    @Test
    void loginShouldFailWhenInputInvalid() {
        ApiResponse<AuthResponse> response1 = authService.login(null, "password123");
        assertFalse(response1.isSuccess());
        assertEquals("VALIDATION_ERROR", response1.getError().getCode());

        ApiResponse<AuthResponse> response2 = authService.login("ivan", null);
        assertFalse(response2.isSuccess());
        assertEquals("VALIDATION_ERROR", response2.getError().getCode());

        ApiResponse<AuthResponse> response3 = authService.login("   ", "password123");
        assertFalse(response3.isSuccess());
        assertEquals("VALIDATION_ERROR", response3.getError().getCode());

        ApiResponse<AuthResponse> response4 = authService.login("ivan", "   ");
        assertFalse(response4.isSuccess());
        assertEquals("VALIDATION_ERROR", response4.getError().getCode());
    }
}