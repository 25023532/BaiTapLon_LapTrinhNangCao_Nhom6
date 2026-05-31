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

    @FXML private Button btnSellerProducts;
    @FXML private Button btnAdminProducts;

    @FXML
    public void initialize() {
        User user = AppContext.getCurrentUser();
        if (user == null) return;
        applyRoleMenu(user);

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

    private void applyRoleMenu(User user) {
        String role = user.getRole() == null ? "" : user.getRole().toUpperCase();
        boolean isSeller = "SELLER".equals(role);
        boolean isAdmin  = "ADMIN".equals(role);
        if (btnSellerProducts != null) { btnSellerProducts.setVisible(isSeller); btnSellerProducts.setManaged(isSeller); }
        if (btnAdminProducts != null) { btnAdminProducts.setVisible(isAdmin); btnAdminProducts.setManaged(isAdmin); }
    }

    @FXML private void handleAuctionList() { try { HelloApplication.showAuctionListView(); } catch (Exception e) {} }
    @FXML private void handleLiveAuction() { try { HelloApplication.showLiveAuctionView(); } catch (Exception e) {} }
    @FXML private void handleSellerProducts() { try { HelloApplication.showMyProductsView(); } catch (Exception e) {} }
    @FXML private void handleAdminProducts() { try { HelloApplication.showProductManagementView(); } catch (Exception e) {} }
    @FXML private void handleWallet() { try { HelloApplication.showWalletView(); } catch (Exception e) {} }
    @FXML private void handleRating() { try { HelloApplication.showRatingView(); } catch (Exception e) {} }
    @FXML private void handleHelp() { try { HelloApplication.showHelpView(); } catch (Exception e) {} }
    @FXML private void handleCategoryDienTu()    { try { HelloApplication.showAuctionListByCategory("Điện tử");    } catch (Exception e) {} }
    @FXML private void handleCategoryMayAnh()    { try { HelloApplication.showAuctionListByCategory("Máy ảnh");    } catch (Exception e) {} }
    @FXML private void handleCategoryLaptop()    { try { HelloApplication.showAuctionListByCategory("Laptop");     } catch (Exception e) {} }
    @FXML private void handleCategoryDienThoai() { try { HelloApplication.showAuctionListByCategory("Điện thoại"); } catch (Exception e) {} }
    @FXML private void handleCategoryDongHo()    { try { HelloApplication.showAuctionListByCategory("Đồng hồ");   } catch (Exception e) {} }
    @FXML private void handleCategoryXeCo()      { try { HelloApplication.showAuctionListByCategory("Xe cộ");      } catch (Exception e) {} }
    @FXML private void handleHistory()           { try { HelloApplication.showHistoryView(); } catch (Exception e) {} }
    @FXML private void handleProfile()           { /* Already here */ }
    @FXML private void handleLogout() {
        AppContext.logout();
        try { HelloApplication.showLoginView(); } catch (Exception e) {}
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
