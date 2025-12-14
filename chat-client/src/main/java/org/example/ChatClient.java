package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Точка входа chat-client.
 */
public class ChatClient extends Application {

    private static final Logger log = LoggerFactory.getLogger(ChatClient.class);

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login_view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 980, 640);

        // Резервное подключение CSS (на случай, если FXML stylesheets не сработал)
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm()
        );

        stage.setTitle("Chat Client");
        stage.setMinWidth(920);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();

        log.info("Окно авторизации запущено");
    }

    public static void main(String[] args) {
        launch(args);
    }
}