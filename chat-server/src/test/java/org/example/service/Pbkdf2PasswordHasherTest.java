package org.example.service;

import org.example.service.security.PasswordHasher;
import org.example.service.security.Pbkdf2PasswordHasher;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/**
 * Набор модульных тестов для проверки корректности реализации
 * {@link Pbkdf2PasswordHasher}, обеспечивающей безопасное хеширование паролей.
 *
 * <p>Тесты проверяют:</p>
 * <ul>
 *     <li>корректность формата хеша;</li>
 *     <li>отличие хеша от исходного пароля;</li>
 *     <li>успешную проверку корректного пароля;</li>
 *     <li>неуспешную проверку при неверном пароле;</li>
 *     <li>наличие соли: одинаковый пароль должен давать разные хеши;</li>
 * </ul>
 *
 * <p>Тесты не проверяют производительность, но гарантируют корректность
 * криптографической логики PBKDF2 в рамках функционального поведения.</p>
 */
class Pbkdf2PasswordHasherTest {
    private final PasswordHasher hasher = new Pbkdf2PasswordHasher();

    /**
     * Проверяет, что результат хеширования:
     * <ul>
     *     <li>не равен исходному паролю;</li>
     *     <li>имеет ожидаемый формат: ITERATIONS:salt:hash;</li>
     * </ul>
     */
    @Test
    void hashShouldNotReturnPlainPassword() {
        String password = "mySecretPassword123";
        String hash = hasher.hash(password);

        assertNotNull(hash);
        assertNotEquals(password, hash);
        // Проверим формат: iterations:salt:hash
        String[] parts = hash.split(":");
        assertEquals(3, parts.length);
    }

    /**
     * Проверяет, что метод {@code verify} корректно подтверждает
     * соответствие пароля ранее сгенерированному хешу.
     */
    @Test
    void verifyShouldReturnTrueForCorrectPassword() {
        String password = "mySecretPassword123";
        String hash = hasher.hash(password);

        assertTrue(hasher.verify(password, hash));
    }

    /**
     * Проверяет, что метод {@code verify} возвращает false
     * при попытке проверить неверный пароль.
     */
    @Test
    void verifyShouldReturnFalseForIncorrectPassword() {
        String password = "mySecretPassword123";
        String otherPassword = "wrongPassword";
        String hash = hasher.hash(password);

        assertFalse(hasher.verify(otherPassword, hash));
    }

    /**
     * Проверяет, что хеширование одного и того же пароля
     * дважды возвращает разные значения.
     *
     * <p>Это гарантирует использование случайной соли и устойчивость
     * к атакам типа rainbow tables.</p>
     */
    @Test
    void hashShouldBeDifferentForSamePasswordBecauseOfSalt() {
        String password = "samePassword";

        String hash1 = hasher.hash(password);
        String hash2 = hasher.hash(password);

        assertNotEquals(hash1, hash2);

        assertTrue(hasher.verify(password, hash1));
        assertTrue(hasher.verify(password, hash2));
    }
}