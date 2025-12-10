package org.example.repository;

import org.example.model.User;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> storage = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1L);

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(storage.get(username));
    }

    @Override
    public boolean existsByUsername(String username) {
        return storage.containsKey(username);
    }

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