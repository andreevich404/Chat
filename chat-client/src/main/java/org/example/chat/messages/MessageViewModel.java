package org.example.chat.messages;

import org.example.model.ChatMessageDto;

import java.time.format.DateTimeFormatter;

/**
 * Форматирование сообщений для UI.
 */
public final class MessageViewModel {

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("HH:mm");

    private final String currentUser;

    public MessageViewModel(String currentUser) {
        this.currentUser = safeTrim(currentUser);
    }

    public String format(ChatMessageDto msg) {
        String time = msg.getSentAt() == null
                ? ""
                : "[" + TS.format(msg.getSentAt()) + "] ";

        boolean fromMe = msg.getFrom() != null &&
                         msg.getFrom().equalsIgnoreCase(currentUser);

        String from = fromMe ? "Вы" : safeTrim(msg.getFrom());

        if (msg.getTo() == null || msg.getTo().isBlank()) {
            return time + from + ": " + safeTrim(msg.getContent());
        }

        String to = msg.getTo().equalsIgnoreCase(currentUser)
                ? "Вы"
                : safeTrim(msg.getTo());

        return time + from + " → " + to + ": " + safeTrim(msg.getContent());
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}