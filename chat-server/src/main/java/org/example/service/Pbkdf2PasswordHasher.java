package org.example.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;


/**
 * Реализация интерфейса {@link PasswordHasher}, использующая алгоритм PBKDF2 (PBKDF2WithHmacSHA1).
 *
 * <p>Особенности реализации:</p>
 * <ul>
 *     <li>Для каждого пароля генерируется криптографически случайная соль (16 байт).</li>
 *     <li>Количество итераций — 65536, что обеспечивает устойчивость к атакам перебора.</li>
 *     <li>Размер ключа — 256 бит.</li>
 *     <li>Формат сохранения хеша:
 *         <pre>iterations:saltBase64:hashBase64</pre>
 *         Такой формат позволяет хранить всю необходимую информацию в одной строке.
 *     </li>
 * </ul>
 *
 * <p>Класс гарантирует невозможность определения исходного пароля
 * и корректную проверку при последующих авторизациях.</p>
 */
public class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_LENGTH = 16; // bytes

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Генерирует безопасный хеш на основе переданного пароля.
     * Хеш включает количество итераций и соль, что делает формат самодостаточным.
     *
     * @param password пароль в открытом виде
     * @return строка формата "iterations:saltBase64:hashBase64"
     * @throws IllegalArgumentException если пароль равен null
     */
    @Override
    public String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Пароль не может быть null");
        }

        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);

        byte[] hash = pbkdf2(password, salt, ITERATIONS, KEY_LENGTH);

        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hash);

        return ITERATIONS + ":" + saltBase64 + ":" + hashBase64;
    }

    /**
     * Проверяет корректность пароля путём пересчёта PBKDF2 и сравнения с сохранённым хешем.
     *
     * @param password    пароль в открытом виде
     * @param storedHash  строка формата "iterations:saltBase64:hashBase64"
     * @return true — если пароль совпадает, иначе false
     */
    @Override
    public boolean verify(String password, String storedHash) {
        if (password == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        String[] parts = storedHash.split(":");
        if (parts.length != 3) {
            return false;
        }

        int iterations;
        try {
            iterations = Integer.parseInt(parts[0]);
        }
        catch (NumberFormatException e) {
            return false;
        }

        byte[] salt;
        byte[] expectedHash;
        try {
            salt = Base64.getDecoder().decode(parts[1]);
            expectedHash = Base64.getDecoder().decode(parts[2]);
        }
        catch (IllegalArgumentException e) {
            return false;
        }

        byte[] actualHash = pbkdf2(password, salt, iterations, expectedHash.length * 8);

        return slowEquals(expectedHash, actualHash);
    }

    /**
     * Внутренний метод вычисления PBKDF2-хеша.
     *
     * @param password   пароль
     * @param salt       соль
     * @param iterations количество итераций
     * @param keyLength  длина ключа (в битах)
     * @return массив байт хеша
     */
    private byte[] pbkdf2(String password, byte[] salt, int iterations, int keyLength) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Ошибка при хэшировании пароля с помощью PBKDF2", e);
        }
    }

    /**
     * Безопасное сравнение двух массивов байт без короткого замыкания.
     *
     * @param a первый массив
     * @param b второй массив
     * @return true если массивы идентичны
     */
    private boolean slowEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}