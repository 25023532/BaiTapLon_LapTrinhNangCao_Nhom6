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

    @FXML private Label            totalProductsLabel;
    @FXML private Label            activeProductsLabel;
    @FXML private Label            soldProductsLabel;
    @FXML private Label            revenueLabel;
    @FXML private VBox             productListBox;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TextField        searchField;

    private String username;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter TIME_ONLY =
            DateTimeFormatter.ofPattern("HH:mm");

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        User user = AppContext.getCurrentUser();
        if (user == null) return;
        username = user.getUsername();

        // Thêm "SẮP DIỄN RA" và "ĐÃ KẾT THÚC" vào filter
        statusFilter.getItems().addAll(
                "Tất cả", "CHỜ DUYỆT", "TỪ CHỐI",
                "SẮP DIỄN RA", "ĐANG ĐẤU GIÁ", "ĐÃ KẾT THÚC",
                "ĐÃ BÁN", "ĐÃ HỦY");

        refreshStats();
        renderList(AppContext.getProducts(username));
    }

    // =========================================================
    // STATS — tính theo displayStatus thực tế
    // =========================================================
    private void refreshStats() {
        List<AppContext.ProductRecord> list = AppContext.getProducts(username);

        totalProductsLabel.setText(String.valueOf(list.size()));

        // "Đang đấu giá" = tính theo thời gian thực
        activeProductsLabel.setText(String.valueOf(
                list.stream()
                    .filter(p -> "ĐANG ĐẤU GIÁ".equals(
                            AppContext.computeDisplayStatus(p)))
                    .count()));

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
        for (AppContext.ProductRecord p : list)
            productListBox.getChildren().add(buildProductRow(p));
    }

    private VBox buildEmptyState() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60, 0, 60, 0));
        Label icon = new Label("📦");
        icon.setStyle("-fx-font-size: 48px;");
        Label msg = new Label(
                "Bạn chưa đăng bán sản phẩm nào.\n"
                + "Nhấn \"＋ Đăng bán mới\" để bắt đầu.");
        msg.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
        msg.setWrapText(true);
        msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        box.getChildren().addAll(icon, msg);
        return box;
    }

    private HBox buildProductRow(AppContext.ProductRecord p) {
        // Tính status hiển thị thực tế theo thời gian
        String displayStatus = AppContext.computeDisplayStatus(p);

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

        // Ghi chú trạng thái
        String noteText = switch (displayStatus) {
            case "CHỜ DUYỆT"   -> "⏳ Đang chờ Admin xét duyệt...";
            case "TỪ CHỐI"     -> "⚠ Sản phẩm bị từ chối. Vui lòng xóa và đăng lại.";
            case "SẮP DIỄN RA" -> "🕐 Đã được duyệt, sắp đến giờ bắt đầu.";
            case "ĐÃ KẾT THÚC" -> "🏁 Phiên đấu giá đã kết thúc.";
            default             -> null;
        };
        if (noteText != null) {
            Label note = new Label(noteText);
            note.setStyle(switch (displayStatus) {
                case "CHỜ DUYỆT"   -> "-fx-text-fill: #fbbf24; -fx-font-size: 12px;";
                case "TỪ CHỐI"     -> "-fx-text-fill: #f87171; -fx-font-size: 12px;";
                case "SẮP DIỄN RA" -> "-fx-text-fill: #60a5fa; -fx-font-size: 12px;";
                case "ĐÃ KẾT THÚC" -> "-fx-text-fill: #94a3b8; -fx-font-size: 12px;";
                default             -> "-fx-font-size: 12px;";
            });
            metaRow.getChildren().add(note);
        }

        info.getChildren().addAll(name, metaRow);

        // Giá + badge — dùng displayStatus cho màu badge
        VBox priceBox = new VBox(4);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        Label startP = new Label("Khởi điểm: " + formatVND(p.startPrice()));
        startP.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        Label curP = new Label(formatVND(p.currentPrice()));
        curP.getStyleClass().add("history-item-price");
        Label badge = new Label(displayStatus);
        badge.getStyleClass().addAll("history-badge", badgeStyle(displayStatus));
        priceBox.getChildren().addAll(startP, curP, badge);

        // Buttons theo displayStatus
        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);
        buildActionButtons(p, displayStatus, actions);

        row.getChildren().addAll(thumb, info, priceBox, actions);
        return row;
    }

    // =========================================================
    // PHÂN QUYỀN NÚT THEO DISPLAY STATUS
    // =========================================================
    /**
     * CHỜ DUYỆT    → ⏳ disabled  +  🗑 Hủy đăng
     * TỪ CHỐI      → ❌ disabled  +  🗑 Xóa
     * SẮP DIỄN RA  → ✏️ Sửa       (Admin đã duyệt, chưa đến giờ)
     * ĐANG ĐẤU GIÁ → 🔴 Vào phiên (đang trong giờ đấu giá)
     * ĐÃ KẾT THÚC  → (không có nút thao tác)
     * ĐÃ BÁN       → 🗑 Xóa
     * ĐÃ HỦY       → 🗑 Xóa
     */
    private void buildActionButtons(AppContext.ProductRecord p,
                                     String displayStatus,
                                     VBox actions) {
        switch (displayStatus) {

            case "CHỜ DUYỆT" -> {
                Button waitBtn = new Button("⏳ Chờ duyệt");
                waitBtn.setDisable(true);
                waitBtn.setStyle(
                        "-fx-background-color: #78350f; -fx-text-fill: #fbbf24;"
                        + "-fx-background-radius: 6; -fx-padding: 5 10 5 10;"
                        + "-fx-min-width: 140;");
                Button delBtn = new Button("🗑 Hủy đăng");
                delBtn.getStyleClass().add("btn-danger");
                delBtn.setOnAction(e -> handleDelete(p));
                actions.getChildren().addAll(waitBtn, delBtn);
            }

            case "TỪ CHỐI" -> {
                Button rejBtn = new Button("❌ Bị từ chối");
                rejBtn.setDisable(true);
                rejBtn.setStyle(
                        "-fx-background-color: #450a0a; -fx-text-fill: #f87171;"
                        + "-fx-background-radius: 6; -fx-padding: 5 10 5 10;"
                        + "-fx-min-width: 140;");
                Button delBtn = new Button("🗑 Xóa");
                delBtn.getStyleClass().add("btn-danger");
                delBtn.setOnAction(e -> handleDelete(p));
                actions.getChildren().addAll(rejBtn, delBtn);
            }

            case "SẮP DIỄN RA" -> {
                // Đã duyệt, chưa đến giờ → cho sửa thời gian kết thúc
                Button editBtn = new Button("✏️ Sửa");
                editBtn.getStyleClass().add("btn-secondary");
                editBtn.setOnAction(e -> handleEdit(p));
                Button delBtn = new Button("🗑 Hủy");
                delBtn.getStyleClass().add("btn-danger");
                delBtn.setOnAction(e -> handleDelete(p));
                actions.getChildren().addAll(editBtn, delBtn);
            }

            case "ĐANG ĐẤU GIÁ" -> {
                // Đang trong giờ → vào phiên
                Button liveBtn = new Button("🔴 Vào phiên");
                liveBtn.getStyleClass().add("btn-primary");
                liveBtn.setStyle(
                        "-fx-font-size: 12px; -fx-padding: 5 10 5 10;"
                        + "-fx-min-width: 140;");
                liveBtn.setOnAction(e -> handleGoLive(p));
                actions.getChildren().add(liveBtn);
            }

            case "ĐÃ KẾT THÚC" -> {
                // Phiên đã kết thúc — không có nút thao tác
            }

            default -> {
                // ĐÃ BÁN / ĐÃ HỦY → cho xóa
                Button delBtn = new Button("🗑 Xóa");
                delBtn.getStyleClass().add("btn-danger");
                delBtn.setOnAction(e -> handleDelete(p));
                actions.getChildren().add(delBtn);
            }
        }
    }

    // =========================================================
    // ADD PRODUCT — status luôn CHỜ DUYỆT
    // =========================================================
    @FXML
    private void handleAddProduct() {
        Dialog<AppContext.ProductRecord> dialog = new Dialog<>();
        dialog.setTitle("Đăng bán sản phẩm mới");
        dialog.setHeaderText("Nhập thông tin sản phẩm");
        dialog.getDialogPane().setPrefWidth(520);

        ButtonType saveBtn =
                new ButtonType("Đăng bán", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20));

        Label infoLabel = new Label(
                "ℹ️  Sản phẩm sẽ được gửi lên Admin để xét duyệt trước khi đấu giá.");
        infoLabel.setStyle(
                "-fx-text-fill: #38bdf8; -fx-font-size: 12px;"
                + "-fx-background-color: #0c1a2e; -fx-padding: 8 12 8 12;"
                + "-fx-background-radius: 6;");
        infoLabel.setWrapText(true);

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

        DatePicker startDate = new DatePicker(LocalDate.now());
        startDate.setPrefWidth(185);
        TextField startTime = new TextField(
                LocalTime.now().plusMinutes(10).format(TIME_ONLY));
        startTime.setPrefWidth(90);

        DatePicker endDate = new DatePicker(LocalDate.now().plusDays(7));
        endDate.setPrefWidth(185);
        TextField endTime = new TextField("23:59");
        endTime.setPrefWidth(90);

        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        errLabel.setWrapText(true);

        HBox startRow = new HBox(8, startDate, new Label("lúc"), startTime);
        startRow.setAlignment(Pos.CENTER_LEFT);
        HBox endRow   = new HBox(8, endDate, new Label("lúc"), endTime);
        endRow.setAlignment(Pos.CENTER_LEFT);

        String ls =
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1e293b;";
        Label lN = new Label("Tên sản phẩm *");      lN.setStyle(ls);
        Label lC = new Label("Danh mục *");           lC.setStyle(ls);
        Label lP = new Label("Giá khởi điểm *");      lP.setStyle(ls);
        Label lS = new Label("Thời gian bắt đầu *");  lS.setStyle(ls);
        Label lE = new Label("Thời gian kết thúc *"); lE.setStyle(ls);

        grid.add(infoLabel,  0, 0, 2, 1);
        grid.add(lN, 0, 1); grid.add(nameField,  1, 1);
        grid.add(lC, 0, 2); grid.add(catBox,     1, 2);
        grid.add(lP, 0, 3); grid.add(priceField, 1, 3);
        grid.add(lS, 0, 4); grid.add(startRow,   1, 4);
        grid.add(lE, 0, 5); grid.add(endRow,     1, 5);
        grid.add(errLabel, 0, 6, 2, 1);

        dialog.getDialogPane().setContent(grid);

        javafx.scene.Node saveNode =
                dialog.getDialogPane().lookupButton(saveBtn);
        saveNode.setDisable(true);
        nameField.textProperty().addListener((obs, o, n) ->
                saveNode.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            errLabel.setText("");

            String nameVal = nameField.getText().trim();
            if (nameVal.isEmpty()) {
                errLabel.setText("⚠ Vui lòng nhập tên sản phẩm.");
                return null;
            }

            double price;
            try {
                price = Double.parseDouble(
                        priceField.getText().trim().replaceAll("[^0-9.]", ""));
                if (price <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                errLabel.setText("⚠ Giá khởi điểm không hợp lệ.");
                return null;
            }

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

            // Kiểm tra trùng với sản phẩm đã duyệt của seller này
            String conflict = AppContext.findTimeConflictForSeller(
                    username, startDT, endDT, null);
            if (conflict != null) {
                errLabel.setText(
                        "⚠ Trùng thời gian với sản phẩm đã được duyệt:\n\""
                        + conflict + "\"");
                return null;
            }

            // Status = CHỜ DUYỆT — AppContext.addProduct() cũng enforce lại
            return new AppContext.ProductRecord(
                    "P-" + UUID.randomUUID().toString()
                              .substring(0, 6).toUpperCase(),
                    nameVal,
                    catBox.getValue() == null ? "Khác" : catBox.getValue(),
                    price, price, 0,
                    "CHỜ DUYỆT",
                    startDT, endDT, "—"
            );
        });

        dialog.showAndWait().ifPresent(product -> {
            if (product == null) return;
            AppContext.addProduct(username, product);

            // Thông báo WebSocket cho Admin (nếu online)
            ServerConnection conn = ServerConnection.getInstance();
            if (conn.isConnected()) {
                conn.sendJson(String.format(
                        "{\"action\":\"NOTIFY_ADMIN_NEW_PRODUCT\","
                        + "\"productId\":\"%s\","
                        + "\"productName\":\"%s\","
                        + "\"sellerName\":\"%s\"}",
                        product.id(), product.name(), username));
            }

            refreshStats();
            renderList(AppContext.getProducts(username));

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Đăng ký thành công");
            info.setHeaderText("✅ Sản phẩm đã được gửi lên!");
            info.setContentText(
                    "\"" + product.name() + "\" đang chờ Admin xét duyệt.\n\n"
                    + "Sau khi được duyệt, sản phẩm sẽ tự động chuyển sang:\n"
                    + "  🕐 SẮP DIỄN RA  →  🔴 ĐANG ĐẤU GIÁ  →  🏁 ĐÃ KẾT THÚC\n"
                    + "theo đúng thời gian bạn đã đặt.");
            info.showAndWait();
        });
    }

    // =========================================================
    // VÀO PHIÊN (khi ĐANG ĐẤU GIÁ)
    // =========================================================
    private void handleGoLive(AppContext.ProductRecord p) {
        org.example.auction.AuctionSession existing =
                AppContext.getGlobalSessions().stream()
                .filter(s -> s.getSessionId().equals(p.id()))
                .findFirst().orElse(null);

        if (existing != null) {
            AppContext.setActiveSession(existing);
        } else {
            // Tạo session nếu chưa có (Admin đã duyệt, đúng giờ)
            if (p.endTime().isBefore(LocalDateTime.now())) {
                showAlert("Hết hạn", "Phiên đấu giá đã hết thời gian.");
                return;
            }
            try {
                double step = Math.max(p.startPrice() * 0.05, 500_000);
                org.example.auction.AuctionSession newSess =
                        new org.example.auction.AuctionSession(
                                p.id(), p.name(),
                                p.startPrice(), step, p.endTime());
                newSess.start();
                AppContext.registerSession(newSess, username);
                AppContext.setActiveSession(newSess);

                // Ghi lại status "ĐANG ĐẤU GIÁ" trong productMap
                AppContext.updateProduct(username, new AppContext.ProductRecord(
                        p.id(), p.name(), p.category(),
                        p.startPrice(), p.startPrice(), 0,
                        "ĐANG ĐẤU GIÁ",
                        p.startTime(), p.endTime(), "—"
                ));
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi", "Không thể vào phiên: " + e.getMessage());
                return;
            }
        }
        try { HelloApplication.showLiveAuctionView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================
    // EDIT — chỉ sửa thời gian kết thúc và tên/danh mục
    // =========================================================
    private void handleEdit(AppContext.ProductRecord p) {
        Dialog<AppContext.ProductRecord> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh sửa sản phẩm");
        dialog.setHeaderText(p.name());
        dialog.getDialogPane().setPrefWidth(500);

        ButtonType saveBtn =
                new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(14);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(p.name());
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

        String ls =
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1e293b;";
        Label lN = new Label("Tên sản phẩm");      lN.setStyle(ls);
        Label lC = new Label("Danh mục");           lC.setStyle(ls);
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
                errLabel.setText(
                        "⚠ Thời gian kết thúc phải sau thời gian bắt đầu.");
                return null;
            }

            // Kiểm tra trùng (bỏ qua chính sản phẩm đang sửa)
            String conflict = AppContext.findTimeConflictForSeller(
                    username, p.startTime(), newEnd, p.id());
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
    // FILTER — lọc theo displayStatus
    // =========================================================
    @FXML private void handleFilter() { applyFilters(); }
    @FXML private void handleSearch() { applyFilters(); }

    private void applyFilters() {
        String keyword = searchField.getText().trim().toLowerCase();
        String status  = statusFilter.getValue();

        List<AppContext.ProductRecord> filtered =
                AppContext.getProducts(username).stream()
            .filter(p -> {
                String ds = AppContext.computeDisplayStatus(p);
                if (status != null && !status.isEmpty()
                        && !"Tất cả".equals(status)
                        && !status.equals(ds)) return false;
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
    private String badgeStyle(String displayStatus) {
        return switch (displayStatus) {
            case "CHỜ DUYỆT"    -> "badge-warn";
            case "TỪ CHỐI"      -> "badge-danger";
            case "SẮP DIỄN RA"  -> "badge-info";
            case "ĐANG ĐẤU GIÁ" -> "badge-success";
            case "ĐÃ KẾT THÚC"  -> "badge-neutral";
            case "ĐÃ BÁN"       -> "badge-success";
            default              -> "badge-neutral";
        };
    }

    private String formatVND(double v) {
        return String.format("₫ %,.0f", v);
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
