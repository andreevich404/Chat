package org.example.service;

import org.example.model.ApiResponse;
import org.example.model.AuthResponse;
import org.example.model.PasswordHasher;
import org.example.model.User;
import org.example.repository.DatabaseException;
import org.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Сервис авторизации и регистрации пользователей.
 *
 * <p>Отвечает за бизнес-логику операций:</p>
 * <ul>
 *     <li>регистрация пользователя ({@link #register(String, String)});</li>
 *     <li>вход пользователя ({@link #login(String, String)}).</li>
 * </ul>
 *
 * <p>Сервис использует:</p>
 * <ul>
 *     <li>{@link UserRepository} — для проверки существования пользователя, поиска и сохранения;</li>
 *     <li>{@link PasswordHasher} — для безопасного хеширования и проверки паролей.</li>
 * </ul>
 *
 * <p>Формат результата:</p>
 * <ul>
 *     <li>возвращается {@link ApiResponse} с {@link AuthResponse} при успехе;</li>
 *     <li>возвращается {@link ApiResponse} с {@code error} при ошибке.</li>
 * </ul>
 *
 * <p>Логирование:</p>
 * <ul>
 *     <li>логируются бизнес-события (успех/отказ) на уровне {@code INFO/WARN};</li>
 *     <li>{@link DatabaseException} не логируется здесь, чтобы исключения логировались ровно один раз
 *         (обычно на уровне репозитория/SQL слоя);</li>
 *     <li>непредвиденные ошибки логируются как {@code ERROR}.</li>
 * </ul>
 */
public class AuthService {

    /**
     * Логгер сервиса авторизации.
     */
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /**
     * Репозиторий пользователей.
     */
    private final UserRepository userRepository;

    /**
     * Компонент хеширования и проверки паролей.
     */
    private final PasswordHasher passwordHasher;

    /**
     * Создаёт сервис авторизации.
     *
     * @param userRepository репозиторий пользователей
     * @param passwordHasher хешер паролей
     */
    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Регистрирует нового пользователя.
     *
     * <p>Поведение:</p>
     * <ul>
     *     <li>валидирует входные данные (username и password не должны быть пустыми);</li>
     *     <li>проверяет, что пользователь с таким именем не существует;</li>
     *     <li>хеширует пароль через {@link PasswordHasher};</li>
     *     <li>создаёт {@link User} и сохраняет его в {@link UserRepository}.</li>
     * </ul>
     *
     * <p>Ошибки:</p>
     * <ul>
     *     <li>{@code VALIDATION_ERROR} — если входные параметры некорректны;</li>
     *     <li>{@code USER_EXISTS} — если пользователь уже существует;</li>
     *     <li>{@code DATABASE_ERROR} — если произошла ошибка доступа к данным;</li>
     *     <li>{@code INTERNAL_ERROR} — если произошла непредвиденная ошибка.</li>
     * </ul>
     *
     * @param username имя пользователя
     * @param password пароль в открытом виде
     * @return {@link ApiResponse} с {@link AuthResponse} при успехе или {@code error} при ошибке
     */
    public ApiResponse<AuthResponse> register(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            log.warn("Регистрация отклонена: username или password пустые");
            return ApiResponse.fail("VALIDATION_ERROR", "username и password обязательны");
        }

        try {
            if (userRepository.existsByUsername(username)) {
                log.warn("Регистрация отклонена: пользователь уже существует, username={}", username);
                return ApiResponse.fail("USER_EXISTS", "Пользователь уже существует");
            }

            String hash = passwordHasher.hash(password);

            User user = new User(username, hash);
            user.setCreatedAt(LocalDateTime.now());

            userRepository.save(user);

            log.info("Пользователь зарегистрирован успешно: username={}", username);
            return ApiResponse.ok(new AuthResponse(username));
        }
        catch (DatabaseException e) {
            return ApiResponse.fail("DATABASE_ERROR", "Ошибка базы данных");
        }
        catch (RuntimeException e) {
            log.error("Неожиданная ошибка при регистрации, username={}", username, e);
            return ApiResponse.fail("INTERNAL_ERROR", "Внутренняя ошибка сервера");
        }
    }

    /**
     * Выполняет вход пользователя.
     *
     * <p>Поведение:</p>
     * <ul>
     *     <li>валидирует входные данные;</li>
     *     <li>ищет пользователя в {@link UserRepository};</li>
     *     <li>проверяет пароль через {@link PasswordHasher#verify(String, String)}.</li>
     * </ul>
     *
     * <p>Ошибки:</p>
     * <ul>
     *     <li>{@code VALIDATION_ERROR} — если входные параметры некорректны;</li>
     *     <li>{@code USER_NOT_FOUND} — если пользователь не найден;</li>
     *     <li>{@code INVALID_PASSWORD} — если пароль неверный;</li>
     *     <li>{@code DATABASE_ERROR} — если произошла ошибка доступа к данным;</li>
     *     <li>{@code INTERNAL_ERROR} — если произошла непредвиденная ошибка.</li>
     * </ul>
     *
     * @param username имя пользователя
     * @param password пароль в открытом виде
     * @return {@link ApiResponse} с {@link AuthResponse} при успехе или {@code error} при ошибке
     */
    public ApiResponse<AuthResponse> login(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            log.warn("Вход отклонен: username или password пустые");
            return ApiResponse.fail("VALIDATION_ERROR", "username и password обязательны");
        }

        try {
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty()) {
                log.warn("Вход отклонен: пользователь не найден, username={}", username);
                return ApiResponse.fail("USER_NOT_FOUND", "Пользователь не найден");
            }

            User user = userOpt.get();

            if (!passwordHasher.verify(password, user.getPasswordHash())) {
                log.warn("Вход отклонен: неверный пароль, username={}", username);
                return ApiResponse.fail("INVALID_PASSWORD", "Неверный пароль");
            }

            log.info("Пользователь вошел успешно: username={}", username);
            return ApiResponse.ok(new AuthResponse(user.getUsername()));

        }
        catch (DatabaseException e) {
            return ApiResponse.fail("DATABASE_ERROR", "Ошибка базы данных");
        }
        catch (RuntimeException e) {
            log.error("Неожиданная ошибка во время входа, username={}", username, e);
            return ApiResponse.fail("INTERNAL_ERROR", "Внутренняя ошибка сервера");
        }
    }

    /**
     * Проверяет строку на {@code null} и пустоту (включая пробельные символы).
     *
     * @param s строка для проверки
     * @return {@code true}, если строка {@code null} или {@link String#isBlank()} возвращает {@code true}
     */
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}