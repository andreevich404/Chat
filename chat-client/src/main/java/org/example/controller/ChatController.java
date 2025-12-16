package org.example.controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import org.example.ChatClientNavigator;
import org.example.chat.ChatSession;
import org.example.chat.actions.ChatActions;
import org.example.chat.actions.LogoutHandler;
import org.example.chat.messages.IncomingMessageRouter;
import org.example.chat.messages.MessageHistoryApplier;
import org.example.chat.messages.MessageViewModel;
import org.example.chat.online.OnlineUsersState;
import org.example.chat.online.OnlineUsersUpdater;
import org.example.model.ChatHistoryResponse;
import org.example.model.ChatMessageDto;
import org.example.net.ClientSocketService;
import org.example.net.events.Events;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Контроллер основного окна чата.
 */
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private static final String ROOM_NAME = "General";
    private static final int HISTORY_LIMIT = 150;

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

    @FXML private Button backToRoomButton;

    private final ObservableList<String> messages = FXCollections.observableArrayList();

    private ChatClientNavigator navigator;
    private ClientSocketService socketService;

    private final ChatSession session = new ChatSession();

    private final OnlineUsersState usersState = new OnlineUsersState();
    private final OnlineUsersUpdater usersUpdater = new OnlineUsersUpdater(usersState);

    private final ChatActions chatActions = new ChatActions(ROOM_NAME);
    private final LogoutHandler logoutHandler = new LogoutHandler();

    private IncomingMessageRouter router;
    private MessageHistoryApplier historyApplier;
    private MessageViewModel formatter;

    private volatile boolean connected;

    public void setNavigator(ChatClientNavigator navigator) {
        this.navigator = navigator;
    }

    public void bindSession(String username, ClientSocketService socketService) {
        this.session.setUsername(username);
        this.socketService = socketService;

        this.router = new IncomingMessageRouter(ROOM_NAME);
        this.historyApplier = new MessageHistoryApplier(ROOM_NAME);
        this.formatter = new MessageViewModel(this.session.getUsername());

        bindSocketListener();

        runOnFx(() -> {
            setConnected(socketService != null && socketService.isConnected());
            switchToRoom(false);
            requestRoomHistory();
        });

        log.info("Сессия привязана (username={}, connected={})", this.session.getUsername(), isServiceConnected());
    }

    @FXML
    private void initialize() {
        messagesList.setItems(messages);
        usersList.setItems(usersState.getUsers());

        subtitleLabel.setText("Комната: " + ROOM_NAME);

        setConnected(false);
        hideError();

        messageField.setOnAction(e -> onSend());
        if (sendButton != null) sendButton.setOnAction(e -> onSend());

        if (backToRoomButton != null) {
            backToRoomButton.setVisible(false);
            backToRoomButton.setManaged(false);
            backToRoomButton.setOnAction(e -> onBackToRoom());
        }

        usersList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        usersList.setOnMouseClicked(e -> {
            String selected = usersList.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            String peer = safeTrim(selected);
            if (peer.isBlank()) return;
            if (peer.equalsIgnoreCase(session.getUsername())) return;

            openDirect(peer);
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
            if (!isServiceConnected()) {
                showError("Нет соединения с сервером");
                return;
            }

            try {
                chatActions.send(session, socketService, text);
            } catch (RuntimeException ex) {
                showError(ex.getMessage() == null ? "Ошибка отправки" : ex.getMessage().trim());
                return;
            }

            messageField.clear();
        });
    }

    @FXML
    public void onBackToRoom() {
        runOnFx(() -> {
            switchToRoom(true);
            requestRoomHistory();
        });
    }

    @FXML
    public void onLogout() {
        ClientSocketService svc = this.socketService;
        if (svc != null) svc.setListener(null);

        hideError();
        messages.clear();
        usersUpdater.reset();
        setConnected(false);
        session.backToRoom();

        if (navigator != null) navigator.showLogin();

        this.socketService = null;
        logoutHandler.logout(session, usersUpdater, svc, true);
    }

    @FXML
    public void onExit() {
        ClientSocketService svc = this.socketService;
        this.socketService = null;
        Platform.exit();
        if (svc != null) {
            Thread t = new Thread(() -> {
                try { svc.disconnect(); } catch (Exception ignored) { }
            }, "client-disconnect");
            t.setDaemon(true);
            t.start();
        }
    }

    private void bindSocketListener() {
        if (socketService == null) return;

        socketService.setListener(event -> {
            if (event instanceof Events.Connected) {
                runOnFx(() -> setConnected(true));
                return;
            }

            if (event instanceof Events.Disconnected) {
                runOnFx(() -> setConnected(false));
                return;
            }

            if (event instanceof Events.UsersSnapshot snap) {
                runOnFx(() -> {
                    usersUpdater.applySnapshot(snap.users());
                    setConnected(true);
                    updateOnlineCount();
                });
                return;
            }

            if (event instanceof Events.UserJoined joined) {
                runOnFx(() -> {
                    usersUpdater.userJoined(joined.username(), joined.onlineCount());
                    updateOnlineCount();
                    setConnected(true);
                });
                return;
            }

            if (event instanceof Events.UserLeft left) {
                runOnFx(() -> {
                    usersUpdater.userLeft(left.username(), left.onlineCount());
                    updateOnlineCount();
                });
                return;
            }

            if (event instanceof Events.History h) {
                onIncomingHistory(h.response());
                return;
            }

            if (event instanceof Events.Message m) {
                onIncomingMessage(m.message());
                return;
            }

            if (event instanceof Events.Error err) {
                runOnFx(() -> {
                    setConnected(false);
                    showError(err.userMessage().isBlank() ? "Ошибка соединения" : err.userMessage());
                });
            }
        });
    }

    private void onIncomingHistory(ChatHistoryResponse resp) {
        if (resp == null) return;

        runOnFx(() -> {
            List<String> lines = historyApplier.apply(resp, session, formatter);
            if (lines.isEmpty()) return;

            messages.clear();
            messages.addAll(lines);
            scrollToEnd();
        });
    }

    private void onIncomingMessage(ChatMessageDto msg) {
        if (msg == null) return;

        runOnFx(() -> {
            if (!router.shouldDisplay(msg, session)) return;

            String line = formatter.format(msg);
            messages.add(line);
            scrollToEnd();
        });
    }

    private void openDirect(String peer) {
        runOnFx(() -> {
            session.openDirect(peer);

            subtitleLabel.setText("Диалог: " + peer);
            setBackVisible(true);

            messages.clear();
            requestDirectHistory(peer);
        });
    }

    private void switchToRoom(boolean clearSelection) {
        session.backToRoom();

        subtitleLabel.setText("Комната: " + ROOM_NAME);
        setBackVisible(false);

        messages.clear();
        if (clearSelection) usersList.getSelectionModel().clearSelection();
    }

    private void setBackVisible(boolean visible) {
        if (backToRoomButton == null) return;
        backToRoomButton.setVisible(visible);
        backToRoomButton.setManaged(visible);
    }

    private void requestRoomHistory() {
        if (socketService != null && socketService.isConnected()) {
            socketService.requestRoomHistory(ROOM_NAME, HISTORY_LIMIT);
        }
    }

    private void requestDirectHistory(String peer) {
        if (socketService != null && socketService.isConnected()) {
            socketService.requestDirectHistory(peer, HISTORY_LIMIT);
        }
    }

    private boolean isServiceConnected() {
        return socketService != null && socketService.isConnected();
    }

    private void updateOnlineCount() {
        int newCount = usersState.getOnlineCount();
        onlineCountLabel.setText("Пользователей: " + newCount);
        animateOnlineCountBadge();
    }

    private void setConnected(boolean value) {
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
    }

    private void scrollToEnd() {
        if (!messages.isEmpty()) messagesList.scrollTo(messages.size() - 1);
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

        new SequentialTransition(scaleUp, scaleDown).play();
        fade.play();
    }

    private void showError(String message) {
        errorLabel.setText(message == null ? "" : message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setText("");
    }

    private static String safeTrim(String s) { return s == null ? "" : s.trim(); }

    private void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}