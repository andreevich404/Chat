package org.example.repository;

import org.example.model.domain.User;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory реализация {@link UserRepository}.
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);
    private final Clock clock;

    /**
     * Создаёт репозиторий с системным временем.
     */
    public InMemoryUserRepository() {
        this(Clock.systemDefaultZone());
    }

    /**
     * Создаёт репозиторий с подменяемым источником времени.
     *
     * @param clock источник времени
     */
    public InMemoryUserRepository(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String key = normalizeLookupKey(username);
        if (key.isEmpty()) return Optional.empty();
        return Optional.ofNullable(usersByUsername.get(key));
    }

    @Override
    public boolean existsByUsername(String username) {
        String key = normalizeLookupKey(username);
        return !key.isEmpty() && usersByUsername.containsKey(key);
    }

    @Override
    public void save(User user) {
        Objects.requireNonNull(user, "user");

        String username = safeTrim(user.getUsername());
        if (username.isEmpty()) {
            throw new IllegalArgumentException("username не должен быть пустым");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new IllegalArgumentException("passwordHash не должен быть пустым");
        }

        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now(clock));
        }
        if (user.getId() == null) {
            user.setId(seq.getAndIncrement());
        }

        usersByUsername.put(normalizeLookupKey(username), user);
    }

    private static String normalizeLookupKey(String username) {
        return safeTrim(username).toLowerCase(Locale.ROOT);
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}