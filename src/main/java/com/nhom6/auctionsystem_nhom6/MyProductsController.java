package com.nhom6.auctionsystem_nhom6;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.user.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    private static final DateTimeFormatter TIME_ONLY =
            DateTimeFormatter.ofPattern("HH:mm");

    // =========================================================
    // INITIALIZE — luôn đọc thẳng từ AppContext
    // =========================================================
    @FXML
    public void initialize() {
        User user = AppContext.getCurrentUser();
        if (user == null) return;
        username = user.getUsername();

        statusFilter.getItems().addAll(
                "Tất cả", "ĐANG ĐẤU GIÁ", "ĐÃ BÁN", "HẾT HẠN", "ĐÃ HỦY");

        refreshStats();
        renderList(AppContext.getProducts(username));
    }

    // =========================================================
    // STATS — tính thẳng từ AppContext
    // =========================================================
    private void refreshStats() {
        List<AppContext.ProductRecord> list = AppContext.getProducts(username);

        totalProductsLabel.setText(String.valueOf(list.size()));

        activeProductsLabel.setText(String.valueOf(
                list.stream().filter(p -> "ĐANG ĐẤU GIÁ".equals(p.status())).count()));

        soldProductsLabel.setText(String.valueOf(
                list.stream().filter(p -> "ĐÃ BÁN".equals(p.status())).count()));

        double rev = list.stream()
                .filter(p -> "ĐÃ BÁN".equals(p.status()))
                .mapToDouble(AppContext.ProductRecord::currentPrice)
                .sum();
        revenueLabel.setText(formatVND(rev));
    }

    // =========================================================
    // RENDER
    // =========================================================
    private void renderList(List<AppContext.ProductRecord> list) {
        productListBox.getChildren().clear();

        if (list.isEmpty()) {
            productListBox.getChildren().add(buildEmptyState());
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
        msg.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
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
        thumb.setStyle(
                "-fx-font-size: 28px; -fx-text-fill: #3b82f6;"
                + "-fx-background-color: #1e3a5f; -fx-padding: 8 12 8 12;"
                + "-fx-background-radius: 8; -fx-min-width: 56; -fx-alignment: center;");

        // Info
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(p.name());
        name.getStyleClass().add("history-item-name");

        HBox metaRow = new HBox(16);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label cat   = new Label("📂 " + p.category());
        Label bids  = new Label("🔨 " + p.bidCount() + " lượt bid");
        Label start = new Label("🕐 Bắt đầu: " + p.startTime().format(DT_FMT));
        Label end   = new Label("⏰ Kết thúc: " + p.endTime().format(DT_FMT));

        cat.getStyleClass().add("history-item-meta");
        bids.getStyleClass().add("history-item-meta");
        start.getStyleClass().add("history-item-meta");
        end.getStyleClass().add("history-item-meta");
        metaRow.getChildren().addAll(cat, bids, start, end);

        if ("ĐÃ BÁN".equals(p.status()) && !"—".equals(p.topBidder())) {
            Label winner = new Label("🏆 Người thắng: " + p.topBidder());
            winner.setStyle("-fx-text-fill: #22d3ee; -fx-font-size: 12px;");
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

        boolean canEdit   = "ĐANG ĐẤU GIÁ".equals(p.status());
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

    // =========================================================
    // ADD PRODUCT
    // =========================================================
    @FXML
    private void handleAddProduct() {

        Dialog<AppContext.ProductRecord> dialog = new Dialog<>();
        dialog.setTitle("Đăng bán sản phẩm mới");
        dialog.setHeaderText("Nhập thông tin sản phẩm");
        dialog.getDialogPane().setPrefWidth(500);

        ButtonType saveBtn = new ButtonType("Đăng bán", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        // ── Form fields ───────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Tên sản phẩm...");
        nameField.setPrefWidth(320);

        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll(
                "Điện tử","Máy ảnh","Laptop","Điện thoại",
                "Đồng hồ","Xe cộ","Khác");
        catBox.setPromptText("Chọn danh mục");
        catBox.setPrefWidth(320);

        TextField priceField = new TextField();
        priceField.setPromptText("VD: 5000000");
        priceField.setPrefWidth(320);

        // Bắt đầu
        DatePicker startDate = new DatePicker(LocalDate.now());
        startDate.setPrefWidth(185);
        TextField startTime = new TextField(
                LocalTime.now().plusMinutes(10).format(TIME_ONLY));
        startTime.setPrefWidth(90);

        // Kết thúc
        DatePicker endDate = new DatePicker(LocalDate.now().plusDays(7));
        endDate.setPrefWidth(185);
        TextField endTime = new TextField("23:59");
        endTime.setPrefWidth(90);

        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        errLabel.setWrapText(true);

        HBox startRow = new HBox(8, startDate, new Label("lúc"), startTime);
        startRow.setAlignment(Pos.CENTER_LEFT);
        HBox endRow   = new HBox(8, endDate,   new Label("lúc"), endTime);
        endRow.setAlignment(Pos.CENTER_LEFT);

        String ls = "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1e293b;";
        Label lN = new Label("Tên sản phẩm *");      lN.setStyle(ls);
        Label lC = new Label("Danh mục *");           lC.setStyle(ls);
        Label lP = new Label("Giá khởi điểm *");      lP.setStyle(ls);
        Label lS = new Label("Thời gian bắt đầu *");  lS.setStyle(ls);
        Label lE = new Label("Thời gian kết thúc *"); lE.setStyle(ls);

        grid.add(lN, 0, 0); grid.add(nameField,  1, 0);
        grid.add(lC, 0, 1); grid.add(catBox,     1, 1);
        grid.add(lP, 0, 2); grid.add(priceField, 1, 2);
        grid.add(lS, 0, 3); grid.add(startRow,   1, 3);
        grid.add(lE, 0, 4); grid.add(endRow,     1, 4);
        grid.add(errLabel, 0, 5, 2, 1);

        dialog.getDialogPane().setContent(grid);

        // Disable nút Đăng bán khi tên rỗng
        javafx.scene.Node saveNode = dialog.getDialogPane().lookupButton(saveBtn);
        saveNode.setDisable(true);
        nameField.textProperty().addListener((obs, o, n) ->
                saveNode.setDisable(n.trim().isEmpty()));

        // ── Result converter ──────────────────────────────────
        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            errLabel.setText("");

            // Validate tên
            String nameVal = nameField.getText().trim();
            if (nameVal.isEmpty()) {
                errLabel.setText("⚠ Vui lòng nhập tên sản phẩm.");
                return null;
            }

            // Validate giá
            double price;
            try {
                price = Double.parseDouble(
                        priceField.getText().trim().replaceAll("[^0-9.]", ""));
                if (price <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                errLabel.setText("⚠ Giá khởi điểm không hợp lệ.");
                return null;
            }

            // Validate thời gian
            LocalDateTime startDT, endDT;
            try {
                startDT = LocalDateTime.of(startDate.getValue(),
                        LocalTime.parse(startTime.getText().trim(), TIME_ONLY));
                endDT   = LocalDateTime.of(endDate.getValue(),
                        LocalTime.parse(endTime.getText().trim(), TIME_ONLY));
            } catch (DateTimeParseException | NullPointerException ex) {
                errLabel.setText("⚠ Thời gian không hợp lệ (định dạng HH:mm).");
                return null;
            }

            if (!endDT.isAfter(startDT)) {
                errLabel.setText("⚠ Thời gian kết thúc phải sau thời gian bắt đầu.");
                return null;
            }
            if (startDT.isBefore(LocalDateTime.now())) {
                errLabel.setText("⚠ Thời gian bắt đầu phải từ hiện tại trở đi.");
                return null;
            }

            // Kiểm tra trùng thời gian
            String conflict = findTimeConflict(startDT, endDT, null);
            if (conflict != null) {
                errLabel.setText(
                        "⚠ Đã có sản phẩm đấu giá trong khoảng thời gian này:\n\""
                        + conflict + "\"");
                return null;
            }

            return new AppContext.ProductRecord(
                    "P-" + UUID.randomUUID().toString()
                              .substring(0, 6).toUpperCase(),
                    nameVal,
                    catBox.getValue() == null ? "Khác" : catBox.getValue(),
                    price, price, 0,
                    "ĐANG ĐẤU GIÁ",   // ✅ thẳng ĐANG ĐẤU GIÁ, không qua duyệt
                    startDT, endDT,
                    "—"
            );
        });

        // ── Hiển thị dialog và xử lý kết quả ─────────────────
        dialog.showAndWait().ifPresent(product -> {
            if (product == null) return;

            // ✅ Lưu sản phẩm
            AppContext.addProduct(username, product);

            // ✅ Tạo AuctionSession và đăng ký để bidder thấy
            try {
                double step = Math.max(product.startPrice() * 0.05, 500_000);
                org.example.auction.AuctionSession session =
                        new org.example.auction.AuctionSession(
                                product.id(),
                                product.name(),
                                product.startPrice(),
                                step,
                                product.endTime()
                        );
                session.start(); // bắt đầu phiên ngay

                AppContext.registerSession(session, username);
                AppContext.setActiveSession(session);

            } catch (Exception e) {
                e.printStackTrace();
            }

            refreshStats();
            renderList(AppContext.getProducts(username));
        });
    }

    // ── Kiểm tra trùng thời gian ──────────────────────────────
    private String findTimeConflict(LocalDateTime newStart,
                                     LocalDateTime newEnd,
                                     String excludeId) {
        for (AppContext.ProductRecord p : AppContext.getProducts(username)) {
            if (excludeId != null && excludeId.equals(p.id())) continue;
            if (!"ĐANG ĐẤU GIÁ".equals(p.status())) continue;
            boolean overlap = newStart.isBefore(p.endTime())
                           && newEnd.isAfter(p.startTime());
            if (overlap) return p.name();
        }
        return null;
    }

    // =========================================================
    // EDIT
    // =========================================================
    private void handleEdit(AppContext.ProductRecord p) {
        Dialog<AppContext.ProductRecord> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh sửa sản phẩm");
        dialog.setHeaderText(p.name());
        dialog.getDialogPane().setPrefWidth(500);

        ButtonType saveBtn = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(14);
        grid.setPadding(new Insets(20));

        TextField nameField  = new TextField(p.name());
        nameField.setPrefWidth(320);

        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll(
                "Điện tử","Máy ảnh","Laptop","Điện thoại",
                "Đồng hồ","Xe cộ","Khác");
        catBox.setValue(p.category());
        catBox.setPrefWidth(320);

        DatePicker endDate = new DatePicker(p.endTime().toLocalDate());
        endDate.setPrefWidth(185);
        TextField endTime = new TextField(p.endTime().format(TIME_ONLY));
        endTime.setPrefWidth(90);

        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        errLabel.setWrapText(true);

        HBox endRow = new HBox(8, endDate, new Label("lúc"), endTime);
        endRow.setAlignment(Pos.CENTER_LEFT);

        String ls = "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1e293b;";
        Label lN = new Label("Tên sản phẩm"); lN.setStyle(ls);
        Label lC = new Label("Danh mục");     lC.setStyle(ls);
        Label lE = new Label("Thời gian kết thúc"); lE.setStyle(ls);

        grid.add(lN, 0, 0); grid.add(nameField, 1, 0);
        grid.add(lC, 0, 1); grid.add(catBox,    1, 1);
        grid.add(lE, 0, 2); grid.add(endRow,    1, 2);
        grid.add(errLabel, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            errLabel.setText("");

            LocalDateTime newEnd;
            try {
                newEnd = LocalDateTime.of(endDate.getValue(),
                        LocalTime.parse(endTime.getText().trim(), TIME_ONLY));
            } catch (Exception ex) {
                errLabel.setText("⚠ Thời gian không hợp lệ."); return null;
            }

            if (!newEnd.isAfter(p.startTime())) {
                errLabel.setText("⚠ Thời gian kết thúc phải sau thời gian bắt đầu.");
                return null;
            }

            // Kiểm tra trùng (bỏ qua chính sản phẩm đang sửa)
            String conflict = findTimeConflict(p.startTime(), newEnd, p.id());
            if (conflict != null) {
                errLabel.setText(
                        "⚠ Trùng thời gian với sản phẩm:\n\"" + conflict + "\"");
                return null;
            }

            String nameVal = nameField.getText().trim().isEmpty()
                    ? p.name() : nameField.getText().trim();
            String catVal  = catBox.getValue() == null
                    ? p.category() : catBox.getValue();

            return new AppContext.ProductRecord(
                    p.id(), nameVal, catVal,
                    p.startPrice(), p.currentPrice(),
                    p.bidCount(), p.status(),
                    p.startTime(), newEnd, p.topBidder()
            );
        });

        dialog.showAndWait().ifPresent(updated -> {
            if (updated == null) return;
            AppContext.updateProduct(username, updated);
            refreshStats();
            renderList(AppContext.getProducts(username));
        });
    }

    // =========================================================
    // DELETE
    // =========================================================
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

    // =========================================================
    // FILTER
    // =========================================================
    @FXML private void handleFilter() { applyFilters(); }
    @FXML private void handleSearch() { applyFilters(); }

    private void applyFilters() {
        String keyword = searchField.getText().trim().toLowerCase();
        String status  = statusFilter.getValue();

        List<AppContext.ProductRecord> filtered =
                AppContext.getProducts(username).stream()
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

    // =========================================================
    // HELPERS
    // =========================================================
    private String productBadgeStyle(String status) {
        return switch (status) {
            case "ĐANG ĐẤU GIÁ" -> "badge-info";
            case "ĐÃ BÁN"       -> "badge-success";
            case "HẾT HẠN",
                 "ĐÃ HỦY"       -> "badge-neutral";
            default              -> "badge-neutral";
        };
    }

    private String formatVND(double v) {
        return String.format("₫ %,.0f", v);
    }
}
