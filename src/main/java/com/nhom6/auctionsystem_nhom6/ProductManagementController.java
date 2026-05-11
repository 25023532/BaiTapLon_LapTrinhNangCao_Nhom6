package com.nhom6.auctionsystem_nhom6;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.auction.AuctionSession;
import org.example.user.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    private static final DateTimeFormatter TIME_ONLY =
            DateTimeFormatter.ofPattern("HH:mm");

    private static final List<ManagedProduct> managedList = new ArrayList<>();

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
            boolean exists = managedList.stream().anyMatch(m -> m.id().equals(p.id()));
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
                managedList.stream().filter(p -> "ĐÃ DUYỆT".equals(p.status())).count()));
        pendingLabel.setText(String.valueOf(
                managedList.stream().filter(p -> "CHỜ DUYỆT".equals(p.status())).count()));
        auctioningLabel.setText(String.valueOf(
                managedList.stream().filter(p -> "ĐANG ĐẤU GIÁ".equals(p.status())).count()));
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
                    "-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-padding: 40 0 40 0;");
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
        price.setStyle(
                "-fx-font-size: 13px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");
        price.setMinWidth(140);

        Label seller = new Label(p.sellerName());
        seller.getStyleClass().add("history-item-meta");
        seller.setMinWidth(120);

        Label badge = new Label(p.status());
        badge.getStyleClass().addAll("history-badge", managedBadgeStyle(p.status()));
        badge.setMinWidth(130);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);
        actions.setMinWidth(200);

        if ("CHỜ DUYỆT".equals(p.status())) {
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

        } else if ("ĐÃ DUYỆT".equals(p.status())) {
            Button startBtn = new Button("⚡ Bắt đầu đấu giá");
            startBtn.getStyleClass().add("btn-primary");
            startBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 10 5 10;");
            startBtn.setOnAction(e -> handleStartAuction(p));

            Button editBtn = new Button("✏️ Sửa");
            editBtn.getStyleClass().add("btn-secondary");
            editBtn.setOnAction(e -> handleEdit(p));

            actions.getChildren().addAll(startBtn, editBtn);

        } else if ("ĐANG ĐẤU GIÁ".equals(p.status())) {
            Button liveBtn = new Button("🔴 Vào phiên");
            liveBtn.getStyleClass().add("btn-primary");
            liveBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 10 5 10;");
            liveBtn.setOnAction(e -> handleGoLive(p));

            Button viewBtn = new Button("👁 Xem");
            viewBtn.getStyleClass().add("btn-secondary");
            viewBtn.setOnAction(e -> handleView(p));

            actions.getChildren().addAll(liveBtn, viewBtn);

        } else {
            Button viewBtn = new Button("👁 Xem");
            viewBtn.getStyleClass().add("btn-secondary");
            viewBtn.setOnAction(e -> handleView(p));
            actions.getChildren().add(viewBtn);
        }

        row.getChildren().addAll(nameBox, cat, price, seller, badge, actions);
        return row;
    }

    // =========================================================
    // ADD PRODUCT
    // =========================================================
    @FXML
    private void handleAdd() {
        Dialog<ManagedProduct> dialog = new Dialog<>();
        dialog.setTitle("Thêm sản phẩm mới");
        dialog.setHeaderText("Nhập thông tin sản phẩm");
        dialog.getDialogPane().setPrefWidth(500);

        ButtonType saveBtn = new ButtonType("Đăng bán", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Tên sản phẩm...");
        nameField.setPrefWidth(320);

        ComboBox<String> catBox = new ComboBox<>();
        catBox.getItems().addAll(
                "Laptop","Điện thoại","Máy ảnh",
                "Điện tử","Đồng hồ","Xe cộ","Khác");
        catBox.setPromptText("Chọn danh mục");
        catBox.setPrefWidth(320);

        TextField priceField = new TextField();
        priceField.setPromptText("VD: 5000000");
        priceField.setPrefWidth(160);

        TextField stepField = new TextField("500000");
        stepField.setPromptText("Bước giá tối thiểu");
        stepField.setPrefWidth(150);

        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        startDatePicker.setPrefWidth(190);
        TextField startTimeField = new TextField(
                LocalTime.now().plusMinutes(10).format(TIME_ONLY));
        startTimeField.setPrefWidth(90);

        DatePicker endDatePicker = new DatePicker(LocalDate.now().plusDays(7));
        endDatePicker.setPrefWidth(190);
        TextField endTimeField = new TextField("23:59");
        endTimeField.setPrefWidth(90);

        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        errLabel.setWrapText(true);

        HBox startRow = new HBox(8, startDatePicker, new Label("lúc"), startTimeField);
        startRow.setAlignment(Pos.CENTER_LEFT);
        HBox endRow = new HBox(8, endDatePicker, new Label("lúc"), endTimeField);
        endRow.setAlignment(Pos.CENTER_LEFT);
        HBox priceRow = new HBox(12, priceField, new Label("Bước giá:"), stepField);
        priceRow.setAlignment(Pos.CENTER_LEFT);

        String lStyle =
                "-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#1e293b;";
        grid.add(new Label("Tên sản phẩm *") {{ setStyle(lStyle); }}, 0, 0);
        grid.add(nameField,  1, 0);
        grid.add(new Label("Danh mục *") {{ setStyle(lStyle); }},     0, 1);
        grid.add(catBox,     1, 1);
        grid.add(new Label("Giá / Bước giá *") {{ setStyle(lStyle); }}, 0, 2);
        grid.add(priceRow,   1, 2);
        grid.add(new Label("Bắt đầu *") {{ setStyle(lStyle); }},     0, 3);
        grid.add(startRow,   1, 3);
        grid.add(new Label("Kết thúc *") {{ setStyle(lStyle); }},    0, 4);
        grid.add(endRow,     1, 4);
        grid.add(errLabel,   0, 5, 2, 1);

        dialog.getDialogPane().setContent(grid);

        javafx.scene.Node saveNode = dialog.getDialogPane().lookupButton(saveBtn);
        saveNode.setDisable(true);
        nameField.textProperty().addListener((obs, o, n) ->
                saveNode.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            errLabel.setText("");

            String nameVal = nameField.getText().trim();
            if (nameVal.isEmpty()) {
                errLabel.setText("⚠ Vui lòng nhập tên sản phẩm."); return null;
            }

            double price;
            try {
                price = Double.parseDouble(
                        priceField.getText().trim().replaceAll("[^0-9.]", ""));
                if (price <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                errLabel.setText("⚠ Giá khởi điểm không hợp lệ."); return null;
            }

            double step;
            try {
                step = Double.parseDouble(
                        stepField.getText().trim().replaceAll("[^0-9.]", ""));
                if (step <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                errLabel.setText("⚠ Bước giá không hợp lệ."); return null;
            }

            LocalDateTime startDT, endDT;
            try {
                startDT = LocalDateTime.of(
                        startDatePicker.getValue(),
                        LocalTime.parse(startTimeField.getText().trim(), TIME_ONLY));
                endDT = LocalDateTime.of(
                        endDatePicker.getValue(),
                        LocalTime.parse(endTimeField.getText().trim(), TIME_ONLY));
            } catch (DateTimeParseException | NullPointerException ex) {
                errLabel.setText("⚠ Thời gian không hợp lệ (HH:mm)."); return null;
            }

            if (!endDT.isAfter(startDT)) {
                errLabel.setText("⚠ Thời gian kết thúc phải sau bắt đầu.");
                return null;
            }

            String conflict = findTimeConflict(startDT, endDT, null);
            if (conflict != null) {
                errLabel.setText("⚠ Trùng thời gian với: \"" + conflict + "\"");
                return null;
            }

            String catVal = catBox.getValue() == null ? "Khác" : catBox.getValue();
            String id = "P-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

            // Lưu step vào tag id để dùng khi bắt đầu (hack đơn giản)
            // Thực tế nên thêm field step vào ManagedProduct
            // Ở đây lưu tạm vào stepMap
            stepMap.put(id, step);

            return new ManagedProduct(
                    id, nameVal, catVal, username, price,
                    "CHỜ DUYỆT",   // mặc định chờ duyệt
                    LocalDateTime.now(), startDT, endDT
            );
        });

        dialog.showAndWait().ifPresent(p -> {
            if (p != null) {
                managedList.add(p);
                AppContext.addProduct(username, new AppContext.ProductRecord(
                        p.id(), p.name(), p.category(),
                        p.startPrice(), p.startPrice(), 0,
                        "CHỜ DUYỆT",
                        p.auctionStart(), p.auctionEnd(), "—"
                ));
                refreshStats();
                renderList(managedList);
            }
        });
    }

    // Map lưu bước giá tạm thời
    private static final java.util.Map<String, Double> stepMap = new java.util.HashMap<>();

    // =========================================================
    // HANDLERS
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

    /**
     * Seller bấm "Bắt đầu đấu giá" → tạo AuctionSession thực,
     * registerSession vào AppContext → Bidder thấy ngay.
     */
    private void handleStartAuction(ManagedProduct p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Bắt đầu đấu giá");
        confirm.setHeaderText(p.name());
        confirm.setContentText(
                "Xác nhận bắt đầu phiên đấu giá?\n"
                + "Bắt đầu: " + p.auctionStart().format(DT_FMT)
                + "\nKết thúc: " + p.auctionEnd().format(DT_FMT));
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            double step = stepMap.getOrDefault(p.id(), p.startPrice() * 0.05);
            if (step <= 0) step = 500_000; // fallback

            try {
                // ── Tạo AuctionSession thực ──────────────────
                AuctionSession session = new AuctionSession(
                        p.id(),
                        p.name(),
                        p.startPrice(),
                        step,
                        p.auctionEnd()   // endTime từ ProductRecord
                );
                session.start(); // OPEN → RUNNING

                // ── Đăng ký vào danh sách chung → Bidder thấy ──
                AppContext.registerSession(session);

                // ── Set active cho Seller ─────────────────────
                AppContext.setActiveSession(session);

                // ── Cập nhật status ───────────────────────────
                replaceStatus(p, "ĐANG ĐẤU GIÁ");
                AppContext.updateProduct(username, new AppContext.ProductRecord(
                        p.id(), p.name(), p.category(),
                        p.startPrice(), p.startPrice(), 0,
                        "ĐANG ĐẤU GIÁ", p.auctionStart(), p.auctionEnd(), "—"
                ));

                // ── Vào LiveAuction ngay ──────────────────────
                HelloApplication.showLiveAuctionView();

            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Lỗi", "Không thể bắt đầu phiên: " + e.getMessage());
            }
        });
    }

    /** Seller vào lại phiên đang chạy */
    private void handleGoLive(ManagedProduct p) {
        // Tìm session trong globalSessions
        AuctionSession existing = AppContext.getGlobalSessions().stream()
                .filter(s -> s.getSessionId().equals(p.id()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            AppContext.setActiveSession(existing);
            try { HelloApplication.showLiveAuctionView(); }
            catch (Exception e) { e.printStackTrace(); }
        } else {
            showAlert("Thông báo", "Phiên này chưa được kích hoạt.");
        }
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
        a.setContentText(
                "ID          : " + p.id()
                + "\nDanh mục  : " + p.category()
                + "\nGiá KĐ   : " + formatVND(p.startPrice())
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
