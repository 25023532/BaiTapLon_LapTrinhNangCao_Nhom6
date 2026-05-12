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

    // =========================================================
    // DATA
    // =========================================================
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

        // ── Kết nối server ────────────────────────────────────
        connectToServer(user);
    }

    // =========================================================
    // ✅ RESOLVE SESSION
    //    Ưu tiên: activeSession → session của Seller → session RUNNING đầu tiên
    // =========================================================
    private AuctionSession resolveSession(User user) {
        // 1. Đã có activeSession → dùng luôn
        AuctionSession active = AppContext.getActiveSession();
        if (active != null) return active;

        String role = user.getRole() == null ? "" : user.getRole().toUpperCase();

        // 2. Seller → tìm session mình đã tạo trong globalSessions
        if ("SELLER".equals(role)) {
            for (AuctionSession s : AppContext.getRunningSessions()) {
                String sellerOfSession =
                        AppContext.getSessionSeller(s.getSessionId());
                if (user.getUsername().equalsIgnoreCase(sellerOfSession)) {
                    return s;
                }
            }
        }

        // 3. Bidder / Admin → lấy session RUNNING đầu tiên có trong globalSessions
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
                        addChatMessage(sender, message, false);
                    }
                }
                case "NEW_BID" -> {
                    String bidder = extractJson(raw, "username");
                    String amount = extractJson(raw, "amount");
                    if (session != null) {
                        try {
                            double amt = Double.parseDouble(amount);
                            currentPriceLabel.setText(
                                    String.format("₫ %,.0f", amt));
                            addChatMessage("System",
                                    bidder + " đặt giá "
                                    + String.format("₫ %,.0f", amt), false);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                case "SYSTEM" -> {
                    String message = extractJson(raw, "message");
                    addChatMessage("System", message, false);
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
        User user = AppContext.getCurrentUser();
        if (user == null) { showAlert("Lỗi", "Bạn chưa đăng nhập."); return; }

        try {
            double newPrice = session.getCurrentPrice() + session.getMinBidStep();
            Bid bid = new Bid(UUID.randomUUID().toString(),
                    user.getUsername(), newPrice);
            session.placeBid(bid);

            // ✅ Cập nhật UI ngay lập tức
            currentPriceLabel.setText(formatVND(session.getCurrentPrice()));
            refreshBidHistory();
            addChatMessage("System",
                    user.getUsername() + " đã đặt giá " + formatVND(newPrice),
                    false);

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
                    (i == hist
