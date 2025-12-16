package org.example.repository;

import java.util.Optional;

/**
 * Репозиторий связи DM-пары пользователей с комнатой.
 */
public interface DirectChatRepository {

    /**
     * Возвращает идентификатор {@code chat_room} для пары пользователей.
     *
     * @param userAId id пользователя A
     * @param userBId id пользователя B
     * @return id комнаты или {@link Optional#empty()}, если пары нет
     * @throws DatabaseException при ошибке доступа к данным
     */
    Optional<Long> findDmRoomId(long userAId, long userBId);

    /**
     * Создаёт связь пары пользователей с уже созданной DM-комнатой.
     *
     * <p>Порядок пары определяется как (min, max).</p>
     *
     * @param userAId     id пользователя A
     * @param userBId     id пользователя B
     * @param chatRoomId  id комнаты типа {@code DM}
     * @return id комнаты, фактически связанной с парой
     * @throws DatabaseException при ошибке доступа к данным
     */
    long createDm(long userAId, long userBId, long chatRoomId);
}