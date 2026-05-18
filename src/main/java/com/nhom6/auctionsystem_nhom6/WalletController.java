package com.nhom6.auctionsystem_nhom6;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class WalletController {

    // =========================================================
    // HEADER
    // =========================================================

    @FXML private Label userAvatarLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label walletHeaderLabel;   // số dư ở header

    // =========================================================
    // WALLET SUMMARY CARD
    // =========================================================

    @FXML private Label balanceLabel;        // số dư lớn ở giữa
    @FXML private Label balanceSubLabel;     // "Số dư khả dụng"

    // =========================================================
    // TABS
    // =========================================================

    @FXML private Button tabDepositBtn;
    @FXML private Button tabPaymentBtn;
    @FXML private Button tabHistoryBtn;

    // =========================================================
    // PANELS
    // =========================================================

    @FXML private VBox depositPanel;
    @FXML private VBox paymentPanel;
    @FXML private VBox historyPanel;

    // ── Nạp tiền ─────────────────────────────────────────────

    @FXML private TextField depositAmountField;
    @FXML private Label     depositMethodLabel;
    @FXML private ComboBox<String> depositMethodBox;
    @FXML private Label     depositResultLabel;

    // ── Thanh toán ───────────────────────────────────────────

    @FXML private TextField paymentAmountField;
    @FXML private TextField paymentDescField;
    @FXML private Label     paymentBalanceLabel;
    @FXML private Label     paymentResultLabel;

    // ── Lịch sử ──────────────────────────────────────────────

    @FXML private VBox transactionListBox;

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
                "Ngân hàng VCB",
                "Ngân hàng TCB",
                "MoMo",
                "ZaloPay",
                "VNPay"
        );
        depositMethodBox.getSelectionModel().selectFirst();

        // Ẩn result labels ban đầu
        depositResultLabel.setVisible(false);
        paymentResultLabel.setVisible(false);

        // Hiện tab mặc định
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
        paymentBalanceLabel.setText("Số dư hiện tại: " + fmt);
    }

    // =========================================================
    // TAB SWITCHING
    // =========================================================

    @FXML private void handleTabDeposit()  { showTab("deposit"); }
    @FXML private void handleTabPayment()  { showTab("payment"); }
    @FXML private void handleTabHistory()  { showTab("history"); loadTransactions(); }

    private void showTab(String tab) {

        depositPanel.setVisible(false);  depositPanel.setManaged(false);
        paymentPanel.setVisible(false);  paymentPanel.setManaged(false);
        historyPanel.setVisible(false);  historyPanel.setManaged(false);

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

    @FXML
    private void handleQuickDeposit1() { depositAmountField.setText("100000"); }
    @FXML
    private void handleQuickDeposit2() { depositAmountField.setText("500000"); }
    @FXML
    private void handleQuickDeposit3() { depositAmountField.setText("1000000"); }
    @FXML
    private void handleQuickDeposit4() { depositAmountField.setText("5000000"); }

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
    // THANH TOÁN
    // =========================================================

    @FXML
    private void handlePayment() {

        paymentResultLabel.setVisible(false);

        String raw = paymentAmountField.getText().trim().replaceAll("[^0-9]", "");
        String desc = paymentDescField.getText().trim();

        if (raw.isEmpty()) {
            showResult(paymentResultLabel, "❌  Vui lòng nhập số tiền.", false);
            return;
        }
        if (desc.isEmpty()) {
            showResult(paymentResultLabel, "❌  Vui lòng nhập nội dung thanh toán.", false);
            return;
        }

        double amount = Double.parseDouble(raw);
        if (amount < 1_000) {
            showResult(paymentResultLabel, "❌  Số tiền tối thiểu là 1,000 ₫.", false);
            return;
        }

        double balance = AppContext.getWalletBalance(username);
        if (balance < amount) {
            showResult(paymentResultLabel,
                    "❌  Số dư không đủ. Hiện có: " + formatVND(balance), false);
            return;
        }

        boolean ok = AppContext.payment(username, amount, desc);

        if (ok) {
            refreshBalance();
            paymentAmountField.clear();
            paymentDescField.clear();
            showResult(paymentResultLabel,
                    "✅  Thanh toán " + formatVND(amount) + " thành công!", true);
            loadTransactions();
        } else {
            showResult(paymentResultLabel, "❌  Thanh toán thất bại. Vui lòng thử lại.", false);
        }
    }

    // =========================================================
    // LỊCH SỬ GIAO DỊCH
    // =========================================================

    private void loadTransactions() {

        transactionListBox.getChildren().clear();

        List<AppContext.TransactionRecord> list =
                AppContext.getTransactions(username);

        if (list.isEmpty()) {
            Label empty = new Label("Chưa có giao dịch nào.");
            empty.setStyle("-fx-text-fill:#64748b; -fx-font-size:14px;");
            transactionListBox.getChildren().add(empty);
            return;
        }

        // Hiện mới nhất trên đầu
        for (int i = list.size() - 1; i >= 0; i--) {

            AppContext.TransactionRecord tx = list.get(i);

            HBox row = new HBox(12);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("""
                    -fx-background-color: #1e293b;
                    -fx-background-radius: 8;
                    -fx-padding: 14 16 14 16;
                    """);

            // Icon
            Label icon = new Label(tx.amount() >= 0 ? "⬆" : "⬇");
            icon.setStyle("-fx-font-size:18px; -fx-text-fill:"
                    + (tx.amount() >= 0 ? "#22c55e" : "#f87171") + ";");

            // Info
            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label typeLabel = new Label(tx.type());
            typeLabel.setStyle("-fx-font-weight:bold; -fx-text-fill:#e2e8f0; -fx-font-size:14px;");

            Label descLabel = new Label(tx.description());
            descLabel.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px;");

            Label timeLabel = new Label(tx.time().format(DT_FMT));
            timeLabel.setStyle("-fx-text-fill:#475569; -fx-font-size:11px;");

            info.getChildren().addAll(typeLabel, descLabel, timeLabel);

            // Amount
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

    @FXML private void handleRating() {
        try { HelloApplication.showRatingView(); }
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
                ? "-fx-text-fill: #22c55e; -fx-font-size:13px;"
                : "-fx-text-fill: #f87171; -fx-font-size:13px;");
        label.setVisible(true);
    }
}
