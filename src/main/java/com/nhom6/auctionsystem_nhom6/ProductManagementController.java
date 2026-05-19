package com.nhom6.auctionsystem_nhom6;

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

    private static final String IMAGE_DIR = "product_images/";

    private static final List<ManagedProduct> managedList = new ArrayList<>();
    private static final Map<String, Double>  stepMap     = new HashMap<>();
    private static final Map<String, String>  imageMap    = new HashMap<>();

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

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        User user = AppContext.getCurrentUser();
        username  = user.getUsername();
        isAdmin   = "ADMIN".equalsIgnoreCase(user.getRole());

        new File(IMAGE_DIR).mkdirs();

        statusFilter.getItems().addAll(
                "Tất cả", "CHỜ DUYỆT", "ĐÃ DUYỆT",
                "ĐANG ĐẤU GIÁ", "ĐÃ BÁN", "TỪ CHỐI");
        categoryFilter.getItems().addAll(
                "Tất cả", "Laptop", "Điện thoại", "Máy ảnh",
                "Điện tử", "Đồng hồ", "Xe cộ", "Khác");

        if (isAdmin)
            syncAllSellersFromAppContext();
        else
            syncFromAppContext(user);

        refreshStats();
        renderList(managedList);
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
    @FXML
    private void handleAdd() {
        Dialog<ManagedProduct> dialog = new Dialog<>();
        dialog.setTitle("Thêm sản phẩm mới");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setPrefWidth(620);
        dialog.getDialogPane().setPrefHeight(620);

        ButtonType saveBtn =
                new ButtonType("✚  Đăng bán", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(saveBtn, ButtonType.CANCEL);

        dialog.getDialogPane().setStyle(
                "-fx-background-color: #0f172a; "
                + "-fx-border-color: #334155; -fx-border-width: 1;");

        // ── Upload ảnh ───────────────────────────────────────
        final String[] selectedImagePath = {null};

        VBox leftPanel = new VBox(16);
        leftPanel.setPrefWidth(200);
        leftPanel.setMinWidth(200);
        leftPanel.setPadding(new Insets(24, 20, 24, 24));
        leftPanel.setAlignment(Pos.TOP_CENTER);
        leftPanel.setStyle("-fx-background-color: #1e293b; "
                + "-fx-border-color: transparent #334155 transparent transparent; "
                + "-fx-border-width: 1;");

        Label imgTitle = new Label("Ảnh sản phẩm");
        imgTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; "
                + "-fx-font-weight: bold;");

        ImageView preview = new ImageView();
        preview.setFitWidth(148);
        preview.setFitHeight(148);
        preview.setPreserveRatio(true);

        StackPane previewBox = new StackPane();
        previewBox.setPrefSize(160, 160);
        previewBox.setMaxSize(160, 160);
        previewBox.setStyle("-fx-background-color: #0f172a; "
                + "-fx-background-radius: 12; -fx-border-color: #334155; "
                + "-fx-border-radius: 12; -fx-border-width: 2; -fx-cursor: hand;");

        VBox emptyHint = new VBox(8);
        emptyHint.setAlignment(Pos.CENTER);
        Label camIcon = new Label("📷");
        camIcon.setStyle("-fx-font-size: 36px;");
        Label hintTxt = new Label("Chưa có ảnh\nClick để chọn");
        hintTxt.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; "
                + "-fx-text-alignment: CENTER;");
        hintTxt.setWrapText(true);
        emptyHint.getChildren().addAll(camIcon, hintTxt);
        previewBox.getChildren().addAll(emptyHint, preview);

        Label fileNameLabel = new Label("Chưa chọn file");
        fileNameLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px;");
        fileNameLabel.setWrapText(true);
        fileNameLabel.setMaxWidth(160);

        Button chooseImgBtn = new Button("📂  Chọn ảnh");
        chooseImgBtn.setMaxWidth(Double.MAX_VALUE);
        chooseImgBtn.setStyle("-fx-background-color: #1e3a5f; "
                + "-fx-text-fill: #38bdf8; -fx-font-size: 12px; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; "
                + "-fx-border-color: #2563eb; -fx-border-radius: 8; "
                + "-fx-border-width: 1; -fx-cursor: hand; "
                + "-fx-padding: 8 0 8 0;");

        Label fmtHint = new Label("PNG · JPG · WEBP · GIF");
        fmtHint.setStyle("-fx-text-fill: #334155; -fx-font-size: 10px;");

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
                            file.toURI().toString(), 148, 148, true, true));
                    previewBox.getChildren().remove(emptyHint);
                    previewBox.setStyle("-fx-background-color: #0f172a; "
                            + "-fx-background-radius: 12; "
                            + "-fx-border-color: #2563eb; "
                            + "-fx-border-radius: 12; -fx-border-width: 2; "
                            + "-fx-cursor: hand;");
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
        rightPanel.setStyle("-fx-background-color: #0f172a;");
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        Label formTitle = new Label("Thông tin sản phẩm");
        formTitle.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 15px; "
                + "-fx-font-weight: bold;");
        VBox.setMargin(formTitle, new Insets(0, 0, 16, 0));

        VBox formFields = new VBox(12);

        TextField nameField = styledTextField("Tên sản phẩm...");
        formFields.getChildren().add(fieldGroup("Tên sản phẩm *", nameField));

        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll("Laptop","Điện thoại","Máy ảnh",
                "Điện tử","Đồng hồ","Xe cộ","Khác");
        catBox.setPromptText("Chọn danh mục");
        catBox.setMaxWidth(Double.MAX_VALUE);
        catBox.setStyle("-fx-background-color: #1e293b; "
                + "-fx-border-color: #334155; -fx-border-radius: 8; "
                + "-fx-background-radius: 8; -fx-font-size: 13px; "
                + "-fx-cursor: hand;");
        // Fix chữ trắng khi chọn item
        catBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                if (!empty && item != null)
                    setStyle("-fx-text-fill: #e2e8f0; -fx-background-color: #1e293b; "
                            + "-fx-font-size: 13px; -fx-padding: 6 12 6 12;");
            }
        });
        catBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Chọn danh mục");
                    setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; "
                            + "-fx-background-color: transparent;");
                }
            }
        });
        formFields.getChildren().add(fieldGroup("Danh mục *", catBox));

        TextField priceField = styledTextField("VD: 5000000");
        TextField stepField  = styledTextField("VD: 500000");
        stepField.setText("500000");
        HBox priceRow = new HBox(12, priceField, stepField);
        HBox.setHgrow(priceField, Priority.ALWAYS);
        HBox.setHgrow(stepField,  Priority.ALWAYS);
        HBox priceLabels = new HBox(12,
                fieldLabel("Giá khởi điểm *"),
                fieldLabel("Bước giá tối thiểu *"));
        HBox.setHgrow(priceLabels.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(priceLabels.getChildren().get(1), Priority.ALWAYS);
        formFields.getChildren().add(new VBox(6, priceLabels, priceRow));

        DatePicker startDate = styledDatePicker(LocalDate.now());
        TextField  startTime = styledTextField("");
        startTime.setText(LocalTime.now().plusMinutes(10).format(TIME_ONLY));
        startTime.setPrefWidth(80);
        startTime.setMaxWidth(80);
        Label startLbl = new Label("lúc");
        startLbl.setStyle("-fx-text-fill: #64748b; -fx-padding: 8 0 0 0;");
        HBox startRow = new HBox(10, startDate, startLbl, startTime);
        HBox.setHgrow(startDate, Priority.ALWAYS);
        formFields.getChildren().add(fieldGroup("Bắt đầu *", startRow));

        DatePicker endDate = styledDatePicker(LocalDate.now().plusDays(7));
        TextField  endTime = styledTextField("");
        endTime.setText("23:59");
        endTime.setPrefWidth(80);
        endTime.setMaxWidth(80);
        Label endLbl = new Label("lúc");
        endLbl.setStyle("-fx-text-fill: #64748b; -fx-padding: 8 0 0 0;");
        HBox endRow = new HBox(10, endDate, endLbl, endTime);
        HBox.setHgrow(endDate, Priority.ALWAYS);
        formFields.getChildren().add(fieldGroup("Kết thúc *", endRow));

        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; "
                + "-fx-padding: 4 0 0 0;");
        errLabel.setWrapText(true);

        rightPanel.getChildren().addAll(formTitle, formFields, errLabel);
        VBox.setVgrow(formFields, Priority.ALWAYS);

        HBox root = new HBox(0);
        root.setPrefHeight(520);
        root.getChildren().addAll(leftPanel, rightPanel);
        dialog.getDialogPane().setContent(root);

        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node saveNode =
                    dialog.getDialogPane().lookupButton(saveBtn);
            javafx.scene.Node cancelNode =
                    dialog.getDialogPane().lookupButton(ButtonType.CANCEL);

            saveNode.setStyle("-fx-background-color: #2563eb; "
                    + "-fx-text-fill: white; -fx-font-weight: bold; "
                    + "-fx-font-size: 13px; -fx-background-radius: 8; "
                    + "-fx-padding: 8 20 8 20; -fx-cursor: hand;");
            cancelNode.setStyle("-fx-background-color: #1e293b; "
                    + "-fx-text-fill: #94a3b8; -fx-font-size: 13px; "
                    + "-fx-background-radius: 8; -fx-border-color: #334155; "
                    + "-fx-border-radius: 8; -fx-border-width: 1; "
                    + "-fx-padding: 8 20 8 20; -fx-cursor: hand;");

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
                    String conflict = findTimeConflict(sDT, eDT, null);
                    if (conflict != null) {
                        errLabel.setText("⚠ Trùng thời gian với: \"" + conflict + "\"");
                        event.consume();
                    }
                } catch (DateTimeParseException ex) {
                    errLabel.setText("⚠ Thời gian không hợp lệ (HH:mm).");
                    event.consume();
                }
            });
        });

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
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

            // Seller → CHỜ DUYỆT | Admin → ĐÃ DUYỆT
            String status = isAdmin ? "ĐÃ DUYỆT" : "CHỜ DUYỆT";
            return new ManagedProduct(id, nameVal, catVal, username,
                    price, status, LocalDateTime.now(), startDT, endDT);
        });

        dialog.showAndWait().ifPresent(p -> {
            if (p == null) return;
            managedList.add(p);
            AppContext.addProduct(username, new AppContext.ProductRecord(
                    p.id(), p.name(), p.category(),
                    p.startPrice(), p.startPrice(), 0,
                    p.status(), p.auctionStart(), p.auctionEnd(), "—"
            ));

            // ── Seller notify Admin qua Server ───────────────
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
            // ─────────────────────────────────────────────────

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
    private TextField styledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setMaxWidth(Double.MAX_VALUE);
        tf.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f1f5f9; "
                + "-fx-prompt-text-fill: #475569; -fx-border-color: #334155; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; "
                + "-fx-padding: 8 12 8 12; -fx-font-size: 13px;");
        return tf;
    }

    private DatePicker styledDatePicker(LocalDate value) {
        DatePicker dp = new DatePicker(value);
        dp.setMaxWidth(Double.MAX_VALUE);
        dp.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; "
                + "-fx-font-size: 13px;");
        return dp;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; "
                + "-fx-font-weight: bold;");
        return l;
    }

    private VBox fieldGroup(String labelText, javafx.scene.Node field) {
        return new VBox(6, fieldLabel(labelText), field);
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
