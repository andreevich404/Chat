package org.example.service;

import org.example.model.ApiResponse;
import org.example.model.AuthRequest;
import org.example.model.AuthResponse;
import org.example.model.ServerEvent;
import org.example.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Обработчик клиентского подключения.
 *
 * <p>Ожидает единый JSON-формат:</p>
 * <pre>
 * { "type": "AUTH_REQUEST", "data": { "action": "LOGIN|REGISTER", "username": "...", "password": "..." } }
 * </pre>
 *
 * <p>Возвращает единый JSON-формат:</p>
 * <pre>
 * { "type": "AUTH_RESPONSE", "data": { ... } }
 * { "type": "ERROR", "data": { "code": "...", "message": "..." } }
 * </pre>
 */
public class ConnectionHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ConnectionHandler.class);

    private final long clientId;
    private final Socket socket;
    private final AuthService authService;

    public ConnectionHandler(long clientId, Socket socket, AuthService authService) {
        this.clientId = clientId;
        this.socket = socket;
        this.authService = authService;
    }

    @Override
    public void run() {
        String remote = String.valueOf(socket.getRemoteSocketAddress());

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
        ) {
            log.info("Обработчик запущен: id={} remote={}", clientId, remote);

            String line;
            while ((line = in.readLine()) != null) {
                String json = line.trim();
                if (json.isEmpty()) {
                    continue;
                }

                handleIncoming(json, out);
            }

            log.info("Клиент отключился нормально: id={} remote={}", clientId, remote);
        }
        catch (IOException e) {
            log.warn("Ошибка соединения клиента: id={} remote={} сообщение={}", clientId, remote, e.getMessage());
            log.debug("Детали ошибки соединения клиента: id={} remote={}", clientId, remote, e);
        }
        catch (RuntimeException e) {
            log.error("Неожиданная ошибка обработчика: id={} remote={}", clientId, remote, e);
        }
        finally {
            closeQuietly();
            log.info("Обработчик остановлен: id={} remote={}", clientId, remote);
        }
    }

    private void handleIncoming(String json, BufferedWriter out) throws IOException {

        final ServerEvent event;
        try {
            event = JsonUtil.fromJson(json, ServerEvent.class);
        }
        catch (RuntimeException e) {
            log.warn("Неверный JSON от клиента id={} msg={}", clientId, e.getMessage());
            send(out, ServerEvent.error("INVALID_JSON", "Неверный JSON"));
            return;
        }

        if (event == null || event.getType() == null || event.getType().isBlank()) {
            send(out, ServerEvent.error("INVALID_REQUEST", "Отсутствует поле type"));
            return;
        }

        switch (event.getType()) {
            case "AUTH_REQUEST" -> handleAuthRequest(event, out);
            default -> send(out, ServerEvent.error("UNKNOWN_TYPE", "Неизвестный тип сообщения: " + event.getType()));
        }
    }

    private void handleAuthRequest(ServerEvent event, BufferedWriter out) throws IOException {

        final AuthRequest req;
        try {
            String dataJson = JsonUtil.toJson(event.getData());
            req = JsonUtil.fromJson(dataJson, AuthRequest.class);
        }
        catch (RuntimeException e) {
            send(out, ServerEvent.error("INVALID_REQUEST", "Поле data имеет неверный формат"));
            return;
        }

        if (req == null) {
            send(out, ServerEvent.error("INVALID_REQUEST", "Поле data обязательно"));
            return;
        }

        String action = req.getAction();
        if (action == null || action.isBlank()) {
            send(out, ServerEvent.error("VALIDATION_ERROR", "Поле action обязательно (LOGIN|REGISTER)"));
            return;
        }

        ApiResponse<AuthResponse> response;
        switch (action.toUpperCase()) {
            case "REGISTER" -> response = authService.register(req.getUsername(), req.getPassword());
            case "LOGIN" -> response = authService.login(req.getUsername(), req.getPassword());
            default -> {
                send(out, ServerEvent.error("UNKNOWN_ACTION", "Неизвестное значение action: " + action));
                return;
            }
        }

        if (response.isSuccess()) {
            send(out, ServerEvent.of("AUTH_RESPONSE", response.getData()));
        }
        else {
            send(out, ServerEvent.of("ERROR", response.getError()));
        }
    }

    private void send(BufferedWriter out, ServerEvent event) throws IOException {
        out.write(JsonUtil.toJson(event));
        out.newLine();
        out.flush();
    }

    private void closeQuietly() {
        try {
            socket.close();
        }
        catch (IOException ignored) {
        }
    }
}