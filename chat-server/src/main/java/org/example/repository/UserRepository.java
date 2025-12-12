package org.example.repository;

import org.example.model.User;
import java.util.Optional;

/**
 * Интерфейс репозитория для работы с сущностью {@link User}.
 *
 * <p>Определяет контракт слоя доступа к данным (Repository Layer)
 * для выполнения базовых CRUD-операций над пользователями.</p>
 *
 * <p>Реализации интерфейса могут использовать различные источники данных:</p>
 * <ul>
 *     <li>реляционную базу данных (JDBC-реализация);</li>
 *     <li>in-memory хранилище (для тестирования);</li>
 *     <li>другие источники при необходимости.</li>
 * </ul>
 *
 * <p>Все ошибки, связанные с доступом к данным, должны быть инкапсулированы
 * в {@link DatabaseException} и выбрасываться как unchecked-исключения.</p>
 */
public interface UserRepository {

    /**
     * Ищет пользователя по имени.
     *
     * @param username имя пользователя
     * @return {@link Optional} с найденным пользователем или пустой {@link Optional},
     *         если пользователь не найден
     * @throws DatabaseException при ошибке доступа к данным
     */
    Optional<User> findByUsername(String username);

    /**
     * Проверяет существование пользователя по имени.
     *
     * @param username имя пользователя
     * @return {@code true}, если пользователь существует, иначе {@code false}
     * @throws DatabaseException при ошибке доступа к данным
     */
    boolean existsByUsername(String username);

    /**
     * Сохраняет пользователя.
     *
     * <p>Поведение метода зависит от реализации:</p>
     * <ul>
     *     <li>если пользователь новый — выполняется добавление;</li>
     *     <li>если пользователь уже существует — выполняется обновление.</li>
     * </ul>
     *
     * @param user пользователь для сохранения
     * @throws DatabaseException при ошибке доступа к данным
     */
    void save(User user) throws DatabaseException;
}