package org.example.chat.actions;

import org.example.chat.ChatSession;
import org.example.chat.online.OnlineUsersUpdater;
import org.example.net.ClientSocketService;

/**
 * Use-case выхода из аккаунта.
 */
public final class LogoutHandler {

    public void logout(ChatSession session,
                       OnlineUsersUpdater usersUpdater,
                       ClientSocketService socket,
                       boolean async) {

        if (session != null) {
            session.backToRoom();
            session.setUsername(null);
        }

        if (usersUpdater != null) {
            usersUpdater.reset();
        }

        if (socket == null) {
            return;
        }

        if (async) {
            Thread t = new Thread(socket::disconnect, "client-logout");
            t.setDaemon(true);
            t.start();
        } else {
            socket.disconnect();
        }
    }
}