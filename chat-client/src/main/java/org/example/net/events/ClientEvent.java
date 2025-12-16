package org.example.net.events;

/**
 * Событие сокет-клиента.
 */
public interface ClientEvent {

    /**
     * Возвращает тип события.
     *
     * @return тип события
     */
    ClientEventType type();
}