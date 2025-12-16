package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import org.example.ChatClientNavigator;
import org.example.net.ClientAuthCallbacks;
import org.example.net.ClientSocketService;
import org.example.util.FxAnimations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;

/**
 * Контроллер окна авторизации.
 */
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    private static final String PROJECT_URL = "https://github.com/andreevich404/chat";

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Button githubButton;

    @FXML private Label errorLabel;
    @FXML private Label capsHintLabel;
    @FXML private ProgressIndicator progress;

    @FXML private HBox card;

    private ChatClientNavigator navigator;
    private ClientSocketService socketService;

    public void setNavigator(ChatClientNavigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        if (card != null) FxAnimations.fadeInUp(card, 14, 0, 280);

        usernameField.textProperty().addListener((obs, o, n) -> clearFieldError(usernameField));
        passwordField.textProperty().addListener((obs, o, n) -> clearFieldError(passwordField));

        usernameField.setOnAction(e -> onLogin());
        passwordField.setOnAction(e -> onLogin());

        passwordField.addEventFilter(KeyEvent.KEY_PRESSED, e -> updateCapsHintSafely());
        passwordField.addEventFilter(KeyEvent.KEY_RELEASED, e -> updateCapsHintSafely());
        passwordField.focusedProperty().addListener((obs, oldV, newV) -> {
            if (newV) updateCapsHintSafely();
            else hideCapsHint();
        });

        hideError();
        setBusy(false);
    }

    @FXML
    public void onLogin() {
        submit("LOGIN");
    }

    @FXML
    public void onRegister() {
        submit("REGISTER");
    }

    @FXML
    public void onOpenGithub() {
        openInBrowser(PROJECT_URL);
    }

    private void submit(String action) {
        Objects.requireNonNull(action, "action");
        hideError();

        String username = safeTrim(usernameField.getText());
        String password = safeTrim(passwordField.getText());

        if (!validate(username, password)) {
            showError("Заполните поля логин и пароль");
            shakeCard();
            return;
        }

        setBusy(true);
        closeSocketQuietly();

        ClientSocketService svc = new ClientSocketService();
        this.socketService = svc;

        svc.setAuthCallbacks(new ClientAuthCallbacks() {
            @Override
            public void onAuthSuccess(String usernameFromServer) {
                runOnFx(() -> {
                    setBusy(false);

                    if (navigator == null) {
                        showError("Навигация не настроена");
                        shakeCard();
                        return;
                    }

                    navigator.showChat(safeTrim(usernameFromServer), svc);
                });
            }

            @Override
            public void onAuthFailed(String reason) {
                runOnFx(() -> {
                    setBusy(false);
                    String msg = safeTrim(reason);
                    showError(msg.isBlank() ? "Ошибка авторизации" : msg);
                    shakeCard();
                });
            }
        });

        svc.setListener(event -> {
            // Авторизация не обрабатывается через Events; здесь слушаем только ошибки транспорта.
            if (event instanceof org.example.net.events.Events.Error err) {
                runOnFx(() -> {
                    setBusy(false);
                    String msg = safeTrim(err.userMessage());
                    showError(msg.isBlank() ? "Ошибка соединения" : msg);
                    shakeCard();
                });
            }
        });

        Thread t = new Thread(() -> connectAndAuth(svc, action, username, password), "auth-submit");
        t.setDaemon(true);
        t.start();
    }

    private void connectAndAuth(ClientSocketService svc, String action, String username, String password) {
        try {
            svc.connect();
            svc.sendAuth(action, username, password);
        } catch (RuntimeException ex) {
            runOnFx(() -> {
                setBusy(false);
                showError(extractUserMessage(ex));
                shakeCard();
            });
        }
    }

    private boolean validate(String username, String password) {
        boolean ok = true;

        if (username.isBlank()) {
            markFieldError(usernameField);
            ok = false;
        }

        if (password.isBlank()) {
            markFieldError(passwordField);
            ok = false;
        }

        return ok;
    }

    private void setBusy(boolean busy) {
        if (progress != null) {
            progress.setManaged(busy);
            progress.setVisible(busy);
        }

        loginButton.setDisable(busy);
        registerButton.setDisable(busy);
        if (githubButton != null) githubButton.setDisable(busy);

        usernameField.setDisable(busy);
        passwordField.setDisable(busy);

        if (busy && progress != null) {
            FxAnimations.fade(progress, 0, 1, 160);
        }
    }

    private void showError(String message) {
        if (errorLabel == null) return;

        errorLabel.setText(message == null ? "" : message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
        FxAnimations.fadeInUp(errorLabel, 6, 0, 180);
    }

    private void hideError() {
        if (errorLabel == null) return;
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setText("");
    }

    private void shakeCard() {
        if (card != null) FxAnimations.shake(card);
    }

    private void updateCapsHintSafely() {
        if (capsHintLabel == null) return;

        if (!passwordField.isFocused()) {
            hideCapsHint();
            return;
        }

        try {
            boolean capsOn = Toolkit.getDefaultToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK);
            capsHintLabel.setManaged(capsOn);
            capsHintLabel.setVisible(capsOn);
        } catch (UnsupportedOperationException e) {
            hideCapsHint();
        } catch (RuntimeException e) {
            log.warn("Cannot determine Caps Lock state", e);
            hideCapsHint();
        }
    }

    private void hideCapsHint() {
        if (capsHintLabel == null) return;
        capsHintLabel.setManaged(false);
        capsHintLabel.setVisible(false);
    }

    private void markFieldError(Control control) {
        HBox row = findInputRow(control);
        if (row != null && !row.getStyleClass().contains("input-error-row")) {
            row.getStyleClass().add("input-error-row");
        }
    }

    private void clearFieldError(Control control) {
        HBox row = findInputRow(control);
        if (row != null) row.getStyleClass().remove("input-error-row");
    }

    private HBox findInputRow(Control control) {
        Node p = control.getParent();
        while (p != null) {
            if (p instanceof HBox h && h.getStyleClass().contains("input-row")) return h;
            p = p.getParent();
        }
        return null;
    }

    private void openInBrowser(String url) {
        Thread t = new Thread(() -> {
            try {
                if (url == null || url.isBlank()) {
                    runOnFx(() -> showError("Ссылка не настроена"));
                    return;
                }
                if (!Desktop.isDesktopSupported()) {
                    runOnFx(() -> showError("Открытие браузера не поддерживается"));
                    return;
                }
                Desktop desktop = Desktop.getDesktop();
                if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                    runOnFx(() -> showError("Открытие браузера не поддерживается"));
                    return;
                }
                desktop.browse(URI.create(url));
            } catch (HeadlessException e) {
                runOnFx(() -> showError("Невозможно открыть браузер в текущем окружении"));
            } catch (IllegalArgumentException e) {
                runOnFx(() -> showError("Некорректная ссылка"));
            } catch (IOException e) {
                runOnFx(() -> showError("Не удалось открыть браузер"));
            } catch (RuntimeException e) {
                log.error("Unexpected error when opening browser", e);
                runOnFx(() -> showError("Внутренняя ошибка при открытии ссылки"));
            }
        }, "github-browser-opener");
        t.setDaemon(true);
        t.start();
    }

    private void closeSocketQuietly() {
        try {
            if (socketService != null) socketService.close();
        } catch (Exception ignored) {
        } finally {
            socketService = null;
        }
    }

    private static String extractUserMessage(Throwable err) {
        if (err == null) return "Неизвестная ошибка";
        String msg = err.getMessage();
        return (msg == null || msg.isBlank()) ? "Ошибка соединения" : msg.trim();
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}