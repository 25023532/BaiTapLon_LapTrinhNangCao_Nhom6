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

    @FXML private MenuItem menuItemHistory;

    // ── Notification bell ─────────────────────────────────────
    @FXML private Button bellButton;
    @FXML private Label  badgeLabel;

    // ── Sidebar: nút theo role ────────────────────────────────
    @FXML private Button btnSellerProducts;
    @FXML private Button btnAdminProducts;

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
    private LocalDateTime  sessionStartTime;

    // ── Notification ──────────────────────────────────────────
    private final NotificationManager notifManager = new NotificationManager();
    private       Popup               notifPopup;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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

        // ── Menu + sidebar theo role ──────────────────────────
        applyRoleMenu(user);

        // ── Session ──────────────────────────────────────────
        session = resolveSession(user);
        if (session != null) {
            AppContext.setActiveSession(session);
            sessionStartTime = LocalDateTime.now();
            showAuctionCard(true);
            loadAuctionInfo();
            refreshBidHistory();
            startCountdown();
        } else {
            showAuctionCard(false);
        }

        // ── Badge ─────────────────────────────────────────────
        updateBadge();

        // ── Server ───────────────────────────────────────────
        connectToServer(user);
    }

    // =========================================================
    // ROLE MENU + SIDEBAR
    // =========================================================
    private void applyRoleMenu(User user) {
        String role    = user.getRole() == null ? "" : user.getRole().toUpperCase();
        boolean isSeller = "SELLER".equals(role);
        boolean isAdmin  = "ADMIN".equals(role);

        // ── Sidebar ──────────────────────────────────────────
        if (btnSellerProducts != null) {
            btnSellerProducts.setVisible(isSeller);
            btnSellerProducts.setManaged(isSeller);
        }
        if (btnAdminProducts != null) {
            btnAdminProducts.setVisible(isAdmin);
            btnAdminProducts.setManaged(isAdmin);
        }

        // ── Profile dropdown ─────────────────────────────────
        switch (role) {
            case "BIDDER" -> {
                if (menuItemHistory != null)
                    menuItemHistory.setText("🛒  Lịch sử mua hàng");
            }
            case "SELLER" -> {
                if (menuItemHistory != null)
                    menuItemHistory.setText("📦  Lịch sử bán hàng");
            }
            default -> {
                if (menuItemHistory != null && profileMenuBtn != null)
                    profileMenuBtn.getItems().remove(menuItemHistory);
            }
        }
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
                if (user.getUsername().equalsIgnoreCase(
                        AppContext.getSessionSeller(s.getSessionId()))) {
                    return s;
                }
            }
        }

        List<AuctionSession> running = AppContext.getRunningSessions();
        return running.isEmpty() ? null : running.get(0);
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
            String type = extractJson(raw, "type");
            if (type == null || type.isEmpty()) return;

            switch (type) {

                case "ONLINE_COUNT" -> {
                    String count = extractJson(raw, "count");
                    Platform.runLater(() -> {
                        if (onlineCountLabel != null)
                            onlineCountLabel.setText("● Online " + count);
                    });
                }

                case "CHAT" -> {
                    String sender  = extractJson(raw, "username");
                    String message = extractJson(raw, "message");
                    User me = AppContext.getCurrentUser();
                    if (me != null && !sender.equals(me.getUsername()))
                        Platform.runLater(() ->
                                addChatMessage(sender, message, false));
                }

                case "NEW_BID" -> {
                    String bidder    = extractJson(raw, "username");
                    String amountStr = extractJson(raw, "amount");
                    String sessionId = extractJson(raw, "sessionId");
                    User me = AppContext.getCurrentUser();
                    if (me == null || bidder.equals(me.getUsername())) return;

                    try {
                        double amt = Double.parseDouble(amountStr);

                        AppContext.getGlobalSessions().stream()
                                .filter(s -> s.getSessionId().equals(sessionId)
                                        || s.getItemName().equals(sessionId))
                                .findFirst()
                                .ifPresent(s -> {
                                    try {
                                        s.placeBid(new Bid(
                                                UUID.randomUUID().toString(),
                                                bidder, amt));
                                    } catch (Exception ignored) {}
                                });

                        Platform.runLater(() -> {
                            if (session != null) {
                                currentPriceLabel.setText(
                                        formatVND(session.getCurrentPrice()));
                                updateCountdown();
                                refreshBidHistory();
                            }
                            addChatMessage("System",
                                    bidder + " đặt giá " + formatVND(amt),
                                    false);

                            if (session != null) {
                                boolean participating =
                                        session.getBidHistory().stream()
                                        .anyMatch(b -> b.getBidderId()
                                                .equals(me.getUsername()));
                                if (participating)
                                    pushNotification(
                                            NotificationManager.NotifType.OUTBID,
                                            "⚠️ Bạn bị vượt giá!",
                                            String.format("%s vừa đặt %s cho \"%s\"",
                                                    bidder, formatVND(amt),
                                                    session.getItemName()));
                            }
                        });
                    } catch (NumberFormatException ignored) {}
                }

                case "SYSTEM" -> {
                    String message = extractJson(raw, "message");
                    Platform.runLater(() ->
                            addChatMessage("System", message, false));
                }

                case "NOTIFY_BIDDER_SESSION_START" -> {
                    String sessionId  = extractJson(raw, "sessionId");
                    String itemName   = extractJson(raw, "itemName");
                    String sellerName = extractJson(raw, "sellerName");
                    String category   = extractJson(raw, "category");
                    double startPrice, minStep;
                    try {
                        startPrice = Double.parseDouble(extractJson(raw, "startPrice"));
                        minStep    = Double.parseDouble(extractJson(raw, "minStep"));
                    } catch (NumberFormatException e) {
                        startPrice = 0; minStep = 500_000;
                    }
                    LocalDateTime endTime;
                    try {
                        endTime = LocalDateTime.parse(extractJson(raw, "endTime"),
                                DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } catch (Exception e) {
                        endTime = LocalDateTime.now().plusHours(1);
                    }

                    final LocalDateTime finalEndTime   = endTime;
                    final double finalStartPrice = startPrice;
                    final double finalMinStep    = minStep;

                    Platform.runLater(() -> {
                        try {
                            org.example.auction.AuctionSession newSession =
                                    new org.example.auction.AuctionSession(
                                            sessionId, itemName,
                                            finalStartPrice, finalMinStep, finalEndTime);
                            newSession.start();
                            AppContext.registerSession(newSession, sellerName);

                            if (AppContext.getActiveSession() == null) {
                                AppContext.setActiveSession(newSession);
                                session = newSession;
                                showAuctionCard(true);
                                loadAuctionInfo();
                                refreshBidHistory();
                                startCountdown();
                            }

                            pushNotification(
                                    NotificationManager.NotifType.SYSTEM,
                                    "🔴 Phiên mới bắt đầu!",
                                    "\"" + itemName + "\" của " + sellerName
                                            + " vừa mở. Giá khởi điểm: "
                                            + String.format("₫ %,.0f", finalStartPrice));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }

                case "NOTIFY_ADMIN_NEW_PRODUCT" -> {
                    String productId   = extractJson(raw, "productId");
                    String productName = extractJson(raw, "productName");
                    String sellerName  = extractJson(raw, "sellerName");
                    String category    = extractJson(raw, "category");
                    double startPrice  = parseDouble(extractJson(raw, "startPrice"));
                    String startTimeStr= extractJson(raw, "startTime");
                    String endTimeStr  = extractJson(raw, "endTime");

                    try {
                        LocalDateTime startTime = LocalDateTime.parse(startTimeStr, ISO_DT);
                        LocalDateTime endTime   = LocalDateTime.parse(endTimeStr, ISO_DT);

                        boolean exists = AppContext.getAllProducts().stream()
                                .anyMatch(p -> p.id().equals(productId));
                        if (!exists) {
                            AppContext.addProduct(sellerName,
                                    new AppContext.ProductRecord(
                                            productId, productName, category,
                                            startPrice, startPrice, 0,
                                            "CHỜ DUYỆT",
                                            startTime, endTime, "—"));
                        }
                    } catch (Exception e) {
                        System.err.println("[MainController] Parse product error: " + e.getMessage());
                    }

                    Platform.runLater(() ->
                        pushNotification(
                                NotificationManager.NotifType.SYSTEM,
                                "📦 Sản phẩm mới cần duyệt",
                                sellerName + " vừa đăng \""
                                + productName + "\" ("
                                + category + ") — Vào Quản lý sản phẩm để duyệt."
                        )
                    );
                }

                case "NOTIFY_SELLER_APPROVED" -> {
                    String productId   = extractJson(raw, "productId");
                    String productName = extractJson(raw, "productName");

                    User me = AppContext.getCurrentUser();
                    if (me != null) {
                        AppContext.getProducts(me.getUsername()).stream()
                                .filter(p -> p.id().equals(productId))
                                .findFirst()
                                .ifPresent(p -> AppContext.updateProduct(
                                        me.getUsername(),
                                        p.withUpdated(p.currentPrice(),
                                                p.bidCount(), "ĐÃ DUYỆT",
                                                p.topBidder())));
                    }

                    Platform.runLater(() ->
                        pushNotification(
                                NotificationManager.NotifType.BID_PLACED,
                                "✅ Sản phẩm được duyệt!",
                                "\"" + productName + "\" đã được Admin duyệt. "
                                + "Bạn có thể bắt đầu phiên đấu giá ngay!"
                        )
                    );
                }


                case "NOTIFY_BIDDER_SESSION_END" -> {
                    String itemName = extractJson(raw, "itemName");
                    Platform.runLater(() ->
                        pushNotification(
                                NotificationManager.NotifType.SYSTEM,
                                "🏁 Phiên đấu giá kết thúc",
                                "\"" + itemName + "\" đã kết thúc."
                        )
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("[MainController] handleServerMessage lỗi: " + e.getMessage());
        }
    }

    // ── Helpers parse JSON ────────────────────────────────────
    private String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) {
            String searchNum = "\"" + key + "\":";
            int s2 = json.indexOf(searchNum);
            if (s2 == -1) return "";
            s2 += searchNum.length();
            int e2 = json.indexOf(",", s2);
            if (e2 == -1) e2 = json.indexOf("}", s2);
            if (e2 == -1) return "";
            return json.substring(s2, e2).trim().replace("\"", "");
        }
        start += search.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? "" : json.substring(start, end);
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException e) { return 0; }
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
    // LOAD AUCTION INFO
    // =========================================================
    private void loadAuctionInfo() {
        if (session == null) return;
        productTitleLabel.setText(session.getItemName());
        productDescLabel.setText(
                "Sản phẩm mới 100%, còn nguyên seal, bảo hành 12 tháng.");
        startPriceLabel.setText(formatVND(session.getStartingPrice()));
        currentPriceLabel.setText(formatVND(session.getCurrentPrice()));
        minStepLabel.setText(formatVND(session.getMinBidStep()));
        endTimeLabel.setText(session.getEndTime().format(DT_FMT));
        statusLabel.setText("● " + session.getStatus().name());
        statusLabel.getStyleClass().setAll("status-badge", "status-running");
    }

    // =========================================================
    // COUNTDOWN
    // =========================================================
    private void startCountdown() {
        if (countdownTimer != null) countdownTimer.stop();
        countdownTimer = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> updateCountdown()));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void updateCountdown() {
        if (session == null) return;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = session.getEndTime();
        if (now.isAfter(end)) {
            hoursLabel.setText("00");
            minsLabel.setText("00");
            secsLabel.setText("00");
            if (statusLabel != null) statusLabel.setText("● KẾT THÚC");
            countdownTimer.stop();
            saveSessionToHistory();
            handleAuctionEnd();
            pushNotification(
                    NotificationManager.NotifType.SYSTEM,
                    "🏁 Phiên đấu giá kết thúc",
                    "Phiên \"" + session.getItemName() + "\" đã kết thúc.");
            return;
        }
        long total = java.time.Duration.between(now, end).getSeconds();
        hoursLabel.setText(String.format("%02d", total / 3600));
        minsLabel.setText(String.format("%02d",  (total % 3600) / 60));
        secsLabel.setText(String.format("%02d",  total % 60));

        if (total == 300)
            pushNotification(
                    NotificationManager.NotifType.AUCTION_ENDING_SOON,
                    "⏰ Sắp hết giờ!",
                    "Còn 5 phút! Giá hiện tại: "
                    + formatVND(session.getCurrentPrice()));
    }

    // =========================================================
    // LƯU PHIÊN VÀO LỊCH SỬ
    // =========================================================
    private void saveSessionToHistory() {
        if (session == null) return;
        LocalDateTime start = sessionStartTime != null
                ? sessionStartTime
                : session.getEndTime().minusHours(1);
        AppContext.finalizeSession(session, start);
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

        double newPrice = session.getCurrentPrice() + session.getMinBidStep();

        // ✅ Kiểm tra số dư
        double balance = AppContext.getWalletBalance(user.getUsername());
        if (balance < newPrice) {
            showAlert("Số dư không đủ",
                    "Bạn cần có ít nhất " + formatVND(newPrice)
                            + " trong ví để đặt giá này.\n"
                            + "Số dư hiện tại: " + formatVND(balance)
                            + "\nCần nạp thêm: " + formatVND(newPrice - balance));
            return;
        }

        try {
            Bid bid = new Bid(UUID.randomUUID().toString(),
                    user.getUsername(), newPrice);
            session.placeBid(bid);

            currentPriceLabel.setText(formatVND(session.getCurrentPrice()));
            updateCountdown();
            refreshBidHistory();
            addChatMessage("System",
                    user.getUsername() + " đã đặt giá "
                            + formatVND(newPrice), false);

            pushNotification(
                    NotificationManager.NotifType.BID_PLACED,
                    "Đặt giá thành công",
                    String.format("Bạn đã đặt %s cho \"%s\"",
                            formatVND(newPrice), session.getItemName()));

            AppContext.addHistory(user.getUsername(),
                    new AppContext.HistoryRecord(
                            "BID-" + UUID.randomUUID().toString()
                                    .substring(0, 6).toUpperCase(),
                            session.getItemName(), newPrice,
                            AppContext.getSessionSeller(session.getSessionId()),
                            "CHỜ XỬ LÝ", true, LocalDateTime.now()));

            ServerConnection conn = ServerConnection.getInstance();
            if (conn.isConnected())
                conn.sendBid(user.getUsername(),
                        session.getItemName(), newPrice);

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
    // SEARCH  ← FIX: thêm method này để khớp với FXML dòng 98
    // =========================================================
    @FXML
    private void handleSearch(javafx.scene.input.KeyEvent event) {
        if (searchField == null) return;
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) return;

        try {
            HelloApplication.showAuctionListByKeyword(keyword);
        } catch (Exception e) {
            // Fallback: nếu chưa có showAuctionListByKeyword,
            // dùng showAuctionListByCategory với keyword
            try {
                HelloApplication.showAuctionListByCategory(keyword);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // =========================================================
    // NOTIFICATION BELL
    // =========================================================
    @FXML
    private void handleBellClick() {
        if (notifPopup != null && notifPopup.isShowing()) {
            notifPopup.hide();
            return;
        }
        notifManager.markAllRead();
        updateBadge();
        notifPopup = NotificationPopup.show(bellButton, notifManager.getAll());
    }

    public void pushNotification(NotificationManager.NotifType type,
                                  String title, String body) {
        Platform.runLater(() -> {
            notifManager.add(new NotificationManager.NotifItem(
                    type, title, body, LocalDateTime.now()));
            updateBadge();
        });
    }

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

    @FXML private void handleSellerProducts() {
        try { HelloApplication.showMyProductsView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleAdminProducts() {
        try { HelloApplication.showProductManagementView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleAuctionSessionHistory() {
        try { HelloApplication.showAuctionSessionHistoryView(); }
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

    @FXML private void handleRating() {
        try { HelloApplication.showRatingView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleHelp() {
        try { HelloApplication.showHelpView(); }
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
            if (i == history.size() - 1)
                row.getStyleClass().add("bid-row-top");
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
    // KET THUC PHIEN
    // =========================================================
    private void handleAuctionEnd() {
        if (session == null) return;
        User me = AppContext.getCurrentUser();
        if (me == null) return;

        var history = session.getBidHistory();
        if (history.isEmpty()) return;

        var winnerBid = history.get(history.size() - 1);
        String winner    = winnerBid.getBidderId();
        double finalPrice = winnerBid.getAmount();

        if (winner.equals(me.getUsername())) {
            // ✅ Thêm đơn chờ thanh toán
            AppContext.addHistory(me.getUsername(), new AppContext.HistoryRecord(
                    "BID-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                    session.getItemName(),
                    finalPrice,
                    AppContext.getSessionSeller(session.getSessionId()),
                    "CHỜ XỬ LÝ",
                    true,
                    LocalDateTime.now()
            ));

            pushNotification(
                    NotificationManager.NotifType.SYSTEM,
                    "🏆 Bạn đã thắng!",
                    "Vào Ví & Giao dịch → Thanh toán để hoàn tất.");
        } else {
            boolean participated = history.stream()
                    .anyMatch(b -> b.getBidderId().equals(me.getUsername()));
            if (participated) {
                pushNotification(
                        NotificationManager.NotifType.SYSTEM,
                        "😔 Bạn đã thua",
                        "Người thắng: " + winner + " (" + formatVND(finalPrice) + ")");
            }
        }
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
