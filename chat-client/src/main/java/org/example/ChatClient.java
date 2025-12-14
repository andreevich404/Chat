package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Точка входа chat-client.
 */
public class ChatClient extends Application {

    private static final Logger log = LoggerFactory.getLogger(ChatClient.class);

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat_view.fxml"));
        Scene scene = new Scene(loader.load(), 1100, 700);

        stage.setTitle("Chat Client");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();

        log.info("Окно чата запущено");
    }

    public static void main(String[] args) {
        launch(args);
    }
}