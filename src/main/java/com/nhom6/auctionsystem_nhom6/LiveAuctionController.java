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

    // ── Chat ──────────────────────────────────────────────────
    @FXML private VBox       liveChatBox;
    @FXML private TextField  liveChatInput;
    @FXML private ScrollPane liveChatScrollPane;
    @FXML private Label      onlineLabel;

    // ── State ─────────────────────────────────────────────────
    private AuctionSession currentSession;
    private Timeline       countdownTimer;
    // ✅ uiRefreshTimer: CHỈ sync UI từ session thật, KHÔNG tự đặt giá
    private Timeline       uiRefreshTimer;
    private boolean        sessionSelected = false;
    // Tracking bid count — chỉ update UI khi có bid mới từ người thật
    private int            lastBidCount    = 0;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        // ── Ẩn anti-sniping label ─────────────────────────────
        if (extensionNoticeLabel != null) {
            extensionNoticeLabel.setVisible(false);
            extensionNoticeLabel.setManaged(false);
        }

        onlineLabel.setText("● -- online");

        // ✅ Load left panel từ globalSessions (thấy phiên của mọi Seller)
        loadLiveSessionList();
        setNoSessionState();

        // ✅ Kết nối server
        connectToServer();

        // ✅ Tự động chọn activeSession nếu có
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
        conn.setListener(this::handleServerMessage);
        if (!conn.isConnected()) {
            User user = AppContext.getCurrentUser();
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
                        addChatMsg(sender, message, false);
                    }
                }
                case "ONLINE_COUNT" -> {
                    String count = extractJson(raw, "count");
                    onlineLabel.setText("● " + count + " online");
                }
                case "NEW_BID" -> {
                    String bidder = extractJson(raw, "username");
                    String amount = extractJson(raw, "amount");
                    if (currentSession != null) {
                        try {
                            double amt = Double.parseDouble(amount);
                            liveCurrentPrice.setText(
                                    String.format("₫ %,.0f", amt));
                            liveLeaderLabel.setText("👑 " + bidder);
                            addChatMsg("System",
                                    bidder + " đặt giá "
                                    + String.format("₫ %,.0f", amt), false);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                case "SYSTEM" -> {
                    String message = extractJson(raw, "message");
                    addChatMsg("System", message, false);
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
    // ✅ LEFT PANEL — đọc từ globalSessions (Bidder thấy phiên Seller tạo)
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
        // Dừng timer cũ
        if (countdownTimer != null) countdownTimer.stop();
        if (uiRefreshTimer != null) uiRefreshTimer.stop();

        currentSession  = session;
        sessionSelected = true;
        lastBidCount    = session.getBidHistory().size();

        AppContext.setActiveSession(session);

        // ── Điền thông tin ────────────────────────────────────
        liveTitleLabel.setText(session.getItemName());
        liveDescLabel.setText(
                "Sản phẩm mới 100%, còn nguyên seal, bảo hành 12 tháng.");
        liveStartPrice.setText(formatVND(session.getStartingPrice()));
        liveCurrentPrice.setText(formatVND(session.getCurrentPrice()));
        liveMinStep.setText(formatVND(session.getMinBidStep()));
        liveStatusLabel.setText("● " + session.getStatus().name());
        liveStatusLabel.getStyleClass().setAll("status-badge", "status-running");

        updateLeaderLabel();
        updateQuickBidLabels();
        refreshBidHistory();
        bidCountLabel.setText(session.getBidHistory().size() + " lượt");

        // ── Refresh left panel ────────────────────────────────
        loadLiveSessionList();

        // ── Start timers ──────────────────────────────────────
        startCountdown();
        startUiRefresh();   // ✅ chỉ sync UI, không tự đặt giá

        participantsLabel.setText(
                "👥 " + (8 + (int)(Math.random() * 15)) + " người tham gia");
        addChatMsg("System",
                "✅ Đã tham gia phiên: " + session.getItemName(), false);
    }

    private void setNoSessionState() {
        liveTitleLabel.setText("Chọn phiên bên trái để bắt đầu");
        liveDescLabel.setText(
                "Các phiên đang LIVE sẽ xuất hiện ở danh sách bên trái.");
        liveStartPrice.setText("—");
        liveCurrentPrice.setText("—");
        liveMinStep.setText("—");
        liveLeaderLabel.setText("—");
        liveHours.setText("--");
        liveMins.setText("--");
        liveSecs.setText("--");
        bidCountLabel.setText("0 lượt");
    }

    // =========================================================
    // COUNTDOWN
    // =========================================================
    private void startCountdown() {
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (currentSession == null) return;
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime end = currentSession.getEndTime();
            if (now.isAfter(end)) {
                liveHours.setText("00");
                liveMins.setText("00");
                liveSecs.setText("00");
                liveStatusLabel.setText("● KẾT THÚC");
                liveStatusLabel.setStyle("-fx-text-fill: #ef4444;");
                countdownTimer.stop();
                if (uiRefreshTimer != null) uiRefreshTimer.stop();
                return;
            }
            long total = java.time.Duration.between(now, end).getSeconds();
            liveHours.setText(String.format("%02d", total / 3600));
            liveMins.setText(String.format("%02d",  (total % 3600) / 60));
            liveSecs.setText(String.format("%02d",  total % 60));
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    // =========================================================
    // ✅ UI REFRESH — CHỈ cập nhật khi có bid mới từ người thật
    //    KHÔNG tự đặt giá, KHÔNG simulate
    // =========================================================
    private void startUiRefresh() {
        uiRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            if (currentSession == null || !sessionSelected) return;
            int currentBidCount = currentSession.getBidHistory().size();
            // Chỉ update nếu có bid mới
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

                    // Anti-sniping
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
    // ✅ PLACE BID — chỉ từ hành động người dùng thật
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
        customBidField.clear();
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

    /**
     * ✅ Đặt giá thật — cập nhật UI ngay sau khi thành công
     *    KHÔNG simulate, KHÔNG auto-bid
     */
    private void placeBid(double amount) {
        User user = AppContext.getCurrentUser();
        Bid  bid  = new Bid(UUID.randomUUID().toString(),
                user.getUsername(), amount);
        try {
            currentSession.placeBid(bid);

            // ✅ Cập nhật UI ngay lập tức
            lastBidCount = currentSession.getBidHistory().size();
            liveCurrentPrice.setText(formatVND(currentSession.getCurrentPrice()));
            updateLeaderLabel();
            updateQuickBidLabels();
            refreshBidHistory();
            bidCountLabel.setText(lastBidCount + " lượt");
            loadLiveSessionList();

            addChatMsg("System",
                    "✅ " + user.getUsername()
                    + " đặt giá " + formatVND(amount), false);

            // Anti-sniping
            try {
                if (currentSession.isLastBidTriggeredExtension()) {
                    showAntiSnipingNotice();
                }
            } catch (Exception ignored) {}

            // Ghi lịch sử bidder
            AppContext.addHistory(user.getUsername(), new AppContext.HistoryRecord(
                    "BID-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                    currentSession.getItemName(), amount,
                    AppContext.getSessionSeller(currentSession.getSessionId()),
                    "CHỜ XỬ LÝ", false, LocalDateTime.now()
            ));

            // Gửi server
            ServerConnection conn = ServerConnection.getInstance();
            if (conn.isConnected())
                conn.sendBid(user.getUsername(),
                        currentSession.getItemName(), amount);

        } catch (InvalidBidException e) {
            showAlert("Bid không hợp lệ",
                    e.getMessage() + "\n\nGiá tối thiểu cần đặt: "
                    + formatVND(currentSession.getCurrentPrice()
                                + currentSession.getMinBidStep()));
        } catch (AuctionClosedException e) {
            showAlert("Phiên đã đóng", e.getMessage());
        }
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

            Label amount = new Label(formatVND(b.getAmount()));
            amount.getStyleClass().add("bid-amount");

            Label time = new Label(b.getTimestamp().format(TIME_FMT));
            time.getStyleClass().add("bid-time");

            row.getChildren().addAll(rank, name, amount, time);
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
                conn.setListener(this::handleServerMessage);
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
        if (countdownTimer != null) countdownTimer.stop();
        if (uiRefreshTimer != null) uiRefreshTimer.stop();
        if (extensionNoticeTimer != null) extensionNoticeTimer.stop();
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
