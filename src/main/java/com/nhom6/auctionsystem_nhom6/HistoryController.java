package com.nhom6.auctionsystem_nhom6;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import org.example.user.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    // Tab: 0 = tất cả, 1 = tab thứ 2
    private int currentTab = 0;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    /** Model đơn giản cho một bản ghi lịch sử */
    record HistoryRecord(
            String id,
            String itemName,
            double amount,
            String counterparty,   // người mua (seller view) hoặc người bán (bidder view)
            String status,         // THÀNH CÔNG | CHỜ XỬ LÝ | THẤT BẠI | ĐÃ HỦY
            boolean wonBid,        // bidder: có thắng không
            LocalDateTime time
    ) {}

    private List<HistoryRecord> allRecords = new ArrayList<>();

    @FXML
    public void initialize() {
        User user = AppContext.getCurrentUser();
        isSeller  = "SELLER".equalsIgnoreCase(user.getRole());

        // Tiêu đề + tab theo role
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

        // Bộ lọc
        statusFilter.getItems().addAll(
                "Tất cả", "THÀNH CÔNG", "CHỜ XỬ LÝ", "THẤT BẠI", "ĐÃ HỦY");

        // Load dữ liệu mẫu
        loadSampleData(user.getUsername());
        refreshStats();
        renderList(allRecords);
    }

    // ── Dữ liệu mẫu ──────────────────────────────────────────
    private void loadSampleData(String username) {
        if (isSeller) {
            allRecords = List.of(
                new HistoryRecord("ORD-001", "MacBook Pro M3 18GB",
                        22_000_000, "bidder07",    "THÀNH CÔNG", true,
                        LocalDateTime.now().minusDays(1)),
                new HistoryRecord("ORD-002", "iPhone 15 Pro Max 256GB",
                        28_500_000, "nguyen_tran", "CHỜ XỬ LÝ",  false,
                        LocalDateTime.now().minusDays(2)),
                new HistoryRecord("ORD-003", "Sony Alpha A7 IV",
                        45_000_000, "camera_pro",  "THÀNH CÔNG", true,
                        LocalDateTime.now().minusDays(5)),
                new HistoryRecord("ORD-004", "Apple Watch Ultra 2",
                        18_900_000, "watch_fan",   "ĐÃ HỦY",    false,
                        LocalDateTime.now().minusDays(7)),
                new HistoryRecord("ORD-005", "Dell XPS 15 9530",
                        32_000_000, "laptop_dev",  "THÀNH CÔNG", true,
                        LocalDateTime.now().minusDays(10)),
                new HistoryRecord("ORD-006", "Nikon Z6 III Body",
                        38_500_000, "photo_viet",  "CHỜ XỬ LÝ", false,
                        LocalDateTime.now().minusHours(3))
            );
        } else {
            allRecords = List.of(
                new HistoryRecord("BID-001", "MacBook Pro M3 18GB",
                        22_000_000, "SellerLong",  "THÀNH CÔNG", true,
                        LocalDateTime.now().minusDays(1)),
                new HistoryRecord("BID-002", "iPhone 15 Pro Max 256GB",
                        27_500_000, "TechStore",   "THẤT BẠI",  false,
                        LocalDateTime.now().minusDays(2)),
                new HistoryRecord("BID-003", "Sony Alpha A7 IV",
                        44_000_000, "CameraShop",  "THẤT BẠI",  false,
                        LocalDateTime.now().minusDays(5)),
                new HistoryRecord("BID-004", "Apple Watch Ultra 2",
                        18_900_000, "WatchWorld",  "THÀNH CÔNG", true,
                        LocalDateTime.now().minusDays(7)),
                new HistoryRecord("BID-005", "Dell XPS 15 9530",
                        31_000_000, "LaptopHub",   "CHỜ XỬ LÝ", false,
                        LocalDateTime.now().minusHours(5)),
                new HistoryRecord("BID-006", "DJI Mini 4 Pro Combo",
                        16_500_000, "DroneViet",   "ĐÃ HỦY",   false,
                        LocalDateTime.now().minusDays(12))
            );
        }
        // Đổi thành mutable list để filter sau
        allRecords = new ArrayList<>(allRecords);
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
                .mapToDouble(HistoryRecord::amount).sum();

        totalCountLabel.setText(String.valueOf(total));
        successCountLabel.setText(String.valueOf(success));
        pendingCountLabel.setText(String.valueOf(pending));
        totalValueLabel.setText(formatVND(value));
    }

    // ── Render danh sách ──────────────────────────────────────
    private void renderList(List<HistoryRecord> records) {
        historyListBox.getChildren().clear();
        if (records.isEmpty()) {
            Label empty = new Label("Không có dữ liệu.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
            historyListBox.getChildren().add(empty);
            return;
        }
        for (HistoryRecord r : records) {
            historyListBox.getChildren().add(buildRow(r));
        }
    }

    private HBox buildRow(HistoryRecord r) {
        HBox row = new HBox(16);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
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
        Label meta = new Label(counterLabel + r.counterparty()
                + "   •   " + r.time().format(DT_FMT)
                + "   •   ID: " + r.id());
        meta.getStyleClass().add("history-item-meta");

        info.getChildren().addAll(name, meta);

        // Giá
        VBox priceBox = new VBox(4);
        priceBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label price = new Label(formatVND(r.amount()));
        price.getStyleClass().add("history-item-price");

        // Badge trạng thái
        Label badge = new Label(r.status());
        badge.getStyleClass().addAll("history-badge", badgeStyle(r.status()));

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

        List<HistoryRecord> filtered = allRecords.stream()
            .filter(r -> {
                // Tab 2: chỉ đơn thắng / hoàn thành
                if (currentTab == 1 && !"THÀNH CÔNG".equals(r.status())) return false;
                // Lọc trạng thái
                if (status != null && !status.isEmpty()
                        && !"Tất cả".equals(status)
                        && !status.equals(r.status())) return false;
                // Tìm kiếm
                if (!keyword.isEmpty()
                        && !r.itemName().toLowerCase().contains(keyword)
                        && !r.counterparty().toLowerCase().contains(keyword)
                        && !r.id().toLowerCase().contains(keyword)) return false;
                return true;
            })
            .collect(Collectors.toList());

        renderList(filtered);
    }

    // ── Chi tiết ──────────────────────────────────────────────
    private void showDetail(HistoryRecord r) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi tiết đơn hàng");
        alert.setHeaderText(r.itemName());
        alert.setContentText(
                "Mã đơn   : " + r.id()            + "\n" +
                "Giá trị  : " + formatVND(r.amount()) + "\n" +
                (isSeller ? "Người mua: " : "Người bán: ") + r.counterparty() + "\n" +
                "Thời gian: " + r.time().format(DT_FMT) + "\n" +
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
