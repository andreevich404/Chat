package org.example.repository;

import org.example.model.domain.User;

import java.util.Optional;

/**
 * Repository для управления пользователями.
 */
public interface UserRepository {

    /**
     * Возвращает пользователя по имени.
     *
     * @param username имя пользователя (ключ поиска)
     * @return найденный пользователь или {@link Optional#empty()}
     * @throws DatabaseException при ошибке доступа к данным
     */
    Optional<User> findByUsername(String username);

    /**
     * Проверяет существование пользователя по имени.
     *
     * @param username имя пользователя (ключ поиска)
     * @return {@code true}, если пользователь существует
     * @throws DatabaseException при ошибке доступа к данным
     */
    boolean existsByUsername(String username);

    /**
     * Сохраняет пользователя.
     *
     * @param user пользователь
     * @throws DatabaseException при ошибке доступа к данным
     */
    void save(User user);
}