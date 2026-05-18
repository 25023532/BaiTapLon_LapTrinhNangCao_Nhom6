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

    private String username;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter TIME_ONLY =
            DateTimeFormatter.ofPattern("HH:mm");

    // Thư mục lưu ảnh sản phẩm (trong resources hoặc thư mục chạy)
    private static final String IMAGE_DIR = "product_images/";

    private static final List<ManagedProduct> managedList = new ArrayList<>();
    private static final Map<String, Double>  stepMap     = new HashMap<>();
    // Map productId → đường dẫn ảnh
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

        // Tạo thư mục lưu ảnh nếu chưa có
        new File(IMAGE_DIR).mkdirs();

        statusFilter.getItems().addAll(
                "Tất cả", "CHỜ DUYỆT", "ĐÃ DUYỆT",
                "ĐANG ĐẤU GIÁ", "ĐÃ BÁN", "TỪ CHỐI");
        categoryFilter.getItems().addAll(
                "Tất cả", "Laptop", "Điện thoại", "Máy ảnh",
                "Điện tử", "Đồng hồ", "Xe cộ", "Khác");

        syncFromAppContext(user);
        refreshStats();
        renderList(managedList);
    }

    private void syncFromAppContext(User user) {
        for (AppContext.ProductRecord p : AppContext.getProducts(user.getUsername())) {
            boolean exists = managedList.stream()
                    .anyMatch(m -> m.id().equals(p.id()));
            if (!exists) {
                String mStatus = switch (p.status()) {
                    case "ĐANG ĐẤU GIÁ" -> "ĐANG ĐẤU GIÁ";
                    case "ĐÃ BÁN"        -> "ĐÃ BÁN";
                    case "CHỜ DUYỆT"     -> "CHỜ DUYỆT";
                    default              -> p.status();
                };
                managedList.add(new ManagedProduct(
                        p.id(), p.name(), p.category(),
                        user.getUsername(), p.startPrice(),
                        mStatus, LocalDateTime.now(),
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
        approvedLabel.setText(String.valueOf(
                managedList.stream()
                        .filter(p -> "ĐÃ DUYỆT".equals(p.status())).count()));
        pendingLabel.setText(String.valueOf(
                managedList.stream()
                        .filter(p -> "CHỜ DUYỆT".equals(p.status())).count()));
        auctioningLabel.setText(String.valueOf(
                managedList.stream()
                        .filter(p -> "ĐANG ĐẤU GIÁ".equals(p.status())).count()));
    }

    // =========================================================
    // RENDER
    // =========================================================
    private void renderList(List<ManagedProduct> list) {
        productListBox.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label(
                    "Chưa có sản phẩm nào. Nhấn \"＋ Thêm sản phẩm\" để bắt đầu.");
            empty.setStyle(
                    "-fx-text-fill: #64748b; -fx-font-size: 14px; "
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

        // ── Ảnh thumbnail ────────────────────────────────────
        ImageView thumb = new ImageView();
        thumb.setFitWidth(48);
        thumb.setFitHeight(48);
        thumb.setPreserveRatio(true);
        thumb.setStyle("-fx-background-radius: 6;");

        String imgPath = imageMap.get(p.id());
        if (imgPath != null && new File(imgPath).exists()) {
            try {
                thumb.setImage(new Image(
                        new File(imgPath).toURI().toString()));
            } catch (Exception ignored) {}
        } else {
            // Placeholder icon nếu chưa có ảnh
            Label placeholder = new Label("📦");
            placeholder.setStyle("-fx-font-size: 28px; -fx-min-width: 48px; "
                    + "-fx-alignment: CENTER;");
            row.getChildren().add(placeholder);
        }
        if (imgPath != null) {
            StackPane thumbBox = new StackPane(thumb);
            thumbBox.setMinWidth(56);
            thumbBox.setStyle("-fx-background-color: #0f172a; "
                    + "-fx-background-radius: 8; -fx-padding: 4;");
            HBox.setMargin(thumbBox, new Insets(0, 12, 0, 0));
            row.getChildren().add(thumbBox);
        }

        // ── Tên + meta ────────────────────────────────────────
        VBox nameBox = new VBox(3);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label name = new Label(p.name());
        name.getStyleClass().add("history-item-name");
        Label idLabel = new Label(
                "ID: " + p.id()
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

        Label seller = new Label(p.sellerName());
        seller.getStyleClass().add("history-item-meta");
        seller.setMinWidth(120);

        Label badge = new Label(p.status());
        badge.getStyleClass().addAll(
                "history-badge", managedBadgeStyle(p.status()));
        badge.setMinWidth(130);

        HBox actions = buildActions(p);
        actions.setMinWidth(220);

        row.getChildren().addAll(nameBox, cat, price, seller, badge, actions);
        return row;
    }

    private HBox buildActions(ManagedProduct p) {
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);

        switch (p.status()) {
            case "CHỜ DUYỆT" -> {
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
            }
            case "ĐÃ DUYỆT" -> {
                Button startBtn = new Button("⚡ Bắt đầu đấu giá");
                startBtn.getStyleClass().add("btn-primary");
                startBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 10 5 10;");
                startBtn.setOnAction(e -> handleStartAuction(p));

                Button editBtn = new Button("✏️ Sửa");
                editBtn.getStyleClass().add("btn-secondary");
                editBtn.setOnAction(e -> handleEdit(p));

                actions.getChildren().addAll(startBtn, editBtn);
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
    // ADD PRODUCT — có phần upload ảnh
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

        dialog.getDialogPane().setStyle("""
                -fx-background-color: #0f172a;
                -fx-border-color: #334155;
                -fx-border-width: 1;
                """);

        // ── CỘT TRÁI: Upload ảnh ─────────────────────────────
        final String[] selectedImagePath = {null};

        VBox leftPanel = new VBox(16);
        leftPanel.setPrefWidth(200);
        leftPanel.setMinWidth(200);
        leftPanel.setPadding(new Insets(24, 20, 24, 24));
        leftPanel.setAlignment(Pos.TOP_CENTER);
        leftPanel.setStyle("""
                -fx-background-color: #1e293b;
                -fx-border-color: transparent #334155 transparent transparent;
                -fx-border-width: 1;
                """);

        Label imgTitle = new Label("Ảnh sản phẩm");
        imgTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");

        ImageView preview = new ImageView();
        preview.setFitWidth(148);
        preview.setFitHeight(148);
        preview.setPreserveRatio(true);

        StackPane previewBox = new StackPane();
        previewBox.setPrefSize(160, 160);
        previewBox.setMaxSize(160, 160);
       catBox.setStyle("""
        -fx-background-color: #1e293b;
        -fx-border-color: #334155;
        -fx-border-radius: 8;
        -fx-background-radius: 8;
        -fx-font-size: 13px;
        -fx-cursor: hand;
        -fx-text-fill: #f1f5f9;
        """);

        VBox emptyHint = new VBox(8);
        emptyHint.setAlignment(Pos.CENTER);
        Label camIcon = new Label("📷");
        camIcon.setStyle("-fx-font-size: 36px;");
        Label hintTxt = new Label("Chưa có ảnh\nClick để chọn");
        hintTxt.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-text-alignment: CENTER;");
        hintTxt.setWrapText(true);
        emptyHint.getChildren().addAll(camIcon, hintTxt);
        previewBox.getChildren().addAll(emptyHint, preview);

        Label fileNameLabel = new Label("Chưa chọn file");
        fileNameLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 10px;");
        fileNameLabel.setWrapText(true);
        fileNameLabel.setMaxWidth(160);

        Button chooseImgBtn = new Button("📂  Chọn ảnh");
        chooseImgBtn.setMaxWidth(Double.MAX_VALUE);
        chooseImgBtn.setStyle("""
                -fx-background-color: #1e3a5f;
                -fx-text-fill: #38bdf8;
                -fx-font-size: 12px;
                -fx-font-weight: bold;
                -fx-background-radius: 8;
                -fx-border-color: #2563eb;
                -fx-border-radius: 8;
                -fx-border-width: 1;
                -fx-cursor: hand;
                -fx-padding: 8 0 8 0;
                """);

        Label fmtHint = new Label("PNG · JPG · WEBP · GIF");
        fmtHint.setStyle("-fx-text-fill: #334155; -fx-font-size: 10px;");

        Runnable openFileChooser = () -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Chọn ảnh sản phẩm");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
            File file = fc.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (file != null) {
                selectedImagePath[0] = file.getAbsolutePath();
                fileNameLabel.setText(file.getName());
                try {
                    Image img = new Image(file.toURI().toString(), 148, 148, true, true);
                    preview.setImage(img);
                    previewBox.getChildren().remove(emptyHint);
                    previewBox.setStyle("""
                            -fx-background-color: #0f172a;
                            -fx-background-radius: 12;
                            -fx-border-color: #2563eb;
                            -fx-border-radius: 12;
                            -fx-border-width: 2;
                            -fx-cursor: hand;
                            """);
                } catch (Exception ex) {
                    fileNameLabel.setText("⚠ Không đọc được ảnh");
                }
            }
        };

        chooseImgBtn.setOnAction(e -> openFileChooser.run());
        previewBox.setOnMouseClicked(e -> openFileChooser.run());

        leftPanel.getChildren().addAll(imgTitle, previewBox, chooseImgBtn, fileNameLabel, fmtHint);

        // ── CỘT PHẢI: Form ────────────────────────────────────
        VBox rightPanel = new VBox(0);
        rightPanel.setPadding(new Insets(24, 24, 24, 24));
        rightPanel.setStyle("-fx-background-color: #0f172a;");
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        Label formTitle = new Label("Thông tin sản phẩm");
        formTitle.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 15px; -fx-font-weight: bold;");
        VBox.setMargin(formTitle, new Insets(0, 0, 16, 0));

        VBox formFields = new VBox(12);

        // Tên sản phẩm
        TextField nameField = styledTextField("Tên sản phẩm...");
        formFields.getChildren().add(fieldGroup("Tên sản phẩm *", nameField));

        // Danh mục
        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll("Laptop","Điện thoại","Máy ảnh","Điện tử","Đồng hồ","Xe cộ","Khác");
        catBox.setPromptText("Chọn danh mục");
        catBox.setMaxWidth(Double.MAX_VALUE);
        catBox.setStyle("""
                -fx-background-color: #1e293b;
                -fx-border-color: #334155;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-font-size: 13px;
                -fx-cursor: hand;
                """);
        formFields.getChildren().add(fieldGroup("Danh mục *", catBox));

        // Giá + bước giá
        TextField priceField = styledTextField("VD: 5,000,000");
        TextField stepField  = styledTextField("VD: 500,000");
        stepField.setText("500000");
        HBox priceRow = new HBox(12, priceField, stepField);
        HBox.setHgrow(priceField, Priority.ALWAYS);
        HBox.setHgrow(stepField,  Priority.ALWAYS);
        HBox priceLabels = new HBox(12);
        Label lGia  = fieldLabel("Giá khởi điểm *");
        Label lStep = fieldLabel("Bước giá tối thiểu *");
        HBox.setHgrow(lGia,  Priority.ALWAYS);
        HBox.setHgrow(lStep, Priority.ALWAYS);
        priceLabels.getChildren().addAll(lGia, lStep);
        VBox priceGroup = new VBox(6, priceLabels, priceRow);
        formFields.getChildren().add(priceGroup);

        // Thời gian bắt đầu
        DatePicker startDate = styledDatePicker(LocalDate.now());
        TextField startTime = styledTextField("");
        startTime.setText(LocalTime.now().plusMinutes(10).format(TIME_ONLY));
        startTime.setPrefWidth(80);
        startTime.setMaxWidth(80);
        Label startLbl = new Label("lúc");
        startLbl.setStyle("-fx-text-fill: #64748b; -fx-padding: 8 0 0 0;");
        HBox startRow = new HBox(10, startDate, startLbl, startTime);
        HBox.setHgrow(startDate, Priority.ALWAYS);
        formFields.getChildren().add(fieldGroup("Bắt đầu *", startRow));

        // Thời gian kết thúc
        DatePicker endDate = styledDatePicker(LocalDate.now().plusDays(7));
        TextField endTime = styledTextField("");
        endTime.setText("23:59");
        endTime.setPrefWidth(80);
        endTime.setMaxWidth(80);
        Label endLbl = new Label("lúc");
        endLbl.setStyle("-fx-text-fill: #64748b; -fx-padding: 8 0 0 0;");
        HBox endRow = new HBox(10, endDate, endLbl, endTime);
        HBox.setHgrow(endDate, Priority.ALWAYS);
        formFields.getChildren().add(fieldGroup("Kết thúc *", endRow));

        // Error label
        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-padding: 4 0 0 0;");
        errLabel.setWrapText(true);

        rightPanel.getChildren().addAll(formTitle, formFields, errLabel);
        VBox.setVgrow(formFields, Priority.ALWAYS);

        // Root layout
        HBox root = new HBox(0);
        root.setPrefHeight(520);
        root.getChildren().addAll(leftPanel, rightPanel);
        dialog.getDialogPane().setContent(root);

        // ✅ Chỉ 1 Platform.runLater duy nhất — sau khi đã có đủ biến
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node saveNode   = dialog.getDialogPane().lookupButton(saveBtn);
            javafx.scene.Node cancelNode = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);

            // Style buttons
            saveNode.setStyle("""
                    -fx-background-color: #2563eb;
                    -fx-text-fill: white;
                    -fx-font-weight: bold;
                    -fx-font-size: 13px;
                    -fx-background-radius: 8;
                    -fx-padding: 8 20 8 20;
                    -fx-cursor: hand;
                    """);
            cancelNode.setStyle("""
                    -fx-background-color: #1e293b;
                    -fx-text-fill: #94a3b8;
                    -fx-font-size: 13px;
                    -fx-background-radius: 8;
                    -fx-border-color: #334155;
                    -fx-border-radius: 8;
                    -fx-border-width: 1;
                    -fx-padding: 8 20 8 20;
                    -fx-cursor: hand;
                    """);

            // Disable khi tên trống
            saveNode.setDisable(nameField.getText().trim().isEmpty());
            nameField.textProperty().addListener((obs, o, n) ->
                    saveNode.setDisable(n.trim().isEmpty()));

            // ✅ Chặn đóng dialog khi validation fail
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
                    double p = Double.parseDouble(
                            priceField.getText().trim().replaceAll("[^0-9.]", ""));
                    if (p <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    errLabel.setText("⚠ Giá khởi điểm không hợp lệ.");
                    event.consume(); return;
                }
                try {
                    double s = Double.parseDouble(
                            stepField.getText().trim().replaceAll("[^0-9.]", ""));
                    if (s <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    errLabel.setText("⚠ Bước giá không hợp lệ.");
                    event.consume(); return;
                }
                if (startDate.getValue() == null || endDate.getValue() == null) {
                    errLabel.setText("⚠ Vui lòng chọn ngày.");
                    event.consume(); return;
                }
                try {
                    LocalDateTime startDT = LocalDateTime.of(startDate.getValue(),
                            LocalTime.parse(startTime.getText().trim(), TIME_ONLY));
                    LocalDateTime endDT   = LocalDateTime.of(endDate.getValue(),
                            LocalTime.parse(endTime.getText().trim(), TIME_ONLY));
                    if (!endDT.isAfter(startDT)) {
                        errLabel.setText("⚠ Thời gian kết thúc phải sau bắt đầu.");
                        event.consume(); return;
                    }
                    String conflict = findTimeConflict(startDT, endDT, null);
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

        // Result converter
        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;

            String nameVal = nameField.getText().trim();
            double price   = Double.parseDouble(
                    priceField.getText().trim().replaceAll("[^0-9.]", ""));
            double step    = Double.parseDouble(
                    stepField.getText().trim().replaceAll("[^0-9.]", ""));
            LocalDateTime startDT = LocalDateTime.of(startDate.getValue(),
                    LocalTime.parse(startTime.getText().trim(), TIME_ONLY));
            LocalDateTime endDT   = LocalDateTime.of(endDate.getValue(),
                    LocalTime.parse(endTime.getText().trim(), TIME_ONLY));
            String catVal = catBox.getValue() == null ? "Khác" : catBox.getValue();
            String id     = "P-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

            stepMap.put(id, step);

            // Copy ảnh
            if (selectedImagePath[0] != null) {
                try {
                    File src  = new File(selectedImagePath[0]);
                    String ext = src.getName().substring(src.getName().lastIndexOf('.'));
                    Path dest  = Paths.get(IMAGE_DIR + id + ext);
                    Files.copy(src.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                    imageMap.put(id, dest.toAbsolutePath().toString());
                } catch (IOException ex) {
                    System.err.println("Lưu ảnh thất bại: " + ex.getMessage());
                }
            }

            return new ManagedProduct(id, nameVal, catVal, username, price,
                    "CHỜ DUYỆT", LocalDateTime.now(), startDT, endDT);
        });

        dialog.showAndWait().ifPresent(p -> {
            if (p != null) {
                managedList.add(p);
                AppContext.addProduct(username, new AppContext.ProductRecord(
                        p.id(), p.name(), p.category(),
                        p.startPrice(), p.startPrice(), 0,
                        "CHỜ DUYỆT", p.auctionStart(), p.auctionEnd(), "—"
                ));
                refreshStats();
                renderList(managedList);
            }
        });
    }

    // ── UI Helper methods ─────────────────────────────────────
    private TextField styledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setMaxWidth(Double.MAX_VALUE);
        tf.setStyle("""
                -fx-background-color: #1e293b;
                -fx-text-fill: #f1f5f9;
                -fx-prompt-text-fill: #475569;
                -fx-border-color: #334155;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-padding: 8 12 8 12;
                -fx-font-size: 13px;
                """);
        return tf;
    }

    private DatePicker styledDatePicker(LocalDate value) {
        DatePicker dp = new DatePicker(value);
        dp.setMaxWidth(Double.MAX_VALUE);
        dp.setStyle("""
                -fx-background-color: #1e293b;
                -fx-border-color: #334155;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                -fx-font-size: 13px;
                """);
        return dp;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; "
                + "-fx-font-weight: bold;");
        return l;
    }

    private VBox fieldGroup(String labelText, javafx.scene.Node field) {
        VBox group = new VBox(6);
        group.getChildren().addAll(fieldLabel(labelText), field);
        return group;
    }

    // =========================================================
    // HANDLERS (giữ nguyên từ bản cũ)
    // =========================================================
    private void handleApprove(ManagedProduct p) {
        replaceStatus(p, "ĐÃ DUYỆT");
        AppContext.updateProduct(username, new AppContext.ProductRecord(
                p.id(), p.name(), p.category(),
                p.startPrice(), p.startPrice(), 0,
                "ĐÃ DUYỆT", p.auctionStart(), p.auctionEnd(), "—"
        ));
    }

    private void handleReject(ManagedProduct p) {
        replaceStatus(p, "TỪ CHỐI");
        AppContext.updateProduct(username, new AppContext.ProductRecord(
                p.id(), p.name(), p.category(),
                p.startPrice(), p.startPrice(), 0,
                "TỪ CHỐI", p.auctionStart(), p.auctionEnd(), "—"
        ));
    }

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
                        "Thời gian kết thúc đã qua, không thể bắt đầu phiên.");
                return;
            }

            try {
                double step = stepMap.getOrDefault(
                        p.id(), p.startPrice() * 0.05);
                if (step <= 0) step = 500_000;

                AuctionSession session = new AuctionSession(
                        p.id(), p.name(),
                        p.startPrice(), step,
                        p.auctionEnd()
                );
                session.start();

                AppContext.registerSession(session, username);
                AppContext.setActiveSession(session);

                replaceStatus(p, "ĐANG ĐẤU GIÁ");
                AppContext.updateProduct(username, new AppContext.ProductRecord(
                        p.id(), p.name(), p.category(),
                        p.startPrice(), p.startPrice(), 0,
                        "ĐANG ĐẤU GIÁ",
                        p.auctionStart(), p.auctionEnd(), "—"
                ));

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
                .findFirst()
                .orElse(null);

        if (existing != null) {
            AppContext.setActiveSession(existing);
        } else {
            if (p.auctionEnd().isBefore(LocalDateTime.now())) {
                showAlert("Hết hạn",
                        "Phiên đấu giá đã hết thời gian, không thể kích hoạt.");
                return;
            }
            try {
                double step = stepMap.getOrDefault(
                        p.id(), p.startPrice() * 0.05);
                if (step <= 0) step = 500_000;

                AuctionSession newSession = new AuctionSession(
                        p.id(), p.name(),
                        p.startPrice(), step,
                        p.auctionEnd()
                );
                newSession.start();

                AppContext.registerSession(newSession, username);
                AppContext.setActiveSession(newSession);

            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi",
                        "Không thể kích hoạt phiên: " + e.getMessage());
                return;
            }
        }

        try { HelloApplication.showLiveAuctionView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void handleEdit(ManagedProduct p) {
        TextInputDialog dlg = new TextInputDialog(p.name());
        dlg.setTitle("Chỉnh sửa tên sản phẩm");
        dlg.setHeaderText(null);
        dlg.setContentText("Tên mới:");
        dlg.showAndWait().ifPresent(newName -> {
            if (!newName.trim().isEmpty()) {
                ManagedProduct updated = new ManagedProduct(
                        p.id(), newName.trim(), p.category(),
                        p.sellerName(), p.startPrice(), p.status(),
                        p.createdAt(), p.auctionStart(), p.auctionEnd()
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

        // Hiện ảnh trong dialog xem chi tiết nếu có
        String imgPath = imageMap.get(p.id());
        if (imgPath != null && new File(imgPath).exists()) {
            try {
                ImageView iv = new ImageView(
                        new Image(new File(imgPath).toURI().toString(),
                                180, 180, true, true));
                a.setGraphic(iv);
            } catch (Exception ignored) {}
        }

        a.setContentText(
                "ID          : " + p.id()
                        + "\nDanh mục  : " + p.category()
                        + "\nGiá KĐ   : " + formatVND(p.startPrice())
                        + "\nBước giá  : " + formatVND(
                        stepMap.getOrDefault(p.id(), 0.0))
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
                p.createdAt(), p.auctionStart(), p.auctionEnd()
        );
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
            boolean overlap = newStart.isBefore(p.auctionEnd())
                    && newEnd.isAfter(p.auctionStart());
            if (overlap) return p.name();
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
