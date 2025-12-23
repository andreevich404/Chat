# Chat Project

## Описание

Этот проект представляет собой чат-приложение, включающее серверную и клиентскую части. Серверная часть обрабатывает соединения и взаимодействует с клиентами, а клиентская часть предназначена для отправки и получения сообщений от сервера.

## Участники команды

| Username                                            | Имя                         | Роль в команде    |
|-----------------------------------------------------|-----------------------------|-------------------|
| [@andreevich404](https://github.com/andreevich404)  | Толкачев Иван Андреевич     | Технический лидер |
| [@MarySim](https://github.com/MarySim)              | Симонова Марина Евгеньевна  | Разработчик       |
| [@reginaa11](https://github.com/reginaa11)          | Симоненко Регина Зиядиновна | Разработчик       |


## Структура проекта
- **[chat-client/](https://github.com/andreevich404/Chat/tree/develop/chat-client)** - Клиентская часть приложения.
- **[chat-server/](https://github.com/andreevich404/Chat/tree/develop/chat-server)** - Серверная часть приложения.

## ER-диаграмма
```mermaid
erDiagram
    USERS {
        BIGINT id PK
        VARCHAR(100) username "NOT NULL, UNIQUE"
        VARCHAR(255) password_hash "NOT NULL"
        TIMESTAMP created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    CHAT_ROOM {
        BIGINT id PK
        VARCHAR(150) name "NOT NULL"
        VARCHAR(10) room_type "NOT NULL, CHECK IN('ROOM','DM')"
        TIMESTAMP created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    USER_CHAT_ROOM {
        BIGINT user_id PK, FK "NOT NULL"
        BIGINT chat_room_id PK, FK "NOT NULL"
        TIMESTAMP joined_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    DIRECT_CHAT {
        BIGINT chat_room_id PK, FK "NOT NULL"
        BIGINT user_low_id FK "NOT NULL"
        BIGINT user_high_id FK "NOT NULL"
        TIMESTAMP created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    MESSAGE {
        BIGINT id PK
        BIGINT chat_room_id FK "NOT NULL"
        BIGINT sender_id FK "NOT NULL"
        VARCHAR(1000) content "NOT NULL"
        TIMESTAMP sent_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    USERS     ||--o{ USER_CHAT_ROOM : "membership"
    CHAT_ROOM ||--o{ USER_CHAT_ROOM : "members"

    CHAT_ROOM ||--o{ MESSAGE        : "contains"
    USERS     ||--o{ MESSAGE        : "sends"

    CHAT_ROOM ||--|| DIRECT_CHAT    : "dm_details"
    USERS     ||--o{ DIRECT_CHAT    : "dm_user_low"
    USERS     ||--o{ DIRECT_CHAT    : "dm_user_high"
```

## Требования

Для работы с проектом необходимо установить следующие инструменты:

- [Java 25+](https://www.oracle.com/java/technologies/downloads/#java21) (для компиляции и запуска приложения).
- [Maven](https://maven.apache.org/install.html) (для управления зависимостями и сборки проекта).
- [JavaFX](https://openjfx.io/) (подтягивается автоматически через Maven (`javafx-maven-plugin`))

## Клонирование репозитория

```bash
git clone https://github.com/andreevich404/Chat.git
cd Chat
```

## Конфигурация

### Сервер

Файл: `chat-server/src/main/resources/application.properties`

Ключи:
- `app.env=dev # dev | prod`
- `server.port=8080`
- `server.host=localhost`
- `db.init.mode=schema # never | schema`
- `jdbcUrl=jdbc:h2:./data/chat-db`
  
Файл: `chat-server/src/main/resources/.env`

Ключи:
- `DB_USERNAME`
- `DB_PASSWORD`

### Клиент

Файл: `chat-client/src/main/resources/application.properties`

Ключи:
- `client.server.host`
- `client.server.port`
- `client.server.connectTimeoutMs`

## Сборка и запуск

### VM Options (для IntelliJ IDEA)

Для клиента и сервера:
```
-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 --enable-native-access=ALL-UNNAMED
```

## Запуск сервера (chat-server)

1) Перейди в модуль:

```bash
cd chat-server
```

2) Запуск:

```bash
mvn clean compile exec:java
```

## Запуск клиента (chat-client)

1) Перейди в модуль:

```bash
cd chat-client
```

2) Запуск:

```bash
mvn clean compile javafx:run
```