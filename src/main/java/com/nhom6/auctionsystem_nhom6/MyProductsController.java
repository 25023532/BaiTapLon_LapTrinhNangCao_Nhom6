package com.nhom6.auctionsystem_nhom6;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MyProductsController {

    @FXML private Label      totalProductsLabel;
    @FXML private Label      activeProductsLabel;
    @FXML private Label      soldProductsLabel;
    @FXML private Label      revenueLabel;
    @FXML private VBox       productListBox;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField  searchField;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    record ProductRecord(
            String id,
            String name,
            String category,
            double startPrice,
            double currentPrice,
            int bidCount,
            String status,      // ĐANG ĐẤU GIÁ | ĐÃ BÁN | CHỜ DUYỆT | HẾT HẠN | ĐÃ HỦY
            LocalDateTime endTime,
            String topBidder
    ) {}

    private List<ProductRecord> allProducts = new ArrayList<>();

    @FXML
    public void initialize() {
        statusFilter.getItems().addAll(
                "Tất cả", "ĐANG ĐẤU GIÁ", "ĐÃ BÁN", "CHỜ DUYỆT", "HẾT HẠN", "ĐÃ HỦY");
        loadSampleData();
        refreshStats();
        renderList(allProducts);
    }

    // ── Dữ liệu mẫu ──────────────────────────────────────────
    private void loadSampleData() {
        allProducts = new ArrayList<>(List.of(
            new ProductRecord("P-001", "MacBook Pro M3 – 18GB RAM, 512GB SSD",
                    "Laptop", 22_000_000, 22_000_000, 0,
                    "ĐANG ĐẤU GIÁ",
                    LocalDateTime.now().plusHours(2), "—"),
            new ProductRecord("P-002", "iPhone 15 Pro Max 256GB – Titanium",
                    "Điện thoại", 25_000_000, 28_500_000, 7,
                    "ĐÃ BÁN",
                    LocalDateTime.now().minusDays(2), "nguyen_tran"),
            new ProductRecord("P-003", "Sony Alpha A7 IV – Body Only",
                    "Máy ảnh", 40_000_000, 45_000_000, 12,
                    "ĐÃ BÁN",
                    LocalDateTime.now().minusDays(5), "camera_pro"),
            new ProductRecord("P-004", "Apple Watch Ultra 2 – 49mm",
                    "Đồng hồ", 17_000_000, 17_000_000, 0,
                    "CHỜ DUYỆT",
                    LocalDateTime.now().plusDays(1), "—"),
            new ProductRecord("P-005", "Dell XPS 15 9530 – i9 RTX 4060",
                    "Laptop", 30_000_000, 32_000_000, 5,
                    "ĐÃ BÁN",
                    LocalDateTime.now().minusDays(10), "laptop_dev"),
            new ProductRecord("P-006", "Nikon Z6 III – Body",
                    "Máy ảnh", 35_000_000, 35_000_000, 2,
                    "ĐANG ĐẤU GIÁ",
                    LocalDateTime.now().plusHours(6), "photo_viet"),
            new ProductRecord("P-007", "DJI Mini 4 Pro Combo",
                    "Điện tử", 15_000_000, 15_000_000, 0,
                    "HẾT HẠN",
                    LocalDateTime.now().minusDays(1), "—")
        ));
    }

    // ── Thống kê ──────────────────────────────────────────────
    private void refreshStats() {
        long total  = allProducts.size();
        long active = allProducts.stream()
                .filter(p -> "ĐANG ĐẤU GIÁ".equals(p.status())).count();
        long sold   = allProducts.stream()
                .filter(p -> "ĐÃ BÁN".equals(p.status())).count();
        double rev  = allProducts.stream()
                .filter(p -> "ĐÃ BÁN".equals(p.status()))
                .mapToDouble(ProductRecord::currentPrice).sum();

        totalProductsLabel.setText(String.valueOf(total));
        activeProductsLabel.setText(String.valueOf(active));
        soldProductsLabel.setText(String.valueOf(sold));
        revenueLabel.setText(formatVND(rev));
    }

    // ── Render ────────────────────────────────────────────────
    private void renderList(List<ProductRecord> list) {
        productListBox.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label("Không có sản phẩm nào.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
            productListBox.getChildren().add(empty);
            return;
        }
        for (ProductRecord p : list) {
            productListBox.getChildren().add(buildProductRow(p));
        }
    }

    private HBox buildProductRow(ProductRecord p) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");
        row.setPadding(new Insets(14, 20, 14, 20));

        // Thumbnail placeholder
        Label thumb = new Label("⬡");
        thumb.setStyle("-fx-font-size: 28px; -fx-text-fill: #3b82f6; "
                + "-fx-background-color: #1e3a5f; -fx-padding: 8px 12px; "
                + "-fx-background-radius: 8px; -fx-min-width: 56px; -fx-alignment: center;");

        // Info
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(p.name());
        name.getStyleClass().add("history-item-name");

        HBox metaRow = new HBox(16);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label cat = new Label("📂 " + p.category());
        cat.getStyleClass().add("history-item-meta");

        Label bids = new Label("🔨 " + p.bidCount() + " lượt bid");
        bids.getStyleClass().add("history-item-meta");

        String timeLabel = "ĐANG ĐẤU GIÁ".equals(p.status())
                ? "⏰ Kết thúc: " + p.endTime().format(DT_FMT)
                : "🕐 " + p.endTime().format(DT_FMT);
        Label time = new Label(timeLabel);
        time.getStyleClass().add("history-item-meta");

        metaRow.getChildren().addAll(cat, bids, time);

        // Nếu đã bán: hiện người thắng
        if ("ĐÃ BÁN".equals(p.status())) {
            Label winner = new Label("🏆 Người thắng: " + p.topBidder());
            winner.getStyleClass().add("history-item-meta");
            winner.setStyle("-fx-text-fill: #22d3ee;");
            metaRow.getChildren().add(winner);
        }

        info.getChildren().addAll(name, metaRow);

        // Giá + badge
        VBox priceBox = new VBox(4);
        priceBox.setAlignment(Pos.CENTER_RIGHT);

        VBox prices = new VBox(2);
        prices.setAlignment(Pos.CENTER_RIGHT);
        Label startP = new Label("Khởi điểm: " + formatVND(p.startPrice()));
        startP.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        Label curP = new Label(formatVND(p.currentPrice()));
        curP.getStyleClass().add("history-item-price");
        prices.getChildren().addAll(startP, curP);

        Label badge = new Label(p.status());
        badge.getStyleClass().addAll("history-badge", productBadgeStyle(p.status()));

        priceBox.getChildren().addAll(prices, badge);

        // Action buttons
        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);

        Button editBtn = new Button("✏️ Sửa");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setDisable(!"CHỜ DUYỆT".equals(p.status())
                && !"ĐANG ĐẤU GIÁ".equals(p.status()));
        editBtn.setOnAction(e -> handleEdit(p));

        Button deleteBtn = new Button("🗑 Xóa");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setDisable("ĐÃ BÁN".equals(p.status())
                || "ĐANG ĐẤU GIÁ".equals(p.status()));
        deleteBtn.setOnAction(e -> handleDelete(p));

        actions.getChildren().addAll(editBtn, deleteBtn);

        row.getChildren().addAll(thumb, info, priceBox, actions);
        return row;
    }

    // ── Handlers ──────────────────────────────────────────────
    @FXML private void handleFilter() { applyFilters(); }
    @FXML private void handleSearch() { applyFilters(); }

    private void applyFilters() {
        String keyword = searchField.getText().trim().toLowerCase();
        String status  = statusFilter.getValue();

        List<ProductRecord> filtered = allProducts.stream()
            .filter(p -> {
                if (status != null && !status.isEmpty()
                        && !"Tất cả".equals(status)
                        && !status.equals(p.status())) return false;
                if (!keyword.isEmpty()
                        && !p.name().toLowerCase().contains(keyword)
                        && !p.category().toLowerCase().contains(keyword)
                        && !p.id().toLowerCase().contains(keyword)) return false;
                return true;
            })
            .collect(Collectors.toList());

        renderList(filtered);
    }

    @FXML
    private void handleAddProduct() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Đăng bán sản phẩm");
        alert.setHeaderText("Tính năng đang phát triển");
        alert.setContentText("Form đăng bán sản phẩm mới sẽ được mở ở đây.");
        alert.showAndWait();
    }

    private void handleEdit(ProductRecord p) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chỉnh sửa sản phẩm");
        alert.setHeaderText(p.name());
        alert.setContentText("Form chỉnh sửa sản phẩm sẽ được mở ở đây.\nID: " + p.id());
        alert.showAndWait();
    }

    private void handleDelete(ProductRecord p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa sản phẩm: " + p.name());
        confirm.setContentText("Bạn có chắc muốn xóa sản phẩm này không?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                allProducts.removeIf(r -> r.id().equals(p.id()));
                refreshStats();
                renderList(allProducts);
            }
        });
    }

    @FXML private void handleBack() {
        try { HelloApplication.showMainView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // ── Helpers ───────────────────────────────────────────────
    private String productBadgeStyle(String status) {
        return switch (status) {
            case "ĐANG ĐẤU GIÁ" -> "badge-info";
            case "ĐÃ BÁN"       -> "badge-success";
            case "CHỜ DUYỆT"    -> "badge-warn";
            case "HẾT HẠN",
                 "ĐÃ HỦY"       -> "badge-neutral";
            default              -> "badge-neutral";
        };
    }

    private String formatVND(double v) {
        return String.format("₫ %,.0f", v);
    }
}
