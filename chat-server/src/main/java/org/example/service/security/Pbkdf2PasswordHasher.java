package org.example.service.security;

import org.example.model.PasswordHasher;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * Реализация {@link PasswordHasher} на основе PBKDF2.
 *
 * <p>Формат хеша:</p>
 * <pre>
 * pbkdf2$&lt;iterations&gt;$&lt;saltBase64&gt;$&lt;hashBase64&gt;
 * </pre>
 */
public class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final String PREFIX = "pbkdf2";
    private static final String DELIM = "\\$";

    // параметры по умолчанию (можно позже вынести в конфиг)
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;

    private final SecureRandom random;

    /**
     * Создаёт хешер с SecureRandom по умолчанию.
     */
    public Pbkdf2PasswordHasher() {
        this(new SecureRandom());
    }

    /**
     * Создаёт хешер с заданным генератором случайных чисел (удобно для тестов).
     *
     * @param random источник случайности
     */
    public Pbkdf2PasswordHasher(SecureRandom random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    @Override
    public String hash(String rawPassword) {
        String pwd = requireNonBlank(rawPassword, "rawPassword");

        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);

        byte[] dk = pbkdf2(pwd.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(dk);

        return PREFIX + "$" + ITERATIONS + "$" + saltB64 + "$" + hashB64;
    }

    @Override
    public boolean verify(String rawPassword, String storedHash) {
        String pwd = requireNonBlank(rawPassword, "rawPassword");
        String hash = requireNonBlank(storedHash, "storedHash");

        Parsed parsed = parseStoredHash(hash);

        byte[] actual = pbkdf2(pwd.toCharArray(), parsed.salt(), parsed.iterations(), parsed.keyLengthBits());
        return MessageDigest.isEqual(actual, parsed.expected());
    }

    private Parsed parseStoredHash(String storedHash) {
        String[] parts = storedHash.split(DELIM);
        // ожидаем 4 части: prefix, iterations, salt, hash
        if (parts.length != 4) {
            throw new IllegalArgumentException("Неверный формат хеша пароля");
        }
        if (!PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException("Неподдерживаемый формат хеша: " + parts[0]);
        }

        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Неверное значение iterations в хеше", e);
        }
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations должен быть > 0");
        }

        byte[] salt;
        byte[] expected;
        try {
            salt = Base64.getDecoder().decode(parts[2]);
            expected = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Неверный Base64 в хеше", e);
        }

        // длину ключа выводим из длины expected (в байтах)
        int keyLengthBits = expected.length * 8;
        if (keyLengthBits <= 0) {
            throw new IllegalArgumentException("Неверная длина ключа в хеше");
        }

        return new Parsed(iterations, salt, expected, keyLengthBits);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка PBKDF2", e);
        }
    }

    private static String requireNonBlank(String s, String name) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException(name + " не должен быть пустым");
        }
        return s;
    }

    /**
     * Результат парсинга сохранённого хеша.
     */
    private record Parsed(int iterations, byte[] salt, byte[] expected, int keyLengthBits) {
    }
}