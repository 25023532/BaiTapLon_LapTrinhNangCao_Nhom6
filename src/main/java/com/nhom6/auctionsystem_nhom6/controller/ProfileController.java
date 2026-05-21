package com.nhom6.auctionsystem_nhom6.controller;

import com.nhom6.auctionsystem_nhom6.AppContext;
import com.nhom6.auctionsystem_nhom6.HelloApplication;
import com.nhom6.auctionsystem_nhom6.ServerConnection;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.user.User;

public class ProfileController {

    @FXML private Label         avatarLabel;
    @FXML private Label         displayNameLabel;
    @FXML private Label         roleLabel;

    @FXML private TextField     usernameField;
    @FXML private TextField     fullNameField;
    @FXML private TextField     emailField;

    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label         messageLabel;

    @FXML
    public void initialize() {
        User user = AppContext.getCurrentUser();

        // Avatar: 2 ký tự đầu username
        String av = user.getUsername().length() >= 2
                ? user.getUsername().substring(0, 2).toUpperCase()
                : user.getUsername().toUpperCase();
        avatarLabel.setText(av);

        displayNameLabel.setText(user.getUsername());
        roleLabel.setText(user.getRole());

        // Điền sẵn thông tin
        usernameField.setText(user.getUsername());

        // Nếu User có getFullName() / getEmail() thì dùng, không thì để trống
        try { fullNameField.setText(user.getFullName()); } catch (Exception ignored) {}
        try { emailField.setText(user.getEmail());       } catch (Exception ignored) {}
    }

    @FXML
    private void handleSave() {
        String fullName       = fullNameField.getText().trim();
        String email          = emailField.getText().trim();
        String newPassword    = newPasswordField.getText();
        String confirmPassword= confirmPasswordField.getText();

        // Validate mật khẩu nếu có nhập
        if (!newPassword.isEmpty()) {
            if (newPassword.length() < 6) {
                showMessage("⚠ Mật khẩu phải có ít nhất 6 ký tự.", false);
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                showMessage("⚠ Mật khẩu xác nhận không khớp.", false);
                newPasswordField.clear();
                confirmPasswordField.clear();
                return;
            }
        }

        // Cập nhật thông tin user
        User user = AppContext.getCurrentUser();
        try { user.setFullName(fullName); } catch (Exception ignored) {}
        try { user.setEmail(email);       } catch (Exception ignored) {}
        if (!newPassword.isEmpty()) {
            try { user.setPassword(newPassword); } catch (Exception ignored) {}
        }

        newPasswordField.clear();
        confirmPasswordField.clear();
        showMessage("✅ Lưu thay đổi thành công!", true);
    }

    @FXML
    private void handleBack() {
        try { HelloApplication.showMainView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void showMessage(String msg, boolean isSuccess) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().setAll(isSuccess ? "login-hint" : "login-error");
        messageLabel.setVisible(true);
    }
}
