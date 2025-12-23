package org.example;

import org.example.repository.jdbc.JdbcChatRoomRepository;
import org.example.repository.jdbc.JdbcDirectChatRepository;
import org.example.repository.jdbc.JdbcMessageRepository;
import org.example.repository.jdbc.JdbcUserRepository;
import org.example.service.security.PasswordHasher;
import org.example.repository.*;
import org.example.service.auth.AuthService;
import org.example.service.chat.ChatMessagingService;
import org.example.service.chat.DefaultChatMessagingService;
import org.example.service.db.ConnectionFactoryService;
import org.example.service.db.DatabaseInitService;
import org.example.service.db.ConfigLoaderService;
import org.example.service.net.ConnectionHandler;
import org.example.service.net.MessageBroadcastService;
import org.example.service.security.Pbkdf2PasswordHasher;
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
 * Точка входа серверного приложения (composition root).
 *
 * <p>Отвечает за:</p>
 * <ul>
 *     <li>загрузку конфигурации;</li>
 *     <li>инициализацию БД (опционально);</li>
 *     <li>создание зависимостей (репозитории / сервисы);</li>
 *     <li>поднятие {@link ServerSocket} и приём клиентов.</li>
 * </ul>
 */
public class ChatServer {

    private static final Logger log = LoggerFactory.getLogger(ChatServer.class);
    private static final AtomicLong CLIENT_SEQ = new AtomicLong(1);

    public static void main(String[] args) {
        ServerConfig cfg = loadServerConfig();
        log.info("Запуск сервера чата... окружение={} адрес={}:{}", cfg.env(), cfg.host(), cfg.port());

        ConnectionFactoryService connectionFactory = createConnectionFactoryOrExit();
        if (connectionFactory == null) return;

        if (!connectionFactory.testConnection()) {
            log.error("Тест соединения с БД не удался. Запуск сервера отменен.");
            return;
        }
        log.info("Тест соединения с БД успешен");

        if (!initDatabase(connectionFactory)) {
            return;
        }

        UserRepository userRepository = new JdbcUserRepository(connectionFactory);
        ChatRoomRepository chatRoomRepository = new JdbcChatRoomRepository(connectionFactory);
        DirectChatRepository directChatRepository = new JdbcDirectChatRepository(connectionFactory);
        MessageRepository messageRepository = new JdbcMessageRepository(connectionFactory);

        PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();
        AuthService authService = new AuthService(userRepository, passwordHasher);

        ChatMessagingService chatService = new DefaultChatMessagingService(
                userRepository, chatRoomRepository, directChatRepository, messageRepository
        );

        MessageBroadcastService broadcastService = new MessageBroadcastService();

        try (ExecutorServiceResource clientPool = new ExecutorServiceResource(Executors.newCachedThreadPool());
             ServerSocket serverSocket = createServerSocket(cfg.host(), cfg.port())) {

            Runtime.getRuntime().addShutdownHook(createShutdownHook(serverSocket, clientPool));

            log.info("Сервер запущен и ожидает новых подключений...");
            acceptLoop(serverSocket, clientPool.get(), authService, chatService, broadcastService);

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

    private static boolean initDatabase(ConnectionFactoryService connectionFactory) {
        try {
            new DatabaseInitService(connectionFactory).init();
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
            log.info("ShutdownHook: остановка сервера...");
            try {
                clientPool.close();
            }
            catch (RuntimeException e) {
                log.warn("Ошибка при остановке пула потоков (игнорируется): {}", e.getMessage());
            }

            try {
                serverSocket.close();
            }
            catch (IOException ignored) {
            }
        }, "server-shutdown-hook");
    }

    private static void acceptLoop(ServerSocket serverSocket,
                                   ExecutorService clientPool,
                                   AuthService authService,
                                   ChatMessagingService chatService,
                                   MessageBroadcastService broadcastService) {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                long clientId = CLIENT_SEQ.getAndIncrement();

                log.info("Клиент подключен: id={} remote={}", clientId, clientSocket.getRemoteSocketAddress());

                clientPool.submit(new ConnectionHandler(
                        clientId, clientSocket, authService, chatService, broadcastService
                ));

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