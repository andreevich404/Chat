package org.example.repository;

import org.example.model.User;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory реализация {@link UserRepository}.
 *
 * <p>Используется преимущественно в модульных тестах и сценариях,
 * где подключение к реальной базе данных не требуется.</p>
 *
 * <p>Особенности реализации:</p>
 * <ul>
 *     <li>Хранение пользователей осуществляется в памяти приложения
 *     с использованием {@link ConcurrentHashMap};</li>
 *     <li>Идентификаторы пользователей генерируются автоматически
 *     с помощью {@link AtomicLong};</li>
 *     <li>Класс является потокобезопасным и может использоваться
 *     в многопоточных тестах;</li>
 *     <li>Не выполняет логирование и не выбрасывает {@link DatabaseException},
 *     так как не взаимодействует с внешними ресурсами.</li>
 * </ul>
 *
 * <p>Данная реализация полностью соответствует контракту {@link UserRepository},
 * но не предназначена для использования в production-среде.</p>
 */
public class InMemoryUserRepository implements UserRepository {

    /**
     * Хранилище пользователей, где ключом является имя пользователя.
     */
    private final Map<String, User> storage = new ConcurrentHashMap<>();

    /**
     * Генератор уникальных идентификаторов пользователей.
     */
    private final AtomicLong idSequence = new AtomicLong(1L);

    /**
     * Ищет пользователя по имени.
     *
     * @param username имя пользователя
     * @return {@link Optional} с найденным пользователем или пустой {@link Optional},
     *         если пользователь не найден
     */
    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(storage.get(username));
    }

    /**
     * Проверяет существование пользователя с указанным именем.
     *
     * @param username имя пользователя
     * @return {@code true}, если пользователь существует, иначе {@code false}
     */
    @Override
    public boolean existsByUsername(String username) {
        return storage.containsKey(username);
    }

    /**
     * Сохраняет пользователя в памяти.
     *
     * <p>Если идентификатор пользователя отсутствует, он будет сгенерирован автоматически.
     * Если дата создания не задана, она будет установлена текущим временем.</p>
     *
     * @param user пользователь для сохранения
     */
    @Override
    public void save(User user) {
        if (user.getId() == null) {
            long id = idSequence.getAndIncrement();
            user.setId(id);
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        storage.put(user.getUsername(), user);
    }
}