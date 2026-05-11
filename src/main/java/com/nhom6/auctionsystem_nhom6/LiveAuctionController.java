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

    // ── Anti-sniping notice ───────────────────────────────────
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

    private AuctionSession   currentSession;
    private Timeline         countdownTimer;
    private Timeline         simulateTimer;
    private boolean          sessionSelected = false;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // ─────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        loadLiveSessionList();
        setNoSessionState();

        if (extensionNoticeLabel != null) {
            extensionNoticeLabel.setVisible(false);
            extensionNoticeLabel.setManaged(false);
        }

        // ✅ Số online mặc định, sẽ được cập nhật từ server
        onlineLabel.setText("● -- online");

        // ✅ Kết nối server với username thực, đăng ký nhận message
        connectToServer();

        if (AppContext.getActiveSession() != null)
            selectSession(AppContext.getActiveSession());
    }

    // ✅ Kết nối server và xử lý tất cả message nhận về
    private void connectToServer() {
        ServerConnection conn = ServerConnection.getInstance();
        if (!conn.isConnected()) {
            User user = AppContext.getCurrentUser();
            conn.connect(user.getUsername()); // gửi LOGIN với username thực
        }

        // Đăng ký listener xử lý message từ server
        conn.setListener(this::handleServerMessage);
    }

    /**
     * ✅ Xử lý tất cả message JSON từ server:
     *    - CHAT       → hiển thị tin nhắn
     *    - ONLINE_COUNT → cập nhật số người online
     *    - NEW_BID    → cập nhật giá đấu
     *    - SYSTEM     → thông báo hệ thống
     */
    private void handleServerMessage(String rawMessage) {
        try {
            // Parse JSON thủ công (không cần thư viện thêm)
            if (rawMessage.contains("\"type\"")) {
                String type = extractJsonValue(rawMessage, "type");

                switch (type) {
                    case "CHAT" -> {
                        String sender  = extractJsonValue(rawMessage, "username");
                        String message = extractJsonValue(rawMessage, "message");
                        User me = AppContext.getCurrentUser();
                        // Không hiện lại tin nhắn của chính mình (đã hiện rồi)
                        if (!sender.equals(me.getUsername())) {
                            boolean isSeller = false; // không biết role của người kia
                            addChatMsg(sender, message, isSeller);
                        }
                    }

                    case "ONLINE_COUNT" -> {
                        String count = extractJsonValue(rawMessage, "count");
                        onlineLabel.setText("● " + count + " online");
                    }

                    case "NEW_BID" -> {
                        String bidder = extractJsonValue(rawMessage, "username");
                        String amount = extractJsonValue(rawMessage, "amount");
                        // Cập nhật UI nếu đang xem phiên này
                        if (currentSession != null) {
                            try {
                                double amt = Double.parseDouble(amount);
                                liveCurrentPrice.setText(formatVND(amt));
                                liveLeaderLabel.setText("👑 " + bidder);
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    case "SYSTEM" -> {
                        String message = extractJsonValue(rawMessage, "message");
                        addChatMsg("System", message, false);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi parse message: " + e.getMessage());
        }
    }

    /** Parse giá trị từ JSON string đơn giản */
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? "" : json.substring(start, end);
    }

    // ── Left panel ────────────────────────────────────────────
    private void loadLiveSessionList() {
        liveSessionListBox.getChildren().clear();
        boolean hasAny = false;

        if (AppContext.getActiveSession() != null) {
            AuctionSession s = AppContext.getActiveSession();
            liveSessionListBox.getChildren().add(
                    buildSessionItem(s.getItemName(), "RUNNING", s.getCurrentPrice(), s));
            hasAny = true;
        }

        User user = AppContext.getCurrentUser();
        if ("SELLER".equalsIgnoreCase(user.getRole())) {
            for (AppContext.ProductRecord p : AppContext.getProducts(user.getUsername())) {
                if ("ĐANG ĐẤU GIÁ".equals(p.status()) || "CHỜ DUYỆT".equals(p.status())) {
                    String status = "ĐANG ĐẤU GIÁ".equals(p.status()) ? "RUNNING" : "UPCOMING";
                    liveSessionListBox.getChildren().add(
                            buildSessionItemFromProduct(p, status));
                    hasAny = true;
                }
            }
        }

        if (!hasAny) {
            VBox emptyBox = new VBox(8);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(30, 10, 30, 10));
            Label icon = new Label("📭");
            icon.setStyle("-fx-font-size: 32px;");
            Label msg = new Label("Chưa có phiên nào.");
            msg.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; "
                    + "-fx-font-weight: bold; -fx-wrap-text: true; "
                    + "-fx-text-alignment: center;");
            Label hint = new Label("Thêm sản phẩm để tạo phiên đấu giá.");
            hint.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; "
                    + "-fx-wrap-text: true; -fx-text-alignment: center;");
            emptyBox.getChildren().addAll(icon, msg, hint);
            liveSessionListBox.getChildren().add(emptyBox);
        }
    }

    private VBox buildSessionItem(String name, String status,
                                  double currentPrice, AuctionSession session) {
        VBox card = new VBox(4);
        card.getStyleClass().add("live-session-item");
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; "
                + "-fx-border-color: #2563eb; -fx-border-radius: 8; -fx-border-width: 1; "
                + "-fx-cursor: hand;");
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("history-item-name");
        nameLabel.setWrapText(true);
        nameLabel.setStyle("-fx-font-size: 12px;");
        HBox meta = new HBox(8);
        Label statusL = new Label("RUNNING".equals(status) ? "🟢 LIVE" : "⏰ SẮP");
        statusL.setStyle("-fx-font-size: 11px; -fx-text-fill: "
                + ("RUNNING".equals(status) ? "#10b981" : "#fbbf24") + ";");
        Label priceL = new Label(formatVND(currentPrice));
        priceL.setStyle("-fx-font-size: 11px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        meta.getChildren().addAll(statusL, priceL);
        card.getChildren().addAll(nameLabel, meta);
        if ("RUNNING".equals(status) && session != null)
            card.setOnMouseClicked(e -> selectSession(session));
        return card;
    }

    private VBox buildSessionItemFromProduct(AppContext.ProductRecord p, String status) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; "
                + "-fx-border-color: #2563eb; -fx-border-radius: 8; -fx-border-width: 1; "
                + "-fx-cursor: hand;");
        Label nameLabel = new Label(p.name());
        nameLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 12px; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);
        HBox meta = new HBox(8);
        Label statusL = new Label("RUNNING".equals(status) ? "🟢 LIVE" : "⏰ SẮP");
        statusL.setStyle("-fx-font-size: 11px; -fx-text-fill: "
                + ("RUNNING".equals(status) ? "#10b981" : "#fbbf24") + ";");
        Label priceL = new Label(formatVND(p.currentPrice()));
        priceL.setStyle("-fx-font-size: 11px; -fx-text-fill: #38bdf8;");
        meta.getChildren().addAll(statusL, priceL);
        card.getChildren().addAll(nameLabel, meta);
        if ("RUNNING".equals(status)) {
            card.setOnMouseClicked(e -> {
                if (AppContext.getActiveSession() != null)
                    selectSession(AppContext.getActiveSession());
                else
                    showAlert("Thông báo",
                            "Vui lòng tham gia từ màn hình chính để tải phiên.");
            });
        }
        return card;
    }

    @FXML private void handleLiveSearch() {
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

    // ── Select session ────────────────────────────────────────
    private void selectSession(AuctionSession session) {
        if (countdownTimer != null) countdownTimer.stop();
        if (simulateTimer  != null) simulateTimer.stop();

        currentSession  = session;
        sessionSelected = true;

        liveTitleLabel.setText(session.getItemName());
        liveDescLabel.setText("Sản phẩm mới 100%, còn nguyên seal. Bảo hành 12 tháng.");
        liveStartPrice.setText(formatVND(session.getStartingPrice()));
        liveCurrentPrice.setText(formatVND(session.getCurrentPrice()));
        liveMinStep.setText(formatVND(session.getMinBidStep()));
        liveStatusLabel.setText("● " + session.getStatus().name());
        liveStatusLabel.getStyleClass().setAll("status-badge", "status-running");

        updateQuickBidLabels();

        var history = session.getBidHistory();
        if (!history.isEmpty())
            liveLeaderLabel.setText("👑 " + history.get(history.size() - 1).getBidderId());

        refreshBidHistory();
        startCountdown();
        startSimulation();

        participantsLabel.setText("👥 " + (8 + (int)(Math.random() * 20)) + " người tham gia");
        addChatMsg("System", "Bạn đã tham gia phiên: " + session.getItemName(), false);
    }

    private void setNoSessionState() {
        liveTitleLabel.setText("Chọn phiên bên trái để bắt đầu");
        liveDescLabel.setText("Các phiên đang LIVE hiện tại sẽ hiện ở danh sách bên trái.");
        liveStartPrice.setText("—");
        liveCurrentPrice.setText("—");
        liveMinStep.setText("—");
        liveHours.setText("--");
        liveMins.setText("--");
        liveSecs.setText("--");
    }

    // ── Countdown ─────────────────────────────────────────────
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
                countdownTimer.stop();
                if (simulateTimer != null) simulateTimer.stop();
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

    // ── Simulate other bidders ────────────────────────────────
    private void startSimulation() {
        String[] bots = {"bidder07", "nguyen_tran", "camera_pro", "laptop_dev"};
        simulateTimer = new Timeline(new KeyFrame(
                Duration.seconds(8 + Math.random() * 12), e -> {
            if (currentSession == null || !sessionSelected) return;
            String bot      = bots[(int)(Math.random() * bots.length)];
            double newPrice = currentSession.getCurrentPrice()
                    + currentSession.getMinBidStep() * (1 + (int)(Math.random() * 3));
            Bid bid = new Bid(UUID.randomUUID().toString(), bot, newPrice);
            try {
                currentSession.placeBid(bid);
                Platform.runLater(() -> {
                    liveCurrentPrice.setText(formatVND(currentSession.getCurrentPrice()));
                    liveLeaderLabel.setText("👑 " + bot);
                    refreshBidHistory();
                    updateQuickBidLabels();
                    addChatMsg(bot, "Tôi vừa đặt " + formatVND(newPrice) + "!", false);
                    bidCountLabel.setText(currentSession.getBidHistory().size() + " lượt");

                    if (currentSession.isLastBidTriggeredExtension())
                        showAntiSnipingNotice();
                });
            } catch (Exception ignored) {}
        }));
        simulateTimer.setCycleCount(Timeline.INDEFINITE);
        simulateTimer.play();
    }

    // ── Place Bid ─────────────────────────────────────────────
    @FXML private void handleCustomBid() {
        if (!sessionSelected || currentSession == null) {
            showAlert("Chưa chọn phiên", "Vui lòng chọn một phiên đấu giá trước.");
            return;
        }
        String text = customBidField.getText().trim().replaceAll("[^0-9.]", "");
        if (text.isEmpty()) { showAlert("Lỗi", "Vui lòng nhập giá muốn đặt."); return; }
        double amount;
        try { amount = Double.parseDouble(text); }
        catch (NumberFormatException ex) { showAlert("Lỗi", "Giá không hợp lệ."); return; }
        placeBid(amount);
        customBidField.clear();
    }

    @FXML private void handleQuickBid1() { placeBidByStep(1); }
    @FXML private void handleQuickBid2() { placeBidByStep(2); }
    @FXML private void handleQuickBid3() { placeBidByStep(5); }

    private void placeBidByStep(int steps) {
        if (!sessionSelected || currentSession == null) {
            showAlert("Chưa chọn phiên", "Vui lòng chọn một phiên đấu giá trước.");
            return;
        }
        placeBid(currentSession.getCurrentPrice()
                + steps * currentSession.getMinBidStep());
    }

    private void placeBid(double amount) {
        User user = AppContext.getCurrentUser();
        Bid  bid  = new Bid(UUID.randomUUID().toString(), user.getUsername(), amount);
        try {
            currentSession.placeBid(bid);
            liveCurrentPrice.setText(formatVND(currentSession.getCurrentPrice()));
            liveLeaderLabel.setText("👑 " + user.getUsername());
            refreshBidHistory();
            updateQuickBidLabels();
            addChatMsg("System", user.getUsername() + " đặt giá " + formatVND(amount), false);
            bidCountLabel.setText(currentSession.getBidHistory().size() + " lượt");

            if (currentSession.isLastBidTriggeredExtension())
                showAntiSnipingNotice();

            AppContext.addHistory(user.getUsername(), new AppContext.HistoryRecord(
                    "BID-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                    currentSession.getItemName(), amount, "SellerLong",
                    "CHỜ XỬ LÝ", false, LocalDateTime.now()
            ));

            // ✅ Gửi bid đúng JSON format
            ServerConnection conn = ServerConnection.getInstance();
            if (conn.isConnected())
                conn.sendBid(user.getUsername(),
                        currentSession.getItemName(), amount);

        } catch (InvalidBidException e) {
            showAlert("Bid không hợp lệ",
                    e.getMessage() + "\nGiá tối thiểu: "
                            + formatVND(currentSession.getCurrentPrice()
                            + currentSession.getMinBidStep()));
        } catch (AuctionClosedException e) {
            showAlert("Phiên đã đóng", e.getMessage());
        }
    }

    private void updateQuickBidLabels() {
        if (currentSession == null) return;
        quickBid1Btn.setText("+1 bước  (" + formatVND(
                currentSession.getCurrentPrice() + currentSession.getMinBidStep()) + ")");
        quickBid2Btn.setText("+2 bước  (" + formatVND(
                currentSession.getCurrentPrice() + 2 * currentSession.getMinBidStep()) + ")");
        quickBid3Btn.setText("+5 bước  (" + formatVND(
                currentSession.getCurrentPrice() + 5 * currentSession.getMinBidStep()) + ")");
    }

    // ── Anti-sniping notice ───────────────────────────────────
    private void showAntiSnipingNotice() {
        if (extensionNoticeLabel == null) return;
        if (extensionNoticeTimer != null) extensionNoticeTimer.stop();

        String newEnd = currentSession.getEndTime().format(TIME_FMT);
        int count     = currentSession.getExtensionCount();

        extensionNoticeLabel.setText(
                "⚡ Có bid vào phút chót! Phiên tự động kéo dài thêm 70 giây. "
                        + "Kết thúc mới: " + newEnd
                        + "  (lần " + count + "/5)");
        extensionNoticeLabel.setVisible(true);
        extensionNoticeLabel.setManaged(true);

        addChatMsg("System",
                "⚡ Anti-sniping kích hoạt! Phiên kéo dài đến " + newEnd
                        + " (lần " + count + "/5)", false);

        extensionNoticeTimer = new Timeline(
                new KeyFrame(Duration.seconds(8), ev -> {
                    extensionNoticeLabel.setVisible(false);
                    extensionNoticeLabel.setManaged(false);
                }));
        extensionNoticeTimer.setCycleCount(1);
        extensionNoticeTimer.play();
    }

    // ── Bid History ───────────────────────────────────────────
    private void refreshBidHistory() {
        liveBidHistoryBox.getChildren().clear();
        if (currentSession == null) return;
        var history = currentSession.getBidHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            Bid b = history.get(i);
            HBox row = new HBox(12);
            row.getStyleClass().add("bid-row");
            row.setPadding(new Insets(6, 10, 6, 10));
            if (i == history.size() - 1) row.getStyleClass().add("bid-row-top");

            Label rank = new Label(
                    i == history.size() - 1 ? "👑" : String.valueOf(history.size() - i));
            rank.setStyle("-fx-font-size: 14px; -fx-min-width: 24px;");

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

    // ── Chat ──────────────────────────────────────────────────
    @FXML private void handleSendLiveChat() {
        String msg = liveChatInput.getText().trim();
        if (msg.isEmpty()) return;
        User user = AppContext.getCurrentUser();

        // ✅ Hiện tin nhắn của mình ngay lập tức
        addChatMsg(user.getUsername(), msg, "SELLER".equals(user.getRole()));
        liveChatInput.clear();

        // ✅ Gửi đúng JSON format để server broadcast cho người khác
        ServerConnection conn = ServerConnection.getInstance();
        if (conn.isConnected()) {
            conn.sendChat(user.getUsername(), msg);
        } else {
            // Thử kết nối lại nếu mất kết nối
            boolean reconnected = conn.connect(user.getUsername());
            if (reconnected) {
                conn.setListener(this::handleServerMessage);
                conn.sendChat(user.getUsername(), msg);
            }
        }
    }

    private void addChatMsg(String sender, String message, boolean isSeller) {
        VBox bubble = new VBox(2);
        bubble.getStyleClass().add(isSeller ? "chat-bubble-seller" : "chat-bubble-buyer");
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

    @FXML private void handleBack() {
        if (countdownTimer != null) countdownTimer.stop();
        if (simulateTimer  != null) simulateTimer.stop();
        try { HelloApplication.showMainView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // ── Helpers ───────────────────────────────────────────────
    private String formatVND(double v) { return String.format("₫ %,.0f", v); }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}