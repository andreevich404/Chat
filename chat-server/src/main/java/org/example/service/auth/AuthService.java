package org.example.service.auth;

import org.example.model.protocol.ApiResponse;
import org.example.model.protocol.AuthResponse;
import org.example.service.security.PasswordHasher;
import org.example.model.domain.User;
import org.example.repository.DatabaseException;
import org.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Сервис авторизации и регистрации пользователей.
 *
 * <p>Ответственность сервиса:</p>
 * <ul>
 *     <li>валидация и нормализация входных данных (username/password);</li>
 *     <li>регистрация пользователя;</li>
 *     <li>аутентификация пользователя;</li>
 *     <li>формирование унифицированного результата через {@link ApiResponse}.</li>
 * </ul>
 *
 * <p>Сервис не должен зависеть от транспортного слоя (socket/HTTP) и не выполняет I/O.</p>
 */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    /**
     * Создаёт сервис авторизации.
     *
     * @param userRepository репозиторий пользователей
     * @param passwordHasher хешер паролей
     */
    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this(userRepository, passwordHasher, Clock.systemDefaultZone());
    }

    /**
     * Создаёт сервис авторизации с подменяемыми часами (удобно для тестирования).
     *
     * @param userRepository репозиторий пользователей
     * @param passwordHasher хешер паролей
     * @param clock источник времени
     */
    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher, Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Регистрирует нового пользователя.
     *
     * @param username имя пользователя (ввод клиента)
     * @param password пароль в открытом виде
     * @return успешный ответ с {@link AuthResponse} или ошибка с кодом из {@link AuthErrorCodes}
     */
    public ApiResponse<AuthResponse> register(String username, String password) {
        NormalizedCredentials creds = normalizeAndValidate(username, password);
        if (!creds.ok()) {
            log.warn("Регистрация отклонена: {}", creds.validationMessage());
            return ApiResponse.fail(AuthErrorCodes.VALIDATION_ERROR, creds.validationMessage());
        }

        String normalizedUsername = creds.username();

        try {
            if (userRepository.existsByUsername(normalizedUsername)) {
                log.warn("Регистрация отклонена: пользователь уже существует, username={}", normalizedUsername);
                return ApiResponse.fail(AuthErrorCodes.USER_EXISTS, "Пользователь уже существует");
            }

            String hash = passwordHasher.hash(creds.password());

            User user = new User(normalizedUsername, hash);
            user.setCreatedAt(LocalDateTime.now(clock));

            userRepository.save(user);

            log.info("Пользователь зарегистрирован успешно: username={}", normalizedUsername);
            return ApiResponse.ok(new AuthResponse(normalizedUsername));

        } catch (DatabaseException e) {
            // БД-ошибки логируются на уровне репозитория/SQL слоя
            return ApiResponse.fail(AuthErrorCodes.DATABASE_ERROR, "Ошибка базы данных");
        } catch (RuntimeException e) {
            log.error("Неожиданная ошибка при регистрации, username={}", normalizedUsername, e);
            return ApiResponse.fail(AuthErrorCodes.INTERNAL_ERROR, "Внутренняя ошибка сервера");
        }
    }

    /**
     * Выполняет вход пользователя.
     *
     * @param username имя пользователя (ввод клиента)
     * @param password пароль в открытом виде
     * @return успешный ответ с {@link AuthResponse} или ошибка с кодом из {@link AuthErrorCodes}
     */
    public ApiResponse<AuthResponse> login(String username, String password) {
        NormalizedCredentials creds = normalizeAndValidate(username, password);
        if (!creds.ok()) {
            log.warn("Вход отклонен: {}", creds.validationMessage());
            return ApiResponse.fail(AuthErrorCodes.VALIDATION_ERROR, creds.validationMessage());
        }

        String normalizedUsername = creds.username();

        try {
            Optional<User> userOpt = userRepository.findByUsername(normalizedUsername);
            if (userOpt.isEmpty()) {
                log.warn("Вход отклонен: пользователь не найден, username={}", normalizedUsername);
                return ApiResponse.fail(AuthErrorCodes.USER_NOT_FOUND, "Пользователь не найден");
            }

            User user = userOpt.get();
            if (!passwordHasher.verify(creds.password(), user.getPasswordHash())) {
                log.warn("Вход отклонен: неверный пароль, username={}", normalizedUsername);
                return ApiResponse.fail(AuthErrorCodes.INVALID_PASSWORD, "Неверный пароль");
            }

            log.info("Пользователь вошел успешно: username={}", normalizedUsername);
            return ApiResponse.ok(new AuthResponse(user.getUsername()));

        } catch (DatabaseException e) {
            return ApiResponse.fail(AuthErrorCodes.DATABASE_ERROR, "Ошибка базы данных");
        } catch (RuntimeException e) {
            log.error("Неожиданная ошибка во время входа, username={}", normalizedUsername, e);
            return ApiResponse.fail(AuthErrorCodes.INTERNAL_ERROR, "Внутренняя ошибка сервера");
        }
    }

    /**
     * Нормализует и валидирует входные данные.
     *
     * <p>Нормализация username:</p>
     * <ul>
     *     <li>trim;</li>
     *     <li>приведение к единому регистру (lowercase, Locale.ROOT).</li>
     * </ul>
     *
     * <p>Валидация:</p>
     * <ul>
     *     <li>username и password обязательны;</li>
     *     <li>username 3..50 символов;</li>
     *     <li>password 6..100 символов.</li>
     * </ul>
     */
    private NormalizedCredentials normalizeAndValidate(String username, String password) {
        String u = safeTrim(username);
        String p = safeTrim(password);

        if (u.isEmpty() || p.isEmpty()) {
            return NormalizedCredentials.invalid("username и password обязательны");
        }

        // нормализация username (единый ключ в БД/протоколе)
        u = u.toLowerCase(Locale.ROOT);

        if (u.length() < 3 || u.length() > 50) {
            return NormalizedCredentials.invalid("username должен быть длиной 3..50 символов");
        }
        if (p.length() < 6 || p.length() > 100) {
            return NormalizedCredentials.invalid("password должен быть длиной 6..100 символов");
        }

        return NormalizedCredentials.ok(u, p);
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Контейнер результата нормализации/валидации.
     */
    private record NormalizedCredentials(boolean ok, String username, String password, String validationMessage) {

        static NormalizedCredentials ok(String username, String password) {
            return new NormalizedCredentials(true, username, password, null);
        }

        static NormalizedCredentials invalid(String message) {
            return new NormalizedCredentials(false, null, null, message);
        }
    }
}