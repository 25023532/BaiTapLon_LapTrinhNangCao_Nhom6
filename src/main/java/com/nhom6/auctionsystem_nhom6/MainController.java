package com.nhom6.auctionsystem_nhom6;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import java.util.List;
import java.util.UUID;

public class MainController {

    // =========================================================
    // HEADER
    // =========================================================
    @FXML private Label      userNameLabel;
    @FXML private Label      userRoleLabel;
    @FXML private Label      userAvatarLabel;
    @FXML private Label      walletLabel;
    @FXML private TextField  searchField;
    @FXML private MenuButton profileMenuBtn;
    @FXML private MenuItem   ordersMenuItem;

    // =========================================================
    // AUCTION CARD & EMPTY STATE
    // =========================================================
    @FXML private HBox auctionCard;
    @FXML private VBox emptyAuctionBox;

    // =========================================================
    // AUCTION INFO
    // =========================================================
    @FXML private Label productTitleLabel;
    @FXML private Label productDescLabel;
    @FXML private Label startPriceLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label minStepLabel;
    @FXML private Label endTimeLabel;
    @FXML private Label statusLabel;

    // =========================================================
    // COUNTDOWN
    // =========================================================
    @FXML private Label hoursLabel;
    @FXML private Label minsLabel;
    @FXML private Label secsLabel;

    // =========================================================
    // BID HISTORY
    // =========================================================
    @FXML private VBox bidHistoryBox;

    // =========================================================
    // CHAT
    // =========================================================
    @FXML private VBox       chatPanel;
    @FXML private VBox       chatMessagesBox;
    @FXML private TextField  chatInput;
    @FXML private ScrollPane chatScrollPane;

    private AuctionSession session;
    private Timeline       countdownTimer;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        User user = AppContext.getCurrentUser();
        if (user == null) {
            showAlert("Lỗi đăng nhập", "Không tìm thấy user hiện tại.");
            return;
        }

        // ── Header ───────────────────────────────────────────
        userNameLabel.setText(user.getUsername());
        userRoleLabel.setText(user.getRole());
        String avatar = user.getUsername().length() >= 2
                ? user.getUsername().substring(0, 2).toUpperCase()
                : user.getUsername().toUpperCase();
        userAvatarLabel.setText(avatar);
        walletLabel.setText(String.format("₫ %,.0f",
                AppContext.getWalletBalance(user.getUsername())));

        applyRoleMenu(user);

        // ── Lấy session ──────────────────────────────────────
        session = AppContext.getActiveSession();

        // Bidder: nếu chưa có activeSession → tự động lấy phiên RUNNING đầu tiên
        if (session == null && "BIDDER".equalsIgnoreCase(user.getRole())) {
            List<AuctionSession> running = AppContext.getRunningSessions();
            if (!running.isEmpty()) {
                session = running.get(0);
                AppContext.setActiveSession(session);
            }
        }

        // ── Hiển thị ─────────────────────────────────────────
        if (session != null) {
            showAuctionCard(true);
            loadAuctionInfo();
            refreshBidHistory();
            startCountdown();
        } else {
            showAuctionCard(false);
        }
    }

    // =========================================================
    // TOGGLE CARD / EMPTY
    // =========================================================
    private void showAuctionCard(boolean hasSession) {
        if (auctionCard != null) {
            auctionCard.setVisible(hasSession);
            auctionCard.setManaged(hasSession);
        }
        if (emptyAuctionBox != null) {
            emptyAuctionBox.setVisible(!hasSession);
            emptyAuctionBox.setManaged(!hasSession);
        }
    }

    // =========================================================
    // ROLE MENU
    // =========================================================
    private void applyRoleMenu(User user) {
        if (ordersMenuItem == null) return;
        String role = user.getRole() == null ? "" : user.getRole().toUpperCase();
        switch (role) {
            case "BIDDER":
                profileMenuBtn.getItems().remove(ordersMenuItem);
                break;
            case "SELLER":
                ordersMenuItem.setText("📦  Sản phẩm đăng bán");
                ordersMenuItem.setOnAction(e -> handleMyProducts());
                break;
        }
    }

    // =========================================================
    // LOAD AUCTION INFO
    // =========================================================
    private void loadAuctionInfo() {
        productTitleLabel.setText(session.getItemName());
        productDescLabel.setText("Sản phẩm mới 100%, còn nguyên seal. Bảo hành 12 tháng.");
        startPriceLabel.setText(formatVND(session.getStartingPrice()));
        currentPriceLabel.setText(formatVND(session.getCurrentPrice()));
        minStepLabel.setText(formatVND(session.getMinBidStep()));
        endTimeLabel.setText(session.getEndTime()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")));
        statusLabel.setText("● " + session.getStatus().name());
        statusLabel.getStyleClass().setAll("status-badge", "status-running");
    }

    // =========================================================
    // COUNTDOWN — tự cập nhật đúng kể cả khi anti-sniping kéo dài
    // =========================================================
    private void startCountdown() {
        countdownTimer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> updateCountdown()));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void updateCountdown() {
        if (session == null) return;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = session.getEndTime(); // luôn lấy endTime mới nhất
        if (now.isAfter(end)) {
            hoursLabel.setText("00");
            minsLabel.setText("00");
            secsLabel.setText("00");
            if (statusLabel != null) statusLabel.setText("● KẾT THÚC");
            countdownTimer.stop();
            return;
        }
        long total = java.time.Duration.between(now, end).getSeconds();
        hoursLabel.setText(String.format("%02d", total / 3600));
        minsLabel.setText(String.format("%02d",  (total % 3600) / 60));
        secsLabel.setText(String.format("%02d",  total % 60));
    }

    // =========================================================
    // PLACE BID
    // =========================================================
    @FXML
    private void handlePlaceBid() {
        if (session == null) {
            showAlert("Chưa có phiên đấu giá",
                    "Hiện tại chưa có sản phẩm nào đang đấu giá.");
            return;
        }
        try {
            User user = AppContext.getCurrentUser();
            if (user == null) { showAlert("Lỗi", "Bạn chưa đăng nhập."); return; }

            double newPrice = session.getCurrentPrice() + session.getMinBidStep();
            Bid bid = new Bid(UUID.randomUUID().toString(), user.getUsername(), newPrice);
            session.placeBid(bid);

            currentPriceLabel.setText(formatVND(session.getCurrentPrice()));
            // Cập nhật countdown ngay nếu anti-sniping vừa gia hạn
            updateCountdown();
            refreshBidHistory();
            addChatMessage("System",
                    user.getUsername() + " đã đặt giá " + formatVND(newPrice), false);

            AppContext.addHistory(user.getUsername(), new AppContext.HistoryRecord(
                    "BID-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                    session.getItemName(), newPrice, "SellerLong",
                    "CHỜ XỬ LÝ", true, LocalDateTime.now()));

            ServerConnection conn = ServerConnection.getInstance();
            if (conn.isConnected())
                conn.send("BID:" + user.getUsername() + ":" + newPrice);

        } catch (InvalidBidException e) {
            showAlert("Bid không hợp lệ", e.getMessage());
        } catch (AuctionClosedException e) {
            showAlert("Phiên đấu giá đã đóng", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi hệ thống", e.getMessage());
        }
    }

    // =========================================================
    // CHAT
    // =========================================================
    @FXML
    private void handleToggleChat() {
        boolean visible = !chatPanel.isVisible();
        chatPanel.setVisible(visible);
        chatPanel.setManaged(visible);
        if (visible) Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    @FXML
    private void handleSendChat() {
        String msg = chatInput.getText().trim();
        if (msg.isEmpty()) return;
        User user = AppContext.getCurrentUser();
        addChatMessage(user.getUsername(), msg,
                user.getRole().equalsIgnoreCase("SELLER"));
        chatInput.clear();
        ServerConnection conn = ServerConnection.getInstance();
        if (conn.isConnected())
            conn.send("CHAT:" + user.getUsername() + ":" + msg);
    }

    // =========================================================
    // SIDEBAR NAVIGATION
    // =========================================================
    @FXML private void handleAuctionList() {
        try { HelloApplication.showAuctionListView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleLiveAuction() {
        // Bidder: nếu chưa có session, lấy phiên running đầu tiên
        if (AppContext.getActiveSession() == null) {
            List<AuctionSession> running = AppContext.getRunningSessions();
            if (!running.isEmpty())
                AppContext.setActiveSession(running.get(0));
            else {
                showAlert("Thông báo",
                        "Hiện chưa có phiên đấu giá nào đang diễn ra.");
                return;
            }
        }
        try { HelloApplication.showLiveAuctionView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleProductManagement() {
        try { HelloApplication.showProductManagementView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================
    // PROFILE MENU
    // =========================================================
    @FXML private void handleProfile() {
        try { HelloApplication.showProfileView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleHistory() {
        try { HelloApplication.showHistoryView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleOrders() {
        try { HelloApplication.showHistoryView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void handleMyProducts() {
        try { HelloApplication.showMyProductsView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleWallet() {
        try { HelloApplication.showWalletView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleLogout() {
        if (countdownTimer != null) countdownTimer.stop();
        AppContext.logout();
        try { HelloApplication.showLoginView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================
    // BID HISTORY
    // =========================================================
    private void refreshBidHistory() {
        bidHistoryBox.getChildren().clear();
        if (session == null) return;
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

    // =========================================================
    // CHAT UI
    // =========================================================
    private void addChatMessage(String sender, String message, boolean isSeller) {
        VBox bubble = new VBox(2);
        bubble.getStyleClass().add(
                isSeller ? "chat-bubble-seller" : "chat-bubble-buyer");
        Label senderLabel = new Label(sender);
        senderLabel.getStyleClass().add("chat-sender");
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.getStyleClass().add("chat-message");
        bubble.getChildren().addAll(senderLabel, msgLabel);
        chatMessagesBox.getChildren().add(bubble);
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    // =========================================================
    // UTIL
    // =========================================================
    private String formatVND(double amount) {
        return String.format("₫ %,.0f", amount);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
