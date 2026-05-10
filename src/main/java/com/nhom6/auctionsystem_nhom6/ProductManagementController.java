package com.nhom6.auctionsystem_nhom6;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.user.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProductManagementController {

    @FXML private Label    totalProductsLabel;
    @FXML private Label    approvedLabel;
    @FXML private Label    pendingLabel;
    @FXML private Label    auctioningLabel;
    @FXML private VBox     productListBox;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private TextField searchField;

    private String username;
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    // Trạng thái nội bộ quản lý sản phẩm
    private static final List<ManagedProduct> managedList = new ArrayList<>();

    public record ManagedProduct(
            String id, String name, String category,
            String sellerName, double startPrice,
            String status,   // CHỜ DUYỆT | ĐÃ DUYỆT | ĐANG ĐẤU GIÁ | ĐÃ BÁN | TỪ CHỐI
            LocalDateTime createdAt, LocalDateTime auctionEnd
    ) {}

    @FXML
    public void initialize() {
        User user = AppContext.getCurrentUser();
        username  = user.getUsername();

        statusFilter.getItems().addAll(
                "Tất cả", "CHỜ DUYỆT", "ĐÃ DUYỆT", "ĐANG ĐẤU GIÁ", "ĐÃ BÁN", "TỪ CHỐI");
        categoryFilter.getItems().addAll(
                "Tất cả", "Laptop", "Điện thoại", "Máy ảnh",
                "Điện tử", "Đồng hồ", "Xe cộ", "Khác");

        // Sync từ AppContext.ProductRecord sang ManagedProduct
        syncFromAppContext(user);

        refreshStats();
        renderList(managedList);
    }

    private void syncFromAppContext(User user) {
        // Thêm sản phẩm từ seller hiện tại nếu chưa có
        for (AppContext.ProductRecord p : AppContext.getProducts(user.getUsername())) {
            boolean exists = managedList.stream()
                    .anyMatch(m -> m.id().equals(p.id()));
            if (!exists) {
                String mStatus = switch (p.status()) {
                    case "ĐANG ĐẤU GIÁ" -> "ĐANG ĐẤU GIÁ";
                    case "ĐÃ BÁN"       -> "ĐÃ BÁN";
                    case "CHỜ DUYỆT"    -> "CHỜ DUYỆT";
                    default             -> p.status();
                };
                managedList.add(new ManagedProduct(
                        p.id(), p.name(), p.category(),
                        user.getUsername(), p.startPrice(),
                        mStatus, LocalDateTime.now(), p.endTime()
                ));
            }
        }
    }

    // ── Stats ─────────────────────────────────────────────────
    private void refreshStats() {
        totalProductsLabel.setText(String.valueOf(managedList.size()));
        approvedLabel.setText(String.valueOf(
                managedList.stream().filter(p -> "ĐÃ DUYỆT".equals(p.status())).count()));
        pendingLabel.setText(String.valueOf(
                managedList.stream().filter(p -> "CHỜ DUYỆT".equals(p.status())).count()));
        auctioningLabel.setText(String.valueOf(
                managedList.stream().filter(p -> "ĐANG ĐẤU GIÁ".equals(p.status())).count()));
    }

    // ── Render ────────────────────────────────────────────────
    private void renderList(List<ManagedProduct> list) {
        productListBox.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label("Chưa có sản phẩm nào. Nhấn \"＋ Thêm sản phẩm\" để bắt đầu.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-padding: 40 0 40 0;");
            productListBox.getChildren().add(empty);
            return;
        }
        for (ManagedProduct p : list) {
            productListBox.getChildren().add(buildRow(p));
        }
    }

    private HBox buildRow(ManagedProduct p) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");
        row.setPadding(new Insets(12, 20, 12, 20));

        // Tên sản phẩm
        VBox nameBox = new VBox(3);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label name = new Label(p.name());
        name.getStyleClass().add("history-item-name");
        Label id = new Label("ID: " + p.id() + "  •  " + p.createdAt().format(DT_FMT));
        id.getStyleClass().add("history-item-meta");
        nameBox.getChildren().addAll(name, id);

        // Danh mục
        Label cat = new Label(p.category());
        cat.getStyleClass().add("history-item-meta");
        cat.setMinWidth(110);

        // Giá
        Label price = new Label(formatVND(p.startPrice()));
        price.getStyleClass().add("history-item-price");
        price.setStyle("-fx-font-size: 13px; -fx-text-fill: #38bdf8;");
        price.setMinWidth(140);

        // Người bán
        Label seller = new Label(p.sellerName());
        seller.getStyleClass().add("history-item-meta");
        seller.setMinWidth(120);

        // Badge trạng thái
        Label badge = new Label(p.status());
        badge.getStyleClass().addAll("history-badge", managedBadgeStyle(p.status()));
        badge.setMinWidth(120);

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);
        actions.setMinWidth(140);

        if ("CHỜ DUYỆT".equals(p.status())) {
            Button approveBtn = new Button("✅ Duyệt");
            approveBtn.setStyle("-fx-background-color: #14532d; -fx-text-fill: #4ade80; "
                    + "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 5 10 5 10;");
            approveBtn.setOnAction(e -> handleApprove(p));

            Button rejectBtn = new Button("❌ Từ chối");
            rejectBtn.getStyleClass().add("btn-danger");
            rejectBtn.setOnAction(e -> handleReject(p));

            actions.getChildren().addAll(approveBtn, rejectBtn);
        } else if ("ĐÃ DUYỆT".equals(p.status())) {
            Button startBtn = new Button("⚡ Bắt đầu đấu giá");
            startBtn.getStyleClass().add("btn-primary");
            startBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 10 5 10;");
            startBtn.setOnAction(e -> handleStartAuction(p));

            Button editBtn = new Button("✏️");
            editBtn.getStyleClass().add("btn-secondary");
            editBtn.setOnAction(e -> handleEdit(p));

            actions.getChildren().addAll(startBtn, editBtn);
        } else {
            Button viewBtn = new Button("👁 Xem");
            viewBtn.getStyleClass().add("btn-secondary");
            viewBtn.setOnAction(e -> handleView(p));
            actions.getChildren().add(viewBtn);
        }

        row.getChildren().addAll(nameBox, cat, price, seller, badge, actions);
        return row;
    }

    // ── Handlers ──────────────────────────────────────────────
    @FXML
    private void handleAdd() {
        Dialog<ManagedProduct> dialog = new Dialog<>();
        dialog.setTitle("Thêm sản phẩm mới");
        dialog.setHeaderText("Nhập thông tin sản phẩm đăng duyệt");

        ButtonType saveBtn = new ButtonType("Gửi duyệt", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Tên sản phẩm...");
        nameField.setPrefWidth(300);

        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll("Laptop","Điện thoại","Máy ảnh","Điện tử","Đồng hồ","Xe cộ","Khác");
        catBox.setPromptText("Chọn danh mục");

        TextField priceField = new TextField();
        priceField.setPromptText("Giá khởi điểm (VNĐ)...");

        TextField sellerField = new TextField(username);
        sellerField.setEditable(false);

        TextField daysField = new TextField("7");
        daysField.setPromptText("Số ngày đấu giá...");

        grid.add(new Label("Tên sản phẩm *"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Danh mục *"),      0, 1); grid.add(catBox, 1, 1);
        grid.add(new Label("Giá khởi điểm *"), 0, 2); grid.add(priceField, 1, 2);
        grid.add(new Label("Người bán"),        0, 3); grid.add(sellerField, 1, 3);
        grid.add(new Label("Thời gian (ngày)"), 0, 4); grid.add(daysField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        javafx.scene.Node saveNode = dialog.getDialogPane().lookupButton(saveBtn);
        saveNode.setDisable(true);
        nameField.textProperty().addListener((obs, o, n) ->
                saveNode.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            double price;
            int days;
            try { price = Double.parseDouble(priceField.getText().trim().replaceAll("[^0-9.]","")); }
            catch (Exception ex) { price = 0; }
            try { days = Math.max(1, Integer.parseInt(daysField.getText().trim())); }
            catch (Exception ex) { days = 7; }
            if (nameField.getText().trim().isEmpty() || price <= 0) return null;

            return new ManagedProduct(
                    "P-" + UUID.randomUUID().toString().substring(0,6).toUpperCase(),
                    nameField.getText().trim(),
                    catBox.getValue() == null ? "Khác" : catBox.getValue(),
                    username, price, "CHỜ DUYỆT",
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(days)
            );
        });

        dialog.showAndWait().ifPresent(p -> {
            if (p != null) {
                managedList.add(p);
                // Đồng thời thêm vào AppContext để seller thấy ở MyProducts
                AppContext.addProduct(username, new AppContext.ProductRecord(
                        p.id(), p.name(), p.category(),
                        p.startPrice(), p.startPrice(), 0,
                        "CHỜ DUYỆT", p.auctionEnd(), "—"
                ));
                refreshStats();
                renderList(managedList);
            }
        });
    }

    private void handleApprove(ManagedProduct p) {
        replaceStatus(p, "ĐÃ DUYỆT");
    }

    private void handleReject(ManagedProduct p) {
        replaceStatus(p, "TỪ CHỐI");
    }

    private void handleStartAuction(ManagedProduct p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Bắt đầu đấu giá");
        confirm.setHeaderText(p.name());
        confirm.setContentText("Xác nhận bắt đầu phiên đấu giá cho sản phẩm này?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                replaceStatus(p, "ĐANG ĐẤU GIÁ");
                AppContext.updateProduct(username, new AppContext.ProductRecord(
                        p.id(), p.name(), p.category(),
                        p.startPrice(), p.startPrice(), 0,
                        "ĐANG ĐẤU GIÁ", p.auctionEnd(), "—"
                ));
            }
        });
    }

    private void handleEdit(ManagedProduct p) {
        TextInputDialog dlg = new TextInputDialog(p.name());
        dlg.setTitle("Chỉnh sửa");
        dlg.setHeaderText("Tên sản phẩm");
        dlg.setContentText("Tên mới:");
        dlg.showAndWait().ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                ManagedProduct updated = new ManagedProduct(
                        p.id(), newName.trim(), p.category(),
                        p.sellerName(), p.startPrice(), p.status(),
                        p.createdAt(), p.auctionEnd()
                );
                int idx = managedList.indexOf(p);
                if (idx >= 0) managedList.set(idx, updated);
                refreshStats();
                renderList(managedList);
            }
        });
    }

    private void handleView(ManagedProduct p) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Chi tiết sản phẩm");
        a.setHeaderText(p.name());
        a.setContentText(
                "ID: " + p.id() + "\nDanh mục: " + p.category()
                + "\nGiá: " + formatVND(p.startPrice())
                + "\nNgười bán: " + p.sellerName()
                + "\nTrạng thái: " + p.status()
                + "\nNgày tạo: " + p.createdAt().format(DT_FMT)
                + "\nKết thúc đấu giá: " + p.auctionEnd().format(DT_FMT)
        );
        a.showAndWait();
    }

    private void replaceStatus(ManagedProduct p, String newStatus) {
        ManagedProduct updated = new ManagedProduct(
                p.id(), p.name(), p.category(), p.sellerName(),
                p.startPrice(), newStatus, p.createdAt(), p.auctionEnd()
        );
        int idx = managedList.indexOf(p);
        if (idx >= 0) managedList.set(idx, updated);
        refreshStats();
        applyFilters();
    }

    @FXML private void handleFilter() { applyFilters(); }
    @FXML private void handleSearch() { applyFilters(); }

    private void applyFilters() {
        String keyword  = searchField.getText().trim().toLowerCase();
        String status   = statusFilter.getValue();
        String category = categoryFilter.getValue();

        List<ManagedProduct> filtered = managedList.stream()
            .filter(p -> {
                if (status != null && !"Tất cả".equals(status)
                        && !status.isEmpty() && !status.equals(p.status())) return false;
                if (category != null && !"Tất cả".equals(category)
                        && !category.isEmpty() && !category.equals(p.category())) return false;
                if (!keyword.isEmpty()
                        && !p.name().toLowerCase().contains(keyword)
                        && !p.sellerName().toLowerCase().contains(keyword)
                        && !p.id().toLowerCase().contains(keyword)) return false;
                return true;
            }).collect(Collectors.toList());

        renderList(filtered);
    }

    @FXML private void handleBack() {
        try { HelloApplication.showMainView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private String managedBadgeStyle(String s) {
        return switch (s) {
            case "ĐÃ DUYỆT"       -> "badge-info";
            case "ĐANG ĐẤU GIÁ"  -> "badge-success";
            case "CHỜ DUYỆT"      -> "badge-warn";
            case "ĐÃ BÁN"         -> "badge-success";
            case "TỪ CHỐI"        -> "badge-danger";
            default               -> "badge-neutral";
        };
    }

    private String formatVND(double v) {
        return String.format("₫ %,.0f", v);
    }
}