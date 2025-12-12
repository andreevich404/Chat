package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Обработчик клиентского подключения.
 *
 * <p>Запускается в отдельном потоке/задаче и отвечает за:</p>
 * <ul>
 *     <li>чтение входящих сообщений от клиента;</li>
 *     <li>обработку отключения клиента;</li>
 *     <li>логирование действий и ошибок;</li>
 * </ul>
 *
 */
public class ConnectionHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ConnectionHandler.class);

    private final long clientId;
    private final Socket socket;

    public ConnectionHandler(long clientId, Socket socket) {
        this.clientId = clientId;
        this.socket = socket;
    }

    @Override
    public void run() {
        String remote = String.valueOf(socket.getRemoteSocketAddress());

        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );
                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
                )
        )
        {
            log.info("Обработчик запущен: id={} remote={}", clientId, remote);

            sendLine(out, "ПОДКЛЮЧЕН " + clientId);

            String line;
            while ((line = in.readLine()) != null) {
                String trimmed = line.trim();

                if (trimmed.isEmpty()) {
                    continue;
                }

                log.info("Сообщение от клиента: id={} сообщение={}", clientId, trimmed);

                sendLine(out, "ПОВТОР: " + trimmed);

                if ("quit".equalsIgnoreCase(trimmed) || "exit".equalsIgnoreCase(trimmed)) {
                    log.info("Клиент запросил отключение: id={}", clientId);
                    break;
                }
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

    private void sendLine(BufferedWriter out, String message) throws IOException {
        out.write(message);
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