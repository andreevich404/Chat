package org.example;

import org.example.net.ClientSocketService;

public interface ChatClientNavigator {

    void showLogin();

    void showChat(String username, ClientSocketService socketService);
}