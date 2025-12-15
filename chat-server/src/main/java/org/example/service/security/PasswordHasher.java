package org.example.service.security;

/**
 * Интерфейс для безопасного хеширования паролей.
 *
 * <p>Требования:</p>
 * <ul>
 *     <li>хеш должен включать соль и параметры алгоритма (внутри строки);</li>
 *     <li>проверка должна выполняться без утечек по времени;</li>
 *     <li>реализация не должна хранить пароль в открытом виде дольше необходимого.</li>
 * </ul>
 */
public interface PasswordHasher {

    /**
     * Хеширует пароль в формате, пригодном для хранения в БД.
     *
     * @param rawPassword пароль в открытом виде
     * @return строка-хеш (включая соль/параметры)
     * @throws IllegalArgumentException если вход некорректен
     */
    String hash(String rawPassword);

    /**
     * Проверяет, соответствует ли пароль сохранённому хешу.
     *
     * @param rawPassword пароль в открытом виде
     * @param storedHash сохранённый хеш
     * @return true если пароль верный
     * @throws IllegalArgumentException если вход некорректен
     */
    boolean verify(String rawPassword, String storedHash);
}