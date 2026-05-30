package com.nhom6.auctionsystem_nhom6.controller;

import com.nhom6.auctionsystem_nhom6.AppContext;
import com.nhom6.auctionsystem_nhom6.HelloApplication;
import com.nhom6.auctionsystem_nhom6.ServerConnection;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
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

public class LiveAuctionController {

    // ── Header ────────────────────────────────────────────────
    @FXML private Label      liveIndicator;
    @FXML private Label      participantsLabel;

    // ── Left session list ─────────────────────────────────────
    @FXML private VBox       liveSessionListBox;
    @FXML private TextField  liveSearchField;

    // ── Center auction card ───────────────────────────────────
    @FXML private Label      liveTitleLabel;
    @FXML private Label      liveDescLabel;
    @FXML private Label      liveStatusLabel;
    @FXML private Label      liveStartPrice;
    @FXML private Label      liveCurrentPrice;
    @FXML private Label      liveMinStep;
    @FXML private Label      liveLeaderLabel;
    @FXML private Label      liveHours;
    @FXML private Label      liveMins;
    @FXML private Label      liveSecs;
    @FXML private ImageView  liveProductImage;
    @FXML private TextField  customBidField;
    @FXML private Button     quickBid1Btn;
    @FXML private Button     quickBid2Btn;
    @FXML private Button     quickBid3Btn;

    // ── Anti-sniping ──────────────────────────────────────────
    @FXML private Label      extensionNoticeLabel;
    private Timeline         extensionNoticeTimer;

    // ── Bid history ───────────────────────────────────────────
    @FXML private VBox       liveBidHistoryBox;
    @FXML private ScrollPane bidScrollPane;
    @FXML private Label      bidCountLabel;

    // ── Chart ──────────────────────────────────────────────────
    @FXML private LineChart<String, Number> bidPriceChart;
    private XYChart.Series<String, Number> priceSeries;

    // ── Chat ──────────────────────────────────────────────────
    @FXML private VBox       liveChatBox;
    @FXML private TextField  liveChatInput;
    @FXML private ScrollPane liveChatScrollPane;
    @FXML private Label      onlineLabel;

    // ── State ─────────────────────────────────────────────────
    private AuctionSession currentSession;
    private Timeline       countdownTimer;
    private Timeline       uiRefreshTimer;
    private boolean        sessionSelected = false;
    private boolean        auctionEnded    = false;
    private int            lastBidCount    = 0;
    private int            currentOnlineCount = 0;

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        if (extensionNoticeLabel != null) {
            extensionNoticeLabel.setVisible(false);
            extensionNoticeLabel.setManaged(false);
        }

        onlineLabel.setText("● 0 online");
        participantsLabel.setText("👥 0 người tham gia");

        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá hiện tại");
        if (bidPriceChart != null) {
            bidPriceChart.getData().add(priceSeries);
            bidPriceChart.setAnimated(false);
        }

        loadLiveSessionList();
        setNoSessionState();
        connectToServer();

        AuctionSession active = AppContext.getActiveSession();
        if (active != null) {
            selectSession(active);
        }
    }

    // =========================================================
    // SERVER CONNECTION
    // =========================================================
    private void connectToServer() {
        ServerConnection conn = ServerConnection.getInstance();
        conn.addListener(this::handleServerMessage);  // addListener, không ghi đè MainController

        User user = AppContext.getCurrentUser();
        if (!conn.isConnected()) {
            conn.connect(user.getUsername());
        }

        conn.sendJson("{\"action\":\"GET_ONLINE_COUNT\"}");
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
                        Platform.runLater(() -> addChatMsg(sender, message, false));
                    }
                }

                case "ONLINE_COUNT" -> {
                    String countStr = extractJson(raw, "count");
                    Platform.runLater(() -> {
                        try {
                            currentOnlineCount = Integer.parseInt(countStr);
                        } catch (NumberFormatException ignored) {
                            currentOnlineCount = 0;
                        }
                        updateOnlineLabels(currentOnlineCount);
                    });
                }

                case "NEW_BID" -> {
                    String bidder    = extractJson(raw, "username");
                    String sessionId = extractJson(raw, "sessionId");
                    String amountStr = extractJson(raw, "amount");
                    User me = AppContext.getCurrentUser();

                    Platform.runLater(() -> {
                        try {
                            double amt = Double.parseDouble(amountStr);

                            if (currentSession != null
                                && currentSession.getSessionId().equals(sessionId)) {

                                liveCurrentPrice.setText(
                                    formatVND(currentSession.getCurrentPrice()));
                                liveLeaderLabel.setText("👑 " + bidder
                                    + " (" + formatVND(amt) + ")");
                                updateQuickBidLabels();
                                refreshBidHistory();
                                lastBidCount = currentSession.getBidHistory().size();
                                bidCountLabel.setText(
                                    currentSession.getBidHistory().size() + " lượt");

                                if (me == null || !bidder.equals(me.getUsername())) {
                                    addChatMsg("System",
                                        "🔨 " + bidder + " đặt giá "
                                            + formatVND(amt), false);
                                }
                            }

                            // Global Notification logic
                            if (me != null && !bidder.equals(me.getUsername())) {
                                AuctionSession target = AppContext.getGlobalSessions().stream()
                                    .filter(s -> s.getSessionId().equals(sessionId))
                                    .findFirst().orElse(null);
                                if (target != null) {
                                    boolean iBidBefore = target.getBidHistory().stream()
                                        .anyMatch(b -> b.getBidderId().equals(me.getUsername()));
                                    if (iBidBefore) {
                                        com.nhom6.auctionsystem_nhom6.NotificationManager.getInstance().add(new com.nhom6.auctionsystem_nhom6.NotificationManager.NotifItem(
                                            com.nhom6.auctionsystem_nhom6.NotificationManager.NotifType.OUTBID,
                                            "⚠️ BỊ VƯỢT GIÁ!",
                                            "Ai đó vừa đặt giá cao hơn bạn tại '" + target.getItemName() + "'. Giá mới: " + formatVND(amt),
                                            LocalDateTime.now()
                                        ));
                                    }
                                }
                            }

                            loadLiveSessionList();

                        } catch (NumberFormatException ignored) {}
                    });
                }

                case "NOTIFY_BIDDER_SESSION_START" -> {
                    String sessionId = extractJson(raw, "sessionId");
                    String itemName  = extractJson(raw, "itemName");
                    Platform.runLater(() -> {
                        loadLiveSessionList();
                        addChatMsg("System",
                            "🟢 Phiên mới bắt đầu: " + itemName, false);
                    });
                }

                case "NOTIFY_BIDDER_SESSION_END", "SESSION_END" -> {
                    String sessionId = extractJson(raw, "sessionId");
                    String itemName  = extractJson(raw, "itemName");
                    String winner    = extractJson(raw, "winner");
                    String priceStr  = extractJson(raw, "finalPrice");
                    User me = AppContext.getCurrentUser();

                    Platform.runLater(() -> {
                        addChatMsg("System",
                            "🔴 Phiên kết thúc: " + itemName, false);

                        // Global Notification logic
                        if (me != null) {
                            try {
                                double finalPrice = priceStr.isEmpty() ? 0 : Double.parseDouble(priceStr);
                                if (me.getUsername().equals(winner)) {
                                    com.nhom6.auctionsystem_nhom6.NotificationManager.getInstance().add(new com.nhom6.auctionsystem_nhom6.NotificationManager.NotifItem(
                                        com.nhom6.auctionsystem_nhom6.NotificationManager.NotifType.BID_WON,
                                        "🏆 CHÚC MỪNG!",
                                        "Bạn đã thắng phiên '" + itemName + "' với giá " + formatVND(finalPrice),
                                        LocalDateTime.now()
                                    ));
                                } else {
                                    AuctionSession target = AppContext.getGlobalSessions().stream()
                                        .filter(s -> s.getSessionId().equals(sessionId))
                                        .findFirst().orElse(null);
                                    boolean iParticipated = target != null && target.getBidHistory().stream()
                                        .anyMatch(b -> b.getBidderId().equals(me.getUsername()));
                                    if (iParticipated) {
                                        com.nhom6.auctionsystem_nhom6.NotificationManager.getInstance().add(new com.nhom6.auctionsystem_nhom6.NotificationManager.NotifItem(
                                            com.nhom6.auctionsystem_nhom6.NotificationManager.NotifType.BID_LOST,
                                            "😞 KẾT THÚC",
                                            "Phiên '" + itemName + "' đã kết thúc. Rất tiếc bạn không thắng lần này.",
                                            LocalDateTime.now()
                                        ));
                                    }
                                }
                            } catch (Exception ignored) {}
                        }

                        if (currentSession != null
                            && currentSession.getSessionId().equals(sessionId)) {
                            liveStatusLabel.setText("● KẾT THÚC");
                            liveStatusLabel.setStyle("-fx-text-fill: #ef4444;");
                            if (countdownTimer != null) countdownTimer.stop();
                            handleAuctionEnd();
                        }

                        loadLiveSessionList();
                    });
                }

                case "SYSTEM" -> {
                    String message = extractJson(raw, "message");
                    Platform.runLater(() -> addChatMsg("System", message, false));
                }

                case "BID_ERROR" -> {
                    String errorMsg = extractJson(raw, "message");
                    Platform.runLater(() -> {
                        showAlert("Đặt giá thất bại", errorMsg);
                        addChatMsg("System", "❌ " + errorMsg, false);
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("handleServerMessage lỗi: " + e.getMessage());
        }
    }

    private void updateOnlineLabels(int count) {
        if (onlineLabel != null)
            onlineLabel.setText("● " + count + " online");
        if (participantsLabel != null)
            participantsLabel.setText("👥 " + count + " người tham gia");
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
    // LEFT PANEL
    // =========================================================
    private void loadLiveSessionList() {
        liveSessionListBox.getChildren().clear();

        List<AuctionSession> runningSessions = AppContext.getRunningSessions();

        if (runningSessions.isEmpty()) {
            VBox emptyBox = new VBox(8);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(30, 10, 30, 10));
            Label icon = new Label("📭");
            icon.setStyle("-fx-font-size: 32px;");
            Label msg = new Label("Chưa có phiên nào đang diễn ra.");
            msg.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; "
                + "-fx-font-weight: bold; -fx-text-alignment: center;");
            Label hint = new Label("Seller cần bắt đầu phiên để hiển thị ở đây.");
            hint.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; "
                + "-fx-text-alignment: center;");
            emptyBox.getChildren().addAll(icon, msg, hint);
            liveSessionListBox.getChildren().add(emptyBox);
            return;
        }

        for (AuctionSession s : runningSessions) {
            liveSessionListBox.getChildren().add(buildSessionItem(s));
        }
    }

    private VBox buildSessionItem(AuctionSession session) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10, 12, 10, 12));

        boolean isSelected = currentSession != null
            && currentSession.getSessionId().equals(session.getSessionId());

        card.setStyle(
            "-fx-background-color: " + (isSelected ? "#1e3a5f" : "#1e293b") + ";"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: " + (isSelected ? "#2563eb" : "#334155") + ";"
                + "-fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;");

        Label nameLabel = new Label(session.getItemName());
        nameLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px; "
            + "-fx-font-weight: bold;");
        nameLabel.setWrapText(true);

        String sellerName = AppContext.getSessionSeller(session.getSessionId());

        HBox metaRow1 = new HBox(8);
        Label statusL = new Label("🟢 LIVE");
        statusL.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981;");
        Label sellerL = new Label("👤 " + sellerName);
        sellerL.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        metaRow1.getChildren().addAll(statusL, sellerL);

        HBox metaRow2 = new HBox(8);
        Label priceL = new Label(formatVND(session.getCurrentPrice()));
        priceL.setStyle("-fx-font-size: 12px; -fx-text-fill: #38bdf8; "
            + "-fx-font-weight: bold;");
        Label bidsL = new Label("🔨 " + session.getBidHistory().size() + " lượt");
        bidsL.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        metaRow2.getChildren().addAll(priceL, bidsL);

        card.getChildren().addAll(nameLabel, metaRow1, metaRow2);

        card.setOnMouseClicked(e -> selectSession(session));
        card.setOnMouseEntered(e -> {
            if (!isSelected)
                card.setStyle(card.getStyle()
                    .replace("#1e293b", "#243447")
                    .replace("#334155", "#475569"));
        });
        card.setOnMouseExited(e -> {
            if (!isSelected)
                card.setStyle(card.getStyle()
                    .replace("#243447", "#1e293b")
                    .replace("#475569", "#334155"));
        });

        return card;
    }

    @FXML
    private void handleLiveSearch() {
        String kw = liveSearchField.getText().trim().toLowerCase();
        liveSessionListBox.getChildren().forEach(node -> {
            if (node instanceof VBox vbox) {
                boolean match = vbox.getChildren().stream()
                    .anyMatch(n -> n instanceof Label l
                        && l.getText().toLowerCase().contains(kw));
                vbox.setVisible(kw.isEmpty() || match);
                vbox.setManaged(kw.isEmpty() || match);
            }
        });
    }

    // =========================================================
    // SELECT SESSION
    // =========================================================
    private void selectSession(AuctionSession session) {
        if (countdownTimer != null) countdownTimer.stop();
        if (uiRefreshTimer  != null) uiRefreshTimer.stop();

        currentSession  = session;
        sessionSelected = true;
        lastBidCount    = session.getBidHistory().size();

        AppContext.setActiveSession(session);

        liveTitleLabel.setText(session.getItemName());
        String desc = ProductManagementController.descMap.get(session.getSessionId());
        if (desc == null || desc.isBlank()) {
            desc = "Sản phẩm đấu giá trực tiếp chất lượng cao.";
        }
        liveDescLabel.setText(desc);

        liveStartPrice.setText(formatVND(session.getStartingPrice()));
        liveCurrentPrice.setText(formatVND(session.getCurrentPrice()));
        liveMinStep.setText(formatVND(session.getMinBidStep()));
        liveStatusLabel.setText("● " + session.getStatus().name());
        liveStatusLabel.getStyleClass().setAll("status-badge", "status-running");

        // Load image
        loadProductImage(session.getSessionId());

        updateLeaderLabel();
        updateQuickBidLabels();
        refreshBidHistory();
        bidCountLabel.setText(session.getBidHistory().size() + " lượt");

        loadLiveSessionList();
        startCountdown();
        startUiRefresh();

        updateOnlineLabels(currentOnlineCount);

        ServerConnection conn = ServerConnection.getInstance();
        if (conn.isConnected())
            conn.sendJson("{\"action\":\"GET_ONLINE_COUNT\"}");

        addChatMsg("System",
            "✅ Đã tham gia phiên: " + session.getItemName(), false);
    }

    private void loadProductImage(String productId) {
        if (liveProductImage == null) return;

        File imageFile = null;
        String path = ProductManagementController.imageMap.get(productId);
        if (path != null) imageFile = new File(path);

        if (imageFile == null || !imageFile.exists()) {
            // Thử tìm ảnh trong thư mục product_images
            String[] extensions = {".jpg", ".png", ".jpeg", ".webp", ".gif"};
            for (String ext : extensions) {
                File f = new File("product_images/" + productId + ext);
                if (f.exists()) {
                    imageFile = f;
                    break;
                }
            }
        }

        if (imageFile != null && imageFile.exists()) {
            try {
                Image img = new Image(imageFile.toURI().toString(), 150, 150, true, true);
                liveProductImage.setImage(img);
                liveProductImage.setVisible(true);
            } catch (Exception e) {
                liveProductImage.setImage(null);
                liveProductImage.setVisible(false);
            }
        } else {
            liveProductImage.setImage(null);
            liveProductImage.setVisible(false);
        }
    }

    private void setNoSessionState() {
        liveTitleLabel.setText("Chọn phiên bên trái để bắt đầu");
        liveDescLabel.setText(
            "Các phiên đang LIVE sẽ xuất hiện ở danh sách bên trái.");
        liveStartPrice.setText("—");
        liveCurrentPrice.setText("—");
        liveMinStep.setText("—");
        liveLeaderLabel.setText("—");
        liveHours.setText("00");
        liveMins.setText("00");
        liveSecs.setText("00");
        if (liveProductImage != null) liveProductImage.setImage(null);
        bidCountLabel.setText("0 lượt");
    }

    // =========================================================
    // COUNTDOWN
    // =========================================================
    private void startCountdown() {
        if (currentSession == null) return;

        // Cập nhật ngay lập tức trước khi chạy Timeline
        updateCountdownUI();

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdownUI()));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void updateCountdownUI() {
        if (currentSession == null) return;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = currentSession.getEndTime();

        if (now.isAfter(end)) {
            liveHours.setText("00");
            liveMins.setText("00");
            liveSecs.setText("00");
            liveStatusLabel.setText("● KẾT THÚC");
            liveStatusLabel.setStyle("-fx-text-fill: #ef4444;");
            if (countdownTimer != null) countdownTimer.stop();
            if (uiRefreshTimer != null) uiRefreshTimer.stop();
            return;
        }

        long total = java.time.Duration.between(now, end).getSeconds();
        liveHours.setText(String.format("%02d", total / 3600));
        liveMins.setText(String.format("%02d", (total % 3600) / 60));
        liveSecs.setText(String.format("%02d", total % 60));
    }

    private void handleAuctionEnd() {
        if (currentSession == null) return;
        User me = AppContext.getCurrentUser();
        if (me == null) return;

        var history = currentSession.getBidHistory();
        if (history.isEmpty()) return;

        var winnerBid  = history.get(history.size() - 1);
        String winner  = winnerBid.getBidderId();
        double finalPrice = winnerBid.getAmount();

        if (winner.equals(me.getUsername())) {
            AppContext.addHistory(me.getUsername(), new AppContext.HistoryRecord(
                "BID-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                currentSession.getItemName(),
                finalPrice,
                AppContext.getSessionSeller(currentSession.getSessionId()),
                "CHỜ XỬ LÝ",
                true,
                LocalDateTime.now()
            ));
            addChatMsg("System", "🏆 Bạn đã thắng! Vào Ví để thanh toán.", false);
        } else {
            boolean participated = history.stream()
                .anyMatch(b -> b.getBidderId().equals(me.getUsername()));
            if (participated) {
                addChatMsg("System", "😔 Bạn đã thua. Người thắng: " + winner, false);
            }
        }
    }

    // =========================================================
    // UI REFRESH
    // =========================================================
    private void startUiRefresh() {
        uiRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            if (currentSession == null || !sessionSelected) return;
            int currentBidCount = currentSession.getBidHistory().size();
            if (currentBidCount > lastBidCount) {
                lastBidCount = currentBidCount;
                Platform.runLater(() -> {
                    liveCurrentPrice.setText(
                        formatVND(currentSession.getCurrentPrice()));
                    updateLeaderLabel();
                    updateQuickBidLabels();
                    refreshBidHistory();
                    bidCountLabel.setText(currentBidCount + " lượt");
                    loadLiveSessionList();

                    try {
                        if (currentSession.isLastBidTriggeredExtension()) {
                            showAntiSnipingNotice();
                        }
                    } catch (Exception ignored) {}
                });
            }
        }));
        uiRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        uiRefreshTimer.play();
    }

    // =========================================================
    // PLACE BID
    // =========================================================
    @FXML
    private void handleCustomBid() {
        if (!sessionSelected || currentSession == null) {
            showAlert("Chưa chọn phiên",
                "Vui lòng chọn một phiên từ danh sách bên trái.");
            return;
        }
        String text = customBidField.getText().trim().replaceAll("[^0-9.]", "");
        if (text.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập giá muốn đặt.");
            return;
        }
        double amount;
        try { amount = Double.parseDouble(text); }
        catch (NumberFormatException ex) {
            showAlert("Lỗi", "Giá không hợp lệ."); return;
        }
        placeBid(amount);
    }

    @FXML private void handleQuickBid1() { placeBidByStep(1); }
    @FXML private void handleQuickBid2() { placeBidByStep(2); }
    @FXML private void handleQuickBid3() { placeBidByStep(5); }

    private void placeBidByStep(int steps) {
        if (!sessionSelected || currentSession == null) {
            showAlert("Chưa chọn phiên",
                "Vui lòng chọn một phiên từ danh sách bên trái.");
            return;
        }
        placeBid(currentSession.getCurrentPrice()
            + steps * currentSession.getMinBidStep());
    }

    private void placeBid(double amount) {
        User user = AppContext.getCurrentUser();

        double balance = AppContext.getWalletBalance(user.getUsername());
        if (balance < amount) {
            showAlert("Số dư không đủ",
                "Số dư hiện tại: " + formatVND(balance)
                    + "\nCần nạp thêm: " + formatVND(amount - balance));
            return;
        }

        ServerConnection conn = ServerConnection.getInstance();
        if (!conn.isConnected()) {
            boolean ok = conn.connect(user.getUsername());
            if (!ok) {
                showAlert("Mất kết nối",
                    "Không thể kết nối tới server. Vui lòng thử lại.");
                return;
            }
            // không cần setListener — đã addListener trong connectToServer()
        }

        String sessionId = AppContext.getAllProducts().stream()
            .filter(p -> p.name().equals(currentSession.getItemName()))
            .map(AppContext.ProductRecord::id)
            .findFirst()
            .orElse(currentSession.getSessionId());
        conn.sendBid(user.getUsername(), sessionId, amount);

        addChatMsg("System",
            "⏳ Đang xử lý giá " + formatVND(amount) + "...", false);

        AppContext.HistoryRecord histRec = new AppContext.HistoryRecord(
            "BID-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
            currentSession.getItemName(), amount,
            AppContext.getSessionSeller(currentSession.getSessionId()),
            "CHỜ XỬ LÝ", true, LocalDateTime.now()
        );
        AppContext.addHistory(user.getUsername(), histRec);

        // Conn.sendAddHistory(user.getUsername(), histRec); // Bỏ vì AppContext.addHistory đã gọi rồi

        customBidField.clear();
    }

    // =========================================================
    // ANTI-SNIPING NOTICE
    // =========================================================
    private void showAntiSnipingNotice() {
        if (extensionNoticeLabel == null) return;
        if (extensionNoticeTimer != null) extensionNoticeTimer.stop();

        try {
            String newEnd = currentSession.getEndTime()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            int count = currentSession.getExtensionCount();
            extensionNoticeLabel.setText(
                "⚡ Bid vào phút chót! Phiên kéo dài thêm. "
                    + "Kết thúc mới: " + newEnd
                    + " (lần " + count + "/5)");
            extensionNoticeLabel.setVisible(true);
            extensionNoticeLabel.setManaged(true);

            addChatMsg("System",
                "⚡ Anti-sniping kích hoạt! Phiên kéo dài đến "
                    + newEnd + " (lần " + count + "/5)", false);

            extensionNoticeTimer = new Timeline(
                new KeyFrame(Duration.seconds(8), ev -> {
                    extensionNoticeLabel.setVisible(false);
                    extensionNoticeLabel.setManaged(false);
                }));
            extensionNoticeTimer.setCycleCount(1);
            extensionNoticeTimer.play();
        } catch (Exception ignored) {}
    }

    // =========================================================
    // BID HISTORY
    // =========================================================
    private void refreshBidHistory() {
        liveBidHistoryBox.getChildren().clear();
        if (currentSession == null) return;

        var history = currentSession.getBidHistory();

        // Cập nhật biểu đồ
        if (priceSeries != null) {
            priceSeries.getData().clear();
            priceSeries.getData().add(new XYChart.Data<>("0", currentSession.getStartingPrice()));
            for (int i = 0; i < history.size(); i++) {
                priceSeries.getData().add(new XYChart.Data<>(String.valueOf(i + 1), history.get(i).getAmount()));
            }
        }

        if (history.isEmpty()) {
            Label empty = new Label("Chưa có lượt đặt giá nào.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; "
                + "-fx-padding: 16 0 0 0;");
            liveBidHistoryBox.getChildren().add(empty);
            return;
        }

        for (int i = history.size() - 1; i >= 0; i--) {
            Bid b = history.get(i);
            HBox row = new HBox(12);
            row.getStyleClass().add("bid-row");
            row.setPadding(new Insets(8, 10, 8, 10));
            if (i == history.size() - 1) row.getStyleClass().add("bid-row-top");

            Label rank = new Label(i == history.size() - 1
                ? "👑" : "#" + (history.size() - i));
            rank.setStyle("-fx-font-size: 14px; -fx-min-width: 28px;");

            Label name = new Label(b.getBidderId());
            name.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px;");
            HBox.setHgrow(name, Priority.ALWAYS);

            Label amountLbl = new Label(formatVND(b.getAmount()));
            amountLbl.getStyleClass().add("bid-amount");

            Label time = new Label(b.getTimestamp().format(TIME_FMT));
            time.getStyleClass().add("bid-time");

            row.getChildren().addAll(rank, name, amountLbl, time);
            liveBidHistoryBox.getChildren().add(row);
        }
        Platform.runLater(() -> bidScrollPane.setVvalue(0));
    }

    // =========================================================
    // CHAT
    // =========================================================
    @FXML
    private void handleSendLiveChat() {
        String msg = liveChatInput.getText().trim();
        if (msg.isEmpty()) return;
        User user = AppContext.getCurrentUser();

        addChatMsg(user.getUsername(), msg,
            "SELLER".equalsIgnoreCase(user.getRole()));
        liveChatInput.clear();

        ServerConnection conn = ServerConnection.getInstance();
        if (conn.isConnected()) {
            conn.sendChat(user.getUsername(), msg);
        } else {
            boolean ok = conn.connect(user.getUsername());
            if (ok) {
                // không cần setListener — đã addListener trong connectToServer()
                conn.sendChat(user.getUsername(), msg);
            }
        }
    }

    private void addChatMsg(String sender, String message, boolean isSeller) {
        VBox bubble = new VBox(2);
        bubble.getStyleClass().add(
            isSeller ? "chat-bubble-seller" : "chat-bubble-buyer");
        bubble.setPadding(new Insets(6, 10, 6, 10));
        Label nameLabel = new Label(sender);
        nameLabel.getStyleClass().add("chat-sender");
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.getStyleClass().add("chat-message");
        bubble.getChildren().addAll(nameLabel, msgLabel);
        liveChatBox.getChildren().add(bubble);
        Platform.runLater(() -> liveChatScrollPane.setVvalue(1.0));
    }

    // =========================================================
    // BACK
    // =========================================================
    @FXML
    private void handleBack() {
        if (countdownTimer       != null) countdownTimer.stop();
        if (uiRefreshTimer       != null) uiRefreshTimer.stop();
        if (extensionNoticeTimer != null) extensionNoticeTimer.stop();
        // Gỡ listener để tránh nhận message khi đã rời màn hình
        ServerConnection.getInstance().removeListener(this::handleServerMessage);
        try { HelloApplication.showMainView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private void updateQuickBidLabels() {
        if (currentSession == null) return;
        double cur  = currentSession.getCurrentPrice();
        double step = currentSession.getMinBidStep();
        quickBid1Btn.setText("+1 bước  (" + formatVND(cur + step) + ")");
        quickBid2Btn.setText("+2 bước  (" + formatVND(cur + 2 * step) + ")");
        quickBid3Btn.setText("+5 bước  (" + formatVND(cur + 5 * step) + ")");
    }

    private void updateLeaderLabel() {
        if (currentSession == null) return;
        var history = currentSession.getBidHistory();
        if (!history.isEmpty()) {
            Bid top = history.get(history.size() - 1);
            liveLeaderLabel.setText(
                "👑 " + top.getBidderId()
                    + "  (" + formatVND(top.getAmount()) + ")");
        } else {
            liveLeaderLabel.setText("Chưa có ai đặt giá");
        }
    }

    private String formatVND(double v) {
        return String.format("₫ %,.0f", v);
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
