package com.nhom6.auctionsystem_nhom6.controller;

import com.nhom6.auctionsystem_nhom6.AppContext;
import com.nhom6.auctionsystem_nhom6.HelloApplication;
import com.nhom6.auctionsystem_nhom6.ServerConnection;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class HelpController {

    @FXML private VBox     faqListBox;
    @FXML private TextField searchHelpField;

    // =========================================================
    // DATA — Câu hỏi thường gặp
    // =========================================================
    private static final String[][] FAQ = {
            {
                    "Làm thế nào để đặt giá?",
                    "Vào mục \"Đấu giá trực tiếp\", chọn phiên đang chạy và nhấn \"ĐẶT GIÁ NGAY!\". " +
                            "Hệ thống sẽ tự động tăng theo bước giá tối thiểu. Bạn cần có đủ số dư trong ví."
            },
            {
                    "Tôi bị vượt giá (outbid), phải làm gì?",
                    "Bạn sẽ nhận thông báo ngay khi bị vượt giá. Vào lại phiên đấu giá và đặt giá " +
                            "cao hơn trước khi phiên kết thúc."
            },
            {
                    "Làm thế nào để nạp tiền vào ví?",
                    "Vào \"Ví & Giao dịch\" → tab \"Nạp tiền\", nhập số tiền cần nạp, chọn phương " +
                            "thức thanh toán (VCB, TCB, MoMo, ZaloPay, VNPay) và nhấn \"XÁC NHẬN NẠP TIỀN\"."
            },
            {
                    "Sau khi thắng đấu giá, tôi cần làm gì?",
                    "Bạn sẽ nhận thông báo \"Chúc mừng! Bạn đã thắng\". Vào \"Ví & Giao dịch\" → " +
                            "\"Thanh toán\" để hoàn tất trong vòng 24 giờ. Quá thời hạn có thể bị phạt vi phạm."
            },
            {
                    "Làm thế nào để đăng bán sản phẩm?",
                    "Vào \"Quản lý sản phẩm\" → nhấn \"+ Thêm sản phẩm\", điền đầy đủ thông tin " +
                            "(tên, danh mục, giá khởi điểm, bước giá, thời gian). Sản phẩm sẽ ở trạng thái " +
                            "\"CHỜ DUYỆT\" cho đến khi Admin xét duyệt."
            },
            {
                    "Auto-bid là gì?",
                    "Auto-bid cho phép hệ thống tự động đặt giá thay bạn khi có người vượt giá, " +
                            "lên đến mức giới hạn bạn đặt. Tính năng này giúp bạn không bỏ lỡ phiên đấu giá " +
                            "khi không online."
            },
            {
                    "Tôi có thể hủy phiên đấu giá không?",
                    "Chỉ Seller mới có thể hủy phiên đấu giá của mình trước khi có lượt bid đầu tiên. " +
                            "Sau khi đã có người đặt giá, phiên không thể hủy."
            },
            {
                    "Thông báo không hiện lên, phải làm sao?",
                    "Nhấn vào icon 🔔 ở góc trên phải để xem danh sách thông báo. " +
                            "Nếu không có thông báo mới, badge số đỏ sẽ không hiển thị. " +
                            "Kiểm tra kết nối mạng nếu thông báo real-time không hoạt động."
            },
            {
                    "Làm thế nào để xem lịch sử giao dịch?",
                    "Vào \"Ví & Giao dịch\" → tab \"Lịch sử GD\". Tất cả các lần nạp tiền và " +
                            "thanh toán đều được lưu tại đây theo thứ tự mới nhất."
            },
            {
                    "Tôi quên mật khẩu, phải làm sao?",
                    "Hiện tại hệ thống chưa hỗ trợ khôi phục mật khẩu tự động. " +
                            "Vui lòng liên hệ Admin để được hỗ trợ đặt lại mật khẩu."
            },
    };

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        renderFaq(FAQ);
    }

    // =========================================================
    // SEARCH
    // =========================================================
    @FXML
    private void handleSearch() {
        String keyword = searchHelpField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            renderFaq(FAQ);
            return;
        }
        var filtered = java.util.Arrays.stream(FAQ)
                .filter(qa -> qa[0].toLowerCase().contains(keyword)
                        || qa[1].toLowerCase().contains(keyword))
                .toArray(String[][]::new);
        renderFaq(filtered);
    }

    // =========================================================
    // RENDER FAQ (accordion style)
    // =========================================================
    private void renderFaq(String[][] items) {
        faqListBox.getChildren().clear();

        if (items.length == 0) {
            Label empty = new Label("Không tìm thấy câu hỏi phù hợp.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; "
                    + "-fx-padding: 24 0 24 0;");
            faqListBox.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < items.length; i++) {
            faqListBox.getChildren().add(buildFaqItem(items[i][0], items[i][1], i));
        }
    }

    private VBox buildFaqItem(String question, String answer, int index) {
        // Answer label (ẩn ban đầu)
        Label answerLabel = new Label(answer);
        answerLabel.setWrapText(true);
        answerLabel.setStyle("""
                -fx-text-fill: #94a3b8;
                -fx-font-size: 13px;
                -fx-padding: 0 16 14  40;
                -fx-line-spacing: 3;
                """);
        answerLabel.setVisible(false);
        answerLabel.setManaged(false);

        // Arrow icon
        Label arrow = new Label("▶");
        arrow.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

        // Number badge
        Label numBadge = new Label(String.valueOf(index + 1));
        numBadge.setStyle("""
                -fx-background-color: #1e3a5f;
                -fx-text-fill: #38bdf8;
                -fx-font-size: 11px;
                -fx-font-weight: bold;
                -fx-background-radius: 50%;
                -fx-min-width: 24px;
                -fx-min-height: 24px;
                -fx-max-width: 24px;
                -fx-max-height: 24px;
                -fx-alignment: CENTER;
                """);

        // Question row
        Label questionLabel = new Label(question);
        questionLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; "
                + "-fx-font-weight: bold;");
        questionLabel.setWrapText(true);
        HBox.setHgrow(questionLabel, Priority.ALWAYS);

        HBox questionRow = new HBox(12, numBadge, questionLabel, arrow);
        questionRow.setAlignment(Pos.CENTER_LEFT);
        questionRow.setPadding(new Insets(14, 16, 14, 16));
        questionRow.setStyle("-fx-cursor: hand;");

        // Container
        VBox item = new VBox(0, questionRow, answerLabel);
        item.setStyle("""
                -fx-background-color: #1e293b;
                -fx-background-radius: 10;
                -fx-border-color: #334155;
                -fx-border-radius: 10;
                -fx-border-width: 1;
                """);

        // Toggle on click
        questionRow.setOnMouseClicked(e -> {
            boolean showing = answerLabel.isVisible();
            answerLabel.setVisible(!showing);
            answerLabel.setManaged(!showing);
            arrow.setText(showing ? "▶" : "▼");
            arrow.setStyle("-fx-text-fill: "
                    + (showing ? "#475569" : "#2563eb")
                    + "; -fx-font-size: 11px;");
            item.setStyle("""
                    -fx-background-color: #1e293b;
                    -fx-background-radius: 10;
                    -fx-border-color: """ + (showing ? "#334155" : "#2563eb") + """
                    ;
                    -fx-border-radius: 10;
                    -fx-border-width: 1;
                    """);
        });

        // Hover
        questionRow.setOnMouseEntered(e ->
                questionRow.setStyle("-fx-cursor: hand; "
                        + "-fx-background-color: rgba(255,255,255,0.03); "
                        + "-fx-background-radius: 10 10 0 0;"));
        questionRow.setOnMouseExited(e ->
                questionRow.setStyle("-fx-cursor: hand;"));

        return item;
    }

    // =========================================================
    // NAVIGATION
    // =========================================================
    @FXML private void handleBack() {
        try { HelloApplication.showMainView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleAuctionList() {
        try { HelloApplication.showAuctionListView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleLiveAuction() {
        try { HelloApplication.showLiveAuctionView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleProductManagement() {
        try { HelloApplication.showProductManagementView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleWallet() {
        try { HelloApplication.showWalletView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleSessionHistory() {
        try { HelloApplication.showAuctionSessionHistoryView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleProfile() {
        try { HelloApplication.showProfileView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleHistory() {
        try { HelloApplication.showHistoryView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleLogout() {
        try { AppContext.logout(); HelloApplication.showLoginView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleRating() {
        try { HelloApplication.showRatingView(); }
        catch (Exception e) { e.printStackTrace(); }
    }
}
