package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.controller.ChatController;
import org.example.controller.LoginController;
import org.example.net.ClientSocketService;
import org.example.service.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Точка входа chat-client.
 */
public class ChatClient extends Application {

    private static final Logger log = LoggerFactory.getLogger(ChatClient.class);

    private Stage stage;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat_view.fxml"));
        Scene scene = new Scene(loader.load(), 1100, 700);

        stage.setTitle("Chat Client");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
        log.info("Приложение запущено");
    }

    @Override
    public void showLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login_view.fxml"));
            Scene scene = new Scene(loader.load(), 980, 640);

            LoginController controller = loader.getController();
            controller.setNavigator(this);

            stage.setScene(scene);
            log.info("Открыто окно авторизации");
        }
        catch (IOException e) {
            log.error("Не удалось загрузить login_view.fxml", e);
            throw new IllegalStateException("Ошибка загрузки окна авторизации", e);
        }
    }

    @Override
    public void showChat(String username, ClientSocketService socketService) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat_view.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 700);

            ChatController controller = loader.getController();
            controller.setNavigator(this);
            controller.bindSession(username, socketService);

        log.info("Окно чата запущено");
    }

    public static void main(String[] args) {
        launch(args);
    }
}