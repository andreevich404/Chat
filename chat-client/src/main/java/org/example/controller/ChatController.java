package org.example.controller;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Контроллер основного окна чата.
 *
 * Требования:
 * - UI обновляется только через Application Thread
 * - список сообщений
 * - поле ввода
 * - список активных пользователей
 * - индикатор соединения
 * - кнопки выхода/деавторизации
 */
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private static final String ROOM_NAME = "General";

    private static final String STYLE_CONN_ON = "conn-dot-on";
    private static final String STYLE_CONN_OFF = "conn-dot-off";

    private static final String STYLE_BADGE_ON = "online-badge-on";
    private static final String STYLE_BADGE_OFF = "online-badge-off";

    @FXML private Label subtitleLabel;

    @FXML private Region connDot;
    @FXML private Label connLabel;

    @FXML private ListView<String> messagesList;
    @FXML private TextField messageField;
    @FXML private Button sendButton;

    @FXML private ListView<String> usersList;
    @FXML private Label onlineCountLabel;

    @FXML private Label errorLabel;

    private final ObservableList<String> messages = FXCollections.observableArrayList();
    private final ObservableList<String> users = FXCollections.observableArrayList();

    private volatile boolean connected = false;
    private volatile int onlineCount = 0;

    @FXML
    private void initialize() {
        messagesList.setItems(messages);
        usersList.setItems(users);

        subtitleLabel.setText("Комната: " + ROOM_NAME);
        setConnected(false);
        messageField.setOnAction(e -> onSend());
        hideError();

        log.info("Окно чата инициализировано");
    }

    public void setConnected(boolean value) {
        runOnFx(() -> {
            this.connected = value;

            if (value) {
                setConnDotStyle(true);
                connLabel.setText("Подключено");
                setOnlineBadgeStyle(true);
            } else {
                setConnDotStyle(false);
                connLabel.setText("Отключено");
                setOnlineBadgeStyle(false);
            }
        });
    }

    public void setUsers(List<String> usernames) {
        runOnFx(() -> {
            users.setAll(usernames);
            setOnlineCountInternal(usernames.size());
        });
    }

    public void addMessage(String message) {
        runOnFx(() -> {
            messages.add(message);
            messagesList.scrollTo(messages.size() - 1);
        });
    }

    @FXML
    public void onSend() {
        runOnFx(() -> {
            hideError();

            String text = safeTrim(messageField.getText());
            if (text.isBlank()) {
                showError("Введите сообщение");
                return;
            }

            if (!connected) {
                showError("Нет соединения с сервером");
                return;
            }

            addMessage("Вы: " + text);
            messageField.clear();

            log.info("Сообщение отправлено (demo): {}", text);
        });
    }

    @FXML
    public void onLogout() {
        runOnFx(() -> {
            hideError();

            setConnected(false);
            users.clear();
            messages.clear();
            setOnlineCountInternal(0);

            log.info("Выполнена деавторизация (demo)");
            showError("Демо: вы вышли из аккаунта");
        });
    }

    @FXML
    public void onExit() {
        runOnFx(() -> {
            log.info("Выход из приложения по кнопке 'Выйти'");
            Platform.exit();
        });
    }

    private void setOnlineCountInternal(int newCount) {
        int prev = this.onlineCount;
        this.onlineCount = newCount;

        onlineCountLabel.setText("Пользователей: " + newCount);

        setOnlineBadgeStyle(connected);

        if (newCount != prev) {
            animateOnlineCountBadge();
        }
    }

    private void setConnDotStyle(boolean isConnected) {
        if (connDot == null) return;

        var classes = connDot.getStyleClass();
        classes.remove(STYLE_CONN_ON);
        classes.remove(STYLE_CONN_OFF);
        classes.add(isConnected ? STYLE_CONN_ON : STYLE_CONN_OFF);
    }

    private void setOnlineBadgeStyle(boolean isConnected) {
        if (onlineCountLabel == null) return;

        var classes = onlineCountLabel.getStyleClass();
        classes.remove(STYLE_BADGE_ON);
        classes.remove(STYLE_BADGE_OFF);
        classes.add(isConnected ? STYLE_BADGE_ON : STYLE_BADGE_OFF);
    }

    private void animateOnlineCountBadge() {
        if (onlineCountLabel == null) return;

        ScaleTransition scaleUp = new ScaleTransition(javafx.util.Duration.millis(110), onlineCountLabel);
        scaleUp.setFromX(1.0);
        scaleUp.setFromY(1.0);
        scaleUp.setToX(1.06);
        scaleUp.setToY(1.06);
        scaleUp.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleDown = new ScaleTransition(javafx.util.Duration.millis(140), onlineCountLabel);
        scaleDown.setFromX(1.06);
        scaleDown.setFromY(1.06);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        scaleDown.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(140), onlineCountLabel);
        fade.setFromValue(1.0);
        fade.setToValue(0.92);
        fade.setAutoReverse(true);
        fade.setCycleCount(2);

        SequentialTransition seq = new SequentialTransition(scaleUp, scaleDown);
        fade.play();
        seq.play();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setText("");
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}