package org.example.repository;

import java.util.Optional;

/**
 * Репозиторий комнат чата.
 */
public interface ChatRoomRepository {

    /**
     * Возвращает идентификатор комнаты типа {@code ROOM} по имени.
     *
     * @param roomName имя комнаты
     * @return идентификатор комнаты или {@link Optional#empty()}, если не найдена
     * @throws DatabaseException при ошибке доступа к данным
     */
    Optional<Long> findRoomIdByName(String roomName);

    /**
     * Создаёт комнату типа {@code ROOM}.
     *
     * <p>Если комната уже существует, реализация может вернуть существующий идентификатор.</p>
     *
     * @param roomName имя комнаты
     * @return идентификатор созданной (или существующей) комнаты
     * @throws DatabaseException при ошибке доступа к данным
     */
    long createRoom(String roomName);

    /**
     * Создаёт комнату типа {@code DM}.
     *
     * <p>Имя комнаты техническое и не предназначено для отображения.</p>
     *
     * @return идентификатор созданной комнаты
     * @throws DatabaseException при ошибке доступа к данным
     */
    long createDirectRoom();
}