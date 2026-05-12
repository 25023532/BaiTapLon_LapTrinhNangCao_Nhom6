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

        HBox metaRow1 = new
