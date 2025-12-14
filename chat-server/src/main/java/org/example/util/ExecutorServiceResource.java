package org.example.util;

import java.util.concurrent.ExecutorService;

/**
 * Обёртка над ExecutorService для корректного закрытия
 * через try-with-resources (требование Sonar).
 */
public final class ExecutorServiceResource implements AutoCloseable {

    private final ExecutorService executor;

    public ExecutorServiceResource(ExecutorService executor) {
        this.executor = executor;
    }

    public ExecutorService get() {
        return executor;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}