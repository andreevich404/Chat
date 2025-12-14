package org.example.model;

/**
 * DTO уведомлений о присутствии пользователей (joined/left) + onlineCount.
 *
 * JSON (data):
 * {
 *   "event": "userJoined|userLeft",
 *   "username": "ivan",
 *   "onlineCount": 3
 * }
 */
public class UserPresenceEvent {

    private final String event;
    private final String username;
    private final int onlineCount;

    public UserPresenceEvent(String event, String username, int onlineCount) {
        this.event = event;
        this.username = username;
        this.onlineCount = onlineCount;
    }

    public String getEvent() {
        return event;
    }

    public String getUsername() {
        return username;
    }

    public int getOnlineCount() {
        return onlineCount;
    }
}