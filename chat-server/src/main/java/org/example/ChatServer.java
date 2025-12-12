package org.example;

import org.example.model.PasswordHasher;
import org.example.repository.JdbcUserRepository;
import org.example.repository.UserRepository;
import org.example.service.AuthService;
import org.example.service.ConnectionFactoryService;
import org.example.service.DatabaseService;
import org.example.service.Pbkdf2PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Точка входа серверного приложения Chat.
 *
 * <p>Отвечает за инициализацию серверного ядра:</p>
 * <ul>
 *     <li>загрузку конфигурации;</li>
 *     <li>проверку подключения к базе данных;</li>
 *     <li>инициализацию репозиториев и сервисов;</li>
 *     <li>подготовку сервера к приёму клиентских соединений.</li>
 * </ul>
 *
 * <p>На текущем этапе реализация ограничивается
 * корректной инициализацией инфраструктуры.</p>
 */
public class ChatServer {

    private static final Logger log = LoggerFactory.getLogger(ChatServer.class);

    public static void main(String[] args) {

        log.info("Starting Chat Server...");

        try {

            DatabaseService databaseService = DatabaseService.load();
            log.info("Database configuration loaded");

            ConnectionFactoryService connectionFactory = ConnectionFactoryService.getInstance();

            if (!connectionFactory.testConnection()) {
                log.error("Database connection test failed. Server startup aborted.");
                return;
            }

            log.info("Database connection established successfully");

            UserRepository userRepository = new JdbcUserRepository(connectionFactory);

            PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();

            AuthService authService = new AuthService(userRepository, passwordHasher);

            log.info("AuthService initialized");

            log.info("Chat Server started successfully and ready to accept connections");
        }
        catch (Exception e) {
            log.error("Fatal error during server startup", e);
        }
    }
}