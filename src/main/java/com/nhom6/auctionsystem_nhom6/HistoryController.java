package com.nhom6.auctionsystem_nhom6;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import org.example.user.User;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class HistoryController {

    @FXML private Label      pageTitleLabel;
    @FXML private Label      totalCountLabel;
    @FXML private Label      totalCountDesc;
    @FXML private Label      successCountLabel;
    @FXML private Label      successCountDesc;
    @FXML private Label      pendingCountLabel;
    @FXML private Label      pendingCountDesc;
    @FXML private Label      totalValueLabel;
    @FXML private Label      totalValueDesc;
    @FXML private Button     tab1Btn;
    @FXML private Button     tab2Btn;
    @FXML private VBox       historyListBox;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField  searchField;

    private boolean isSeller;
    private int     currentTab = 0;
    private List<AppContext.HistoryRecord> allRecords;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @FXML
    public void initialize() {
        User user = AppContext.getCurrentUser();
        isSeller  = "SELLER".equalsIgnoreCase(user.getRole());

        // ── Tiêu đề & nhãn theo role ──────────────────────────
        if (isSeller) {
            pageTitleLabel.setText("Lịch sử bán hàng");
            tab2Btn.setText("🏆  Đơn hoàn thành");
            totalCountDesc.setText("Tổng đơn bán");
            successCountDesc.setText("Hoàn thành");
            pendingCountDesc.setText("Chờ thanh toán");
            totalValueDesc.setText("Doanh thu");
        } else {
            pageTitleLabel.setText("Lịch sử mua hàng");
            tab2Btn.setText("🏆  Đơn thắng giá");
            totalCountDesc.setText("Lượt đặt giá");
            successCountDesc.setText("Thắng giá");
            pendingCountDesc.setText("Đang đấu giá");
            totalValueDesc.setText("Tổng chi");
        }

        statusFilter.getItems().addAll(
                "Tất cả", "THÀNH CÔNG", "CHỜ XỬ LÝ", "THẤT BẠI", "ĐÃ HỦY");

        // ── Lấy dữ liệu thật từ AppContext ───────────────────
        allRecords = AppContext.getHistory(user.getUsername());

        refreshStats();
        renderList(allRecords);
    }

    // ── Thống kê ──────────────────────────────────────────────
    private void refreshStats() {
        long total   = allRecords.size();
        long success = allRecords.stream()
                .filter(r -> "THÀNH CÔNG".equals(r.status())).count();
        long pending = allRecords.stream()
                .filter(r -> "CHỜ XỬ LÝ".equals(r.status())).count();
        double value = allRecords.stream()
                .filter(r -> "THÀNH CÔNG".equals(r.status()))
                .mapToDouble(AppContext.HistoryRecord::amount).sum();

        totalCountLabel.setText(String.valueOf(total));
        successCountLabel.setText(String.valueOf(success));
        pendingCountLabel.setText(String.valueOf(pending));
        totalValueLabel.setText(formatVND(value));
    }

    // ── Render danh sách ──────────────────────────────────────
    private void renderList(List<AppContext.HistoryRecord> records) {
        historyListBox.getChildren().clear();

        if (records.isEmpty()) {
            VBox empty = buildEmptyState();
            historyListBox.getChildren().add(empty);
            return;
        }

        for (AppContext.HistoryRecord r : records) {
            historyListBox.getChildren().add(buildRow(r));
        }
    }

    private VBox buildEmptyState() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60, 0, 60, 0));

        Label icon = new Label(isSeller ? "📦" : "🛒");
        icon.setStyle("-fx-font-size: 48px;");

        Label msg = new Label(isSeller
                ? "Chưa có đơn bán nào.\nKhi có người mua sản phẩm, đơn sẽ hiển thị ở đây."
                : "Chưa có lịch sử mua hàng.\nKhi bạn thắng đấu giá, đơn sẽ hiển thị ở đây.");
        msg.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-text-alignment: center;");
        msg.setWrapText(true);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        box.getChildren().addAll(icon, msg);
        return box;
    }

    private HBox buildRow(AppContext.HistoryRecord r) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");
        row.setPadding(new Insets(14, 20, 14, 20));

        // Icon trạng thái
        Label icon = new Label(statusIcon(r.status()));
        icon.setMinWidth(32);
        icon.setStyle("-fx-font-size: 20px;");

        // Thông tin chính
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(r.itemName());
        name.getStyleClass().add("history-item-name");

        String counterLabel = isSeller ? "Người mua: " : "Người bán: ";
        Label meta = new Label(
                counterLabel + r.counterparty()
                + "   •   " + r.time().format(DT_FMT)
                + "   •   ID: " + r.id());
        meta.getStyleClass().add("history-item-meta");

        info.getChildren().addAll(name, meta);

        // Giá + badge
        VBox priceBox = new VBox(4);
        priceBox.setAlignment(Pos.CENTER_RIGHT);

        Label price = new Label(formatVND(r.amount()));
        price.getStyleClass().add("history-item-price");

        Label badge = new Label(r.status());
        badge.getStyleClass().addAll("history-badge", badgeStyle(r.status()));
        badge.setAlignment(Pos.CENTER_RIGHT);

        priceBox.getChildren().addAll(price, badge);

        // Nút chi tiết
        Button detailBtn = new Button("Chi tiết →");
        detailBtn.getStyleClass().add("btn-secondary");
        detailBtn.setOnAction(e -> showDetail(r));

        row.getChildren().addAll(icon, info, priceBox, detailBtn);
        return row;
    }

    // ── Tab handlers ──────────────────────────────────────────
    @FXML private void handleTab1() {
        currentTab = 0;
        tab1Btn.getStyleClass().setAll("tab-btn", "tab-active");
        tab2Btn.getStyleClass().setAll("tab-btn");
        applyFilters();
    }

    @FXML private void handleTab2() {
        currentTab = 1;
        tab2Btn.getStyleClass().setAll("tab-btn", "tab-active");
        tab1Btn.getStyleClass().setAll("tab-btn");
        applyFilters();
    }

    @FXML private void handleFilter() { applyFilters(); }
    @FXML private void handleSearch() { applyFilters(); }

    private void applyFilters() {
        String keyword = searchField.getText().trim().toLowerCase();
        String status  = statusFilter.getValue();

        List<AppContext.HistoryRecord> filtered = allRecords.stream()
            .filter(r -> {
                if (currentTab == 1 && !"THÀNH CÔNG".equals(r.status())) return false;
                if (status != null && !status.isEmpty()
                        && !"Tất cả".equals(status)
                        && !status.equals(r.status())) return false;
                if (!keyword.isEmpty()
                        && !r.itemName().toLowerCase().contains(keyword)
                        && !r.counterparty().toLowerCase().contains(keyword)
                        && !r.id().toLowerCase().contains(keyword)) return false;
                return true;
            })
            .collect(Collectors.toList());

        renderList(filtered);
    }

    private void showDetail(AppContext.HistoryRecord r) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi tiết đơn hàng");
        alert.setHeaderText(r.itemName());
        alert.setContentText(
                "Mã đơn    : " + r.id()               + "\n" +
                "Giá trị   : " + formatVND(r.amount()) + "\n" +
                (isSeller ? "Người mua : " : "Người bán : ") + r.counterparty() + "\n" +
                "Thời gian : " + r.time().format(DT_FMT) + "\n" +
                "Trạng thái: " + r.status()
        );
        alert.showAndWait();
    }

    @FXML private void handleBack() {
        try { HelloApplication.showMainView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // ── Helpers ───────────────────────────────────────────────
    private String statusIcon(String status) {
        return switch (status) {
            case "THÀNH CÔNG" -> "✅";
            case "CHỜ XỬ LÝ" -> "⏳";
            case "THẤT BẠI"  -> "❌";
            case "ĐÃ HỦY"    -> "🚫";
            default           -> "📄";
        };
    }

    private String badgeStyle(String status) {
        return switch (status) {
            case "THÀNH CÔNG" -> "badge-success";
            case "CHỜ XỬ LÝ" -> "badge-warn";
            case "THẤT BẠI"  -> "badge-danger";
            case "ĐÃ HỦY"    -> "badge-neutral";
            default           -> "badge-neutral";
        };
    }

    private String formatVND(double v) {
        return String.format("₫ %,.0f", v);
    }
}
