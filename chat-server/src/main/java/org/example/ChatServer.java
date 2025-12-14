package org.example;

import org.example.model.PasswordHasher;
import org.example.repository.JdbcUserRepository;
import org.example.repository.UserRepository;
import org.example.service.*;
import org.example.util.ExecutorServiceResource;
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
        ServerConfig cfg = loadServerConfig();
        log.info("Запуск сервера чата... окружение={} адрес={}:{}", cfg.env(), cfg.host(), cfg.port());

        ConnectionFactoryService connectionFactory = createConnectionFactoryOrExit();
        if (connectionFactory == null) {
            return;
        }

        if (!connectionFactory.testConnection()) {
            log.error("Тест соединения с БД не удался. Запуск сервера отменен.");
            return;
        }
        log.info("Тест соединения с БД успешен");

        UserRepository userRepository = new JdbcUserRepository(connectionFactory);
        PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();
        AuthService authService = new AuthService(userRepository, passwordHasher);

        if (!initDatabase(cfg.env(), connectionFactory, authService, userRepository)) {
            return;
        }

        MessageBroadcastService broadcastService = new MessageBroadcastService();

        try (
                ExecutorServiceResource clientPool =
                        new ExecutorServiceResource(Executors.newCachedThreadPool());
                ServerSocket serverSocket =
                        createServerSocket(cfg.host(), cfg.port())
        ) {
            Runtime.getRuntime().addShutdownHook(createShutdownHook(serverSocket, clientPool));

            log.info("Сервер запущен и ожидает новых подключений...");
            acceptLoop(serverSocket, clientPool.get(), authService, broadcastService);

        }
        catch (Exception e) {
            log.error("Фатальная ошибка при запуске сервера", e);
        }
        finally {
            log.info("Сервер остановлен.");
        }
    }

    private static ServerConfig loadServerConfig() {
        String host = ConfigLoaderService.getString("server.host");
        int port = ConfigLoaderService.getInt("server.port");
        String env = ConfigLoaderService.getString("app.env");

        if (host == null || host.isBlank()) host = "localhost";
        if (port <= 0) port = 8080;
        if (env == null || env.isBlank()) env = "prod";

        return new ServerConfig(host, port, env);
    }

    private static ConnectionFactoryService createConnectionFactoryOrExit() {
        try {
            return ConnectionFactoryService.getInstance();
        }
        catch (RuntimeException e) {
            log.error("Не удалось инициализировать ConnectionFactoryService. Запуск сервера отменен.", e);
            return null;
        }
    }

    private static boolean initDatabase(String env,
                                        ConnectionFactoryService connectionFactory,
                                        AuthService authService,
                                        UserRepository userRepository) {
        try {
            DatabaseInitService initService = new DatabaseInitService(connectionFactory);

            if ("dev".equalsIgnoreCase(env)) {
                initService.setDevUserSeeder(new DevUserSeederService(authService, userRepository));
                initService.setDevMessageSeeder(new DevMessageSeederService(connectionFactory));
            }

            initService.init();
            String mode = ConfigLoaderService.getString("db.init.mode");
            log.info("Инициализация БД завершена (режим={})", mode);
            return true;

        }
        catch (Exception e) {
            log.error("Ошибка инициализации БД. Запуск сервера отменен.", e);
            return false;
        }
    }

    private static ServerSocket createServerSocket(String host, int port) throws IOException {
        InetAddress bindAddr = InetAddress.getByName(host);
        return new ServerSocket(port, 50, bindAddr);
    }

    private static Thread createShutdownHook(ServerSocket serverSocket, ExecutorServiceResource clientPool) {
        return new Thread(() -> {
            log.info("Триггер Shutdown сработал. Остановка сервера...");
            try {
                clientPool.close();
            }
            catch (RuntimeException e) {
                log.warn("Ошибка при остановке пула потоков (игнорируется при shutdown): {}", e.getMessage());
            }

            try {
                serverSocket.close();
            }
            catch (IOException ignored) {
                // Игнорируем shutdown hook который пытается закрыть сокет при завершении процесса.
            }
        }, "server-shutdown-hook");
    }

    private static void acceptLoop(ServerSocket serverSocket,
                                   ExecutorService clientPool,
                                   AuthService authService,
                                   MessageBroadcastService broadcastService) {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                long clientId = CLIENT_SEQ.getAndIncrement();

                log.info("Клиент подключен: id={} remote={}", clientId, clientSocket.getRemoteSocketAddress());

                clientPool.submit(new ConnectionHandler(clientId, clientSocket, authService, broadcastService));

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

    private record ServerConfig(String host, int port, String env) {
    }
}