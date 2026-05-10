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
import java.util.UUID;

public class MainController {

    // =========================================================
    // HEADER
    // =========================================================

    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label userAvatarLabel;
    @FXML private Label walletLabel;

    @FXML private TextField searchField;

    @FXML private MenuButton profileMenuBtn;

    // IMPORTANT:
    // Trong FXML phải thêm:
    // <MenuItem fx:id="ordersMenuItem" ... />

    @FXML private MenuItem ordersMenuItem;

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

    @FXML private VBox chatPanel;
    @FXML private VBox chatMessagesBox;

    @FXML private TextField chatInput;

    @FXML private ScrollPane chatScrollPane;

    // =========================================================
    // DATA
    // =========================================================

    private AuctionSession session;

    private Timeline countdownTimer;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        User user = AppContext.getCurrentUser();

        session = AppContext.getActiveSession();

        // ===== FIX NULL USER =====
        if (user == null) {

            showAlert(
                    "Lỗi đăng nhập",
                    "Không tìm thấy user hiện tại."
            );

            return;
        }

        // ===== FIX NULL SESSION =====
        if (session == null) {

            showAlert(
                    "Lỗi phiên đấu giá",
                    "Không tìm thấy phiên đấu giá."
            );

            return;
        }

        // =====================================================
        // HEADER
        // =====================================================

        userNameLabel.setText(user.getUsername());

        userRoleLabel.setText(user.getRole());

        String avatar;

        if (user.getUsername().length() >= 2) {

            avatar = user.getUsername()
                    .substring(0, 2)
                    .toUpperCase();

        } else {

            avatar = user.getUsername()
                    .toUpperCase();
        }

        userAvatarLabel.setText(avatar);

        walletLabel.setText("0 ₫");

        // =====================================================
        // ROLE MENU
        // =====================================================

        applyRoleMenu(user);

        // =====================================================
        // LOAD UI
        // =====================================================

        loadAuctionInfo();

        refreshBidHistory();

        startCountdown();

        // =====================================================
        // SAMPLE CHAT
        // =====================================================

        addChatMessage(
                "SellerLong",
                "Sản phẩm còn bảo hành chính hãng.",
                true
        );

        addChatMessage(
                "bidder07",
                "Bước giá tiếp theo là bao nhiêu?",
                false
        );

        addChatMessage(
                "SellerLong",
                "Bước giá tối thiểu là 500,000 VNĐ.",
                true
        );
    }

    // =========================================================
    // ROLE MENU
    // =========================================================

    private void applyRoleMenu(User user) {

        if (ordersMenuItem == null) {
            return;
        }

        String role = user.getRole();

        if (role == null) {
            role = "";
        }

        role = role.toUpperCase();

        switch (role) {

            case "BIDDER":

                profileMenuBtn.getItems()
                        .remove(ordersMenuItem);

                break;

            case "SELLER":

                ordersMenuItem.setText(
                        "📦  Sản phẩm đăng bán"
                );

                ordersMenuItem.setOnAction(
                        e -> handleMyProducts()
                );

                break;

            default:
                break;
        }
    }

    // =========================================================
    // LOAD AUCTION INFO
    // =========================================================

    private void loadAuctionInfo() {

        productTitleLabel.setText(
                session.getItemName()
        );

        productDescLabel.setText(
                "Laptop cao cấp – Mới 100%, nguyên seal, bảo hành 12 tháng."
        );

        startPriceLabel.setText(
                formatVND(session.getStartingPrice())
        );

        currentPriceLabel.setText(
                formatVND(session.getCurrentPrice())
        );

        minStepLabel.setText(
                formatVND(session.getMinBidStep())
        );

        endTimeLabel.setText(
                session.getEndTime()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "HH:mm:ss dd/MM/yyyy"
                                )
                        )
        );

        statusLabel.setText(
                "● " + session.getStatus().name()
        );

        statusLabel.getStyleClass().setAll(
                "status-badge",
                "status-running"
        );
    }

    // =========================================================
    // COUNTDOWN
    // =========================================================

    private void startCountdown() {

        countdownTimer = new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        e -> updateCountdown()
                )
        );

        countdownTimer.setCycleCount(
                Timeline.INDEFINITE
        );

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

        long total =
                java.time.Duration
                        .between(now, end)
                        .getSeconds();

        hoursLabel.setText(
                String.format("%02d", total / 3600)
        );

        minsLabel.setText(
                String.format("%02d",
                        (total % 3600) / 60)
        );

        secsLabel.setText(
                String.format("%02d",
                        total % 60)
        );
    }

    // =========================================================
    // PLACE BID
    // =========================================================

    @FXML
    private void handlePlaceBid() {

        try {

            User user = AppContext.getCurrentUser();

            if (user == null) {

                showAlert(
                        "Lỗi",
                        "Bạn chưa đăng nhập."
                );

                return;
            }

            double newPrice =
                    session.getCurrentPrice()
                            + session.getMinBidStep();

            Bid bid = new Bid(
                    UUID.randomUUID().toString(),
                    user.getUsername(),
                    newPrice
            );

            session.placeBid(bid);

            // =================================================
            // UPDATE UI
            // =================================================

            currentPriceLabel.setText(
                    formatVND(session.getCurrentPrice())
            );

            refreshBidHistory();

            addChatMessage(
                    "System",
                    user.getUsername()
                            + " đã đặt giá "
                            + formatVND(newPrice),
                    false
            );

            // =================================================
            // SAVE HISTORY
            // =================================================

            AppContext.addHistory(

                    user.getUsername(),

                    new AppContext.HistoryRecord(

                            "BID-"
                                    + UUID.randomUUID()
                                    .toString()
                                    .substring(0, 6)
                                    .toUpperCase(),

                            session.getItemName(),

                            newPrice,

                            "SellerLong",

                            "CHỜ XỬ LÝ",

                            true,

                            LocalDateTime.now()
                    )
            );

            // =================================================
            // SERVER
            // =================================================

            ServerConnection conn =
                    ServerConnection.getInstance();

            if (conn.isConnected()) {

                conn.send(
                        "BID:"
                                + user.getUsername()
                                + ":"
                                + newPrice
                );
            }

        } catch (InvalidBidException e) {

            showAlert(
                    "Bid không hợp lệ",
                    e.getMessage()
            );

        } catch (AuctionClosedException e) {

            showAlert(
                    "Phiên đấu giá đã đóng",
                    e.getMessage()
            );

        } catch (Exception e) {

            e.printStackTrace();

            showAlert(
                    "Lỗi hệ thống",
                    e.getMessage()
            );
        }
    }

    // =========================================================
    // CHAT
    // =========================================================

    @FXML
    private void handleToggleChat() {

        boolean visible =
                !chatPanel.isVisible();

        chatPanel.setVisible(visible);

        chatPanel.setManaged(visible);

        if (visible) {

            Platform.runLater(() ->
                    chatScrollPane.setVvalue(1.0)
            );
        }
    }

    @FXML
    private void handleSendChat() {

        String msg =
                chatInput.getText().trim();

        if (msg.isEmpty()) {
            return;
        }

        User user =
                AppContext.getCurrentUser();

        boolean isSeller =
                user.getRole()
                        .equalsIgnoreCase("SELLER");

        addChatMessage(
                user.getUsername(),
                msg,
                isSeller
        );

        chatInput.clear();

        ServerConnection conn =
                ServerConnection.getInstance();

        if (conn.isConnected()) {

            conn.send(
                    "CHAT:"
                            + user.getUsername()
                            + ":"
                            + msg
            );
        }
    }

    // =========================================================
    // PROFILE MENU
    // =========================================================

    @FXML
    private void handleProfile() {

        try {

            HelloApplication.showProfileView();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    @FXML
    private void handleHistory() {

        try {

            HelloApplication.showHistoryView();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    @FXML
    private void handleOrders() {

        try {

            HelloApplication.showHistoryView();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void handleMyProducts() {

        try {

            HelloApplication.showMyProductsView();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    @FXML
    private void handleLogout() {

        try {

            if (countdownTimer != null) {
                countdownTimer.stop();
            }

            AppContext.logout();

            HelloApplication.showLoginView();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // BID HISTORY
    // =========================================================

    private void refreshBidHistory() {

        bidHistoryBox.getChildren().clear();

        var history =
                session.getBidHistory();

        for (int i = history.size() - 1;
             i >= 0;
             i--) {

            Bid b = history.get(i);

            HBox row = new HBox(12);

            row.getStyleClass().add(
                    "bid-row"
            );

            if (i == history.size() - 1) {

                row.getStyleClass().add(
                        "bid-row-top"
                );
            }

            Label name = new Label(
                    (i == history.size() - 1
                            ? "👑 "
                            : "")
                            + b.getBidderId()
            );

            Label amount = new Label(
                    formatVND(b.getAmount())
            );

            Label time = new Label(
                    b.getTimestamp()
                            .format(TIME_FMT)
            );

            amount.getStyleClass()
                    .add("bid-amount");

            time.getStyleClass()
                    .add("bid-time");

            HBox.setHgrow(
                    name,
                    Priority.ALWAYS
            );

            row.getChildren().addAll(
                    name,
                    amount,
                    time
            );

            bidHistoryBox.getChildren()
                    .add(row);
        }
    }

    // =========================================================
    // CHAT UI
    // =========================================================

    private void addChatMessage(
            String sender,
            String message,
            boolean isSeller
    ) {

        VBox bubble = new VBox(2);

        bubble.getStyleClass().add(
                isSeller
                        ? "chat-bubble-seller"
                        : "chat-bubble-buyer"
        );

        Label senderLabel =
                new Label(sender);

        senderLabel.getStyleClass()
                .add("chat-sender");

        Label msgLabel =
                new Label(message);

        msgLabel.setWrapText(true);

        msgLabel.getStyleClass()
                .add("chat-message");

        bubble.getChildren().addAll(
                senderLabel,
                msgLabel
        );

        chatMessagesBox.getChildren()
                .add(bubble);

        Platform.runLater(() ->
                chatScrollPane.setVvalue(1.0)
        );
    }

    // =========================================================
    // UTIL
    // =========================================================

    private String formatVND(double amount) {

        return String.format(
                "₫ %,.0f",
                amount
        );
    }

    private void showAlert(
            String title,
            String message
    ) {

        Alert alert = new Alert(
                Alert.AlertType.WARNING
        );

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(message);

        alert.showAndWait();
    }
}
