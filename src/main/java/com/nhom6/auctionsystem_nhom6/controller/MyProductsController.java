package com.nhom6.auctionsystem_nhom6.controller;

import com.nhom6.auctionsystem_nhom6.AppContext;
import com.nhom6.auctionsystem_nhom6.HelloApplication;
import com.nhom6.auctionsystem_nhom6.ServerConnection;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.example.user.User;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
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

    /** Lưu đường dẫn ảnh: productId → imagePath */
    private static final Map<String, String> productImages = new HashMap<>();

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

        statusFilter.getItems().addAll(
            "Tất cả", "CHỜ DUYỆT", "TỪ CHỐI",
            "SẮP DIỄN RA", "ĐANG ĐẤU GIÁ", "ĐÃ KẾT THÚC",
            "ĐÃ BÁN", "ĐÃ HỦY");

        refreshStats();
        renderList(AppContext.getProducts(username));
    }

    // =========================================================
    // STATS
    // =========================================================
    private void refreshStats() {
        List<AppContext.ProductRecord> list = AppContext.getProducts(username);

        totalProductsLabel.setText(String.valueOf(list.size()));
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
        String displayStatus = AppContext.computeDisplayStatus(p);

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");
        row.setPadding(new Insets(14, 20, 14, 20));

        // ── Thumbnail (ảnh thật hoặc icon mặc định) ──────────
        StackPane thumb = buildThumbnail(p.id(), 64);

        // ── Info ──────────────────────────────────────────────
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

        // ── Giá + badge ──────────────────────────────────────
        VBox priceBox = new VBox(4);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        Label startP = new Label("Khởi điểm: " + formatVND(p.startPrice()));
        startP.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        Label curP = new Label(formatVND(p.currentPrice()));
        curP.getStyleClass().add("history-item-price");
        Label badge = new Label(displayStatus);
        badge.getStyleClass().addAll("history-badge", badgeStyle(displayStatus));
        priceBox.getChildren().addAll(startP, curP, badge);

        // ── Actions ──────────────────────────────────────────
        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);
        buildActionButtons(p, displayStatus, actions);

        row.getChildren().addAll(thumb, info, priceBox, actions);
        return row;
    }

    /**
     * Tạo thumbnail: nếu có ảnh thật thì hiển thị, không thì dùng icon mặc định.
     */
    private StackPane buildThumbnail(String productId, double size) {
        StackPane pane = new StackPane();
        pane.setMinSize(size, size);
        pane.setMaxSize(size, size);
        pane.setStyle(
            "-fx-background-color: #1e3a5f;"
                + "-fx-background-radius: 8;");

        String imagePath = productImages.get(productId);
        if (imagePath != null) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    ImageView iv = new ImageView(
                        new Image(file.toURI().toString(),
                            size, size, true, true));
                    iv.setFitWidth(size);
                    iv.setFitHeight(size);
                    iv.setPreserveRatio(true);
                    pane.getChildren().add(iv);
                    return pane;
                }
            } catch (Exception ignored) {}
        }
        // Fallback icon
        Label icon = new Label("⬡");
        icon.setStyle("-fx-font-size: 28px; -fx-text-fill: #3b82f6;");
        pane.getChildren().add(icon);
        return pane;
    }

    // =========================================================
    // PHÂN QUYỀN NÚT THEO DISPLAY STATUS
    // =========================================================
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
                Button editBtn = new Button("✏️ Sửa");
                editBtn.getStyleClass().add("btn-secondary");
                editBtn.setOnAction(e -> handleEdit(p));
                Button delBtn = new Button("🗑 Hủy");
                delBtn.getStyleClass().add("btn-danger");
                delBtn.setOnAction(e -> handleDelete(p));
                actions.getChildren().addAll(editBtn, delBtn);
            }
            case "ĐANG ĐẤU GIÁ" -> {
                Button liveBtn = new Button("🔴 Vào phiên");
                liveBtn.getStyleClass().add("btn-primary");
                liveBtn.setStyle(
                    "-fx-font-size: 12px; -fx-padding: 5 10 5 10;"
                        + "-fx-min-width: 140;");
                liveBtn.setOnAction(e -> handleGoLive(p));
                actions.getChildren().add(liveBtn);
            }
            case "ĐÃ KẾT THÚC" -> { /* Không có nút */ }
            default -> {
                Button delBtn = new Button("🗑 Xóa");
                delBtn.getStyleClass().add("btn-danger");
                delBtn.setOnAction(e -> handleDelete(p));
                actions.getChildren().add(delBtn);
            }
        }
    }

    // =========================================================
    // ADD PRODUCT — bỏ infoLabel, thêm chọn ảnh
    // =========================================================
    @FXML
    private void handleAddProduct() {
        Dialog<AppContext.ProductRecord> dialog = new Dialog<>();
        dialog.setTitle("Đăng bán sản phẩm mới");
        dialog.setHeaderText("Nhập thông tin sản phẩm");
        dialog.getDialogPane().setPrefWidth(540);

        ButtonType saveBtn =
            new ButtonType("Đăng bán", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
            .addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20));

        // ── Fields ──────────────────────────────────────────
        TextField nameField = new TextField();
        nameField.setPromptText("Tên sản phẩm...");
        nameField.setPrefWidth(320);

        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll(
            "Điện tử", "Máy ảnh", "Laptop", "Điện thoại",
            "Đồng hồ", "Xe cộ", "Khác");
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

        // ── Chọn ảnh sản phẩm ────────────────────────────────
        final String[] selectedImagePath = {null};

        StackPane previewPane = new StackPane();
        previewPane.setMinSize(80, 80);
        previewPane.setMaxSize(80, 80);
        previewPane.setStyle(
            "-fx-background-color: #1e293b;"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: #334155;"
                + "-fx-border-radius: 8;"
                + "-fx-border-width: 2;");
        Label previewIcon = new Label("📷");
        previewIcon.setStyle("-fx-font-size: 28px;");
        previewPane.getChildren().add(previewIcon);

        Label imgNameLabel = new Label("Chưa chọn ảnh");
        imgNameLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        Button chooseImgBtn = new Button("📂  Chọn ảnh");
        chooseImgBtn.setStyle(
            "-fx-background-color: #1e293b; -fx-text-fill: #e2e8f0;"
                + "-fx-border-color: #334155; -fx-border-radius: 6;"
                + "-fx-background-radius: 6; -fx-cursor: hand;"
                + "-fx-font-size: 12px; -fx-padding: 6 14 6 14;");

        Button removeImgBtn = new Button("✕ Xóa ảnh");
        removeImgBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #f87171;"
                + "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
        removeImgBtn.setVisible(false);
        removeImgBtn.setManaged(false);

        chooseImgBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Chọn ảnh sản phẩm");
            fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                    "Ảnh (PNG, JPG, JPEG, GIF, WEBP)",
                    "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
            File file = fc.showOpenDialog(
                dialog.getDialogPane().getScene().getWindow());
            if (file != null) {
                selectedImagePath[0] = file.getAbsolutePath();
                imgNameLabel.setText("✅ " + file.getName());
                imgNameLabel.setStyle(
                    "-fx-text-fill: #22c55e; -fx-font-size: 12px;");
                // Hiển thị preview ảnh
                try {
                    ImageView iv = new ImageView(
                        new Image(file.toURI().toString(), 80, 80, true, true));
                    iv.setFitWidth(80);
                    iv.setFitHeight(80);
                    iv.setPreserveRatio(true);
                    previewPane.getChildren().setAll(iv);
                    previewPane.setStyle(
                        "-fx-background-color: #0f172a;"
                            + "-fx-background-radius: 8;"
                            + "-fx-border-color: #22c55e;"
                            + "-fx-border-radius: 8;"
                            + "-fx-border-width: 2;");
                } catch (Exception ex) {
                    previewPane.getChildren().setAll(previewIcon);
                }
                removeImgBtn.setVisible(true);
                removeImgBtn.setManaged(true);
            }
        });

        removeImgBtn.setOnAction(e -> {
            selectedImagePath[0] = null;
            previewPane.getChildren().setAll(previewIcon);
            previewPane.setStyle(
                "-fx-background-color: #1e293b;"
                    + "-fx-background-radius: 8;"
                    + "-fx-border-color: #334155;"
                    + "-fx-border-radius: 8;"
                    + "-fx-border-width: 2;");
            imgNameLabel.setText("Chưa chọn ảnh");
            imgNameLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            removeImgBtn.setVisible(false);
            removeImgBtn.setManaged(false);
        });

        VBox imgBtnCol = new VBox(6, chooseImgBtn, imgNameLabel, removeImgBtn);
        imgBtnCol.setAlignment(Pos.CENTER_LEFT);
        HBox imgRow = new HBox(14, previewPane, imgBtnCol);
        imgRow.setAlignment(Pos.CENTER_LEFT);

        // ── Error label ──────────────────────────────────────
        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        errLabel.setWrapText(true);

        HBox startRow = new HBox(8, startDate, new Label("lúc"), startTime);
        startRow.setAlignment(Pos.CENTER_LEFT);
        HBox endRow = new HBox(8, endDate, new Label("lúc"), endTime);
        endRow.setAlignment(Pos.CENTER_LEFT);

        String ls =
            "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1e293b;";
        Label lImg = new Label("Ảnh sản phẩm");  lImg.setStyle(ls);
        Label lN   = new Label("Tên sản phẩm *"); lN.setStyle(ls);
        Label lC   = new Label("Danh mục *");      lC.setStyle(ls);
        Label lP   = new Label("Giá khởi điểm *"); lP.setStyle(ls);
        Label lS   = new Label("Thời gian bắt đầu *"); lS.setStyle(ls);
        Label lE   = new Label("Thời gian kết thúc *"); lE.setStyle(ls);

        // ── Lưới form — ảnh đặt đầu tiên, bỏ infoLabel ──────
        grid.add(lImg,     0, 0); grid.add(imgRow,    1, 0);
        grid.add(lN,       0, 1); grid.add(nameField, 1, 1);
        grid.add(lC,       0, 2); grid.add(catBox,    1, 2);
        grid.add(lP,       0, 3); grid.add(priceField,1, 3);
        grid.add(lS,       0, 4); grid.add(startRow,  1, 4);
        grid.add(lE,       0, 5); grid.add(endRow,    1, 5);
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
                errLabel.setText("⚠ Giá khởi điểm không hợp lệ."); return null;
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
                errLabel.setText(
                    "⚠ Thời gian kết thúc phải sau thời gian bắt đầu.");
                return null;
            }
            if (startDT.isBefore(LocalDateTime.now())) {
                errLabel.setText(
                    "⚠ Thời gian bắt đầu phải từ hiện tại trở đi.");
                return null;
            }

            String conflict = findTimeConflict(startDT, endDT, null);
            if (conflict != null) {
                errLabel.setText(
                    "⚠ Trùng thời gian với sản phẩm: \"" + conflict + "\"\n"
                        + "Vui lòng chọn khoảng thời gian khác.");
                return null;
            }

            return new AppContext.ProductRecord(
                "P-" + UUID.randomUUID().toString()
                    .substring(0, 6).toUpperCase(),
                nameVal,
                catBox.getValue() == null ? "Khác" : catBox.getValue(),
                price, price, 0, "CHỜ DUYỆT",
                startDT, endDT, "—");
        });

        dialog.showAndWait().ifPresent(product -> {
            if (product == null) return;

            // Lưu ảnh nếu có
            if (selectedImagePath[0] != null)
                productImages.put(product.id(), selectedImagePath[0]);

            AppContext.addProduct(username, product);

            // Notify Admin qua WebSocket
            ServerConnection conn = ServerConnection.getInstance();
            if (conn.isConnected()) {
                conn.sendJson(String.format(
                    "{\"action\":\"NOTIFY_ADMIN_NEW_PRODUCT\","
                        + "\"productId\":\"%s\","
                        + "\"productName\":\"%s\","
                        + "\"sellerName\":\"%s\","
                        + "\"category\":\"%s\","
                        + "\"startPrice\":%.0f,"
                        + "\"startTime\":\"%s\","
                        + "\"endTime\":\"%s\"}",
                    product.id(), product.name(), username,
                    product.category(), product.startPrice(),
                    product.startTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    product.endTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            }

            refreshStats();
            renderList(AppContext.getProducts(username));
        });
    }

    // =========================================================
    // KIỂM TRA TRÙNG THỜI GIAN
    // =========================================================
    private String findTimeConflict(LocalDateTime newStart,
                                    LocalDateTime newEnd,
                                    String excludeId) {
        for (AppContext.ProductRecord p : AppContext.getProducts(username)) {
            if (excludeId != null && excludeId.equals(p.id())) continue;
            String ds = AppContext.computeDisplayStatus(p);
            if ("TỪ CHỐI".equals(ds) || "ĐÃ BÁN".equals(ds)
                || "ĐÃ KẾT THÚC".equals(ds) || "ĐÃ HỦY".equals(ds)) continue;
            boolean overlap = newStart.isBefore(p.endTime())
                && newEnd.isAfter(p.startTime());
            if (overlap) return p.name() + " [" + ds + "]";
        }
        return null;
    }

    // =========================================================
    // VÀO PHIÊN
    // =========================================================
    private void handleGoLive(AppContext.ProductRecord p) {
        org.example.auction.AuctionSession existing =
            AppContext.getGlobalSessions().stream()
                .filter(s -> s.getSessionId().equals(p.id()))
                .findFirst().orElse(null);

        if (existing != null) {
            AppContext.setActiveSession(existing);
        } else {
            if (p.endTime().isBefore(LocalDateTime.now())) {
                showAlert("Hết hạn", "Phiên đấu giá đã hết thời gian."); return;
            }
            try {
                double step = Math.max(p.startPrice() * 0.05, 500_000);
                org.example.auction.AuctionSession newSess =
                    new org.example.auction.AuctionSession(
                        p.id(), p.name(), p.startPrice(), step, p.endTime());
                newSess.start();
                AppContext.registerSession(newSess, username);
                AppContext.setActiveSession(newSess);
                ServerConnection conn = ServerConnection.getInstance();
                if (conn.isConnected()) {
                    conn.sendSessionStart(p.id(), p.name(), p.startPrice(),
                        step, p.endTime(), username, p.category());
                }
                AppContext.updateProduct(username, new AppContext.ProductRecord(
                    p.id(), p.name(), p.category(),
                    p.startPrice(), p.startPrice(), 0,
                    "ĐANG ĐẤU GIÁ", p.startTime(), p.endTime(), "—"));
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi", "Không thể vào phiên: " + e.getMessage()); return;
            }
        }
        try { HelloApplication.showLiveAuctionView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================
    // EDIT
    // =========================================================
    private void handleEdit(AppContext.ProductRecord p) {
        Dialog<AppContext.ProductRecord> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh sửa sản phẩm");
        dialog.setHeaderText(p.name());
        dialog.getDialogPane().setPrefWidth(540);

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

        // ── Chọn ảnh (edit) ──────────────────────────────────
        final String[] editImagePath = {productImages.get(p.id())};

        StackPane previewPane = buildThumbnailPreview(
            editImagePath[0], 80);

        Label imgNameLabel = new Label(
            editImagePath[0] != null
                ? "✅ " + new File(editImagePath[0]).getName()
                : "Chưa có ảnh");
        imgNameLabel.setStyle(editImagePath[0] != null
            ? "-fx-text-fill: #22c55e; -fx-font-size: 12px;"
            : "-fx-text-fill: #64748b; -fx-font-size: 12px;");

        Button chooseImgBtn = new Button("📂  Đổi ảnh");
        chooseImgBtn.setStyle(
            "-fx-background-color: #1e293b; -fx-text-fill: #e2e8f0;"
                + "-fx-border-color: #334155; -fx-border-radius: 6;"
                + "-fx-background-radius: 6; -fx-cursor: hand;"
                + "-fx-font-size: 12px; -fx-padding: 6 14 6 14;");

        chooseImgBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Chọn ảnh sản phẩm");
            fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                    "Ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
            File file = fc.showOpenDialog(
                dialog.getDialogPane().getScene().getWindow());
            if (file != null) {
                editImagePath[0] = file.getAbsolutePath();
                imgNameLabel.setText("✅ " + file.getName());
                imgNameLabel.setStyle(
                    "-fx-text-fill: #22c55e; -fx-font-size: 12px;");
                try {
                    ImageView iv = new ImageView(
                        new Image(file.toURI().toString(), 80, 80, true, true));
                    iv.setFitWidth(80);
                    iv.setFitHeight(80);
                    previewPane.getChildren().setAll(iv);
                    previewPane.setStyle(
                        "-fx-background-color: #0f172a;"
                            + "-fx-background-radius: 8;"
                            + "-fx-border-color: #22c55e;"
                            + "-fx-border-radius: 8;"
                            + "-fx-border-width: 2;");
                } catch (Exception ex) {}
            }
        });

        VBox imgBtnCol = new VBox(6, chooseImgBtn, imgNameLabel);
        imgBtnCol.setAlignment(Pos.CENTER_LEFT);
        HBox imgRow = new HBox(14, previewPane, imgBtnCol);
        imgRow.setAlignment(Pos.CENTER_LEFT);

        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        errLabel.setWrapText(true);

        HBox endRow = new HBox(8, endDate, new Label("lúc"), endTime);
        endRow.setAlignment(Pos.CENTER_LEFT);

        String ls =
            "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1e293b;";
        grid.add(styledLabel("Ảnh sản phẩm", ls), 0, 0);
        grid.add(imgRow,                            1, 0);
        grid.add(styledLabel("Tên sản phẩm", ls),  0, 1);
        grid.add(nameField,                         1, 1);
        grid.add(styledLabel("Danh mục", ls),       0, 2);
        grid.add(catBox,                            1, 2);
        grid.add(styledLabel("Thời gian kết thúc", ls), 0, 3);
        grid.add(endRow,                            1, 3);
        grid.add(errLabel,                          0, 4, 2, 1);

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
            String conflict = findTimeConflict(p.startTime(), newEnd, p.id());
            if (conflict != null) {
                errLabel.setText(
                    "⚠ Trùng thời gian với sản phẩm: \"" + conflict + "\"");
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
                p.startTime(), newEnd, p.topBidder());
        });

        dialog.showAndWait().ifPresent(updated -> {
            if (updated == null) return;
            if (editImagePath[0] != null)
                productImages.put(updated.id(), editImagePath[0]);
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
                productImages.remove(p.id());
                ServerConnection conn = ServerConnection.getInstance();
                if (conn.isConnected()) {
                    conn.sendJson("{\"action\":\"REMOVE_PRODUCT\","
                        + "\"seller\":\"" + username + "\","
                        + "\"productId\":\"" + p.id() + "\"}");
                }
                refreshStats();
                renderList(AppContext.getProducts(username));
            }
        });
    }

    // =========================================================
    // FILTER / SEARCH
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

    @FXML private void handleStartAll() {
        List<AppContext.ProductRecord> products = AppContext.getProducts(username);
        int started = 0;
        for (AppContext.ProductRecord p : products) {
            String ds = AppContext.computeDisplayStatus(p);
            if (!"ĐƯỢC DUYỆT".equals(ds) && !"CHỜ DUYỆT".equals(ds)) continue;
            if (p.endTime().isBefore(java.time.LocalDateTime.now())) continue;
            boolean alreadyRunning = AppContext.getGlobalSessions().stream()
                .anyMatch(s -> s.getSessionId().equals(p.id()));
            if (alreadyRunning) continue;
            try {
                double step = Math.max(p.startPrice() * 0.05, 500_000);
                org.example.auction.AuctionSession newSess =
                    new org.example.auction.AuctionSession(
                        p.id(), p.name(), p.startPrice(), step, p.endTime());
                newSess.start();
                AppContext.registerSession(newSess, username);
                ServerConnection conn = ServerConnection.getInstance();
                if (conn.isConnected()) {
                    conn.sendSessionStart(p.id(), p.name(), p.startPrice(),
                        step, p.endTime(), username, p.category());
                }
                AppContext.updateProduct(username, new AppContext.ProductRecord(
                    p.id(), p.name(), p.category(),
                    p.startPrice(), p.startPrice(), 0,
                    "ĐANG ĐẤU GIÁ", p.startTime(), p.endTime(), "—"));
                started++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (started > 0) {
            showAlert("Thông báo", "Đã bắt đầu " + started + " phiên đấu giá.");
            refreshStats();
            renderList(AppContext.getProducts(username));
        } else {
            showAlert("Thông báo", "Không có sản phẩm nào sẵn sàng để bắt đầu.");
        }
    }

    @FXML private void handleBack() {
        try { HelloApplication.showMainView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    /** Tạo preview pane cho edit dialog */
    private StackPane buildThumbnailPreview(String imagePath, double size) {
        StackPane pane = new StackPane();
        pane.setMinSize(size, size);
        pane.setMaxSize(size, size);

        if (imagePath != null) {
            try {
                File f = new File(imagePath);
                if (f.exists()) {
                    ImageView iv = new ImageView(
                        new Image(f.toURI().toString(), size, size, true, true));
                    iv.setFitWidth(size);
                    iv.setFitHeight(size);
                    pane.setStyle(
                        "-fx-background-color: #0f172a;"
                            + "-fx-background-radius: 8;"
                            + "-fx-border-color: #22c55e;"
                            + "-fx-border-radius: 8;"
                            + "-fx-border-width: 2;");
                    pane.getChildren().add(iv);
                    return pane;
                }
            } catch (Exception ignored) {}
        }
        Label icon = new Label("📷");
        icon.setStyle("-fx-font-size: 28px;");
        pane.setStyle(
            "-fx-background-color: #1e293b;"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: #334155;"
                + "-fx-border-radius: 8;"
                + "-fx-border-width: 2;");
        pane.getChildren().add(icon);
        return pane;
    }

    private Label styledLabel(String text, String style) {
        Label l = new Label(text);
        l.setStyle(style);
        return l;
    }

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
