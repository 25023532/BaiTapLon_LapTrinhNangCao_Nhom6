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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Popup;
import javafx.util.Duration;

import org.example.auction.AuctionSession;
import org.example.auction.Bid;
import org.example.exception.AuctionClosedException;
import org.example.exception.InvalidBidException;
import org.example.user.User;

import java.io.File;
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
    @FXML private Label      sessionCountLabel;

    private List<AuctionSession> runningSessions;
    private int            currentIndex = 0;
    private AuctionSession session;
    private Timeline       countdownTimer;
    private LocalDateTime  sessionStartTime;
    private boolean        auctionEnded = false;

    private Timeline onlineRefreshTimer;
    private final NotificationManager notifManager = new NotificationManager();
    private       Popup               notifPopup;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @FXML
    public void initialize() {
        User user = AppContext.getCurrentUser();
        if (user == null) return;

        userNameLabel.setText(user.getUsername());
        userRoleLabel.setText(user.getRole());
        String avatar = user.getUsername().length() >= 2
            ? user.getUsername().substring(0, 2).toUpperCase()
            : user.getUsername().toUpperCase();
        userAvatarLabel.setText(avatar);
        walletLabel.setText(String.format("₫ %,.0f", AppContext.getWalletBalance(user.getUsername())));

        applyRoleMenu(user);
        refreshSessions();
        updateBadge();
        setOnlineCount(AppContext.getOnlineUserCount());
        connectToServer(user);
    }

    private void applyRoleMenu(User user) {
        String role = user.getRole() == null ? "" : user.getRole().toUpperCase();
        boolean isSeller = "SELLER".equals(role);
        boolean isAdmin  = "ADMIN".equals(role);

        if (btnSellerProducts != null) { btnSellerProducts.setVisible(isSeller); btnSellerProducts.setManaged(isSeller); }
        if (btnAdminProducts != null) { btnAdminProducts.setVisible(isAdmin); btnAdminProducts.setManaged(isAdmin); }

        if ("BIDDER".equals(role)) { if (menuItemHistory != null) menuItemHistory.setText("Lịch sử mua hàng"); }
        else if ("SELLER".equals(role)) { if (menuItemHistory != null) menuItemHistory.setText("Lịch sử bán hàng"); }
    }

    private void refreshSessions() {
        runningSessions = AppContext.getRunningSessions();
        if (runningSessions != null && !runningSessions.isEmpty()) {
            if (currentIndex >= runningSessions.size()) currentIndex = 0;
            session = runningSessions.get(currentIndex);
            AppContext.setActiveSession(session);
            sessionStartTime = LocalDateTime.now();
            auctionEnded = false;
            showAuctionCard(true);
            loadAuctionInfo();
            refreshBidHistory();
            startCountdown();
            if (sessionCountLabel != null) sessionCountLabel.setText(String.format("Phiên %d / %d", currentIndex + 1, runningSessions.size()));
        } else {
            session = null;
            showAuctionCard(false);
            refreshBidHistory();
            if (sessionCountLabel != null) sessionCountLabel.setText("0 phiên");
        }
    }

    @FXML private void handlePrevSession() { if (runningSessions != null && runningSessions.size() > 1) { currentIndex = (currentIndex - 1 + runningSessions.size()) % runningSessions.size(); refreshSessions(); } }
    @FXML private void handleNextSession() { if (runningSessions != null && runningSessions.size() > 1) { currentIndex = (currentIndex + 1) % runningSessions.size(); refreshSessions(); } }

    private void connectToServer(User user) {
        ServerConnection conn = ServerConnection.getInstance();
        conn.setListener(this::handleServerMessage);
        if (!conn.isConnected()) {
            Thread t = new Thread(() -> {
                boolean ok = conn.connect(user.getUsername());
                conn.setOnSessionSynced(() -> Platform.runLater(this::refreshSessions));
                Platform.runLater(() -> {
                    if (ok) { conn.sendJson("{\"action\":\"GET_ONLINE_COUNT\"}"); stopOnlineRefreshTimer(); }
                    else { setOnlineCount(AppContext.getOnlineUserCount()); startOnlineRefreshTimer(); }
                });
            });
            t.setDaemon(true); t.start();
        } else { conn.sendJson("{\"action\":\"GET_ONLINE_COUNT\"}"); }
    }

    private void setOnlineCount(int count) { if (onlineCountLabel != null) onlineCountLabel.setText("Online " + Math.max(1, count)); }
    private void startOnlineRefreshTimer() {
        stopOnlineRefreshTimer();
        onlineRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(10), e -> {
            if (ServerConnection.getInstance().isConnected()) { ServerConnection.getInstance().sendJson("{\"action\":\"GET_ONLINE_COUNT\"}"); stopOnlineRefreshTimer(); }
            else { Platform.runLater(() -> setOnlineCount(AppContext.getOnlineUserCount())); }
        }));
        onlineRefreshTimer.setCycleCount(Timeline.INDEFINITE); onlineRefreshTimer.play();
    }
    private void stopOnlineRefreshTimer() { if (onlineRefreshTimer != null) { onlineRefreshTimer.stop(); onlineRefreshTimer = null; } }

    private void handleServerMessage(String raw) {
        try {
            String type = extractJson(raw, "type");
            if (type == null) return;
            User me = AppContext.getCurrentUser();

            switch (type) {
                case "ONLINE_COUNT" -> {
                    int c = Integer.parseInt(extractJson(raw, "count"));
                    AppContext.setServerOnlineCount(c);
                    Platform.runLater(() -> setOnlineCount(c));
                }
                case "CHAT" -> {
                    String s = extractJson(raw, "username"), m = extractJson(raw, "message");
                    if (me != null && !s.equals(me.getUsername()))
                        Platform.runLater(() -> addChatMessage(s, m, false));
                }
                case "NEW_BID" -> {
                    String b = extractJson(raw, "username");
                    double a = Double.parseDouble(extractJson(raw, "amount"));
                    String sid = extractJson(raw, "sessionId");
                    Platform.runLater(() -> {
                        if (session != null && sid.equals(session.getSessionId())) {
                            currentPriceLabel.setText(formatVND(session.getCurrentPrice()));
                            refreshBidHistory();
                        }
                        addChatMessage("System", b + " đặt giá " + formatVND(a), false);
                    });
                }
                case "NOTIFY_BIDDER_SESSION_START" -> Platform.runLater(this::refreshSessions);
            }
        } catch (Exception ignored) {}
    }

    private String extractJson(String json, String key) {
        String s = "\"" + key + "\":\""; int i = json.indexOf(s);
        if (i != -1) { i += s.length(); int e = json.indexOf("\"", i); return json.substring(i, e); }
        s = "\"" + key + "\":"; i = json.indexOf(s);
        if (i == -1) return null; i += s.length(); int e = json.indexOf(",", i); if (e == -1) e = json.indexOf("}", i);
        return json.substring(i, e).trim().replace("\"", "");
    }

    private void showAuctionCard(boolean has) { if (auctionCard != null) { auctionCard.setVisible(has); auctionCard.setManaged(has); } if (emptyAuctionBox != null) { emptyAuctionBox.setVisible(!has); emptyAuctionBox.setManaged(!has); } }

    private void loadAuctionInfo() {
        if (session == null) return;
        productTitleLabel.setText(session.getItemName());

        // Cố gắng lấy mô tả từ ProductManagementController hoặc AppContext
        String desc = ProductManagementController.descMap.get(session.getSessionId());
        if (desc == null || desc.isBlank()) {
            desc = AppContext.getAllProducts().stream()
                    .filter(p -> p.id().equals(session.getSessionId()))
                    .map(p -> "Sản phẩm thuộc danh mục " + p.category() + ". Giá khởi điểm: " + formatVND(p.startPrice()))
                    .findFirst().orElse("Sản phẩm đấu giá chất lượng cao.");
        }
        productDescLabel.setText(desc);

        startPriceLabel.setText(formatVND(session.getStartingPrice()));
        currentPriceLabel.setText(formatVND(session.getCurrentPrice()));
        minStepLabel.setText(formatVND(session.getMinBidStep()));
        endTimeLabel.setText(session.getEndTime().format(DT_FMT));
        statusLabel.setText("  " + session.getStatus().name());

        String id = session.getSessionId();
        File imgF = findProductImage(id);

        if (imgF != null && imgF.exists()) {
            try {
                productImageView.setImage(new Image(imgF.toURI().toString(), 200, 140, true, true));
                productImageView.setVisible(true);
                productImageIcon.setVisible(false);
            } catch (Exception e) {
                productImageView.setVisible(false);
                productImageIcon.setVisible(true);
            }
        } else {
            productImageView.setVisible(false);
            productImageIcon.setVisible(true);
        }
    }

    private File findProductImage(String id) {
        // 1. Kiểm tra trong imageMap của ProductManagementController
        String path = ProductManagementController.imageMap.get(id);
        if (path != null) {
            File f = new File(path);
            if (f.exists()) return f;
        }

        // 2. Kiểm tra trong thư mục product_images relative
        for (String ext : new String[]{".jpg", ".png", ".jpeg", ".webp", ".gif"}) {
            File f = new File("product_images/" + id + ext);
            if (f.exists()) return f;
        }

        // 3. Kiểm tra trong thư mục product_images tuyệt đối (D:/123/product_images/)
        String absDir = "D:/123/product_images/";
        for (String ext : new String[]{".jpg", ".png", ".jpeg", ".webp", ".gif"}) {
            File f = new File(absDir + id + ext);
            if (f.exists()) return f;
        }

        return null;
    }

    private void startCountdown() { if (countdownTimer != null) countdownTimer.stop(); countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdown())); countdownTimer.setCycleCount(Timeline.INDEFINITE); countdownTimer.play(); }
    private void updateCountdown() {
        if (session == null) return;
        LocalDateTime now = LocalDateTime.now(), end = session.getEndTime();
        if (now.isAfter(end)) {
            if (auctionEnded) return; auctionEnded = true;
            hoursLabel.setText("00"); minsLabel.setText("00"); secsLabel.setText("00");
            statusLabel.setText("KẾT THÚC"); countdownTimer.stop();
            AppContext.finalizeSession(session, sessionStartTime);
            handleAuctionEnd(); return;
        }
        long t = java.time.Duration.between(now, end).getSeconds();
        hoursLabel.setText(String.format("%02d", t/3600)); minsLabel.setText(String.format("%02d", (t%3600)/60)); secsLabel.setText(String.format("%02d", t%60));
    }

    @FXML private void handlePlaceBid() {
        if (session == null) return;
        User u = AppContext.getCurrentUser();
        double p = session.getCurrentPrice() + session.getMinBidStep();
        if (AppContext.getWalletBalance(u.getUsername()) < p) { showAlert("Số dư không đủ", "Cần " + formatVND(p)); return; }
        try {
            session.placeBid(new Bid(UUID.randomUUID().toString(), u.getUsername(), p));
            currentPriceLabel.setText(formatVND(session.getCurrentPrice()));
            refreshBidHistory();
            if (ServerConnection.getInstance().isConnected()) ServerConnection.getInstance().sendBid(u.getUsername(), session.getSessionId(), p);
        } catch (Exception e) { showAlert("Lỗi", e.getMessage()); }
    }

    @FXML private void handleSearch(javafx.scene.input.KeyEvent e) {
        String k = searchField.getText().trim(); if (k.isEmpty()) return;
        try { HelloApplication.showAuctionListByKeyword(k); } catch (Exception ex) {}
    }

    @FXML private void handleBellClick() {
        if (notifPopup != null && notifPopup.isShowing()) { notifPopup.hide(); return; }
        notifManager.markAllRead(); updateBadge();
        notifPopup = NotificationPopup.show(bellButton, notifManager.getAll());
    }

    public void pushNotification(NotificationManager.NotifType t, String ti, String b) { Platform.runLater(() -> { notifManager.add(new NotificationManager.NotifItem(t, ti, b, LocalDateTime.now())); updateBadge(); }); }
    private void updateBadge() { if (badgeLabel == null) return; int u = notifManager.countUnread(); badgeLabel.setText(u > 9 ? "9+" : String.valueOf(u)); badgeLabel.setVisible(u > 0); badgeLabel.setManaged(u > 0); }

    @FXML private void handleToggleChat() { boolean v = !chatPanel.isVisible(); chatPanel.setVisible(v); chatPanel.setManaged(v); if (v) Platform.runLater(() -> chatScrollPane.setVvalue(1.0)); }
    @FXML private void handleSendChat() {
        String m = chatInput.getText().trim(); if (m.isEmpty()) return;
        User u = AppContext.getCurrentUser(); addChatMessage(u.getUsername(), m, "SELLER".equalsIgnoreCase(u.getRole())); chatInput.clear();
        if (ServerConnection.getInstance().isConnected()) ServerConnection.getInstance().sendChat(u.getUsername(), m);
    }

    @FXML private void handleAuctionList() { try { HelloApplication.showAuctionListView(); } catch (Exception e) {} }
    @FXML private void handleLiveAuction() { try { HelloApplication.showLiveAuctionView(); } catch (Exception e) {} }
    @FXML private void handleCategoryDienTu()    { try { HelloApplication.showAuctionListByCategory("Điện tử");    } catch (Exception e) {} }
    @FXML private void handleCategoryMayAnh()    { try { HelloApplication.showAuctionListByCategory("Máy ảnh");    } catch (Exception e) {} }
    @FXML private void handleCategoryLaptop()    { try { HelloApplication.showAuctionListByCategory("Laptop");     } catch (Exception e) {} }
    @FXML private void handleCategoryDienThoai() { try { HelloApplication.showAuctionListByCategory("Điện thoại"); } catch (Exception e) {} }
    @FXML private void handleCategoryDongHo()    { try { HelloApplication.showAuctionListByCategory("Đồng hồ");   } catch (Exception e) {} }
    @FXML private void handleCategoryXeCo()      { try { HelloApplication.showAuctionListByCategory("Xe cộ");      } catch (Exception e) {} }
    @FXML private void handleSellerProducts() { try { HelloApplication.showMyProductsView(); } catch (Exception e) {} }
    @FXML private void handleAdminProducts() { try { HelloApplication.showProductManagementView(); } catch (Exception e) {} }
    @FXML private void handleProfile() { try { HelloApplication.showProfileView(); } catch (Exception e) {} }
    @FXML private void handleHistory() { try { HelloApplication.showHistoryView(); } catch (Exception e) {} }
    @FXML private void handleWallet() { try { HelloApplication.showWalletView(); } catch (Exception e) {} }
    @FXML private void handleRating() { try { HelloApplication.showRatingView(); } catch (Exception e) {} }
    @FXML private void handleHelp() { try { HelloApplication.showHelpView(); } catch (Exception e) {} }
    @FXML private void handleLogout() {
        if (countdownTimer != null) countdownTimer.stop(); stopOnlineRefreshTimer();
        User u = AppContext.getCurrentUser();
        if (u != null) AppContext.markUserOffline(u.getUsername());
        ServerConnection.getInstance().disconnect(); AppContext.logout();
        try { HelloApplication.showLoginView(); } catch (Exception e) {}
    }

    private void refreshBidHistory() {
        bidHistoryBox.getChildren().clear();
        if (session == null) {
            Label e = new Label("Chọn một phiên để xem lịch sử.");
            e.setStyle("-fx-text-fill: #64748b; -fx-padding: 20; -fx-font-style: italic;");
            bidHistoryBox.getChildren().add(e);
            return;
        }

        var h = session.getBidHistory();
        if (h.isEmpty()) {
            Label e = new Label("Chưa có lượt đặt giá nào.");
            e.setStyle("-fx-text-fill: #64748b; -fx-padding: 20;");
            bidHistoryBox.getChildren().add(e);
            return;
        }

        for (int i = h.size() - 1; i >= 0; i--) {
            Bid b = h.get(i);
            HBox r = new HBox(12);
            r.getStyleClass().add("bid-row");
            if (i == h.size() - 1) r.getStyleClass().add("bid-row-top");
            r.setAlignment(Pos.CENTER_LEFT);
            r.setPadding(new Insets(10, 14, 10, 14));

            Label n = new Label((i == h.size() - 1 ? "👑 " : "") + b.getBidderId());
            n.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (i == h.size() - 1 ? "#FFFFFF" : "#1F0C40") + ";");

            Label a = new Label(formatVND(b.getAmount()));
            a.getStyleClass().add("bid-amount");
            if (i == h.size() - 1) a.setStyle("-fx-text-fill: #4ade80;");

            Label t = new Label(b.getTimestamp().format(TIME_FMT));
            t.getStyleClass().add("bid-time");
            if (i == h.size() - 1) t.setStyle("-fx-text-fill: #B0A0D0;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            r.getChildren().addAll(n, spacer, a, t);
            bidHistoryBox.getChildren().add(r);
        }
    }

    private void addChatMessage(String s, String m, boolean isS) {
        VBox b = new VBox(2); b.getStyleClass().add(isS ? "chat-bubble-seller" : "chat-bubble-buyer");
        Label sl = new Label(s); sl.getStyleClass().add("chat-sender");
        Label ml = new Label(m); ml.setWrapText(true); ml.getStyleClass().add("chat-message");
        b.getChildren().addAll(sl, ml); chatMessagesBox.getChildren().add(b);
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    private void handleAuctionEnd() {
        if (session == null) return; User me = AppContext.getCurrentUser(); var h = session.getBidHistory(); if (h.isEmpty()) return;
        var wb = h.get(h.size() - 1); String w = wb.getBidderId(); double f = wb.getAmount();
        if (w.equals(me.getUsername())) { AppContext.addHistory(me.getUsername(), new AppContext.HistoryRecord("BID-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(), session.getItemName(), f, AppContext.getSessionSeller(session.getSessionId()), "CHỜ XỬ LÝ", true, LocalDateTime.now())); }
    }

    private String formatVND(double a) { return String.format("₫ %,.0f", a); }
    private void showAlert(String t, String m) { Alert a = new Alert(Alert.AlertType.WARNING); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
}
