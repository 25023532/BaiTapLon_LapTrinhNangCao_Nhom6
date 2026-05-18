package com.nhom6.auctionsystem_nhom6;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class WalletController {

    // =========================================================
    // HEADER
    // =========================================================
    @FXML private Label              userAvatarLabel;
    @FXML private Label              userNameLabel;
    @FXML private Label              userRoleLabel;
    @FXML private Label              walletHeaderLabel;

    // =========================================================
    // BALANCE CARD
    // =========================================================
    @FXML private Label              balanceLabel;

    // =========================================================
    // TABS
    // =========================================================
    @FXML private Button             tabDepositBtn;
    @FXML private Button             tabPaymentBtn;
    @FXML private Button             tabHistoryBtn;

    // =========================================================
    // PANELS
    // =========================================================
    @FXML private VBox               depositPanel;
    @FXML private VBox               paymentPanel;
    @FXML private VBox               historyPanel;

    // ── Nạp tiền ─────────────────────────────────────────────
    @FXML private TextField          depositAmountField;
    @FXML private ComboBox<String>   depositMethodBox;
    @FXML private Label              depositResultLabel;

    // ── Thanh toán ───────────────────────────────────────────
    @FXML private Label              paymentBalanceLabel;
    @FXML private VBox               pendingOrdersBox;    // danh sách đơn chờ thanh toán
    @FXML private Label              paymentResultLabel;

    // ── Lịch sử ──────────────────────────────────────────────
    @FXML private VBox               transactionListBox;

    // =========================================================
    // DATA
    // =========================================================
    private String username;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("HH:mm  dd/MM/yyyy");

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        var user = AppContext.getCurrentUser();
        if (user == null) return;

        username = user.getUsername();

        // Header
        userNameLabel.setText(username);
        userRoleLabel.setText(user.getRole());
        String av = username.length() >= 2
                ? username.substring(0, 2).toUpperCase()
                : username.toUpperCase();
        userAvatarLabel.setText(av);

        // Phương thức nạp tiền
        depositMethodBox.getItems().addAll(
                "Ngân hàng VCB", "Ngân hàng TCB",
                "MoMo", "ZaloPay", "VNPay");
        depositMethodBox.getSelectionModel().selectFirst();

        depositResultLabel.setVisible(false);
        paymentResultLabel.setVisible(false);

        refreshBalance();
        showTab("deposit");
        loadTransactions();
    }

    // =========================================================
    // REFRESH BALANCE
    // =========================================================
    private void refreshBalance() {
        double balance = AppContext.getWalletBalance(username);
        String fmt = formatVND(balance);
        balanceLabel.setText(fmt);
        walletHeaderLabel.setText(fmt);
        paymentBalanceLabel.setText(fmt);
    }

    // =========================================================
    // TAB SWITCHING
    // =========================================================
    @FXML private void handleTabDeposit() { showTab("deposit"); }
    @FXML private void handleTabPayment() { showTab("payment"); loadPendingOrders(); }
    @FXML private void handleTabHistory() { showTab("history"); loadTransactions(); }

    private void showTab(String tab) {
        depositPanel.setVisible(false); depositPanel.setManaged(false);
        paymentPanel.setVisible(false); paymentPanel.setManaged(false);
        historyPanel.setVisible(false); historyPanel.setManaged(false);

        tabDepositBtn.getStyleClass().remove("tab-active");
        tabPaymentBtn.getStyleClass().remove("tab-active");
        tabHistoryBtn.getStyleClass().remove("tab-active");

        switch (tab) {
            case "deposit" -> {
                depositPanel.setVisible(true); depositPanel.setManaged(true);
                tabDepositBtn.getStyleClass().add("tab-active");
            }
            case "payment" -> {
                paymentPanel.setVisible(true); paymentPanel.setManaged(true);
                tabPaymentBtn.getStyleClass().add("tab-active");
            }
            case "history" -> {
                historyPanel.setVisible(true); historyPanel.setManaged(true);
                tabHistoryBtn.getStyleClass().add("tab-active");
            }
        }
    }

    // =========================================================
    // NẠP TIỀN
    // =========================================================
    @FXML private void handleQuickDeposit1() { depositAmountField.setText("100000"); }
    @FXML private void handleQuickDeposit2() { depositAmountField.setText("500000"); }
    @FXML private void handleQuickDeposit3() { depositAmountField.setText("1000000"); }
    @FXML private void handleQuickDeposit4() { depositAmountField.setText("5000000"); }

    @FXML
    private void handleDeposit() {
        depositResultLabel.setVisible(false);

        String raw = depositAmountField.getText().trim().replaceAll("[^0-9]", "");
        if (raw.isEmpty()) {
            showResult(depositResultLabel, "❌  Vui lòng nhập số tiền cần nạp.", false);
            return;
        }
        double amount = Double.parseDouble(raw);
        if (amount < 10_000) {
            showResult(depositResultLabel, "❌  Số tiền tối thiểu là 10,000 ₫.", false);
            return;
        }
        String method = depositMethodBox.getValue();
        boolean ok = AppContext.deposit(username, amount);
        if (ok) {
            refreshBalance();
            depositAmountField.clear();
            showResult(depositResultLabel,
                    "✅  Nạp " + formatVND(amount) + " qua " + method + " thành công!", true);
            loadTransactions();
        } else {
            showResult(depositResultLabel, "❌  Nạp tiền thất bại. Vui lòng thử lại.", false);
        }
    }

    // =========================================================
    // THANH TOÁN ĐƠN ĐẤU GIÁ
    // =========================================================

    /**
     * Lấy các HistoryRecord có wonBid=true và status="CHỜ XỬ LÝ"
     * → đây là những đơn thắng đấu giá chưa thanh toán.
     */
    private void loadPendingOrders() {
        pendingOrdersBox.getChildren().clear();
        paymentResultLabel.setVisible(false);

        List<AppContext.HistoryRecord> pending = AppContext.getHistory(username)
                .stream()
                .filter(r -> r.wonBid() && "CHỜ XỬ LÝ".equals(r.status()))
                .collect(Collectors.toList());

        if (pending.isEmpty()) {
            VBox emptyBox = new VBox(8);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setStyle("-fx-padding: 32 0 32 0;");

            Label icon = new Label("🎉");
            icon.setStyle("-fx-font-size: 36px;");
            Label msg = new Label("Không có đơn nào chờ thanh toán.");
            msg.setStyle("-fx-text-fill:#64748b; -fx-font-size:14px;");
            Label hint = new Label("Khi bạn thắng một phiên đấu giá, đơn sẽ xuất hiện tại đây.");
            hint.setStyle("-fx-text-fill:#475569; -fx-font-size:12px;");
            hint.setWrapText(true);

            emptyBox.getChildren().addAll(icon, msg, hint);
            pendingOrdersBox.getChildren().add(emptyBox);
            return;
        }

        for (AppContext.HistoryRecord record : pending) {
            pendingOrdersBox.getChildren().add(buildOrderCard(record));
        }
    }

    /**
     * Tạo card cho mỗi đơn thắng đấu giá chờ thanh toán.
     */
    private VBox buildOrderCard(AppContext.HistoryRecord record) {
        VBox card = new VBox(12);
        card.setStyle("""
                -fx-background-color: #1e293b;
                -fx-background-radius: 10;
                -fx-border-color: #334155;
                -fx-border-radius: 10;
                -fx-border-width: 1;
                -fx-padding: 16 20 16 20;
                """);

        // ── Tiêu đề đơn ───────────────────────────────────
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label trophy = new Label("🏆");
        trophy.setStyle("-fx-font-size: 20px;");

        VBox titleInfo = new VBox(3);
        HBox.setHgrow(titleInfo, Priority.ALWAYS);

        Label itemName = new Label(record.itemName());
        itemName.setStyle("-fx-text-fill:#e2e8f0; -fx-font-size:15px; -fx-font-weight:bold;");

        Label seller = new Label("Người bán: " + record.counterparty());
        seller.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px;");

        Label timeLabel = new Label("Thắng lúc: " + record.time().format(DT_FMT));
        timeLabel.setStyle("-fx-text-fill:#475569; -fx-font-size:11px;");

        titleInfo.getChildren().addAll(itemName, seller, timeLabel);

        // Badge trạng thái
        Label badge = new Label("⏳ CHỜ THANH TOÁN");
        badge.setStyle("""
                -fx-background-color: #1c2a1e;
                -fx-text-fill: #fbbf24;
                -fx-font-size: 11px;
                -fx-font-weight: bold;
                -fx-background-radius: 6;
                -fx-padding: 4 10 4 10;
                -fx-border-color: #fbbf24;
                -fx-border-radius: 6;
                -fx-border-width: 1;
                """);

        titleRow.getChildren().addAll(trophy, titleInfo, badge);

        // ── Separator ─────────────────────────────────────
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #334155;");

        // ── Giá + nút thanh toán ──────────────────────────
        HBox actionRow = new HBox(16);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        VBox priceBox = new VBox(3);
        HBox.setHgrow(priceBox, Priority.ALWAYS);
        Label priceLabel = new Label("Số tiền cần thanh toán");
        priceLabel.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px;");
        Label priceValue = new Label(formatVND(record.amount()));
        priceValue.setStyle("-fx-text-fill:#38bdf8; -fx-font-size:20px; -fx-font-weight:bold;");
        priceBox.getChildren().addAll(priceLabel, priceValue);

        // Kiểm tra số dư đủ không
        double balance = AppContext.getWalletBalance(username);
        boolean canPay = balance >= record.amount();

        Button payBtn = new Button(canPay ? "💳  THANH TOÁN NGAY" : "❌  Số dư không đủ");
        payBtn.setStyle(canPay
                ? "-fx-background-color:#2563eb; -fx-text-fill:white; "
                  + "-fx-font-weight:bold; -fx-font-size:13px; "
                  + "-fx-background-radius:8; -fx-padding:10 20 10 20; -fx-cursor:hand;"
                : "-fx-background-color:#374151; -fx-text-fill:#6b7280; "
                  + "-fx-font-weight:bold; -fx-font-size:13px; "
                  + "-fx-background-radius:8; -fx-padding:10 20 10 20;");
        payBtn.setDisable(!canPay);

        if (canPay) {
            payBtn.setOnAction(e -> handlePayOrder(record, payBtn, card));
        }

        actionRow.getChildren().addAll(priceBox, payBtn);

        // Cảnh báo nếu không đủ tiền
        if (!canPay) {
            HBox warnBox = new HBox(8);
            warnBox.setAlignment(Pos.CENTER_LEFT);
            warnBox.setStyle("""
                    -fx-background-color:#2d1515;
                    -fx-background-radius:8;
                    -fx-padding:10 14 10 14;
                    -fx-border-color:#ef4444;
                    -fx-border-radius:8;
                    -fx-border-width:1;
                    """);
            Label warnIcon = new Label("⚠️");
            warnIcon.setStyle("-fx-font-size:14px;");
            Label warnText = new Label(
                    "Số dư ví không đủ. Cần nạp thêm "
                    + formatVND(record.amount() - balance) + " để thanh toán.");
            warnText.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:12px;");
            warnText.setWrapText(true);
            warnBox.getChildren().addAll(warnIcon, warnText);

            card.getChildren().addAll(titleRow, sep, actionRow, warnBox);
        } else {
            card.getChildren().addAll(titleRow, sep, actionRow);
        }

        return card;
    }

    /**
     * Xử lý thanh toán cho một đơn cụ thể.
     */
    private void handlePayOrder(AppContext.HistoryRecord record,
                                 Button payBtn, VBox card) {
        paymentResultLabel.setVisible(false);

        double balance = AppContext.getWalletBalance(username);
        if (balance < record.amount()) {
            showResult(paymentResultLabel,
                    "❌  Số dư không đủ. Cần nạp thêm "
                    + formatVND(record.amount() - balance) + ".", false);
            return;
        }

        String desc = "Thanh toán đấu giá: " + record.itemName()
                + " (Mã: " + record.id() + ")";
        boolean ok = AppContext.payment(username, record.amount(), desc);

        if (ok) {
            // Cập nhật trạng thái HistoryRecord → "THÀNH CÔNG"
            List<AppContext.HistoryRecord> list = AppContext.getHistory(username);
            for (int i = 0; i < list.size(); i++) {
                AppContext.HistoryRecord r = list.get(i);
                if (r.id().equals(record.id())) {
                    list.set(i, new AppContext.HistoryRecord(
                            r.id(), r.itemName(), r.amount(),
                            r.counterparty(), "THÀNH CÔNG",
                            r.wonBid(), r.time()));
                    break;
                }
            }

            refreshBalance();
            loadTransactions();

            // Hiện card "đã thanh toán" thay vì xóa
            card.setStyle(card.getStyle()
                    .replace("#1e293b", "#0f2a1a")
                    .replace("#334155", "#166534"));
            card.getChildren().removeIf(n -> n instanceof HBox hb
                    && hb.getChildren().stream()
                        .anyMatch(c -> c instanceof Button));

            HBox doneRow = new HBox(8);
            doneRow.setAlignment(Pos.CENTER);
            doneRow.setStyle("-fx-padding: 8 0 0 0;");
            Label doneIcon = new Label("✅");
            doneIcon.setStyle("-fx-font-size:16px;");
            Label doneText = new Label("Đã thanh toán thành công "
                    + formatVND(record.amount()));
            doneText.setStyle("-fx-text-fill:#4ade80; -fx-font-size:14px; "
                    + "-fx-font-weight:bold;");
            doneRow.getChildren().addAll(doneIcon, doneText);
            card.getChildren().add(doneRow);

            showResult(paymentResultLabel,
                    "✅  Thanh toán " + record.itemName()
                    + " – " + formatVND(record.amount()) + " thành công!", true);
        } else {
            showResult(paymentResultLabel,
                    "❌  Thanh toán thất bại. Vui lòng thử lại.", false);
        }
    }

    // =========================================================
    // LỊCH SỬ GIAO DỊCH
    // =========================================================
    private void loadTransactions() {
        transactionListBox.getChildren().clear();

        List<AppContext.TransactionRecord> list = AppContext.getTransactions(username);

        if (list.isEmpty()) {
            Label empty = new Label("Chưa có giao dịch nào.");
            empty.setStyle("-fx-text-fill:#64748b; -fx-font-size:14px;");
            transactionListBox.getChildren().add(empty);
            return;
        }

        for (int i = list.size() - 1; i >= 0; i--) {
            AppContext.TransactionRecord tx = list.get(i);

            HBox row = new HBox(12);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("""
                    -fx-background-color: #1e293b;
                    -fx-background-radius: 8;
                    -fx-padding: 14 16 14 16;
                    """);

            Label icon = new Label(tx.amount() >= 0 ? "⬆" : "⬇");
            icon.setStyle("-fx-font-size:18px; -fx-text-fill:"
                    + (tx.amount() >= 0 ? "#22c55e" : "#f87171") + ";");

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label typeLabel = new Label(tx.type());
            typeLabel.setStyle("-fx-font-weight:bold; -fx-text-fill:#e2e8f0; "
                    + "-fx-font-size:14px;");
            Label descLabel = new Label(tx.description());
            descLabel.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px;");
            Label timeLabel = new Label(tx.time().format(DT_FMT));
            timeLabel.setStyle("-fx-text-fill:#475569; -fx-font-size:11px;");

            info.getChildren().addAll(typeLabel, descLabel, timeLabel);

            VBox amountBox = new VBox();
            amountBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            boolean isPositive = tx.amount() >= 0;
            Label amountLabel = new Label(
                    (isPositive ? "+" : "") + formatVND(tx.amount()));
            amountLabel.setStyle("-fx-font-weight:bold; -fx-font-size:15px; -fx-text-fill:"
                    + (isPositive ? "#22c55e" : "#f87171") + ";");
            Label statusLabel = new Label(tx.status());
            statusLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b;");

            amountBox.getChildren().addAll(amountLabel, statusLabel);
            row.getChildren().addAll(icon, info, amountBox);
            transactionListBox.getChildren().add(row);
        }
    }

    // =========================================================
    // NAVIGATION
    // =========================================================
    @FXML private void handleBack() {
        try { HelloApplication.showMainView(); }
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

    @FXML private void handleLogout() {
        try { AppContext.logout(); HelloApplication.showLoginView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================
    // UTIL
    // =========================================================
    private String formatVND(double amount) {
        return String.format("₫ %,.0f", amount);
    }

    private void showResult(Label label, String msg, boolean success) {
        label.setText(msg);
        label.setStyle(success
                ? "-fx-text-fill:#22c55e; -fx-font-size:13px;"
                : "-fx-text-fill:#f87171; -fx-font-size:13px;");
        label.setVisible(true);
    }
}
