package org.example.service.chat;

import org.example.model.domain.ChatMessage;
import org.example.model.domain.User;
import org.example.model.protocol.ChatMessageDto;
import org.example.repository.ChatRoomRepository;
import org.example.repository.DatabaseException;
import org.example.repository.DirectChatRepository;
import org.example.repository.MessageRepository;
import org.example.repository.UserRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Реализация {@link ChatMessagingService} поверх репозиториев.
 */
public class DefaultChatMessagingService implements ChatMessagingService {

    private static final String DEFAULT_ROOM = "General";

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final DirectChatRepository directChatRepository;
    private final MessageRepository messageRepository;
    private final Clock clock;

    public DefaultChatMessagingService(UserRepository userRepository,
                                       ChatRoomRepository chatRoomRepository,
                                       DirectChatRepository directChatRepository,
                                       MessageRepository messageRepository) {
        this(userRepository, chatRoomRepository, directChatRepository, messageRepository, Clock.systemDefaultZone());
    }

    public DefaultChatMessagingService(UserRepository userRepository,
                                       ChatRoomRepository chatRoomRepository,
                                       DirectChatRepository directChatRepository,
                                       MessageRepository messageRepository,
                                       Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.chatRoomRepository = Objects.requireNonNull(chatRoomRepository, "chatRoomRepository");
        this.directChatRepository = Objects.requireNonNull(directChatRepository, "directChatRepository");
        this.messageRepository = Objects.requireNonNull(messageRepository, "messageRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void postToRoom(String room, String fromUser, String content, LocalDateTime sentAt) {
        String roomName = normalizeRoom(room);

        long roomId = ensurePublicRoom(roomName);
        long senderId = requireUserId(fromUser);

        messageRepository.saveMessage(roomId, senderId, content, normalizeSentAt(sentAt));
    }

    @Override
    public void postDirect(String fromUser, String toUser, String content, LocalDateTime sentAt) {
        long fromId = requireUserId(fromUser);
        long toId = requireUserId(toUser);

        long dmRoomId = ensureDirectRoom(fromId, toId);

        messageRepository.saveMessage(dmRoomId, fromId, content, normalizeSentAt(sentAt));
    }

    @Override
    public List<ChatMessageDto> getRoomHistory(String room, int limit) {
        String roomName = normalizeRoom(room);
        long roomId = ensurePublicRoom(roomName);

        List<ChatMessage> raw = messageRepository.loadHistory(roomId, Math.max(1, limit));
        List<ChatMessageDto> out = new ArrayList<>(raw.size());

        for (ChatMessage m : raw) {
            out.add(new ChatMessageDto(roomName, m.getFrom(), null, m.getContent(), m.getSentAt()));
        }
        return out;
    }

    @Override
    public List<ChatMessageDto> getDirectHistory(String userA, String userB, int limit) {
        long aId = requireUserId(userA);
        long bId = requireUserId(userB);

        Optional<Long> dmRoom = directChatRepository.findDmRoomId(aId, bId);
        if (dmRoom.isEmpty()) return List.of();

        List<ChatMessage> raw = messageRepository.loadHistory(dmRoom.get(), Math.max(1, limit));
        List<ChatMessageDto> out = new ArrayList<>(raw.size());

        for (ChatMessage m : raw) {
            String from = m.getFrom();
            String to = from != null && from.equalsIgnoreCase(userA) ? userB : userA;
            out.add(new ChatMessageDto(null, from, to, m.getContent(), m.getSentAt()));
        }
        return out;
    }

    private long ensurePublicRoom(String roomName) {
        return chatRoomRepository.createRoom(roomName);
    }

    private long ensureDirectRoom(long userAId, long userBId) {
        Optional<Long> existing = directChatRepository.findDmRoomId(userAId, userBId);
        if (existing.isPresent()) return existing.get();

        long chatRoomId = chatRoomRepository.createDirectRoom();

        return directChatRepository.createDm(userAId, userBId, chatRoomId);
    }

    private long requireUserId(String username) {
        String uname = safeTrim(username);
        if (uname.isEmpty()) {
            throw new IllegalArgumentException("username не должен быть пустым");
        }

        User user = userRepository.findByUsername(uname)
                .orElseThrow(() -> new DatabaseException("Пользователь не найден: " + uname, null));

        Long id = user.getId();
        if (id == null || id <= 0) {
            throw new DatabaseException("У пользователя отсутствует корректный id: " + uname, null);
        }
        return id;
    }

    private static String normalizeRoom(String room) {
        String r = safeTrim(room);
        return r.isEmpty() ? DEFAULT_ROOM : r;
    }

    private LocalDateTime normalizeSentAt(LocalDateTime sentAt) {
        return sentAt != null ? sentAt : LocalDateTime.now(clock);
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}