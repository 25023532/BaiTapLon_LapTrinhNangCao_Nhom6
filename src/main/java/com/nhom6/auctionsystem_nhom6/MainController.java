package com.nhom6.auctionsystem_nhom6;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.example.auction.AuctionSession;
import org.example.auction.Bid;
import org.example.exception.AuctionClosedException;
import org.example.exception.InvalidBidException;
import org.example.user.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class MainController {

    // ── Header ────────────────────────────────────────────────
    @FXML private Label      userNameLabel;
    @FXML private Label      userRoleLabel;
    @FXML private Label      userAvatarLabel;
    @FXML private Label      walletLabel;
    @FXML private TextField  searchField;
    @FXML private MenuButton profileMenuBtn;

    // ── Profile Menu Item (ẩn/đổi theo role) ─────────────────
    @FXML private MenuItem   ordersMenuItem;

    // ── Auction Card ──────────────────────────────────────────
    @FXML private Label productTitleLabel;
    @FXML private Label productDescLabel;
    @FXML private Label startPriceLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label minStepLabel;
    @FXML private Label endTimeLabel;
    @FXML private Label statusLabel;

    // ── Countdown ─────────────────────────────────────────────
    @FXML private Label hoursLabel;
    @FXML private Label minsLabel;
    @FXML private Label secsLabel;

    // ── Bid History ───────────────────────────────────────────
    @FXML private VBox bidHistoryBox;

    // ── Chat ──────────────────────────────────────────────────
    @FXML private VBox       chatPanel;
    @FXML private VBox       chatMessagesBox;
    @FXML private TextField  chatInput;
    @FXML private ScrollPane chatScrollPane;

    private AuctionSession session;
    private Timeline       countdownTimer;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // ═════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        User user = AppContext.getCurrentUser();
        session   = AppContext.getActiveSession();

        // ── Header info ───────────────────────────────────────
        userNameLabel.setText(user.getUsername());
        userRoleLabel.setText(user.getRole());
        String av = user.getUsername().length() >= 2
                ? user.getUsername().substring(0, 2).toUpperCase()
                : user.getUsername().toUpperCase();
        userAvatarLabel.setText(av);
        walletLabel.setText("0 ₫");

        // ── Menu theo role ────────────────────────────────────
        applyRoleMenu(user);

        loadAuctionInfo();
        refreshBidHistory();
        startCountdown();

        // Chat mẫu
        addChatMessage("SellerLong", "Sản phẩm còn bảo hành chính hãng.", true);
        addChatMessage("bidder07",   "Bước giá tiếp theo là bao nhiêu?",   false);
        addChatMessage("SellerLong", "Bước giá tối thiểu là 500,000 VNĐ.", true);
    }

    // ── Role-based menu ───────────────────────────────────────
    /**
     * BIDDER  → xoá hoàn toàn mục ordersMenuItem khỏi menu
     * SELLER  → đổi text "Đơn hàng" → "Sản phẩm đăng bán" + gắn handler mới
     * (khác)  → giữ nguyên "Đơn hàng" + handler #handleOrders
     */
    private void applyRoleMenu(User user) {
        String role = user.getRole() == null ? "" : user.getRole().toUpperCase();
        switch (role) {
            case "BIDDER":
                profileMenuBtn.getItems().remove(ordersMenuItem);
                break;
            case "SELLER":
                ordersMenuItem.setText("📦  Sản phẩm đăng bán");
                ordersMenuItem.setOnAction(e -> handleMyProducts());
                break;
            default:
                break;
        }
    }

    // ── Toggle Chat ───────────────────────────────────────────
    @FXML
    private void handleToggleChat() {
        boolean nowVisible = !chatPanel.isVisible();
        chatPanel.setVisible(nowVisible);
        chatPanel.setManaged(nowVisible);
        if (nowVisible)
            Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    // ── Auction Info ──────────────────────────────────────────
    private void loadAuctionInfo() {
        productTitleLabel.setText(session.getItemName());
        productDescLabel.setText(
                "Laptop cao cấp – Mới 100%, còn nguyên seal. Bảo hành 12 tháng.");
        startPriceLabel.setText(formatVND(session.getStartingPrice()));
        currentPriceLabel.setText(formatVND(session.getCurrentPrice()));
        minStepLabel.setText(formatVND(session.getMinBidStep()));
        endTimeLabel.setText(session.getEndTime()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM")));
        statusLabel.setText("● " + session.getStatus().name());
        statusLabel.getStyleClass().setAll("status-badge", "status-running");
    }

    // ── Countdown ─────────────────────────────────────────────
    private void startCountdown() {
        countdownTimer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> updateCountdown()));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void updateCountdown() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = session.getEndTime();
        if (now.isAfter(end)) {
            hoursLabel.setText("00");
            minsLabel.setText("00");
            secsLabel.setText("00");
            countdownTimer.stop();
            return;
        }
        long total = java.time.Duration.between(now, end).getSeconds();
        hoursLabel.setText(String.format("%02d", total / 3600));
        minsLabel.setText(String.format("%02d",  (total % 3600) / 60));
        secsLabel.setText(String.format("%02d",  total % 60));
    }

    // ── Place Bid ─────────────────────────────────────────────
    @FXML
    private void handlePlaceBid() {
        User   user     = AppContext.getCurrentUser();
        double newPrice = session.getCurrentPrice() + session.getMinBidStep();
        Bid    bid      = new Bid(UUID.randomUUID().toString(),
                                  user.getUsername(), newPrice);
        try {
            session.placeBid(bid);
            currentPriceLabel.setText(formatVND(session.getCurrentPrice()));
            refreshBidHistory();
            addChatMessage("System",
                    user.getUsername() + " đã đặt giá " + formatVND(newPrice), false);

            ServerConnection conn = ServerConnection.getInstance();
            if (conn.isConnected())
                conn.send("BID:" + user.getUsername() + ":" + newPrice);

        } catch (InvalidBidException e) {
            showAlert("Bid không hợp lệ", e.getMessage());
        } catch (AuctionClosedException e) {
            showAlert("Phiên đã đóng", e.getMessage());
        }
    }

    // ── Send Chat ─────────────────────────────────────────────
    @FXML
    private void handleSendChat() {
        String msg = chatInput.getText().trim();
        if (msg.isEmpty()) return;
        User user = AppContext.getCurrentUser();
        addChatMessage(user.getUsername(), msg, "SELLER".equals(user.getRole()));
        chatInput.clear();

        ServerConnection conn = ServerConnection.getInstance();
        if (conn.isConnected())
            conn.send("CHAT:" + user.getUsername() + ":" + msg);
    }

    // ── Profile Menu Handlers ─────────────────────────────────
    @FXML
    private void handleProfile() {
        try { HelloApplication.showProfileView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Bidder  → mở lịch sử mua hàng
     * Seller  → mở lịch sử bán hàng
     * (dùng chung history-view.fxml, controller tự nhận role)
     */
    @FXML
    private void handleHistory() {
        try { HelloApplication.showHistoryView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Giữ lại cho role mặc định (không phải BIDDER/SELLER).
     * SELLER dùng handleMyProducts() được gắn qua setOnAction.
     */
    @FXML
    private void handleOrders() {
        try { HelloApplication.showHistoryView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    /** Chỉ gọi khi role = SELLER, gắn qua applyRoleMenu() */
    private void handleMyProducts() {
        try { HelloApplication.showMyProductsView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // ── Logout ────────────────────────────────────────────────
    @FXML
    private void handleLogout() {
        if (countdownTimer != null) countdownTimer.stop();
        AppContext.logout();
        try { HelloApplication.showLoginView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // ── Bid History ───────────────────────────────────────────
    private void refreshBidHistory() {
        bidHistoryBox.getChildren().clear();
        var history = session.getBidHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            Bid b = history.get(i);
            HBox row = new HBox(12);
            row.getStyleClass().add("bid-row");
            if (i == history.size() - 1) row.getStyleClass().add("bid-row-top");

            Label name   = new Label(
                    (i == history.size() - 1 ? "👑 " : "") + b.getBidderId());
            Label amount = new Label(formatVND(b.getAmount()));
            Label time   = new Label(b.getTimestamp().format(TIME_FMT));
            amount.getStyleClass().add("bid-amount");
            time.getStyleClass().add("bid-time");
            HBox.setHgrow(name, Priority.ALWAYS);
            row.getChildren().addAll(name, amount, time);
            bidHistoryBox.getChildren().add(row);
        }
    }

    // ── Chat Helpers ──────────────────────────────────────────
    private void addChatMessage(String sender, String message, boolean isSeller) {
        VBox bubble = new VBox(2);
        bubble.getStyleClass().add(
                isSeller ? "chat-bubble-seller" : "chat-bubble-buyer");
        Label nameLabel = new Label(sender);
        nameLabel.getStyleClass().add("chat-sender");
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.getStyleClass().add("chat-message");
        bubble.getChildren().addAll(nameLabel, msgLabel);
        chatMessagesBox.getChildren().add(bubble);
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    // ── Utility ───────────────────────────────────────────────
    private String formatVND(double amount) {
        return String.format("₫ %,.0f", amount);
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
