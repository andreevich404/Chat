package org.example.net.events;

/**
 * Подписчик на события сокет-клиента.
 */
@FunctionalInterface
public interface ClientEventListener {

    /**
     * Обрабатывает событие.
     *
     * @param event событие
     */
    void onEvent(ClientEvent event);
}