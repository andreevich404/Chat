package org.example.service;

import org.example.model.ApiResponse;
import org.example.model.AuthResponse;
import org.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dev-сервис для создания тестовых пользователей.
 *
 * <p>Используется только в окружении dev.</p>
 *
 * <p>Пользователи создаются через {@link AuthService},
 * что гарантирует корректное хеширование паролей.</p>
 *
 * <p>Логины и пароли тестовых пользователей выводятся в консоль
 * исключительно для удобства разработки.</p>
 */
public class DevUserSeederService {

    private static final Logger log = LoggerFactory.getLogger(DevUserSeederService.class);

    private final AuthService authService;
    private final UserRepository userRepository;

    public DevUserSeederService(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    /**
     * Создаёт набор тестовых пользователей.
     */
    public void seed() {
        createUser("alice", "123");
        createUser("bob", "123");
        createUser("ivan", "123");
    }

    private void createUser(String username, String password) {

        if (userRepository.existsByUsername(username)) {
            log.info("Dev пользователь уже существует: username={}", username);
            return;
        }

        ApiResponse<AuthResponse> response =
                authService.register(username, password);

        if (response.isSuccess()) {
            log.info("DEV ПОЛЬЗОВАТЕЛЬ СОЗДАН -> username='{}' password='{}'", username, password);
        }
        else {
            log.warn(
                    "Не удалось создать dev пользователя: username={} code={} message={}",
                    username,
                    response.getError().getCode(),
                    response.getError().getMessage()
            );
        }
    }
}