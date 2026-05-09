package com.nhom6.auctionsystem_nhom6;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import org.example.service.AuthService;
import org.example.user.User;

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    private final AuthService authService = AppContext.getAuthService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleLogin();
        });
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        errorLabel.setVisible(false);

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
            return;
        }

        User user = authService.login(username, password);
        if (user == null) {
            showError("Sai tên đăng nhập hoặc mật khẩu.");
            passwordField.clear();
            return;
        }

        AppContext.setCurrentUser(user);

        ServerConnection conn = ServerConnection.getInstance();
        if (conn.isConnected()) conn.send("LOGIN:" + username + ":" + password);

        try {
            HelloApplication.showMainView();
        } catch (Exception e) {
            showError("Lỗi khi mở giao diện: " + e.getMessage());
        }
    }

    // ← Sửa: mở màn hình đăng ký thật thay vì Alert
    @FXML
    private void handleRegister() {
        try {
            HelloApplication.showRegisterView();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
