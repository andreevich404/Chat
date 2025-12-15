package org.example.service;

import org.example.model.protocol.AuthRequest;
import org.example.model.domain.ChatMessage;
import org.example.model.protocol.ServerEvent;
import org.example.service.security.PasswordHasher;
import org.example.repository.InMemoryUserRepository;
import org.example.repository.UserRepository;
import org.example.service.auth.AuthService;
import org.example.service.net.ConnectionHandler;
import org.example.service.net.MessageBroadcastService;
import org.example.service.security.Pbkdf2PasswordHasher;
import org.example.util.JsonUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionHandlerBroadcastTest {

    private final ExecutorService pool = Executors.newCachedThreadPool();

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @Test
    void chatMessageShouldBeBroadcastedToAllExceptSender() throws Exception {
        MessageBroadcastService broadcastService = new MessageBroadcastService();
        UserRepository userRepository = new InMemoryUserRepository();
        PasswordHasher hasher = new Pbkdf2PasswordHasher();
        AuthService authService = new AuthService(userRepository, hasher);

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            Future<?> accept1 = pool.submit(() -> acceptAndRunHandler(serverSocket, 1L, authService, broadcastService));
            Future<?> accept2 = pool.submit(() -> acceptAndRunHandler(serverSocket, 2L, authService, broadcastService));

            try (TestClient clientA = new TestClient("localhost", port);
                 TestClient clientB = new TestClient("localhost", port)) {

                // AUTH
                clientA.sendEvent(ServerEvent.of("AUTH_REQUEST", new AuthRequest("REGISTER", "alice", "123")));
                clientB.sendEvent(ServerEvent.of("AUTH_REQUEST", new AuthRequest("REGISTER", "bob", "123")));

                assertNotNull(clientA.readUntilType("AUTH_RESPONSE", 3000));
                assertNotNull(clientB.readUntilType("AUTH_RESPONSE", 3000));

                // SEND CHAT
                ChatMessage msg = new ChatMessage("General", "ignoredByServer", "hello", LocalDateTime.now());
                clientA.sendEvent(ServerEvent.of("CHAT_MESSAGE", msg));

                // B must receive CHAT_MESSAGE
                ReadResult bRes = clientB.readUntilTypeWithTrace("CHAT_MESSAGE", 3000);
                if (bRes.line == null) {
                    ReadResult aTrace = clientA.readAnyWithTrace(600);
                    fail("B не получил CHAT_MESSAGE. B events=" + bRes.types + " | A events=" + aTrace.types);
                }

                // A must NOT receive CHAT_MESSAGE
                ReadResult aChat = clientA.readUntilTypeWithTrace("CHAT_MESSAGE", 700);
                assertNull(aChat.line, "A не должен получить CHAT_MESSAGE. A events=" + aChat.types);

                // A must NOT receive ERROR for valid message
                ReadResult aErr = clientA.readUntilTypeWithTrace("ERROR", 700);
                assertNull(aErr.line, "A не должен получить ERROR на валидное сообщение. A events=" + aErr.types);
            }

            accept1.get(2, TimeUnit.SECONDS);
            accept2.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void blankContentShouldReturnErrorAndNotBroadcast() throws Exception {
        MessageBroadcastService broadcastService = new MessageBroadcastService();
        UserRepository userRepository = new InMemoryUserRepository();
        PasswordHasher hasher = new Pbkdf2PasswordHasher();
        AuthService authService = new AuthService(userRepository, hasher);

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            Future<?> accept1 = pool.submit(() -> acceptAndRunHandler(serverSocket, 1L, authService, broadcastService));
            Future<?> accept2 = pool.submit(() -> acceptAndRunHandler(serverSocket, 2L, authService, broadcastService));

            try (TestClient clientA = new TestClient("localhost", port);
                 TestClient clientB = new TestClient("localhost", port)) {

                clientA.sendEvent(ServerEvent.of("AUTH_REQUEST", new AuthRequest("REGISTER", "alice", "123")));
                clientB.sendEvent(ServerEvent.of("AUTH_REQUEST", new AuthRequest("REGISTER", "bob", "123")));
                assertNotNull(clientA.readUntilType("AUTH_RESPONSE", 3000));
                assertNotNull(clientB.readUntilType("AUTH_RESPONSE", 3000));

                ChatMessage blank = new ChatMessage("General", "ignored", "   ", LocalDateTime.now());
                clientA.sendEvent(ServerEvent.of("CHAT_MESSAGE", blank));

                ReadResult aErr = clientA.readUntilTypeWithTrace("ERROR", 3000);
                assertNotNull(aErr.line, "Ожидался ERROR. A events=" + aErr.types);

                ReadResult bChat = clientB.readUntilTypeWithTrace("CHAT_MESSAGE", 1000);
                assertNull(bChat.line, "Не должно быть рассылки. B events=" + bChat.types);
            }

            accept1.get(2, TimeUnit.SECONDS);
            accept2.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void tooLongContentShouldReturnErrorAndNotBroadcast() throws Exception {
        MessageBroadcastService broadcastService = new MessageBroadcastService();
        UserRepository userRepository = new InMemoryUserRepository();
        PasswordHasher hasher = new Pbkdf2PasswordHasher();
        AuthService authService = new AuthService(userRepository, hasher);

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            Future<?> accept1 = pool.submit(() -> acceptAndRunHandler(serverSocket, 1L, authService, broadcastService));
            Future<?> accept2 = pool.submit(() -> acceptAndRunHandler(serverSocket, 2L, authService, broadcastService));

            try (TestClient clientA = new TestClient("localhost", port);
                 TestClient clientB = new TestClient("localhost", port)) {

                clientA.sendEvent(ServerEvent.of("AUTH_REQUEST", new AuthRequest("REGISTER", "alice", "123")));
                clientB.sendEvent(ServerEvent.of("AUTH_REQUEST", new AuthRequest("REGISTER", "bob", "123")));
                assertNotNull(clientA.readUntilType("AUTH_RESPONSE", 3000));
                assertNotNull(clientB.readUntilType("AUTH_RESPONSE", 3000));

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 1001; i++) sb.append('a');

                ChatMessage tooLong = new ChatMessage("General", "ignored", sb.toString(), LocalDateTime.now());
                clientA.sendEvent(ServerEvent.of("CHAT_MESSAGE", tooLong));

                ReadResult aErr = clientA.readUntilTypeWithTrace("ERROR", 3000);
                assertNotNull(aErr.line, "Ожидался ERROR. A events=" + aErr.types);

                ReadResult bChat = clientB.readUntilTypeWithTrace("CHAT_MESSAGE", 1000);
                assertNull(bChat.line, "Не должно быть рассылки. B events=" + bChat.types);
            }

            accept1.get(2, TimeUnit.SECONDS);
            accept2.get(2, TimeUnit.SECONDS);
        }
    }

    private void acceptAndRunHandler(ServerSocket serverSocket,
                                     long clientId,
                                     AuthService authService,
                                     MessageBroadcastService broadcastService) {
        try {
            Socket socket = serverSocket.accept();
            ConnectionHandler handler = new ConnectionHandler(clientId, socket, authService, broadcastService);
            handler.run();
        } catch (IOException ignored) {
        }
    }

    private static final class ReadResult {
        final String line;
        final List<String> types;

        private ReadResult(String line, List<String> types) {
            this.line = line;
            this.types = types;
        }
    }

    private static final class TestClient implements Closeable {
        private final Socket socket;
        private final BufferedReader in;
        private final BufferedWriter out;

        private TestClient(String host, int port) throws IOException {
            this.socket = new Socket(host, port);
            this.socket.setSoTimeout(1200);

            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        void sendEvent(ServerEvent event) throws IOException {
            out.write(JsonUtil.toJson(event));
            out.newLine();
            out.flush();
        }

        String readUntilType(String expectedType, long timeoutMs) throws IOException {
            return readUntilTypeWithTrace(expectedType, timeoutMs).line;
        }

        ReadResult readAnyWithTrace(long timeoutMs) throws IOException {
            long deadline = System.currentTimeMillis() + timeoutMs;
            List<String> types = new ArrayList<>();

            while (System.currentTimeMillis() < deadline) {
                String line = tryReadLineOnce();
                if (line == null) continue;

                types.add(extractTypeSafe(line));
            }
            return new ReadResult(null, types);
        }

        ReadResult readUntilTypeWithTrace(String expectedType, long timeoutMs) throws IOException {
            long deadline = System.currentTimeMillis() + timeoutMs;
            List<String> types = new ArrayList<>();

            while (System.currentTimeMillis() < deadline) {
                String line = tryReadLineOnce();
                if (line == null) continue;

                String type = extractTypeSafe(line);
                types.add(type);

                if (expectedType.equals(type)) {
                    return new ReadResult(line, types);
                }
            }
            return new ReadResult(null, types);
        }

        private String extractTypeSafe(String jsonLine) {
            try {
                ServerEvent evt = JsonUtil.fromJson(jsonLine, ServerEvent.class);
                if (evt == null || evt.getType() == null) return "NULL_TYPE";
                return evt.getType();
            } catch (RuntimeException e) {
                return "UNPARSEABLE";
            }
        }

        private String tryReadLineOnce() throws IOException {
            try {
                return in.readLine();
            } catch (java.net.SocketTimeoutException e) {
                return null;
            }
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}