package com.nhom6.auctionsystem_nhom6;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import org.example.user.User;

import java.io.File;

public class ProfileController {

    @FXML private Label         avatarLabel;
    @FXML private ImageView     avatarImageView;
    @FXML private VBox          avatarOverlay;
    @FXML private Label         avatarHintLabel;
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

        // Avatar chữ tắt
        String av = user.getUsername().length() >= 2
                ? user.getUsername().substring(0, 2).toUpperCase()
                : user.getUsername().toUpperCase();
        avatarLabel.setText(av);

        // Nếu đã có ảnh lưu trong AppContext thì hiện luôn
        Image savedImg = AppContext.getAvatarImage();
        if (savedImg != null) showImage(savedImg);

        // Clip tròn cho ImageView
        Circle clip = new Circle(40, 40, 40);
        avatarImageView.setClip(clip);

        displayNameLabel.setText(user.getUsername());
        roleLabel.setText(user.getRole());
        usernameField.setText(user.getUsername());
        fullNameField.setText(user.getFullName());
        emailField.setText(user.getEmail());
    }

    @FXML
    private void handleChangeAvatar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn ảnh đại diện");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Ảnh (JPG, PNG)", "*.jpg", "*.jpeg", "*.png")
        );

        File file = chooser.showOpenDialog(
                HelloApplication.getPrimaryStage());

        if (file == null) return;

        // Kiểm tra dung lượng ≤ 2MB
        if (file.length() > 2 * 1024 * 1024) {
            showMessage("⚠ Ảnh quá lớn! Vui lòng chọn ảnh dưới 2MB.", false);
            return;
        }

        Image img = new Image(file.toURI().toString(), 80, 80, false, true);
        showImage(img);
        AppContext.setAvatarImage(img); // lưu để dùng lại ở header
        showMessage("✅ Đã cập nhật ảnh đại diện!", true);
    }

    private void showImage(Image img) {
        avatarImageView.setImage(img);
        avatarImageView.setVisible(true);
        avatarLabel.setVisible(false);   // ẩn chữ tắt
    }

    @FXML
    private void handleSave() {
        String fullName        = fullNameField.getText().trim();
        String email           = emailField.getText().trim();
        String newPassword     = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

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

        User user = AppContext.getCurrentUser();
        user.setFullName(fullName);
        user.setEmail(email);
        if (!newPassword.isEmpty()) user.setPassword(newPassword);

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
