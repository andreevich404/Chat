package org.example;

import org.example.model.PasswordHasher;
import org.example.repository.JdbcUserRepository;
import org.example.repository.UserRepository;
import org.example.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Основной класс запуска сервера.
 */
public class ChatServer {

    private static final Logger log = LoggerFactory.getLogger(ChatServer.class);

    private static final AtomicLong CLIENT_SEQ = new AtomicLong(1);

    public static void main(String[] args) {

        String host = ConfigLoaderService.getString("server.host");
        int port = ConfigLoaderService.getInt("server.port");
        String env = ConfigLoaderService.getString("app.env");

        if (host == null || host.isBlank()) {
            host = "localhost";
        }
        if (port <= 0) {
            port = 8080;
        }
        if (env == null || env.isBlank()) {
            env = "prod";
        }

        log.info("Запуск сервера чата... окружение={} адрес={}{}", env, host, ":" + port);

        ConnectionFactoryService connectionFactory;
        try {
            connectionFactory = ConnectionFactoryService.getInstance();
        }
        catch (RuntimeException e) {
            log.error("Не удалось инициализировать ConnectionFactoryService. Запуск сервера отменен.", e);
            return;
        }

        if (!connectionFactory.testConnection()) {
            log.error("Тест соединения с БД не удался. Запуск сервера отменен.");
            return;
        }
        log.info("Тест соединения с БД успешен");

        try {
            UserRepository userRepository = new JdbcUserRepository(connectionFactory);
            PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();
            AuthService authService = new AuthService(userRepository, passwordHasher);

            DatabaseInitService initService = new DatabaseInitService(connectionFactory);

            if ("dev".equalsIgnoreCase(env)) {

                DevUserSeederService devUserSeeder = new DevUserSeederService(authService, userRepository);

                DevMessageSeederService devMessageSeeder = new DevMessageSeederService(connectionFactory);

                initService.setDevUserSeeder(devUserSeeder);
                initService.setDevMessageSeeder(devMessageSeeder);
            }

            initService.init();
            log.info("Инициализация БД завершена (режим={})", ConfigLoaderService.getString("db.init.mode"));

        }
        catch (Exception e) {
            log.error("Ошибка инициализации БД. Запуск сервера отменен.", e);
            return;
        }

        ExecutorService clientPool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host))) {

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Триггер Shutdown сработал. Остановка сервера...");
                clientPool.shutdownNow();
                try {
                    serverSocket.close();
                }
                catch (IOException ignored) {
                }
            }));

            log.info("Сервер запущен и ожидает новых подключений...");

            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    long clientId = CLIENT_SEQ.getAndIncrement();

                    log.info("Клиент подключен: id={} remote={}", clientId, clientSocket.getRemoteSocketAddress());

                    clientPool.submit(new ConnectionHandler(clientId, clientSocket));

                }
                catch (IOException e) {
                    if (serverSocket.isClosed()) {
                        log.info("Сокет сервера закрыт. Цикл приема подключений остановлен.");
                        break;
                    }
                    log.error("Ошибка при приеме клиентского подключения", e);

                }
                catch (RuntimeException e) {
                    log.error("Неожиданная ошибка в цикле приема подключений", e);
                }
            }

        }
        catch (Exception e) {
            log.error("Фатальная ошибка при запуске сервера", e);

        }
        finally {
            clientPool.shutdownNow();
            log.info("Сервер остановлен.");
        }
    }
}