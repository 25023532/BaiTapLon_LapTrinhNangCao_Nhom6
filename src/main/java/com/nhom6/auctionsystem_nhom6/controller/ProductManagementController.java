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
import org.example.auction.AuctionSession;
import org.example.user.User;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
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

    private String  username;
    private boolean isAdmin;

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter TIME_ONLY =
        DateTimeFormatter.ofPattern("HH:mm");

    private static final String IMAGE_DIR = "D:/123/product_images/";

    private final List<ManagedProduct> managedList = new ArrayList<>();
    private static final Map<String, Double>  stepMap     = new HashMap<>();
    public static final Map<String, String>  descMap     = new HashMap<>();
    public static final Map<String, String> imageMap = new HashMap<>();

    static {
        try {
            java.io.File dir = new java.io.File(IMAGE_DIR);
            if (dir.exists() && dir.isDirectory()) {
                for (java.io.File f : dir.listFiles()) {
                    String fileName = f.getName(); // e.g. "P-ABC123.png"
                    int dot = fileName.lastIndexOf('.');
                    if (dot > 0) {
                        String productId = fileName.substring(0, dot); // "P-ABC123"
                        imageMap.put(productId, f.getAbsolutePath());
                    }
                }
                System.out.println("[ImageMap] Loaded " + imageMap.size() + " images from disk.");
            }
        } catch (Exception e) {
            System.err.println("[ImageMap] Failed to load images: " + e.getMessage());
        }
    }

    public record ManagedProduct(
        String        id,
        String        name,
        String        category,
        String        sellerName,
        double        startPrice,
        String        status,
        LocalDateTime createdAt,
        LocalDateTime auctionStart,
        LocalDateTime auctionEnd
    ) {}

    @FXML private Button btnSellerProducts;
    @FXML private Button btnAdminProducts;

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        User user = AppContext.getCurrentUser();
        if (user != null) {
            applyRoleMenu(user);
        }
        username  = user.getUsername();
        isAdmin   = "ADMIN".equalsIgnoreCase(user.getRole());

        new File(IMAGE_DIR).mkdirs();

        statusFilter.getItems().addAll(
            "Tất cả", "CHỜ DUYỆT", "ĐÃ DUYỆT",
            "ĐANG ĐẤU GIÁ", "ĐÃ BÁN", "TỪ CHỐI");
        categoryFilter.getItems().addAll(
            "Tất cả", "Laptop", "Điện thoại", "Máy ảnh",
            "Điện tử", "Đồng hồ", "Xe cộ", "Khác");

        managedList.clear();
        if (isAdmin)
            syncAllSellersFromAppContext();
        else
            syncFromAppContext(user);

        refreshStats();
        renderList(managedList);
    }

    private void applyRoleMenu(User user) {
        String role = user.getRole() == null ? "" : user.getRole().toUpperCase();
        boolean isSeller = "SELLER".equals(role);
        boolean isAdmin  = "ADMIN".equals(role);
        if (btnSellerProducts != null) { btnSellerProducts.setVisible(isSeller); btnSellerProducts.setManaged(isSeller); }
        if (btnAdminProducts != null) { btnAdminProducts.setVisible(isAdmin); btnAdminProducts.setManaged(isAdmin); }
    }

    @FXML private void handleAuctionList() { try { HelloApplication.showAuctionListView(); } catch (Exception e) {} }
    @FXML private void handleLiveAuction() { try { HelloApplication.showLiveAuctionView(); } catch (Exception e) {} }
    @FXML private void handleSellerProducts() { try { HelloApplication.showMyProductsView(); } catch (Exception e) {} }
    @FXML private void handleAdminProducts() { /* Already here */ }
    @FXML private void handleWallet() { try { HelloApplication.showWalletView(); } catch (Exception e) {} }
    @FXML private void handleRating() { try { HelloApplication.showRatingView(); } catch (Exception e) {} }
    @FXML private void handleHelp() { try { HelloApplication.showHelpView(); } catch (Exception e) {} }
    @FXML private void handleCategoryDienTu()    { try { HelloApplication.showAuctionListByCategory("Điện tử");    } catch (Exception e) {} }
    @FXML private void handleCategoryMayAnh()    { try { HelloApplication.showAuctionListByCategory("Máy ảnh");    } catch (Exception e) {} }
    @FXML private void handleCategoryLaptop()    { try { HelloApplication.showAuctionListByCategory("Laptop");     } catch (Exception e) {} }
    @FXML private void handleCategoryDienThoai() { try { HelloApplication.showAuctionListByCategory("Điện thoại"); } catch (Exception e) {} }
    @FXML private void handleCategoryDongHo()    { try { HelloApplication.showAuctionListByCategory("Đồng hồ");   } catch (Exception e) {} }
    @FXML private void handleCategoryXeCo()      { try { HelloApplication.showAuctionListByCategory("Xe cộ");      } catch (Exception e) {} }
    @FXML private void handleHistory()           { try { HelloApplication.showHistoryView(); } catch (Exception e) {} }
    @FXML private void handleProfile()           { try { HelloApplication.showProfileView(); } catch (Exception e) {} }
    @FXML private void handleLogout() {
        AppContext.logout();
        try { HelloApplication.showLoginView(); } catch (Exception e) {}
    }

    private void syncFromAppContext(User user) {
        for (AppContext.ProductRecord p :
            AppContext.getProducts(user.getUsername())) {
            boolean exists = managedList.stream()
                .anyMatch(m -> m.id().equals(p.id()));
            if (!exists) {
                managedList.add(new ManagedProduct(
                    p.id(), p.name(), p.category(),
                    user.getUsername(), p.startPrice(),
                    p.status(), LocalDateTime.now(),
                    p.startTime(), p.endTime()
                ));
            }
        }
    }

    private void syncAllSellersFromAppContext() {
        for (AppContext.ProductRecord p : AppContext.getAllProducts()) {
            boolean exists = managedList.stream()
                .anyMatch(m -> m.id().equals(p.id()));
            if (!exists) {
                String sellerName = AppContext.getSessionSeller(p.id());
                if (sellerName == null || sellerName.isBlank())
                    sellerName = "—";
                managedList.add(new ManagedProduct(
                    p.id(), p.name(), p.category(),
                    sellerName, p.startPrice(),
                    p.status(), LocalDateTime.now(),
                    p.startTime(), p.endTime()
                ));
            }
        }
    }

    // =========================================================
    // STATS
    // =========================================================
    private void refreshStats() {
        totalProductsLabel.setText(String.valueOf(managedList.size()));
        approvedLabel.setText(String.valueOf(managedList.stream()
            .filter(p -> "ĐÃ DUYỆT".equals(p.status())).count()));
        pendingLabel.setText(String.valueOf(managedList.stream()
            .filter(p -> "CHỜ DUYỆT".equals(p.status())).count()));
        auctioningLabel.setText(String.valueOf(managedList.stream()
            .filter(p -> "ĐANG ĐẤU GIÁ".equals(p.status())).count()));
    }

    // =========================================================
    // RENDER
    // =========================================================
    private void renderList(List<ManagedProduct> list) {
        productListBox.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label(isAdmin
                ? "Chưa có sản phẩm nào cần duyệt."
                : "Chưa có sản phẩm nào. Nhấn \"＋ Thêm sản phẩm\".");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; "
                + "-fx-padding: 40 0 40 0;");
            productListBox.getChildren().add(empty);
            return;
        }
        for (ManagedProduct p : list)
            productListBox.getChildren().add(buildRow(p));
    }

    private HBox buildRow(ManagedProduct p) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");
        row.setPadding(new Insets(12, 20, 12, 20));

        // Ảnh thumbnail
        String imgPath = imageMap.get(p.id());
        if (imgPath != null && new File(imgPath).exists()) {
            try {
                ImageView thumb = new ImageView(
                    new Image(new File(imgPath).toURI().toString()));
                thumb.setFitWidth(48);
                thumb.setFitHeight(48);
                thumb.setPreserveRatio(true);
                StackPane thumbBox = new StackPane(thumb);
                thumbBox.setMinWidth(56);
                thumbBox.setStyle("-fx-background-color: #0f172a; "
                    + "-fx-background-radius: 8; -fx-padding: 4;");
                HBox.setMargin(thumbBox, new Insets(0, 12, 0, 0));
                row.getChildren().add(thumbBox);
            } catch (Exception ignored) {}
        } else {
            Label placeholder = new Label("📦");
            placeholder.setStyle("-fx-font-size: 28px; -fx-min-width: 48px; "
                + "-fx-alignment: CENTER;");
            row.getChildren().add(placeholder);
        }

        // Tên + meta
        VBox nameBox = new VBox(3);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label name = new Label(p.name());
        name.getStyleClass().add("history-item-name");
        Label idLabel = new Label(
            "ID: " + p.id()
                + "  •  Người bán: " + p.sellerName()
                + "  •  Bắt đầu: " + p.auctionStart().format(DT_FMT)
                + "  •  Kết thúc: " + p.auctionEnd().format(DT_FMT));
        idLabel.getStyleClass().add("history-item-meta");
        nameBox.getChildren().addAll(name, idLabel);

        Label cat = new Label(p.category());
        cat.getStyleClass().add("history-item-meta");
        cat.setMinWidth(110);

        Label price = new Label(formatVND(p.startPrice()));
        price.setStyle("-fx-font-size: 13px; -fx-text-fill: #38bdf8; "
            + "-fx-font-weight: bold;");
        price.setMinWidth(140);

        Label badge = new Label(p.status());
        badge.getStyleClass().addAll("history-badge",
            managedBadgeStyle(p.status()));
        badge.setMinWidth(130);

        HBox actions = buildActions(p);
        actions.setMinWidth(240);

        row.getChildren().addAll(nameBox, cat, price, badge, actions);
        return row;
    }

    // =========================================================
    // ACTIONS — PHÂN QUYỀN
    // =========================================================
    private HBox buildActions(ManagedProduct p) {
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);

        switch (p.status()) {
            case "CHỜ DUYỆT" -> {
                if (isAdmin) {
                    Button approveBtn = new Button("✅ Duyệt");
                    approveBtn.setStyle(
                        "-fx-background-color: #14532d; -fx-text-fill: #4ade80; "
                            + "-fx-background-radius: 6; -fx-cursor: hand; "
                            + "-fx-font-size: 12px; -fx-padding: 5 10 5 10;");
                    approveBtn.setOnAction(e -> handleApprove(p));

                    Button rejectBtn = new Button("❌ Từ chối");
                    rejectBtn.getStyleClass().add("btn-danger");
                    rejectBtn.setOnAction(e -> handleReject(p));

                    actions.getChildren().addAll(approveBtn, rejectBtn);
                } else {
                    Label waitLabel = new Label("⏳ Chờ Admin duyệt");
                    waitLabel.setStyle("-fx-text-fill: #94a3b8; "
                        + "-fx-font-size: 12px; -fx-font-style: italic;");

                    Button deleteBtn = new Button("🗑 Xóa");
                    deleteBtn.getStyleClass().add("btn-danger");
                    deleteBtn.setOnAction(e -> handleDelete(p));

                    actions.getChildren().addAll(waitLabel, deleteBtn);
                }
            }
            case "ĐÃ DUYỆT" -> {
                if (isAdmin) {
                    Button viewBtn = new Button("👁 Xem");
                    viewBtn.getStyleClass().add("btn-secondary");
                    viewBtn.setOnAction(e -> handleView(p));
                    actions.getChildren().add(viewBtn);
                } else {
                    Button startBtn = new Button("⚡ Bắt đầu đấu giá");
                    startBtn.getStyleClass().add("btn-primary");
                    startBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 10 5 10;");
                    startBtn.setOnAction(e -> handleStartAuction(p));

                    Button editBtn = new Button("✏️ Sửa");
                    editBtn.getStyleClass().add("btn-secondary");
                    editBtn.setOnAction(e -> handleEdit(p));

                    actions.getChildren().addAll(startBtn, editBtn);
                }
            }
            case "ĐANG ĐẤU GIÁ" -> {
                Button liveBtn = new Button("🔴 Vào phiên");
                liveBtn.getStyleClass().add("btn-primary");
                liveBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 10 5 10;");
                liveBtn.setOnAction(e -> handleGoLive(p));

                Button viewBtn = new Button("👁 Xem");
                viewBtn.getStyleClass().add("btn-secondary");
                viewBtn.setOnAction(e -> handleView(p));

                actions.getChildren().addAll(liveBtn, viewBtn);
            }
            default -> {
                Button viewBtn = new Button("👁 Xem");
                viewBtn.getStyleClass().add("btn-secondary");
                viewBtn.setOnAction(e -> handleView(p));
                actions.getChildren().add(viewBtn);
            }
        }
        return actions;
    }

    // =========================================================
    // ADD PRODUCT
    // =========================================================
    @FXML private void handleAdd() {
        Dialog<ManagedProduct> dialog = new Dialog<>();
        dialog.setTitle("Thêm sản phẩm mới");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setPrefWidth(640);
        dialog.getDialogPane().setPrefHeight(620);

        ButtonType saveBtn =
            new ButtonType("✚  Đăng bán", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
            .addAll(saveBtn, ButtonType.CANCEL);

        dialog.getDialogPane().setStyle(
            "-fx-background-color: #FFF8F0; "
                + "-fx-border-color: #1A1A1A; -fx-border-width: 4;");

        // ── Upload ảnh ───────────────────────────────────────
        final String[] selectedImagePath = {null};

        VBox leftPanel = new VBox(16);
        leftPanel.setPrefWidth(220);
        leftPanel.setMinWidth(220);
        leftPanel.setPadding(new Insets(24, 20, 24, 24));
        leftPanel.setAlignment(Pos.TOP_CENTER);
        leftPanel.setStyle("-fx-background-color: #FFFFFF; "
            + "-fx-border-color: transparent #1A1A1A transparent transparent; "
            + "-fx-border-width: 3;");

        Label imgTitle = new Label("ẢNH SẢN PHẨM");
        imgTitle.setStyle("-fx-text-fill: #1A1A1A; -fx-font-size: 11px; "
            + "-fx-font-weight: 900; -fx-letter-spacing: 1.5px;");

        ImageView preview = new ImageView();
        preview.setFitWidth(160);
        preview.setFitHeight(160);
        preview.setPreserveRatio(true);

        StackPane previewBox = new StackPane();
        previewBox.setPrefSize(180, 180);
        previewBox.setMaxSize(180, 180);
        previewBox.setStyle("-fx-background-color: #FFFFFF; "
            + "-fx-border-color: #1A1A1A; -fx-border-width: 3; "
            + "-fx-cursor: hand; -fx-effect: dropshadow(one-pass-box, #1A1A1A, 0, 0, 4, 4);");

        VBox emptyHint = new VBox(8);
        emptyHint.setAlignment(Pos.CENTER);
        Label camIcon = new Label("📷");
        camIcon.setStyle("-fx-font-size: 42px;");
        Label hintTxt = new Label("CHƯA CÓ ẢNH\nCLICK ĐỂ CHỌN");
        hintTxt.setStyle("-fx-text-fill: #1A1A1A; -fx-font-size: 10px; "
            + "-fx-font-weight: 900; -fx-text-alignment: CENTER;");
        hintTxt.setWrapText(true);
        emptyHint.getChildren().addAll(camIcon, hintTxt);
        previewBox.getChildren().addAll(emptyHint, preview);

        Label fileNameLabel = new Label("Chưa chọn file");
        fileNameLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 10px;");
        fileNameLabel.setWrapText(true);
        fileNameLabel.setMaxWidth(180);

        Button chooseImgBtn = new Button("📂  CHỌN ẢNH");
        chooseImgBtn.setMaxWidth(Double.MAX_VALUE);
        chooseImgBtn.setStyle("-fx-background-color: #FFFFFF; "
            + "-fx-text-fill: #1F0C40; -fx-font-size: 12px; "
            + "-fx-font-weight: 900; -fx-background-radius: 0; "
            + "-fx-border-color: #1A1A1A; -fx-border-width: 3; "
            + "-fx-cursor: hand; -fx-padding: 10 0 10 0; "
            + "-fx-effect: dropshadow(one-pass-box, #1A1A1A, 0, 0, 3, 3);");

        Label fmtHint = new Label("PNG · JPG · WEBP · GIF");
        fmtHint.setStyle("-fx-text-fill: #999999; -fx-font-size: 10px; -fx-font-weight: bold;");

        Runnable openFileChooser = () -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Chọn ảnh sản phẩm");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
            File file = fc.showOpenDialog(
                dialog.getDialogPane().getScene().getWindow());
            if (file != null) {
                selectedImagePath[0] = file.getAbsolutePath();
                fileNameLabel.setText(file.getName());
                try {
                    preview.setImage(new Image(
                        file.toURI().toString(), 180, 180, true, true));
                    previewBox.getChildren().remove(emptyHint);
                } catch (Exception ex) {
                    fileNameLabel.setText("⚠ Không đọc được ảnh");
                }
            }
        };

        chooseImgBtn.setOnAction(e -> openFileChooser.run());
        previewBox.setOnMouseClicked(e -> openFileChooser.run());
        leftPanel.getChildren().addAll(
            imgTitle, previewBox, chooseImgBtn, fileNameLabel, fmtHint);

        // ── Form ─────────────────────────────────────────────
        VBox rightPanel = new VBox(0);
        rightPanel.setPadding(new Insets(24));
        rightPanel.setStyle("-fx-background-color: #FFF8F0;");
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        Label formTitle = new Label("THÔNG TIN SẢN PHẨM");
        formTitle.setStyle("-fx-text-fill: #1F0C40; -fx-font-size: 16px; "
            + "-fx-font-weight: 900; -fx-letter-spacing: 1.5px;");
        VBox.setMargin(formTitle, new Insets(0, 0, 20, 0));

        VBox formFields = new VBox(15);

        TextField nameField = styledTextFieldNeo("Tên sản phẩm...");
        formFields.getChildren().add(fieldGroupNeo("TÊN SẢN PHẨM *", nameField));

        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll("Laptop","Điện thoại","Máy ảnh",
            "Điện tử","Đồng hồ","Xe cộ","Khác");
        catBox.setPromptText("Chọn danh mục");
        catBox.setMaxWidth(Double.MAX_VALUE);
        catBox.setStyle("-fx-background-color: #FFFFFF; "
            + "-fx-border-color: #1A1A1A; -fx-border-width: 3; "
            + "-fx-background-radius: 0; -fx-font-size: 13px; "
            + "-fx-cursor: hand; -fx-padding: 8 12 8 12;");

        catBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null)
                    setStyle("-fx-text-fill: #1A1A1A; -fx-background-color: #FFFFFF; "
                        + "-fx-font-size: 13px; -fx-padding: 8 12 8 12;");
            }
        });

        formFields.getChildren().add(fieldGroupNeo("DANH MỤC *", catBox));

        TextArea descArea = new TextArea();
        descArea.setPromptText("Mô tả chi tiết sản phẩm...");
        descArea.setPrefHeight(80);
        descArea.setWrapText(true);
        descArea.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #1A1A1A; "
            + "-fx-border-color: #1A1A1A; -fx-border-width: 3; "
            + "-fx-background-radius: 0; -fx-font-size: 13px;");
        formFields.getChildren().add(fieldGroupNeo("MÔ TẢ SẢN PHẨM", descArea));

        TextField priceField = styledTextFieldNeo("VD: 5.000.000");
        TextField stepField  = styledTextFieldNeo("VD: 500.000");
        stepField.setText("500000");

        HBox priceRow = new HBox(15, priceField, stepField);
        HBox.setHgrow(priceField, Priority.ALWAYS);
        HBox.setHgrow(stepField,  Priority.ALWAYS);

        HBox priceLabels = new HBox(15,
            fieldLabelNeo("GIÁ KHỞI ĐIỂM (₫) *"),
            fieldLabelNeo("BƯỚC GIÁ TỐI THIỂU (₫) *"));
        priceLabels.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        formFields.getChildren().add(new VBox(8, priceLabels, priceRow));

        DatePicker startDate = styledDatePickerNeo(LocalDate.now());
        TextField  startTime = styledTextFieldNeo("");
        startTime.setText(LocalTime.now().plusMinutes(10).format(TIME_ONLY));
        startTime.setPrefWidth(100);

        Label startLbl = new Label("LÚC");
        startLbl.setStyle("-fx-text-fill: #1A1A1A; -fx-font-weight: 900; -fx-padding: 12 0 0 0; -fx-font-size: 10px;");
        HBox startRow = new HBox(10, startDate, startLbl, startTime);
        HBox.setHgrow(startDate, Priority.ALWAYS);
        formFields.getChildren().add(fieldGroupNeo("BẮT ĐẦU *", startRow));

        DatePicker endDate = styledDatePickerNeo(LocalDate.now().plusDays(7));
        TextField  endTime = styledTextFieldNeo("");
        endTime.setText("23:59");
        endTime.setPrefWidth(100);

        Label endLbl = new Label("LÚC");
        endLbl.setStyle("-fx-text-fill: #1A1A1A; -fx-font-weight: 900; -fx-padding: 12 0 0 0; -fx-font-size: 10px;");
        HBox endRow = new HBox(10, endDate, endLbl, endTime);
        HBox.setHgrow(endDate, Priority.ALWAYS);
        formFields.getChildren().add(fieldGroupNeo("KẾT THÚC *", endRow));

        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #FF3B3B; -fx-font-size: 12px; "
            + "-fx-font-weight: bold; -fx-padding: 10 0 0 0;");
        errLabel.setWrapText(true);

        rightPanel.getChildren().addAll(formTitle, formFields, errLabel);
        VBox.setVgrow(formFields, Priority.ALWAYS);

        HBox root = new HBox(0);
        root.getChildren().addAll(leftPanel, rightPanel);
        dialog.getDialogPane().setContent(root);

        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node saveNode =
                dialog.getDialogPane().lookupButton(saveBtn);
            javafx.scene.Node cancelNode =
                dialog.getDialogPane().lookupButton(ButtonType.CANCEL);

            saveNode.setStyle("-fx-background-color: #FF6B35; "
                + "-fx-text-fill: white; -fx-font-weight: 900; "
                + "-fx-font-size: 14px; -fx-background-radius: 0; "
                + "-fx-border-color: #1A1A1A; -fx-border-width: 3; "
                + "-fx-padding: 12 30 12 30; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(one-pass-box, #1A1A1A, 0, 0, 4, 4);");

            cancelNode.setStyle("-fx-background-color: #FFFFFF; "
                + "-fx-text-fill: #1A1A1A; -fx-font-size: 14px; "
                + "-fx-font-weight: 900; -fx-background-radius: 0; "
                + "-fx-border-color: #1A1A1A; -fx-border-width: 3; "
                + "-fx-padding: 12 30 12 30; -fx-cursor: hand;");

            saveNode.setDisable(nameField.getText().trim().isEmpty());
            nameField.textProperty().addListener((obs, o, n) ->
                saveNode.setDisable(n.trim().isEmpty()));

            saveNode.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                errLabel.setText("");
                if (nameField.getText().trim().isEmpty()) {
                    errLabel.setText("⚠ Vui lòng nhập tên sản phẩm.");
                    event.consume(); return;
                }
                if (catBox.getValue() == null) {
                    errLabel.setText("⚠ Vui lòng chọn danh mục.");
                    event.consume(); return;
                }
                try {
                    double pr = Double.parseDouble(priceField.getText()
                        .trim().replaceAll("[^0-9.]", ""));
                    if (pr <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    errLabel.setText("⚠ Giá khởi điểm không hợp lệ.");
                    event.consume(); return;
                }
                try {
                    double st = Double.parseDouble(stepField.getText()
                        .trim().replaceAll("[^0-9.]", ""));
                    if (st <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    errLabel.setText("⚠ Bước giá không hợp lệ.");
                    event.consume(); return;
                }
                if (startDate.getValue() == null || endDate.getValue() == null) {
                    errLabel.setText("⚠ Vui lòng chọn ngày.");
                    event.consume(); return;
                }
                try {
                    LocalDateTime sDT = LocalDateTime.of(startDate.getValue(),
                        LocalTime.parse(startTime.getText().trim(), TIME_ONLY));
                    LocalDateTime eDT = LocalDateTime.of(endDate.getValue(),
                        LocalTime.parse(endTime.getText().trim(), TIME_ONLY));
                    if (!eDT.isAfter(sDT)) {
                        errLabel.setText("⚠ Thời gian kết thúc phải sau bắt đầu.");
                        event.consume(); return;
                    }
                } catch (DateTimeParseException ex) {
                    errLabel.setText("⚠ Thời gian không hợp lệ (HH:mm).");
                    event.consume();
                }
            });
        });

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;

            try {
                String nameVal = nameField.getText().trim();
                double price   = Double.parseDouble(priceField.getText()
                    .trim().replaceAll("[^0-9.]", ""));
                double step    = Double.parseDouble(stepField.getText()
                    .trim().replaceAll("[^0-9.]", ""));
                LocalDateTime startDT = LocalDateTime.of(startDate.getValue(),
                    LocalTime.parse(startTime.getText().trim(), TIME_ONLY));
                LocalDateTime endDT   = LocalDateTime.of(endDate.getValue(),
                    LocalTime.parse(endTime.getText().trim(), TIME_ONLY));
                String catVal = catBox.getValue() == null
                    ? "Khác" : catBox.getValue();
                String id = "P-" + UUID.randomUUID()
                    .toString().substring(0, 6).toUpperCase();

                stepMap.put(id, step);
                descMap.put(id, descArea.getText().trim());

                if (selectedImagePath[0] != null) {
                    try {
                        File src = new File(selectedImagePath[0]);
                        String ext = src.getName()
                            .substring(src.getName().lastIndexOf('.'));
                        Path dest = Paths.get(IMAGE_DIR + id + ext);
                        Files.copy(src.toPath(), dest,
                            StandardCopyOption.REPLACE_EXISTING);
                        imageMap.put(id, dest.toAbsolutePath().toString());
                    } catch (IOException ex) {
                        System.err.println("Lưu ảnh thất bại: " + ex.getMessage());
                    }
                }

                String status = isAdmin ? "ĐÃ DUYỆT" : "CHỜ DUYỆT";
                return new ManagedProduct(id, nameVal, catVal, username,
                    price, status, LocalDateTime.now(), startDT, endDT);
            } catch (Exception e) {
                return null;
            }
        });

        dialog.showAndWait().ifPresent(p -> {
            if (p == null) return;
            managedList.add(p);
            AppContext.addProduct(username, new AppContext.ProductRecord(
                p.id(), p.name(), p.category(),
                p.startPrice(), p.startPrice(), 0,
                p.status(), p.auctionStart(), p.auctionEnd(), "—"
            ));

            if (!isAdmin) {
                ServerConnection conn = ServerConnection.getInstance();
                if (conn.isConnected()) {
                    conn.sendProductPending(
                        p.id(), p.name(), username,
                        p.category(), p.startPrice(),
                        p.auctionStart(), p.auctionEnd()
                    );
                }
            }

            refreshStats();
            renderList(managedList);
        });
    }

    // =========================================================
    // HANDLERS
    // =========================================================

    /** Admin duyệt → notify Seller qua Server */
    private void handleApprove(ManagedProduct p) {
        replaceStatus(p, "ĐÃ DUYỆT");
        AppContext.updateProduct(p.sellerName(), new AppContext.ProductRecord(
            p.id(), p.name(), p.category(),
            p.startPrice(), p.startPrice(), 0,
            "ĐÃ DUYỆT", p.auctionStart(), p.auctionEnd(), "—"
        ));

        // ── Notify Seller qua Server ──────────────────────
        ServerConnection conn = ServerConnection.getInstance();
        if (conn.isConnected()) {
            conn.sendProductApproved(p.id(), p.sellerName(), p.name());
        }
        // ─────────────────────────────────────────────────
    }

    /** Admin từ chối → notify Seller qua Server */
    private void handleReject(ManagedProduct p) {
        // Hỏi lý do từ chối
        TextInputDialog reasonDlg = new TextInputDialog("Không đạt yêu cầu");
        reasonDlg.setTitle("Lý do từ chối");
        reasonDlg.setHeaderText("Từ chối: " + p.name());
        reasonDlg.setContentText("Lý do:");
        String reason = reasonDlg.showAndWait()
            .orElse("Không đạt yêu cầu");

        replaceStatus(p, "TỪ CHỐI");
        AppContext.updateProduct(p.sellerName(), new AppContext.ProductRecord(
            p.id(), p.name(), p.category(),
            p.startPrice(), p.startPrice(), 0,
            "TỪ CHỐI", p.auctionStart(), p.auctionEnd(), "—"
        ));

        // ── Notify Seller qua Server ──────────────────────
        ServerConnection conn = ServerConnection.getInstance();
        if (conn.isConnected()) {
            conn.sendProductRejected(p.id(), p.sellerName(), p.name(), reason);
        }
        // ─────────────────────────────────────────────────
    }

    /** Seller bắt đầu phiên → notify Bidder qua Server */
    private void handleStartAuction(ManagedProduct p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Bắt đầu đấu giá");
        confirm.setHeaderText(p.name());
        confirm.setContentText(
            "Xác nhận bắt đầu phiên đấu giá?\n"
                + "Bắt đầu : " + p.auctionStart().format(DT_FMT)
                + "\nKết thúc: " + p.auctionEnd().format(DT_FMT));

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            if (p.auctionEnd().isBefore(LocalDateTime.now())) {
                showAlert("Hết hạn",
                    "Thời gian kết thúc đã qua, không thể bắt đầu.");
                return;
            }

            try {
                double step = stepMap.getOrDefault(
                    p.id(), p.startPrice() * 0.05);
                if (step <= 0) step = 500_000;

                AuctionSession session = new AuctionSession(
                    p.id(), p.name(), p.startPrice(), step, p.auctionEnd());
                session.start();

                AppContext.registerSession(session, username);
                AppContext.setActiveSession(session);

                replaceStatus(p, "ĐANG ĐẤU GIÁ");
                AppContext.updateProduct(username, new AppContext.ProductRecord(
                    p.id(), p.name(), p.category(),
                    p.startPrice(), p.startPrice(), 0,
                    "ĐANG ĐẤU GIÁ", p.auctionStart(), p.auctionEnd(), "—"
                ));

                // ── Notify Bidder qua Server ──────────────
                ServerConnection conn = ServerConnection.getInstance();
                if (conn.isConnected()) {
                    conn.sendSessionStart(
                        session.getSessionId(),
                        session.getItemName(),
                        session.getStartingPrice(),
                        session.getMinBidStep(),
                        session.getEndTime(),
                        username,
                        p.category()
                    );
                }
                // ─────────────────────────────────────────

                HelloApplication.showLiveAuctionView();

            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi", "Không thể bắt đầu phiên: " + e.getMessage());
            }
        });
    }

    private void handleGoLive(ManagedProduct p) {
        AuctionSession existing = AppContext.getGlobalSessions().stream()
            .filter(s -> s.getSessionId().equals(p.id()))
            .findFirst().orElse(null);

        if (existing != null) {
            AppContext.setActiveSession(existing);
        } else {
            if (p.auctionEnd().isBefore(LocalDateTime.now())) {
                showAlert("Hết hạn", "Phiên đã hết thời gian.");
                return;
            }
            try {
                double step = stepMap.getOrDefault(
                    p.id(), p.startPrice() * 0.05);
                if (step <= 0) step = 500_000;
                AuctionSession ns = new AuctionSession(
                    p.id(), p.name(), p.startPrice(), step, p.auctionEnd());
                ns.start();
                AppContext.registerSession(ns, username);
                AppContext.setActiveSession(ns);

                // Notify Bidder
                ServerConnection conn = ServerConnection.getInstance();
                if (conn.isConnected()) {
                    conn.sendSessionStart(ns.getSessionId(), ns.getItemName(),
                        ns.getStartingPrice(), ns.getMinBidStep(),
                        ns.getEndTime(), username, p.category());
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi", "Không thể kích hoạt phiên: " + e.getMessage());
                return;
            }
        }
        try { HelloApplication.showLiveAuctionView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void handleEdit(ManagedProduct p) {
        TextInputDialog dlg = new TextInputDialog(p.name());
        dlg.setTitle("Chỉnh sửa tên");
        dlg.setHeaderText(null);
        dlg.setContentText("Tên mới:");
        dlg.showAndWait().ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                ManagedProduct updated = new ManagedProduct(
                    p.id(), newName.trim(), p.category(),
                    p.sellerName(), p.startPrice(), p.status(),
                    p.createdAt(), p.auctionStart(), p.auctionEnd());
                int idx = managedList.indexOf(p);
                if (idx >= 0) managedList.set(idx, updated);
                refreshStats();
                renderList(managedList);
            }
        });
    }

    private void handleDelete(ManagedProduct p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa: " + p.name());
        confirm.setContentText("Bạn có chắc muốn xóa không?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                managedList.remove(p);
                AppContext.removeProduct(username, p.id());
                refreshStats();
                renderList(managedList);
            }
        });
    }

    private void handleView(ManagedProduct p) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Chi tiết sản phẩm");
        a.setHeaderText(p.name());
        String imgPath = imageMap.get(p.id());
        if (imgPath != null && new File(imgPath).exists()) {
            try {
                a.setGraphic(new ImageView(new Image(
                    new File(imgPath).toURI().toString(), 160, 160, true, true)));
            } catch (Exception ignored) {}
        }
        a.setContentText(
            "ID          : " + p.id()
                + "\nDanh mục  : " + p.category()
                + "\nGiá KĐ   : " + formatVND(p.startPrice())
                + "\nBước giá  : " + formatVND(stepMap.getOrDefault(p.id(), 0.0))
                + "\nNgười bán : " + p.sellerName()
                + "\nTrạng thái: " + p.status()
                + "\nBắt đầu   : " + p.auctionStart().format(DT_FMT)
                + "\nKết thúc  : " + p.auctionEnd().format(DT_FMT)
        );
        a.showAndWait();
    }

    private void replaceStatus(ManagedProduct p, String newStatus) {
        ManagedProduct updated = new ManagedProduct(
            p.id(), p.name(), p.category(), p.sellerName(),
            p.startPrice(), newStatus,
            p.createdAt(), p.auctionStart(), p.auctionEnd());
        int idx = managedList.indexOf(p);
        if (idx >= 0) managedList.set(idx, updated);
        refreshStats();
        applyFilters();
    }

    private String findTimeConflict(LocalDateTime newStart,
                                    LocalDateTime newEnd,
                                    String excludeId) {
        for (ManagedProduct p : managedList) {
            if (excludeId != null && excludeId.equals(p.id())) continue;
            if (!"ĐANG ĐẤU GIÁ".equals(p.status())
                && !"ĐÃ DUYỆT".equals(p.status())) continue;
            if (newStart.isBefore(p.auctionEnd())
                && newEnd.isAfter(p.auctionStart()))
                return p.name();
        }
        return null;
    }

    // =========================================================
    // FILTER & SEARCH
    // =========================================================
    @FXML private void handleAddMultiple() { /* TODO */ }
    @FXML private void handleApproveAll()  { /* TODO */ }
    @FXML private void handleStartAll()    { /* TODO */ }
    @FXML private void handleFilter() { applyFilters(); }
    @FXML private void handleSearch() { applyFilters(); }

    private void applyFilters() {
        String keyword  = searchField.getText().trim().toLowerCase();
        String status   = statusFilter.getValue();
        String category = categoryFilter.getValue();
        List<ManagedProduct> filtered = managedList.stream()
            .filter(p -> {
                if (status != null && !"Tất cả".equals(status)
                    && !status.isEmpty()
                    && !status.equals(p.status())) return false;
                if (category != null && !"Tất cả".equals(category)
                    && !category.isEmpty()
                    && !category.equals(p.category())) return false;
                if (!keyword.isEmpty()
                    && !p.name().toLowerCase().contains(keyword)
                    && !p.sellerName().toLowerCase().contains(keyword)
                    && !p.id().toLowerCase().contains(keyword))
                    return false;
                return true;
            }).collect(Collectors.toList());
        renderList(filtered);
    }

    @FXML private void handleBack() {
        try { HelloApplication.showMainView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================
    // UI HELPERS
    // =========================================================
    private TextField styledTextFieldNeo(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setMaxWidth(Double.MAX_VALUE);
        tf.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #1A1A1A; "
            + "-fx-prompt-text-fill: #999999; -fx-border-color: #1A1A1A; "
            + "-fx-border-width: 3; -fx-background-radius: 0; "
            + "-fx-padding: 10 14 10 14; -fx-font-size: 13px;");
        return tf;
    }

    private DatePicker styledDatePickerNeo(LocalDate value) {
        DatePicker dp = new DatePicker(value);
        dp.setMaxWidth(Double.MAX_VALUE);
        dp.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #1A1A1A; "
            + "-fx-border-width: 3; -fx-background-radius: 0; "
            + "-fx-font-size: 13px;");
        return dp;
    }

    private Label fieldLabelNeo(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #1A1A1A; -fx-font-size: 10px; "
            + "-fx-font-weight: 900; -fx-letter-spacing: 1.2px;");
        return l;
    }

    private VBox fieldGroupNeo(String labelText, javafx.scene.Node field) {
        return new VBox(6, fieldLabelNeo(labelText), field);
    }

    private String managedBadgeStyle(String s) {
        return switch (s) {
            case "ĐÃ DUYỆT"      -> "badge-info";
            case "ĐANG ĐẤU GIÁ" -> "badge-success";
            case "CHỜ DUYỆT"     -> "badge-warn";
            case "ĐÃ BÁN"        -> "badge-success";
            case "TỪ CHỐI"       -> "badge-danger";
            default              -> "badge-neutral";
        };
    }

    private String formatVND(double v) { return String.format("₫ %,.0f", v); }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
