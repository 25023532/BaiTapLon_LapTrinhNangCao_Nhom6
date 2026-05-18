package com.nhom6.auctionsystem_nhom6;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import org.example.service.AuthService;
import org.example.user.Admin;
import org.example.user.Bidder;
import org.example.user.Seller;
import org.example.user.User;

import java.util.UUID;

public class RegisterController {

    @FXML private TextField        usernameField;
    @FXML private TextField        fullNameField;
    @FXML private TextField        emailField;
    @FXML private PasswordField    passwordField;
    @FXML private PasswordField    confirmField;
    @FXML private ComboBox<String> roleBox;
    @FXML private Label            errorLabel;
    @FXML private Label            successLabel;

    private final AuthService authService = AppContext.getAuthService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        roleBox.getItems().addAll(
                "Người đấu giá (Bidder)",
                "Người bán (Seller)",
                "Quản trị viên (Admin)");   // ← thêm Admin
        roleBox.setValue("Người đấu giá (Bidder)");

        confirmField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleRegister();
        });
    }

    @FXML
    private void handleRegister() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);

        String username  = usernameField.getText().trim();
        String fullName  = fullNameField.getText().trim();
        String email     = emailField.getText().trim();
        String password  = passwordField.getText();
        String confirm   = confirmField.getText();
        String roleValue = roleBox.getValue();

        // ── Validate ──────────────────────────────────────────
        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showError("Vui lòng điền đầy đủ các trường bắt buộc (*)."); return;
        }
        if (username.length() < 4) {
            showError("Tên đăng nhập phải có ít nhất 4 ký tự."); return;
        }
        if (password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự."); return;
        }
        if (!password.equals(confirm)) {
            showError("Mật khẩu xác nhận không khớp.");
            confirmField.clear(); return;
        }
        if (authService.isRegistered(username)) {
            showError("Tên đăng nhập '" + username + "' đã tồn tại."); return;
        }

        // ── Tạo user theo role ────────────────────────────────
        String id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        User newUser;
        if (roleValue != null && roleValue.contains("Seller")) {
            newUser = new Seller(id, username, password, email, fullName);
        } else if (roleValue != null && roleValue.contains("Admin")) {
            newUser = new Admin(id, username, password, email, fullName);
        } else {
            newUser = new Bidder(id, username, password, email, fullName);
        }

        try {
            authService.register(newUser);
            showSuccess("Đăng ký thành công! Bạn có thể đăng nhập ngay.");
            clearForm();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToLogin() {
        try { HelloApplication.showLoginView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void clearForm() {
        usernameField.clear();
        fullNameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmField.clear();
        roleBox.setValue("Người đấu giá (Bidder)");
    }

    private void showError(String msg) {
        errorLabel.setText("⚠ " + msg);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }

    private void showSuccess(String msg) {
        successLabel.setText("✅ " + msg);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }
}
