package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import org.example.util.FxAnimations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.URI;

/**
 * Контроллер окна авторизации.
 *
 * Валидация:
 * - username/password не пустые
 *
 * UI:
 * - отображение ошибки
 * - индикатор загрузки
 * - подсказка Caps Lock (через AWT Toolkit)
 * - кнопка GitHub: открывает браузер со ссылкой проекта
 */
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    private static final String PROJECT_URL = "https://github.com/andreevich404/chat";

    private static final int MAX_MESSAGE_LENGTH = 1000;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Button githubButton;

    @FXML private Label errorLabel;
    @FXML private Label capsHintLabel;
    @FXML private ProgressIndicator progress;

    @FXML private HBox card;

    @FXML
    private void initialize() {
        // Защита от NPE, если fx:id не совпал или узел переименован в FXML
        if (card != null) {
            FxAnimations.fadeInUp(card, 14, 0, 280);
        }
        else {
            log.warn("fx:id='card' не найден в FXML — анимация не будет применена");
        }

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
    }

    @FXML
    public void onLogin() {
        submit("LOGIN");
    }

    @FXML
    public void onRegister() {
        submit("REGISTER");
    }

    /**
     * Открывает страницу проекта в браузере.
     * Вызывается из FXML: onAction="#onOpenGithub"
     */
    @FXML
    public void onOpenGithub() {
        log.info("Открытие ссылки проекта: {}", PROJECT_URL);

        new Thread(() -> {
            try {
                if (PROJECT_URL == null || PROJECT_URL.isBlank()) {
                    Platform.runLater(() -> showError("Ссылка на проект не настроена"));
                    return;
                }

                if (!Desktop.isDesktopSupported()) {
                    Platform.runLater(() -> showError("Открытие браузера не поддерживается в этой среде"));
                    return;
                }

                Desktop desktop = Desktop.getDesktop();
                if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                    Platform.runLater(() -> showError("Открытие браузера не поддерживается"));
                    return;
                }

                desktop.browse(URI.create(PROJECT_URL));
            }
            catch (HeadlessException e) {
                log.warn("Невозможно открыть браузер (headless окружение)");
                Platform.runLater(() -> showError("Невозможно открыть браузер в текущем окружении"));
            }
            catch (IllegalArgumentException e) {
                log.warn("Некорректный URL проекта: {}", PROJECT_URL, e);
                Platform.runLater(() -> showError("Некорректная ссылка проекта"));
            }
            catch (IOException e) {
                log.warn("Ошибка открытия браузера для URL: {}", PROJECT_URL, e);
                Platform.runLater(() -> showError("Не удалось открыть браузер"));
            }
            catch (RuntimeException e) {
                log.error("Неожиданная ошибка при открытии браузера", e);
                Platform.runLater(() -> showError("Внутренняя ошибка при открытии ссылки"));
            }
        }, "github-browser-opener").start();
    }

    private void submit(String action) {
        hideError();

        String username = safeTrim(usernameField.getText());
        String password = safeTrim(passwordField.getText());

        boolean ok = true;

        if (username.isBlank()) {
            markFieldError(usernameField);
            ok = false;
        }
        if (password.isBlank()) {
            markFieldError(passwordField);
            ok = false;
        }

        if (!ok) {
            showError("Заполните поля логин и пароль");
            if (card != null) {
                FxAnimations.shake(card);
            }
            log.warn("Валидация формы не пройдена: пустые поля (action={})", action);
            return;
        }

        setBusy(true);

        new Thread(() -> {
            try {
                Thread.sleep(350);
            }
            catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            Platform.runLater(() -> {
                setBusy(false);
                log.info("Нажата кнопка {} (username={})", action, username);

                if ("LOGIN".equals(action) && "fail".equalsIgnoreCase(username)) {
                    showError("Неверный логин или пароль");
                    if (card != null) {
                        FxAnimations.shake(card);
                    }
                } else {
                    showError("Демо: запрос отправлен (action=" + action + ")");
                }
            });
        }, "auth-submit-demo").start();
    }

    private void setBusy(boolean busy) {
        progress.setManaged(busy);
        progress.setVisible(busy);

        loginButton.setDisable(busy);
        registerButton.setDisable(busy);
        if (githubButton != null) {
            githubButton.setDisable(busy);
        }
        usernameField.setDisable(busy);
        passwordField.setDisable(busy);

        if (busy) {
            FxAnimations.fade(progress, 0, 1, 160);
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
        FxAnimations.fadeInUp(errorLabel, 6, 0, 180);
    }

    private void hideError() {
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setText("");
    }

    private void updateCapsHintSafely() {
        if (!passwordField.isFocused()) {
            hideCapsHint();
            return;
        }

        try {
            boolean capsOn = Toolkit.getDefaultToolkit()
                    .getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK);

            if (capsOn) {
                capsHintLabel.setManaged(true);
                capsHintLabel.setVisible(true);
            } else {
                hideCapsHint();
            }
        }
        catch (UnsupportedOperationException e) {
            hideCapsHint();
        }
        catch (RuntimeException e) {
            log.warn("Cannot determine Caps Lock state", e);
            hideCapsHint();
        }
    }

    private void hideCapsHint() {
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
        if (row != null) {
            row.getStyleClass().remove("input-error-row");
        }
    }

    private HBox findInputRow(Control control) {
        Node p = control.getParent();
        while (p != null) {
            if (p instanceof HBox h && h.getStyleClass().contains("input-row")) {
                return h;
            }
            p = p.getParent();
        }
        return null;
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}