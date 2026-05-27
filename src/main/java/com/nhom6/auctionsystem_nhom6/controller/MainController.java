package com.nhom6.auctionsystem_nhom6.controller;

import com.nhom6.auctionsystem_nhom6.AppContext;
import com.nhom6.auctionsystem_nhom6.HelloApplication;
import com.nhom6.auctionsystem_nhom6.ServerConnection;
import com.nhom6.auctionsystem_nhom6.NotificationManager;
import com.nhom6.auctionsystem_nhom6.NotificationPopup;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
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

    @FXML private Label      userNameLabel;
    @FXML private Label      userRoleLabel;
    @FXML private Label      userAvatarLabel;
    @FXML private Label      walletLabel;
    @FXML private TextField  searchField;
    @FXML private MenuButton profileMenuBtn;
    @FXML private MenuItem   menuItemHistory;
    @FXML private Button     bellButton;
    @FXML private Label      badgeLabel;
    @FXML private Button     btnSellerProducts;
    @FXML private Button     btnAdminProducts;
    @FXML private HBox       auctionCard;
    @FXML private VBox       emptyAuctionBox;
    @FXML private Label      productTitleLabel;
    @FXML private Label      productDescLabel;
    @FXML private Label      startPriceLabel;
    @FXML private Label      currentPriceLabel;
    @FXML private Label      minStepLabel;
    @FXML private Label      endTimeLabel;
    @FXML private Label      statusLabel;
    @FXML private Label      hoursLabel;
    @FXML private Label      minsLabel;
    @FXML private Label      secsLabel;
    @FXML private VBox       bidHistoryBox;
    @FXML private StackPane productImagePane;
    @FXML private ImageView productImageView;
    @FXML private Label     productImageIcon;
    @FXML private VBox       chatPanel;
    @FXML private VBox       chatMessagesBox;
    @FXML private TextField  chatInput;
    @FXML private ScrollPane chatScrollPane;
    @FXML private Label      onlineCountLabel;

    private AuctionSession session;
    private Timeline       countdownTimer;
    private LocalDateTime  sessionStartTime;
    private boolean        auctionEnded = false;

    // Timer định kỳ refresh online count khi offline
    private Timeline onlineRefreshTimer;

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
            showAlert("Loi dang nhap", "Khong tim thay user hien tai.");
            return;
        }

        userNameLabel.setText(user.getUsername());
        userRoleLabel.setText(user.getRole());
        String avatar = user.getUsername().length() >= 2
                ? user.getUsername().substring(0, 2).toUpperCase()
                : user.getUsername().toUpperCase();
        userAvatarLabel.setText(avatar);
        walletLabel.setText(String.format("d %,.0f",
                AppContext.getWalletBalance(user.getUsername())));

        applyRoleMenu(user);

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

        updateBadge();

        // Hien thi so nguoi online tu AppContext ngay lap tuc (it nhat la 1)
        setOnlineCount(AppContext.getOnlineUserCount());

        // Ket noi server trong background
        connectToServer(user);
    }

    // =========================================================
    // ROLE MENU + SIDEBAR
    // =========================================================
    private void applyRoleMenu(User user) {
        String  role     = user.getRole() == null ? "" : user.getRole().toUpperCase();
        boolean isSeller = "SELLER".equals(role);
        boolean isAdmin  = "ADMIN".equals(role);

        if (btnSellerProducts != null) {
            btnSellerProducts.setVisible(isSeller);
            btnSellerProducts.setManaged(isSeller);
        }
        if (btnAdminProducts != null) {
            btnAdminProducts.setVisible(isAdmin);
            btnAdminProducts.setManaged(isAdmin);
        }

        switch (role) {
            case "BIDDER" -> {
                if (menuItemHistory != null)
                    menuItemHistory.setText("Lich su mua hang");
            }
            case "SELLER" -> {
                if (menuItemHistory != null)
                    menuItemHistory.setText("Lich su ban hang");
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
            Thread t = new Thread(() -> {
                boolean ok = conn.connect(user.getUsername());
                conn.setOnSessionSynced(() -> {
                    if (session == null) {
                        session = resolveSession(user);
                        if (session != null) {
                            AppContext.setActiveSession(session);
                            sessionStartTime = LocalDateTime.now();
                            showAuctionCard(true);
                            loadAuctionInfo();
                            refreshBidHistory();
                            startCountdown();
                        }
                    }
                });
                Platform.runLater(() -> {
                    if (ok) {
                        // Server online: yeu cau so chinh xac
                        conn.sendJson("{\"action\":\"GET_ONLINE_COUNT\"}");
                        stopOnlineRefreshTimer();
                    } else {
                        // Offline: dung local count va refresh dinh ky
                        setOnlineCount(AppContext.getOnlineUserCount());
                        startOnlineRefreshTimer();
                    }
                });
            }, "WS-Connect-BG");
            t.setDaemon(true);
            t.start();
        } else {
            conn.sendJson("{\"action\":\"GET_ONLINE_COUNT\"}");
        }
    }

    // =========================================================
    // ONLINE COUNT
    // =========================================================

    /**
     * Cap nhat label online count.
     * Luon hien thi it nhat 1 (ban than user dang online).
     */
    private void setOnlineCount(int count) {
        int display = Math.max(1, count);
        if (onlineCountLabel != null)
            onlineCountLabel.setText("Online " + display);
    }

    /**
     * Timer moi 10 giay refresh count khi offline.
     * Tu dong dung khi server ket noi duoc.
     */
    private void startOnlineRefreshTimer() {
        stopOnlineRefreshTimer();
        onlineRefreshTimer = new Timeline(
                new KeyFrame(Duration.seconds(10), e -> {
                    ServerConnection conn = ServerConnection.getInstance();
                    if (conn.isConnected()) {
                        conn.sendJson("{\"action\":\"GET_ONLINE_COUNT\"}");
                        stopOnlineRefreshTimer();
                    } else {
                        Platform.runLater(() ->
                                setOnlineCount(AppContext.getOnlineUserCount()));
                    }
                })
        );
        onlineRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        onlineRefreshTimer.play();
    }

    private void stopOnlineRefreshTimer() {
        if (onlineRefreshTimer != null) {
            onlineRefreshTimer.stop();
            onlineRefreshTimer = null;
        }
    }

    // =========================================================
    // HANDLE SERVER MESSAGE
    // =========================================================
    private void handleServerMessage(String raw) {
        try {
            String type = extractJson(raw, "type");
            if (type == null || type.isEmpty()) return;

            switch (type) {

                case "ONLINE_COUNT" -> {
                    String countStr = extractJson(raw, "count");
                    Platform.runLater(() -> {
                        try {
                            int count = Integer.parseInt(countStr.trim());
                            // Dong bo ve AppContext de cac man hinh khac dung
                            AppContext.setServerOnlineCount(count);
                            setOnlineCount(count);
                        } catch (NumberFormatException ex) {
                            setOnlineCount(AppContext.getOnlineUserCount());
                        }
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
                        Platform.runLater(() -> {
                            if (session != null) {
                                currentPriceLabel.setText(
                                        formatVND(session.getCurrentPrice()));
                                updateCountdown();
                                refreshBidHistory();
                            }
                            addChatMessage("System",
                                    bidder + " dat gia " + formatVND(amt), false);

                            if (session != null) {
                                java.util.List<Bid> hist = session.getBidHistory();
                                System.out.println("[OUTBID DEBUG] hist.size=" + hist.size()
                                    + " | me=" + me.getUsername() + " | bidder=" + bidder);
                                boolean iWasTopBidder = hist.size() >= 2 &&
                                    hist.get(hist.size() - 2).getBidderId().equals(me.getUsername());
                                System.out.println("[OUTBID DEBUG] iWasTopBidder=" + iWasTopBidder);
                                if (iWasTopBidder && !bidder.equals(me.getUsername()))
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

                case "NOTIFY_ADMIN_NEW_PRODUCT" -> {
                    String productId    = extractJson(raw, "productId");
                    String productName  = extractJson(raw, "productName");
                    String sellerName   = extractJson(raw, "sellerName");
                    String category     = extractJson(raw, "category");
                    double startPrice   = parseDouble(extractJson(raw, "startPrice"));
                    String startTimeStr = extractJson(raw, "startTime");
                    String endTimeStr   = extractJson(raw, "endTime");

                    try {
                        LocalDateTime startTime =
                                LocalDateTime.parse(startTimeStr, ISO_DT);
                        LocalDateTime endTime =
                                LocalDateTime.parse(endTimeStr, ISO_DT);
                        boolean exists = AppContext.getAllProducts().stream()
                                .anyMatch(p -> p.id().equals(productId));
                        if (!exists) {
                            AppContext.addProduct(sellerName,
                                    new AppContext.ProductRecord(
                                            productId, productName, category,
                                            startPrice, startPrice, 0,
                                            "CHO DUYET", startTime, endTime, "—"));
                        }
                    } catch (Exception e) {
                        System.err.println("[MainController] Parse product: "
                                + e.getMessage());
                    }

                    Platform.runLater(() ->
                        pushNotification(
                                NotificationManager.NotifType.SYSTEM,
                                "San pham moi can duyet",
                                sellerName + " vua dang \""
                                + productName + "\" (" + category
                                + ") — Vao Quan ly san pham de duyet."));
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
                                                p.bidCount(), "DA DUYET",
                                                p.topBidder())));
                    }
                    Platform.runLater(() ->
                        pushNotification(
                                NotificationManager.NotifType.BID_PLACED,
                                "San pham duoc duyet!",
                                "\"" + productName + "\" da duoc Admin duyet."));
                }

                case "NOTIFY_BIDDER_SESSION_START" -> {
                    String sessionId  = extractJson(raw, "sessionId");
                    String itemName   = extractJson(raw, "itemName");
                    String sellerName = extractJson(raw, "sellerName");
                    double startPrice, minStep;
                    try {
                        startPrice = Double.parseDouble(
                                extractJson(raw, "startPrice"));
                        minStep    = Double.parseDouble(
                                extractJson(raw, "minStep"));
                    } catch (NumberFormatException e) {
                        startPrice = 0; minStep = 500_000;
                    }
                    LocalDateTime endTime;
                    try {
                        endTime = LocalDateTime.parse(
                                extractJson(raw, "endTime"), ISO_DT);
                    } catch (Exception e) {
                        endTime = LocalDateTime.now().plusHours(1);
                    }

                    final LocalDateTime finalEnd   = endTime;
                    final double        finalStart = startPrice;
                    final double        finalStep  = minStep;

                    Platform.runLater(() -> {
                        try {
                            AuctionSession newSession = new AuctionSession(
                                    sessionId, itemName, finalStart,
                                    finalStep, finalEnd);
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
                                    "Phien moi bat dau!",
                                    "\"" + itemName + "\" cua " + sellerName
                                    + ". Gia KD: "
                                    + String.format("d %,.0f", finalStart));
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }

                case "NOTIFY_BIDDER_SESSION_END" -> {
                    String itemName = extractJson(raw, "itemName");
                    Platform.runLater(() ->
                        pushNotification(
                                NotificationManager.NotifType.SYSTEM,
                                "Phien dau gia ket thuc",
                                "\"" + itemName + "\" da ket thuc."));
                }
            }
        } catch (Exception e) {
            System.err.println("[MainController] handleServerMessage: "
                    + e.getMessage());
        }
    }

    // =========================================================
    // JSON PARSER
    // =========================================================
    private String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start != -1) {
            start += search.length();
            int end = json.indexOf("\"", start);
            return end == -1 ? "" : json.substring(start, end);
        }
        String searchNum = "\"" + key + "\":";
        int s2 = json.indexOf(searchNum);
        if (s2 == -1) return "";
        s2 += searchNum.length();
        int e2 = json.indexOf(",", s2);
        if (e2 == -1) e2 = json.indexOf("}", s2);
        if (e2 == -1) return "";
        return json.substring(s2, e2).trim().replace("\"", "");
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    // =========================================================
    // AUCTION CARD
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

    private void loadAuctionInfo() {
        if (session == null) return;
        productTitleLabel.setText(session.getItemName());
        productDescLabel.setText(
            "San pham moi 100%, con nguyen seal, bao hanh 12 thang.");
        startPriceLabel.setText(formatVND(session.getStartingPrice()));
        currentPriceLabel.setText(formatVND(session.getCurrentPrice()));
        minStepLabel.setText(formatVND(session.getMinBidStep()));
        endTimeLabel.setText(session.getEndTime().format(DT_FMT));
        statusLabel.setText("  " + session.getStatus().name());
        statusLabel.getStyleClass().setAll("status-badge", "status-running");

        // ── Load ảnh sản phẩm ─────────────────────────────────
        String imgPath = ProductManagementController.imageMap.get(session.getSessionId());
        System.out.println("[DEBUG] sessionId=" + session.getSessionId()
            + " | imageMap=" + ProductManagementController.imageMap);
        if (imgPath != null) {
            try {
                Image img = new Image(new java.io.File(imgPath).toURI().toString());
                productImageView.setImage(img);
                productImageView.setVisible(true);
                productImageIcon.setVisible(false);
            } catch (Exception ignored) {
                productImageView.setVisible(false);
                productImageIcon.setVisible(true);
            }
        } else {
            productImageView.setVisible(false);
            productImageIcon.setVisible(true);
        }
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
            if (auctionEnded) return;
            auctionEnded = true;
            hoursLabel.setText("00");
            minsLabel.setText("00");
            secsLabel.setText("00");
            if (statusLabel != null) statusLabel.setText("KET THUC");
            countdownTimer.stop();
            saveSessionToHistory();
            handleAuctionEnd();
            pushNotification(NotificationManager.NotifType.SYSTEM,
                    "Phien dau gia ket thuc",
                    "Phien \"" + session.getItemName() + "\" da ket thuc.");
            return;
        }
        long total = java.time.Duration.between(now, end).getSeconds();
        hoursLabel.setText(String.format("%02d", total / 3600));
        minsLabel.setText(String.format("%02d",  (total % 3600) / 60));
        secsLabel.setText(String.format("%02d",  total % 60));
        if (total == 300)
            pushNotification(NotificationManager.NotifType.AUCTION_ENDING_SOON,
                    "Sap het gio!",
                    "Con 5 phut! Gia: " + formatVND(session.getCurrentPrice()));
    }

    private void saveSessionToHistory() {
        if (session == null) return;
        LocalDateTime start = sessionStartTime != null
                ? sessionStartTime : session.getEndTime().minusHours(1);
        AppContext.finalizeSession(session, start);
    }

    // =========================================================
    // PLACE BID
    // =========================================================
    @FXML
    private void handlePlaceBid() {
        if (session == null) {
            showAlert("Chưa có phiên", "Hiện chưa có phiên nào đang diễn ra.");
            return;
        }
        User user = AppContext.getCurrentUser();
        if (user == null) { showAlert("Lỗi", "Bạn chưa đăng nhập."); return; }

        double newPrice = session.getCurrentPrice() + session.getMinBidStep();
        double balance  = AppContext.getWalletBalance(user.getUsername());
        if (balance < newPrice) {
            showAlert("Số dư không đủ!",
                    "Cần: " + formatVND(newPrice)
                    + "\nSố dư: " + formatVND(balance));
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
                    user.getUsername() + " da dat gia " + formatVND(newPrice),
                    false);
            pushNotification(NotificationManager.NotifType.BID_PLACED,
                    "Dat gia thanh cong",
                    String.format("Ban da dat %s cho \"%s\"",
                            formatVND(newPrice), session.getItemName()));

            ServerConnection conn = ServerConnection.getInstance();
            if (conn.isConnected())
                conn.sendBid(user.getUsername(),
                        session.getSessionId(), newPrice);

        } catch (InvalidBidException e) {
            showAlert("Bid khong hop le", e.getMessage());
        } catch (AuctionClosedException e) {
            showAlert("Phien da dong", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Loi he thong", e.getMessage());
        }
    }

    // =========================================================
    // SEARCH
    // =========================================================
    @FXML
    private void handleSearch(javafx.scene.input.KeyEvent event) {
        if (searchField == null) return;
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) return;
        try { HelloApplication.showAuctionListByKeyword(keyword); }
        catch (Exception e) {
            try { HelloApplication.showAuctionListByCategory(keyword); }
            catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    // =========================================================
    // NOTIFICATION BELL
    // =========================================================
    @FXML
    private void handleBellClick() {
        if (notifPopup != null && notifPopup.isShowing()) {
            notifPopup.hide(); return;
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
    // NAVIGATION
    // =========================================================
    @FXML private void handleAuctionList() {
        try { HelloApplication.showAuctionListView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleLiveAuction() {
        if (AppContext.getActiveSession() == null) {
            List<AuctionSession> running = AppContext.getRunningSessions();
            if (!running.isEmpty()) AppContext.setActiveSession(running.get(0));
            else { showAlert("Thong bao", "Hien chua co phien nao dang dien ra."); return; }
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
        try { HelloApplication.showAuctionListByCategory("Dien tu"); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleCategoryMayAnh() {
        try { HelloApplication.showAuctionListByCategory("May anh"); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleCategoryLaptop() {
        try { HelloApplication.showAuctionListByCategory("Laptop"); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleCategoryDienThoai() {
        try { HelloApplication.showAuctionListByCategory("Dien thoai"); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleCategoryDongHo() {
        try { HelloApplication.showAuctionListByCategory("Dong ho"); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleCategoryXeCo() {
        try { HelloApplication.showAuctionListByCategory("Xe co"); }
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

    @FXML private void handleWallet() {
        try { HelloApplication.showWalletView(); }
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

    @FXML private void handleLogout() {
        if (countdownTimer != null) countdownTimer.stop();
        stopOnlineRefreshTimer();
        AppContext.markUserOffline(AppContext.getCurrentUser() != null
                ? AppContext.getCurrentUser().getUsername() : "");
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
            Label empty = new Label("Chua co luot dat gia nao.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
            bidHistoryBox.getChildren().add(empty);
            return;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            Bid b = history.get(i);
            HBox row = new HBox(12);
            row.getStyleClass().add("bid-row");
            if (i == history.size() - 1) row.getStyleClass().add("bid-row-top");
            Label name = new Label(
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
        var    winnerBid  = history.get(history.size() - 1);
        String winner     = winnerBid.getBidderId();
        double finalPrice = winnerBid.getAmount();
        if (winner.equals(me.getUsername())) {
            AppContext.addHistory(me.getUsername(),
                    new AppContext.HistoryRecord(
                            "BID-" + UUID.randomUUID().toString()
                                    .substring(0, 6).toUpperCase(),
                            session.getItemName(), finalPrice,
                            AppContext.getSessionSeller(session.getSessionId()),
                            "CHO XU LY", true, LocalDateTime.now()));
            pushNotification(NotificationManager.NotifType.SYSTEM,
                    "Ban da thang!",
                    "Vao Vi & Giao dich -> Thanh toan de hoan tat.");
        } else {
            boolean participated = history.stream()
                    .anyMatch(b -> b.getBidderId().equals(me.getUsername()));
            if (participated)
                pushNotification(NotificationManager.NotifType.SYSTEM,
                        "Ban da thua",
                        "Nguoi thang: " + winner
                        + " (" + formatVND(finalPrice) + ")");
        }
    }

    // =========================================================
    // UTIL
    // =========================================================
    private String formatVND(double amount) {
        return String.format("d %,.0f", amount);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("""
        -fx-background-color: #1e293b;
        -fx-border-color: #334155;
        -fx-border-width: 1;
        -fx-border-radius: 8;
        -fx-background-radius: 8;
    """);
        dialogPane.lookup(".content.label").setStyle("""
        -fx-text-fill: #e2e8f0;
        -fx-font-size: 14px;
    """);
        dialogPane.lookupButton(ButtonType.OK).setStyle("""
        -fx-background-color: #2563eb;
        -fx-text-fill: white;
        -fx-font-weight: bold;
        -fx-background-radius: 6;
        -fx-padding: 6 20 6 20;
        -fx-cursor: hand;
    """);

        alert.setGraphic(null);

        alert.showAndWait();
    }
}
