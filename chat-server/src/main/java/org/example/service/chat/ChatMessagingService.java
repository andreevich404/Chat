package org.example.service.chat;

import org.example.model.protocol.ChatMessageDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Use-case сервис сообщений чата.
 */
public interface ChatMessagingService {

    /**
     * Сохраняет сообщение в ROOM.
     *
     * @param room     имя комнаты (если пустое — используется значение по умолчанию реализацией)
     * @param fromUser имя отправителя
     * @param content  текст сообщения
     * @param sentAt   время отправки (если {@code null} — используется текущее)
     */
    void postToRoom(String room, String fromUser, String content, LocalDateTime sentAt);

    /**
     * Сохраняет личное сообщение (DM) и гарантирует наличие DM комнаты.
     *
     * @param fromUser имя отправителя
     * @param toUser   имя получателя
     * @param content  текст сообщения
     * @param sentAt   время отправки (если {@code null} — используется текущее)
     */
    void postDirect(String fromUser, String toUser, String content, LocalDateTime sentAt);

    /**
     * Возвращает историю ROOM.
     *
     * @param room  имя комнаты
     * @param limit лимит (>= 1)
     * @return история в виде DTO
     */
    List<ChatMessageDto> getRoomHistory(String room, int limit);

    /**
     * Возвращает историю DM.
     *
     * @param userA первый пользователь
     * @param userB второй пользователь
     * @param limit лимит (>= 1)
     * @return история в виде DTO
     */
    List<ChatMessageDto> getDirectHistory(String userA, String userB, int limit);
}