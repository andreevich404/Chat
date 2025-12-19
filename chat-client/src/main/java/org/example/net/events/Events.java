package org.example.net.events;

import org.example.model.ChatHistoryResponse;
import org.example.model.ChatMessageDto;

import java.util.List;

/**
 * Набор доменных событий клиента, получаемых по сети.
 *
 * <p>События предназначены для маршрутизации в UI-слое без прямой зависимости
 * от реализации сокета.</p>
 */
public final class Events {

    private Events() {
    }

    /**
     * Базовый тип сетевых событий клиента.
     */
    public sealed interface Event permits
            Connected,
            Disconnected,
            UsersSnapshot,
            UserJoined,
            UserLeft,
            History,
            Message,
            Error {
    }

    /**
     * Событие успешного подключения к серверу.
     */
    public record Connected() implements Event {
    }

    /**
     * Событие разрыва соединения с сервером.
     */
    public record Disconnected() implements Event {
    }

    /**
     * Снимок списка пользователей онлайн.
     *
     * @param users список пользователей
     */
    public record UsersSnapshot(List<String> users) implements Event {
        public UsersSnapshot {
            users = (users == null) ? List.of() : List.copyOf(users);
        }
    }

    /**
     * Пользователь вошёл в сеть.
     *
     * @param username    имя пользователя
     * @param onlineCount количество пользователей онлайн (если неизвестно — -1)
     */
    public record UserJoined(String username, int onlineCount) implements Event {
    }

    /**
     * Пользователь вышел из сети.
     *
     * @param username    имя пользователя
     * @param onlineCount количество пользователей онлайн (если неизвестно — -1)
     */
    public record UserLeft(String username, int onlineCount) implements Event {
    }

    /**
     * Ответ с историей сообщений.
     *
     * @param response полезная нагрузка
     */
    public record History(ChatHistoryResponse response) implements Event {
    }

    /**
     * Входящее сообщение (ROOM или DM).
     *
     * @param message полезная нагрузка
     */
    public record Message(ChatMessageDto message) implements Event {
    }

    /**
     * Ошибка уровня сети/протокола.
     *
     * @param userMessage сообщение для UI
     * @param cause       причина (может быть null)
     */
    public record Error(String userMessage, Throwable cause) implements Event {
        public Error {
            userMessage = (userMessage == null) ? "" : userMessage;
        }
    }
}