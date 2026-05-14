package com.nhom6.auctionsystem_nhom6;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Popup;
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

    // ── Notification bell ─────────────────────────────────────
    @FXML private Button bellButton;
    @FXML private Label  badgeLabel;

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
    @FXML private Label      onlineCountLabel;

    // =========================================================
    // DATA
    // =========================================================
    private AuctionSession session;
    private Timeline       countdownTimer;

    // ── Notification ──────────────────────────────────────────
    private final NotificationManager notifManager = new NotificationManager();
    private       Popup               notifPopup;

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
        walletLabel.setText(
                String.format("₫ %,.0f", AppContext.getWalletBalance(user.getUsername())));

        // ── Menu theo role ────────────────────────────────────
        applyRoleMenu(user);

        // ── Session ──────────────────────────────────────────
        session = resolveSession(user);

        if (session != null) {
            AppContext.setActiveSession(session);
            showAuctionCard(true);
            loadAuctionInfo();
            refreshBidHistory();
            startCountdown();
        } else {
            showAuctionCard(false);
        }

        // ── Badge ẩn lúc đầu ─────────────────────────────────
        updateBadge();

        // ── Kết nối server ────────────────────────────────────
        connectToServer(user);
    }

    // =========================================================
    // RESOLVE SESSION
    // =========================================================
    private AuctionSession resolveSession(User user) {
        AuctionSession active = AppContext.getActiveSession();
        if (active != null) return active;

        String role = user.getRole() == null ? "" : user.getRole().toUpperCase();

        if ("SELLER".equals(role)) {
            for (AuctionSession s : AppContext.getRunningSessions()) {
                String sellerOfSession =
                        AppContext.getSessionSeller(s.getSessionId());
                if (user.getUsername().equalsIgnoreCase(sellerOfSession)) {
                    return s;
                }
            }
        }

        List<AuctionSession> running = AppContext.getRunningSessions();
        if (!running.isEmpty()) return running.get(0);

        return null;
    }

    // =========================================================
    // SERVER CONNECTION
    // =========================================================
    private void connectToServer(User user) {
        ServerConnection conn = ServerConnection.getInstance();
        conn.setListener(this::handleServerMessage);
        if (!conn.isConnected()) {
            conn.connect(user.getUsername());
        } else {
            conn.sendJson("{\"action\":\"GET_ONLINE_COUNT\"}");
        }
    }

    private void handleServerMessage(String raw) {
        try {
            if (!raw.contains("\"type\"")) return;
            String type = extractJson(raw, "type");
            switch (type) {
                case "CHAT" -> {
                    String sender  = extractJson(raw, "username");
                    String message = extractJson(raw, "message");
                    User me = AppContext.getCurrentUser();
                    if (me != null && !sender.equals(me.getUsername())) {
                        Platform.runLater(() -> addChatMessage(sender, message, false));
                    }
                }

                case "ONLINE_COUNT" -> {
                    String count = extractJson(raw, "count");
                    Platform.runLater(() -> {
                        if (onlineCountLabel != null)
                            onlineCountLabel.setText("● Online " + count);
                    });
                }

                case "NEW_BID" -> {
                    String bidder = extractJson(raw, "username");
                    String amount = extractJson(raw, "amount");
                    if (session != null) {
                        try {
                            double amt = Double.parseDouble(amount);
                            Platform.runLater(() -> {
                                currentPriceLabel.setText(String.format("₫ %,.0f", amt));
                                addChatMessage("System",
                                        bidder + " đặt giá " + String.format("₫ %,.0f", amt),
                                        false);
                            });

                            // Thông báo outbid nếu mình đang tham gia
                            User me = AppContext.getCurrentUser();
                            if (me != null && !bidder.equals(me.getUsername())) {
                                boolean iParticipating = session.getBidHistory()
                                        .stream()
                                        .anyMatch(b -> b.getBidderId().equals(me.getUsername()));
                                if (iParticipating) {
                                    pushNotification(
                                            NotificationManager.NotifType.OUTBID,
                                            "Bạn bị vượt giá!",
                                            String.format("%s vừa đặt ₫%,.0f cho %s",
                                                    bidder, amt, session.getItemName())
                                    );
                                }
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }

                case "SYSTEM" -> {
                    String message = extractJson(raw, "message");
                    Platform.runLater(() -> addChatMessage("System", message, false));
                }
            }
        } catch (Exception e) {
            System.err.println("handleServerMessage lỗi: " + e.getMessage());
        }
    }

    private String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? "" : json.substring(start, end);
    }

    // =========================================================
    // AUCTION CARD TOGGLE
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
            default:
                break;
        }
    }

    // =========================================================
    // LOAD AUCTION INFO
    // =========================================================
    private void loadAuctionInfo() {
        productTitleLabel.setText(session.getItemName());
        productDescLabel.setText(
                "Sản phẩm mới 100%, còn nguyên seal, bảo hành 12 tháng.");
        startPriceLabel.setText(formatVND(session.getStartingPrice()));
        currentPriceLabel.setText(formatVND(session.getCurrentPrice()));
        minStepLabel.setText(formatVND(session.getMinBidStep()));
        endTimeLabel.setText(session.getEndTime()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")));
        statusLabel.setText("● " + session.getStatus().name());
        statusLabel.getStyleClass().setAll("status-badge", "status-running");
    }

    // =========================================================
    // COUNTDOWN
    // =========================================================
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
            // Thông báo phiên kết thúc
            pushNotification(
                    NotificationManager.NotifType.SYSTEM,
                    "Phiên đấu giá kết thúc",
                    "Phiên đấu giá \"" + session.getItemName() + "\" đã kết thúc."
            );
            return;
        }
        long total = java.time.Duration.between(now, end).getSeconds();
        hoursLabel.setText(String.format("%02d", total / 3600));
        minsLabel.setText(String.format("%02d",  (total % 3600) / 60));
        secsLabel.setText(String.format("%02d",  total % 60));

        // Cảnh báo còn đúng 5 phút
        if (total == 300) {
            pushNotification(
                    NotificationManager.NotifType.AUCTION_ENDING_SOON,
                    "Sắp hết giờ!",
                    String.format("Còn 5 phút! Giá hiện tại: %s", formatVND(session.getCurrentPrice()))
            );
        }
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
        User user = AppContext.getCurrentUser();
        if (user == null) { showAlert("Lỗi", "Bạn chưa đăng nhập."); return; }

        try {
            double newPrice = session.getCurrentPrice() + session.getMinBidStep();
            Bid bid = new Bid(UUID.randomUUID().toString(),
                    user.getUsername(), newPrice);
            session.placeBid(bid);

            // Cập nhật UI
            currentPriceLabel.setText(formatVND(session.getCurrentPrice()));
            refreshBidHistory();
            addChatMessage("System",
                    user.getUsername() + " đã đặt giá " + formatVND(newPrice),
                    false);

            // Thông báo đặt giá thành công
            pushNotification(
                    NotificationManager.NotifType.BID_PLACED,
                    "Đặt giá thành công",
                    String.format("Bạn đã đặt %s cho \"%s\"",
                            formatVND(newPrice), session.getItemName())
            );

            // Ghi lịch sử
            AppContext.addHistory(user.getUsername(), new AppContext.HistoryRecord(
                    "BID-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                    session.getItemName(), newPrice,
                    AppContext.getSessionSeller(session.getSessionId()),
                    "CHỜ XỬ LÝ", true, LocalDateTime.now()));

            // Gửi server
            ServerConnection conn = ServerConnection.getInstance();
            if (conn.isConnected())
                conn.sendBid(user.getUsername(), session.getItemName(), newPrice);

        } catch (InvalidBidException e) {
            showAlert("Bid không hợp lệ", e.getMessage());
            pushNotification(
                    NotificationManager.NotifType.SYSTEM,
                    "Bid không hợp lệ",
                    e.getMessage()
            );
        } catch (AuctionClosedException e) {
            showAlert("Phiên đấu giá đã đóng", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi hệ thống", e.getMessage());
        }
    }

    // =========================================================
    // NOTIFICATION BELL
    // =========================================================
    @FXML
    private void handleBellClick() {
        // Toggle: đang mở thì đóng
        if (notifPopup != null && notifPopup.isShowing()) {
            notifPopup.hide();
            return;
        }
        // Đánh dấu đã đọc, reset badge
        notifManager.markAllRead();
        updateBadge();

        // Hiện popup
        notifPopup = NotificationPopup.show(bellButton, notifManager.getAll());
    }

    /**
     * Thêm thông báo mới và cộng badge.
     * Có thể gọi từ bất kỳ thread nào (tự wrap Platform.runLater).
     */
    public void pushNotification(NotificationManager.NotifType type,
                                 String title, String body) {
        Platform.runLater(() -> {
            notifManager.add(new NotificationManager.NotifItem(
                    type, title, body, LocalDateTime.now()));
            updateBadge();
        });
    }

    /** Cập nhật số đỏ trên icon chuông. */
    private void updateBadge() {
        if (badgeLabel == null) return;
        int unread = notifManager.countUnread();
        badgeLabel.setText(unread > 9 ? "9+" : String.valueOf(unread));
        badgeLabel.setVisible(unread > 0);
        badgeLabel.setManaged(unread > 0);
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
                "SELLER".equalsIgnoreCase(user.getRole()));
        chatInput.clear();

        ServerConnection conn = ServerConnection.getInstance();
        if (conn.isConnected()) {
            conn.sendChat(user.getUsername(), msg);
        } else {
            boolean ok = conn.connect(user.getUsername());
            if (ok) {
                conn.setListener(this::handleServerMessage);
                conn.sendChat(user.getUsername(), msg);
            }
        }
    }

    // =========================================================
    // SIDEBAR NAVIGATION
    // =========================================================
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

    @FXML private void handleCategoryDienTu() {
        try { HelloApplication.showAuctionListByCategory("Điện tử"); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleCategoryMayAnh() {
        try { HelloApplication.showAuctionListByCategory("Máy ảnh"); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleCategoryLaptop() {
        try { HelloApplication.showAuctionListByCategory("Laptop"); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleCategoryDienThoai() {
        try { HelloApplication.showAuctionListByCategory("Điện thoại"); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleCategoryDongHo() {
        try { HelloApplication.showAuctionListByCategory("Đồng hồ"); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleCategoryXeCo() {
        try { HelloApplication.showAuctionListByCategory("Xe cộ"); }
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
        ServerConnection.getInstance().disconnect();
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
        if (history.isEmpty()) {
            Label empty = new Label("Chưa có lượt đặt giá nào.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
            bidHistoryBox.getChildren().add(empty);
            return;
        }
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
    private void addChatMessage(String sender, String message,
                                boolean isSeller) {
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