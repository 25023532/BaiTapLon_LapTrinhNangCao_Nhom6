package com.nhom6.auctionsystem_nhom6;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.user.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MyProductsController {

    @FXML private Label      totalProductsLabel;
    @FXML private Label      activeProductsLabel;
    @FXML private Label      soldProductsLabel;
    @FXML private Label      revenueLabel;
    @FXML private VBox       productListBox;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField  searchField;

    private String username;
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @FXML
    public void initialize() {
        User user = AppContext.getCurrentUser();
        username  = user.getUsername();

        statusFilter.getItems().addAll(
                "Tất cả", "ĐANG ĐẤU GIÁ", "ĐÃ BÁN", "CHỜ DUYỆT", "HẾT HẠN", "ĐÃ HỦY");

        // ── Lấy dữ liệu thật từ AppContext (ban đầu rỗng) ────
        refreshStats();
        renderList(AppContext.getProducts(username));
    }

    // ── Thống kê ──────────────────────────────────────────────
    private void refreshStats() {
        List<AppContext.ProductRecord> list = AppContext.getProducts(username);
        long total  = list.size();
        long active = list.stream()
                .filter(p -> "ĐANG ĐẤU GIÁ".equals(p.status())).count();
        long sold   = list.stream()
                .filter(p -> "ĐÃ BÁN".equals(p.status())).count();
        double rev  = list.stream()
                .filter(p -> "ĐÃ BÁN".equals(p.status()))
                .mapToDouble(AppContext.ProductRecord::currentPrice).sum();

        totalProductsLabel.setText(String.valueOf(total));
        activeProductsLabel.setText(String.valueOf(active));
        soldProductsLabel.setText(String.valueOf(sold));
        revenueLabel.setText(formatVND(rev));
    }

    // ── Render ────────────────────────────────────────────────
    private void renderList(List<AppContext.ProductRecord> list) {
        productListBox.getChildren().clear();

        if (list.isEmpty()) {
            VBox empty = buildEmptyState();
            productListBox.getChildren().add(empty);
            return;
        }

        for (AppContext.ProductRecord p : list) {
            productListBox.getChildren().add(buildProductRow(p));
        }
    }

    private VBox buildEmptyState() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60, 0, 60, 0));

        Label icon = new Label("📦");
        icon.setStyle("-fx-font-size: 48px;");

        Label msg = new Label(
                "Bạn chưa đăng bán sản phẩm nào.\nNhấn \"＋ Đăng bán mới\" để bắt đầu.");
        msg.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-text-alignment: center;");
        msg.setWrapText(true);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        box.getChildren().addAll(icon, msg);
        return box;
    }

    private HBox buildProductRow(AppContext.ProductRecord p) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");
        row.setPadding(new Insets(14, 20, 14, 20));

        // Thumbnail
        Label thumb = new Label("⬡");
        thumb.setStyle("-fx-font-size: 28px; -fx-text-fill: #3b82f6; "
                + "-fx-background-color: #1e3a5f; -fx-padding: 8 12 8 12; "
                + "-fx-background-radius: 8; -fx-min-width: 56; -fx-alignment: center;");

        // Info
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(p.name());
        name.getStyleClass().add("history-item-name");

        HBox metaRow = new HBox(16);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label cat  = new Label("📂 " + p.category());
        Label bids = new Label("🔨 " + p.bidCount() + " lượt bid");
        String timeStr = "ĐANG ĐẤU GIÁ".equals(p.status())
                ? "⏰ Kết thúc: " + p.endTime().format(DT_FMT)
                : "🕐 " + p.endTime().format(DT_FMT);
        Label time = new Label(timeStr);

        cat.getStyleClass().add("history-item-meta");
        bids.getStyleClass().add("history-item-meta");
        time.getStyleClass().add("history-item-meta");
        metaRow.getChildren().addAll(cat, bids, time);

        if ("ĐÃ BÁN".equals(p.status()) && !p.topBidder().equals("—")) {
            Label winner = new Label("🏆 Người thắng: " + p.topBidder());
            winner.getStyleClass().add("history-item-meta");
            winner.setStyle("-fx-text-fill: #22d3ee;");
            metaRow.getChildren().add(winner);
        }

        info.getChildren().addAll(name, metaRow);

        // Giá + badge
        VBox priceBox = new VBox(4);
        priceBox.setAlignment(Pos.CENTER_RIGHT);

        Label startP = new Label("Khởi điểm: " + formatVND(p.startPrice()));
        startP.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        Label curP = new Label(formatVND(p.currentPrice()));
        curP.getStyleClass().add("history-item-price");

        Label badge = new Label(p.status());
        badge.getStyleClass().addAll("history-badge", productBadgeStyle(p.status()));

        priceBox.getChildren().addAll(startP, curP, badge);

        // Buttons
        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);

        boolean canEdit = "CHỜ DUYỆT".equals(p.status())
                || "ĐANG ĐẤU GIÁ".equals(p.status());
        boolean canDelete = !"ĐÃ BÁN".equals(p.status())
                && !"ĐANG ĐẤU GIÁ".equals(p.status());

        Button editBtn = new Button("✏️ Sửa");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setDisable(!canEdit);
        editBtn.setOnAction(e -> handleEdit(p));

        Button deleteBtn = new Button("🗑 Xóa");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setDisable(!canDelete);
        deleteBtn.setOnAction(e -> handleDelete(p));

        actions.getChildren().addAll(editBtn, deleteBtn);
        row.getChildren().addAll(thumb, info, priceBox, actions);
        return row;
    }

    // ── Add Product Dialog ────────────────────────────────────
    @FXML
    private void handleAddProduct() {
        // Tạo Dialog form đăng bán
        Dialog<AppContext.ProductRecord> dialog = new Dialog<>();
        dialog.setTitle("Đăng bán sản phẩm mới");
        dialog.setHeaderText("Nhập thông tin sản phẩm");

        ButtonType saveBtn = new ButtonType("Đăng bán", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        // Form fields
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField nameField      = new TextField();
        nameField.setPromptText("Tên sản phẩm...");
        nameField.setPrefWidth(300);

        ComboBox<String> catBox  = new ComboBox<>();
        catBox.getItems().addAll("Điện tử","Máy ảnh","Laptop","Điện thoại","Đồng hồ","Xe cộ","Khác");
        catBox.setPromptText("Chọn danh mục");
        catBox.setPrefWidth(300);

        TextField priceField     = new TextField();
        priceField.setPromptText("Giá khởi điểm (VNĐ)...");

        TextField daysField      = new TextField("7");
        daysField.setPromptText("Số ngày đấu giá...");

        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");

        grid.add(new Label("Tên sản phẩm *"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Danh mục *"), 0, 1);
        grid.add(catBox, 1, 1);
        grid.add(new Label("Giá khởi điểm *"), 0, 2);
        grid.add(priceField, 1, 2);
        grid.add(new Label("Thời gian (ngày) *"), 0, 3);
        grid.add(daysField, 1, 3);
        grid.add(errLabel, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Disable nút Save nếu tên rỗng
        javafx.scene.Node saveNode = dialog.getDialogPane().lookupButton(saveBtn);
        saveNode.setDisable(true);
        nameField.textProperty().addListener((obs, o, n) ->
                saveNode.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;

            String nameVal = nameField.getText().trim();
            String catVal  = catBox.getValue() == null ? "Khác" : catBox.getValue();
            double price;
            int    days;

            try {
                price = Double.parseDouble(
                        priceField.getText().trim().replaceAll("[^0-9.]", ""));
            } catch (NumberFormatException ex) {
                price = 0;
            }
            try {
                days = Integer.parseInt(daysField.getText().trim());
                if (days < 1) days = 1;
            } catch (NumberFormatException ex) {
                days = 7;
            }

            if (nameVal.isEmpty() || price <= 0) return null;

            return new AppContext.ProductRecord(
                    "P-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                    nameVal, catVal, price, price, 0,
                    "CHỜ DUYỆT",
                    LocalDateTime.now().plusDays(days),
                    "—"
            );
        });

        dialog.showAndWait().ifPresent(product -> {
            if (product != null) {
                AppContext.addProduct(username, product);
                refreshStats();
                renderList(AppContext.getProducts(username));
            }
        });
    }

    // ── Edit Dialog ───────────────────────────────────────────
    private void handleEdit(AppContext.ProductRecord p) {
        Dialog<AppContext.ProductRecord> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh sửa sản phẩm");
        dialog.setHeaderText(p.name());

        ButtonType saveBtn = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField nameField  = new TextField(p.name());
        nameField.setPrefWidth(300);

        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll("Điện tử","Máy ảnh","Laptop","Điện thoại","Đồng hồ","Xe cộ","Khác");
        catBox.setValue(p.category());
        catBox.setPrefWidth(300);

        TextField priceField = new TextField(String.valueOf((long) p.startPrice()));
        // Chỉ cho đổi giá nếu CHỜ DUYỆT
        priceField.setDisable(!"CHỜ DUYỆT".equals(p.status()));

        grid.add(new Label("Tên sản phẩm"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Danh mục"), 0, 1);
        grid.add(catBox, 1, 1);
        grid.add(new Label("Giá khởi điểm"), 0, 2);
        grid.add(priceField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            double newPrice;
            try {
                newPrice = Double.parseDouble(
                        priceField.getText().trim().replaceAll("[^0-9.]", ""));
            } catch (NumberFormatException ex) {
                newPrice = p.startPrice();
            }
            return new AppContext.ProductRecord(
                    p.id(),
                    nameField.getText().trim().isEmpty() ? p.name() : nameField.getText().trim(),
                    catBox.getValue() == null ? p.category() : catBox.getValue(),
                    newPrice, newPrice,
                    p.bidCount(), p.status(), p.endTime(), p.topBidder()
            );
        });

        dialog.showAndWait().ifPresent(updated -> {
            if (updated != null) {
                AppContext.updateProduct(username, updated);
                refreshStats();
                renderList(AppContext.getProducts(username));
            }
        });
    }

    // ── Delete ────────────────────────────────────────────────
    private void handleDelete(AppContext.ProductRecord p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa: " + p.name());
        confirm.setContentText("Bạn có chắc muốn xóa sản phẩm này không?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                AppContext.removeProduct(username, p.id());
                refreshStats();
                renderList(AppContext.getProducts(username));
            }
        });
    }

    // ── Filter ────────────────────────────────────────────────
    @FXML private void handleFilter() { applyFilters(); }
    @FXML private void handleSearch() { applyFilters(); }

    private void applyFilters() {
        String keyword = searchField.getText().trim().toLowerCase();
        String status  = statusFilter.getValue();

        List<AppContext.ProductRecord> filtered = AppContext.getProducts(username).stream()
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
