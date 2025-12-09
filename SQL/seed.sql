-- Пользователи
INSERT INTO users (id, username, password_hash, created_at)
VALUES
    (1, 'alice', 'hash_alice', CURRENT_TIMESTAMP - 10),
    (2, 'bob',   'hash_bob',   CURRENT_TIMESTAMP - 9),
    (3, 'ivan',  'hash_ivan',  CURRENT_TIMESTAMP - 8);

-- Комнаты
INSERT INTO chat_room (id, name, created_at)
VALUES
    (1, 'General',   CURRENT_TIMESTAMP - 7),
    (2, 'Developers', CURRENT_TIMESTAMP - 6);

-- Связи пользователь–комната
INSERT INTO user_chat_room (user_id, chat_room_id, joined_at)
VALUES
    (1, 1, CURRENT_TIMESTAMP - 5),
    (2, 1, CURRENT_TIMESTAMP - 5),
    (3, 1, CURRENT_TIMESTAMP - 4),
    (2, 2, CURRENT_TIMESTAMP - 3),
    (3, 2, CURRENT_TIMESTAMP - 3);

-- Сообщения
INSERT INTO message (id, chat_room_id, sender_id, content, sent_at)
VALUES
    (1, 1, 1, 'Привет всем!',              CURRENT_TIMESTAMP - 4),
    (2, 1, 2, 'Привет, Alice!',            CURRENT_TIMESTAMP - 3),
    (3, 2, 3, 'Кто пушил последний коммит?', CURRENT_TIMESTAMP - 2),
    (4, 2, 2, 'Я, проверяю Sonar.',        CURRENT_TIMESTAMP - 1);