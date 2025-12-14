package org.example.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Доменная модель пользователя системы.
 *
 * <p>Класс представляет сущность пользователя, которая используется
 * во всех слоях приложения: сервисном, репозиторном и протокольном.</p>
 *
 * <p>Объект {@code User} хранит только данные и не содержит бизнес-логики.</p>
 *
 * <p>Пароль хранится исключительно в виде хэша
 * (в открытом виде пароль в системе не сохраняется).</p>
 */
public class User {

    /**
     * Уникальный идентификатор пользователя.
     * Может быть {@code null} до сохранения в базе данных.
     */
    private Long id;

    /**
     * Уникальное имя пользователя.
     */
    private String username;

    /**
     * Хэш пароля пользователя.
     */
    private String passwordHash;

    /**
     * Дата и время создания пользователя.
     */
    private LocalDateTime createdAt;

    /**
     * Создаёт объект пользователя со всеми полями.
     *
     * @param id           идентификатор пользователя
     * @param username     имя пользователя (не может быть {@code null})
     * @param passwordHash хэш пароля (не может быть {@code null})
     * @param createdAt    дата и время создания
     * @throws NullPointerException если {@code username} или {@code passwordHash} равны {@code null}
     */
    public User(Long id, String username, String passwordHash, LocalDateTime createdAt) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "имя пользователя не должно быть null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "хэш пароля не должен быть null");
        this.createdAt = createdAt;
    }

    /**
     * Создаёт нового пользователя без идентификатора и даты создания.
     *
     * <p>Используется перед сохранением пользователя в базе данных.</p>
     *
     * @param username     имя пользователя
     * @param passwordHash хэш пароля
     */
    public User(String username, String passwordHash) {
        this(null, username, passwordHash, null);
    }

    /**
     * Возвращает идентификатор пользователя.
     *
     * @return идентификатор или {@code null}, если пользователь ещё не сохранён
     */
    public Long getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор пользователя.
     *
     * @param id идентификатор пользователя
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Возвращает имя пользователя.
     *
     * @return имя пользователя
     */
    public String getUsername() {
        return username;
    }

    /**
     * Возвращает хэш пароля пользователя.
     *
     * @return хэш пароля
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Возвращает дату и время создания пользователя.
     *
     * @return дата и время создания или {@code null}, если не установлено
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Устанавливает дату и время создания пользователя.
     *
     * @param createdAt дата и время создания
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}