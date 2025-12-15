package org.example.repository;

import org.example.model.domain.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий сообщений.
 */
public interface MessageRepository {

    /**
     * Сохраняет сообщение в указанную комнату.
     *
     * @param roomId   id комнаты
     * @param senderId id отправителя
     * @param content  текст сообщения
     * @param sentAt   время отправки (не {@code null})
     * @return id созданного сообщения
     * @throws DatabaseException при ошибке доступа к данным
     */
    long saveMessage(long roomId, long senderId, String content, LocalDateTime sentAt);

    /**
     * Возвращает историю сообщений комнаты.
     *
     * @param roomId id комнаты
     * @param limit  максимальное количество сообщений (>= 1)
     * @return список сообщений (по возрастанию времени)
     * @throws DatabaseException при ошибке доступа к данным
     */
    List<ChatMessage> loadHistory(long roomId, int limit);
}