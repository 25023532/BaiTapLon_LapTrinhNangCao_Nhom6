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

    private void refreshStats() {
        List<AppContext.ProductRecord> list = AppContext.getProducts(username);
        totalProductsLabel.setText(String.valueOf(list.size()));
        activeProductsLabel.setText(String.valueOf(
            list.stream()
                .filter(p -> "ĐANG ĐẤU GIÁ".equals(AppContext.computeDisplayStatus(p)))
                .count()));
        soldProductsLabel.setText(String.valueOf(
            list.stream().filter(p -> "ĐÃ BÁN".equals(p.status())).count()));
        double rev = list.stream()
            .filter(p -> "ĐÃ BÁN".equals(p.status()))
            .mapToDouble(AppContext.ProductRecord::currentPrice)
            .sum();
        revenueLabel.setText(formatVND(rev));
    }

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
        Label msg = new Label("Bạn chưa đăng bán sản phẩm nào.\nNhấn \"＋ Đăng bán mới\" để bắt đầu.");
        msg.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-text-alignment: CENTER;");
        msg.setWrapText(true);
        box.getChildren().addAll(icon, msg);
        return box;
    }

    private HBox buildProductRow(AppContext.ProductRecord p) {
        String displayStatus = AppContext.computeDisplayStatus(p);
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");
        row.setPadding(new Insets(14, 20, 14, 20));

        StackPane thumb = buildThumbnail(p.id(), 64);
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(p.name());
        name.getStyleClass().add("history-item-name");

        HBox metaRow = new HBox(16);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label cat   = new Label("📂 " + p.category());
        Label bids  = new Label("🔨 " + p.bidCount() + " lượt bid");
        Label start = new Label("🕐 " + p.startTime().format(DT_FMT));
        cat.getStyleClass().add("history-item-meta");
        bids.getStyleClass().add("history-item-meta");
        start.getStyleClass().add("history-item-meta");
        metaRow.getChildren().addAll(cat, bids, start);

        info.getChildren().addAll(name, metaRow);

        VBox priceBox = new VBox(4);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        Label curP = new Label(formatVND(p.currentPrice()));
        curP.getStyleClass().add("history-item-price");
        Label badge = new Label(displayStatus);
        badge.getStyleClass().addAll("history-badge", badgeStyle(displayStatus));
        priceBox.getChildren().addAll(curP, badge);

        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);
        buildActionButtons(p, displayStatus, actions);

        row.getChildren().addAll(thumb, info, priceBox, actions);
        return row;
    }

    private StackPane buildThumbnail(String productId, double size) {
        StackPane pane = new StackPane();
        pane.setMinSize(size, size);
        pane.setMaxSize(size, size);
        pane.setStyle("-fx-background-color: #1e3a5f; -fx-background-radius: 8;");
        String path = productImages.get(productId);
        if (path == null) path = ProductManagementController.imageMap.get(productId);
        if (path != null) {
            try {
                File f = new File(path);
                if (f.exists()) {
                    ImageView iv = new ImageView(new Image(f.toURI().toString(), size, size, true, true));
                    iv.setFitWidth(size); iv.setFitHeight(size);
                    pane.getChildren().add(iv);
                    return pane;
                }
            } catch (Exception ignored) {}
        }
        Label icon = new Label("⬡");
        icon.setStyle("-fx-font-size: 28px; -fx-text-fill: #3b82f6;");
        pane.getChildren().add(icon);
        return pane;
    }

    private void buildActionButtons(AppContext.ProductRecord p, String displayStatus, VBox actions) {
        switch (displayStatus) {
            case "CHỜ DUYỆT" -> {
                Button waitBtn = new Button("⏳ Chờ duyệt");
                waitBtn.setDisable(true);
                waitBtn.setStyle("-fx-background-color: #78350f; -fx-text-fill: #fbbf24; -fx-background-radius: 6; -fx-padding: 5 10 5 10; -fx-min-width: 120;");
                Button delBtn = new Button("🗑 Hủy");
                delBtn.getStyleClass().add("btn-danger");
                delBtn.setOnAction(e -> handleDelete(p));
                actions.getChildren().addAll(waitBtn, delBtn);
            }
            case "SẮP DIỄN RA", "ĐÃ DUYỆT" -> {
                Button startBtn = new Button("⚡ Bắt đầu");
                startBtn.getStyleClass().add("btn-primary");
                startBtn.setOnAction(e -> handleGoLive(p));
                actions.getChildren().add(startBtn);
            }
            case "ĐANG ĐẤU GIÁ" -> {
                Button liveBtn = new Button("🔴 Vào phiên");
                liveBtn.getStyleClass().add("btn-primary");
                liveBtn.setOnAction(e -> handleGoLive(p));
                actions.getChildren().add(liveBtn);
            }
            default -> {
                Button delBtn = new Button("🗑 Xóa");
                delBtn.getStyleClass().add("btn-danger");
                delBtn.setOnAction(e -> handleDelete(p));
                actions.getChildren().add(delBtn);
            }
        }
    }

    @FXML
    private void handleAddProduct() {
        Dialog<AppContext.ProductRecord> dialog = new Dialog<>();
        dialog.setTitle("Đăng bán sản phẩm mới");
        dialog.getDialogPane().setPrefWidth(640);
        dialog.getDialogPane().setPrefHeight(620);

        ButtonType saveBtnType = new ButtonType("✚  Đăng bán", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color: #FFF8F0; -fx-border-color: #1A1A1A; -fx-border-width: 4;");

        final String[] selectedImagePath = {null};
        VBox leftPanel = new VBox(16);
        leftPanel.setPrefWidth(220); leftPanel.setPadding(new Insets(24));
        leftPanel.setAlignment(Pos.TOP_CENTER);
        leftPanel.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: transparent #1A1A1A transparent transparent; -fx-border-width: 3;");

        ImageView preview = new ImageView();
        preview.setFitWidth(160); preview.setFitHeight(160); preview.setPreserveRatio(true);
        StackPane previewBox = new StackPane(new Label("📷"), preview);
        previewBox.setPrefSize(180, 180);
        previewBox.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #1A1A1A; -fx-border-width: 3; -fx-effect: dropshadow(one-pass-box, #1A1A1A, 0, 0, 4, 4);");

        Button chooseImgBtn = new Button("📂  CHỌN ẢNH");
        chooseImgBtn.setMaxWidth(Double.MAX_VALUE);
        chooseImgBtn.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #1F0C40; -fx-font-weight: 900; -fx-border-color: #1A1A1A; -fx-border-width: 3;");
        chooseImgBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File file = fc.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (file != null) {
                selectedImagePath[0] = file.getAbsolutePath();
                preview.setImage(new Image(file.toURI().toString(), 180, 180, true, true));
            }
        });
        leftPanel.getChildren().addAll(new Label("ẢNH SẢN PHẨM"), previewBox, chooseImgBtn);

        VBox rightPanel = new VBox(15);
        rightPanel.setPadding(new Insets(24));
        rightPanel.setStyle("-fx-background-color: #FFF8F0;");
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        TextField nameField = styledTextFieldNeo("Tên sản phẩm...");
        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll("Laptop","Điện thoại","Máy ảnh","Điện tử","Đồng hồ","Xe cộ","Khác");
        catBox.setMaxWidth(Double.MAX_VALUE);
        catBox.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #1A1A1A; -fx-border-width: 3;");

        TextArea descArea = new TextArea(); descArea.setPrefHeight(80); descArea.setWrapText(true);
        descArea.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #1A1A1A; -fx-border-width: 3;");

        TextField priceField = styledTextFieldNeo("VD: 5.000.000");
        DatePicker startDate = new DatePicker(LocalDate.now());
        TextField startTime = styledTextFieldNeo(LocalTime.now().plusMinutes(5).format(TIME_ONLY));
        DatePicker endDate = new DatePicker(LocalDate.now().plusDays(7));
        TextField endTime = styledTextFieldNeo("23:59");

        Label errLabel = new Label(); errLabel.setStyle("-fx-text-fill: #FF3B3B; -fx-font-weight: bold;");
        rightPanel.getChildren().addAll(
            fieldGroupNeo("TÊN SẢN PHẨM *", nameField),
            fieldGroupNeo("DANH MỤC *", catBox),
            fieldGroupNeo("MÔ TẢ SẢN PHẨM", descArea),
            fieldGroupNeo("GIÁ KHỞI ĐIỂM (₫) *", priceField),
            fieldGroupNeo("BẮT ĐẦU *", new HBox(10, startDate, startTime)),
            fieldGroupNeo("KẾT THÚC *", new HBox(10, endDate, endTime)),
            errLabel
        );

        HBox root = new HBox(leftPanel, rightPanel);
        dialog.getDialogPane().setContent(root);

        javafx.application.Platform.runLater(() -> {
            Button saveNode = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
            saveNode.setStyle("-fx-background-color: #FF6B35; -fx-text-fill: white; -fx-font-weight: 900; -fx-border-color: #1A1A1A; -fx-border-width: 3; -fx-effect: dropshadow(one-pass-box, #1A1A1A, 0, 0, 4, 4);");
            saveNode.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                try {
                    if (nameField.getText().trim().isEmpty()) throw new Exception("Vui lòng nhập tên.");
                    Double.parseDouble(priceField.getText().replaceAll("[^0-9.]",""));
                    LocalDateTime s = LocalDateTime.of(startDate.getValue(), LocalTime.parse(startTime.getText().trim(), TIME_ONLY));
                    LocalDateTime e = LocalDateTime.of(endDate.getValue(), LocalTime.parse(endTime.getText().trim(), TIME_ONLY));
                    if (!e.isAfter(s)) throw new Exception("Thời gian kết thúc phải sau bắt đầu.");
                    if (s.isBefore(LocalDateTime.now().minusMinutes(1))) throw new Exception("Thời gian bắt đầu không hợp lệ.");
                } catch (Exception ex) {
                    errLabel.setText("⚠ " + ex.getMessage());
                    event.consume();
                }
            });
        });

        dialog.setResultConverter(btn -> {
            if (btn != saveBtnType) return null;
            try {
                String id = "P-" + UUID.randomUUID().toString().substring(0,6).toUpperCase();
                double p = Double.parseDouble(priceField.getText().replaceAll("[^0-9.]",""));
                LocalDateTime s = LocalDateTime.of(startDate.getValue(), LocalTime.parse(startTime.getText().trim(), TIME_ONLY));
                LocalDateTime e = LocalDateTime.of(endDate.getValue(), LocalTime.parse(endTime.getText().trim(), TIME_ONLY));
                if (selectedImagePath[0] != null) productImages.put(id, selectedImagePath[0]);
                ProductManagementController.descMap.put(id, descArea.getText().trim());
                return new AppContext.ProductRecord(id, nameField.getText().trim(), catBox.getValue(), p, p, 0, "CHỜ DUYỆT", s, e, "—");
            } catch (Exception ex) { return null; }
        });

        dialog.showAndWait().ifPresent(p -> {
            AppContext.addProduct(username, p);
            refreshStats();
            renderList(AppContext.getProducts(username));
        });
    }

    private void handleGoLive(AppContext.ProductRecord p) {
        org.example.auction.AuctionSession existing = AppContext.getGlobalSessions().stream()
            .filter(s -> s.getSessionId().equals(p.id())).findFirst().orElse(null);
        if (existing != null) {
            AppContext.setActiveSession(existing);
        } else {
            if (p.endTime().isBefore(LocalDateTime.now())) { showAlert("Hết hạn", "Đã hết thời gian."); return; }
            try {
                double step = Math.max(p.startPrice() * 0.05, 500_000);
                org.example.auction.AuctionSession ns = new org.example.auction.AuctionSession(p.id(), p.name(), p.startPrice(), step, p.endTime());
                ns.start();
                AppContext.registerSession(ns, username);
                AppContext.setActiveSession(ns);
                ServerConnection.getInstance().sendSessionStart(p.id(), p.name(), p.startPrice(), step, p.endTime(), username, p.category());
                AppContext.updateProduct(username, p.withUpdated(p.startPrice(), 0, "ĐANG ĐẤU GIÁ", "—"));
            } catch (Exception e) { showAlert("Lỗi", e.getMessage()); return; }
        }
        try { HelloApplication.showLiveAuctionView(); } catch (Exception e) {}
    }

    private void handleDelete(AppContext.ProductRecord p) {
        AppContext.removeProduct(username, p.id());
        refreshStats(); renderList(AppContext.getProducts(username));
    }

    @FXML private void handleFilter() { applyFilters(); }
    @FXML private void handleSearch() { applyFilters(); }

    private void applyFilters() {
        String kw = searchField.getText().trim().toLowerCase();
        String st = statusFilter.getValue();
        List<AppContext.ProductRecord> filtered = AppContext.getProducts(username).stream()
            .filter(p -> {
                String ds = AppContext.computeDisplayStatus(p);
                if (st != null && !"Tất cả".equals(st) && !st.equals(ds)) return false;
                if (!kw.isEmpty() && !p.name().toLowerCase().contains(kw)) return false;
                return true;
            }).collect(Collectors.toList());
        renderList(filtered);
    }

    @FXML private void handleStartAll() {
        for (AppContext.ProductRecord p : AppContext.getProducts(username)) {
            String ds = AppContext.computeDisplayStatus(p);
            if (("ĐÃ DUYỆT".equals(ds) || "SẮP DIỄN RA".equals(ds)) && p.endTime().isAfter(LocalDateTime.now()))
                handleGoLive(p);
        }
        refreshStats(); renderList(AppContext.getProducts(username));
    }

    @FXML private void handleBack() { try { HelloApplication.showMainView(); } catch (Exception e) {} }

    private TextField styledTextFieldNeo(String prompt) {
        TextField tf = new TextField(); tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #1A1A1A; -fx-border-width: 3; -fx-padding: 10;");
        return tf;
    }

    private VBox fieldGroupNeo(String labelText, javafx.scene.Node field) {
        Label l = new Label(labelText); l.setStyle("-fx-font-weight: 900; -fx-font-size: 10px;");
        return new VBox(5, l, field);
    }

    private String badgeStyle(String s) {
        return switch (s) {
            case "CHỜ DUYỆT" -> "badge-warn";
            case "ĐANG ĐẤU GIÁ" -> "badge-success";
            case "SẮP DIỄN RA" -> "badge-info";
            default -> "badge-neutral";
        };
    }

    private String formatVND(double v) { return String.format("₫ %,.0f", v); }
    private void showAlert(String t, String m) { Alert a = new Alert(Alert.AlertType.WARNING); a.setTitle(t); a.setContentText(m); a.showAndWait(); }
}
