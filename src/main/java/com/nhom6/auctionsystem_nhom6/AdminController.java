package com.nhom6.auctionsystem_nhom6;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.example.auction.AuctionSession;
import org.example.auction.AuctionStatus;
import org.example.service.AuthService;
import org.example.user.User;

import java.util.ArrayList;
import java.util.List;

public class AdminController {

    // ── Header / Stats ────────────────────────────────────────
    @FXML private Label      totalUsersLabel;
    @FXML private Label      totalProductsLabel;
    @FXML private Label      pendingLabel;
    @FXML private Label      activeSessionsLabel;

    // ── Profile dropdown ──────────────────────────────────────
    @FXML private MenuButton adminMenuBtn;
    @FXML private Label      adminNameLabel;
    @FXML private Label      adminAvatarLabel;

    // ── Content ───────────────────────────────────────────────
    @FXML private VBox contentBox;

    private final AuthService authService = AppContext.getAuthService();
    private String currentTab = "users";

    // Auto-refresh mỗi 3 giây để bắt sản phẩm mới từ bất kỳ seller nào
    private Timeline autoRefreshTimeline;

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        User me = AppContext.getCurrentUser();

        String name = me.getUsername();
        adminNameLabel.setText(name);
        String avatar = name.length() >= 2
                ? name.substring(0, 2).toUpperCase()
                : name.toUpperCase();
        adminAvatarLabel.setText(avatar);

        refreshStats();
        showUsers();

        // Auto-refresh: cập nhật stats + tab sản phẩm liên tục
        // Đảm bảo bất kỳ seller nào đăng sản phẩm đều hiện lên ngay
        autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(3), e -> {
                    refreshStats();
                    if ("products".equals(currentTab)) {
                        showProducts();
                    }
                })
        );
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    // =========================================================
    // PROFILE HANDLERS
    // =========================================================
    @FXML
    private void handleProfile() {
        try { HelloApplication.showProfileView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout() {
        if (autoRefreshTimeline != null) autoRefreshTimeline.stop();
        AppContext.logout();
        try { HelloApplication.showLoginView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================
    // THỐNG KÊ — đọc trực tiếp từ getAllProducts()
    // =========================================================
    private void refreshStats() {
        List<User> users = new ArrayList<>(authService.getAllUsers().values());
        totalUsersLabel.setText(String.valueOf(users.size()));

        // getAllProducts() gộp sản phẩm của TẤT CẢ seller
        List<AppContext.ProductRecord> allProducts = AppContext.getAllProducts();
        totalProductsLabel.setText(String.valueOf(allProducts.size()));

        // Đếm CHỜ DUYỆT từ mọi seller
        long pending = allProducts.stream()
                .filter(p -> "CHỜ DUYỆT".equals(p.status()))
                .count();
        pendingLabel.setText(String.valueOf(pending));

        long activeSessions = AppContext.getGlobalSessions().stream()
                .filter(s -> s.getStatus() == AuctionStatus.RUNNING)
                .count();
        activeSessionsLabel.setText(String.valueOf(activeSessions));
    }

    // =========================================================
    // TAB: NGƯỜI DÙNG
    // =========================================================
    @FXML
    public void showUsers() {
        currentTab = "users";
        contentBox.getChildren().clear();

        HBox header = buildTableHeader(
                "Tên đăng nhập", "Họ tên", "Email", "Vai trò", "Hành động");
        contentBox.getChildren().add(header);

        List<User> users = new ArrayList<>(authService.getAllUsers().values());
        if (users.isEmpty()) {
            contentBox.getChildren().add(emptyLabel("Chưa có người dùng nào."));
            return;
        }

        for (User u : users) {
            HBox row = new HBox(0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("history-row");
            row.setPadding(new Insets(10, 20, 10, 20));

            Label username = colLabel(u.getUsername(), 160, true);
            Label fullName = colLabel(
                    u.getFullName() == null || u.getFullName().isEmpty()
                            ? "—" : u.getFullName(), 180, false);
            Label email    = colLabel(
                    u.getEmail() == null || u.getEmail().isEmpty()
                            ? "—" : u.getEmail(), 200, false);

            Label roleBadge = new Label(u.getRole());
            roleBadge.getStyleClass().addAll(
                    "history-badge", roleBadgeStyle(u.getRole()));
            roleBadge.setMinWidth(120);

            Button deleteBtn = new Button("🗑 Xóa");
            deleteBtn.getStyleClass().add("btn-danger");
            boolean isSelf = u.getUsername()
                    .equals(AppContext.getCurrentUser().getUsername());
            deleteBtn.setDisable(isSelf);
            deleteBtn.setOnAction(e -> handleDeleteUser(u));

            HBox actions = new HBox(8, deleteBtn);
            actions.setAlignment(Pos.CENTER);

            row.getChildren().addAll(username, fullName, email, roleBadge, actions);
            contentBox.getChildren().add(row);
        }
    }

    // =========================================================
    // TAB: SẢN PHẨM CHỜ DUYỆT
    // Hiển thị sản phẩm CHỜ DUYỆT của TẤT CẢ seller đã đăng ký.
    // Bất kỳ seller nào (dù mới tạo tài khoản) đăng sản phẩm
    // đều hiện lên đây nhờ getAllPendingProducts().
    // =========================================================
    @FXML
    public void showProducts() {
        currentTab = "products";
        contentBox.getChildren().clear();

        HBox header = buildTableHeader(
                "Tên sản phẩm", "Danh mục", "Người bán", "Trạng thái", "Hành động");
        contentBox.getChildren().add(header);

        // getAllPendingProducts() = lọc CHỜ DUYỆT từ productMap của MỌI seller
        List<AppContext.ProductRecord> pending = AppContext.getAllPendingProducts();

        if (pending.isEmpty()) {
            contentBox.getChildren().add(
                    emptyLabel("✅  Không có sản phẩm nào đang chờ duyệt."));
            refreshStats();
            return;
        }

        for (AppContext.ProductRecord p : pending) {
            // Tra ngược productMap để lấy đúng sellerUsername
            String sellerName = AppContext.getSellerForProduct(p.id());

            HBox row = buildPendingRow(p, sellerName);
            contentBox.getChildren().add(row);
        }

        refreshStats();
    }

    /**
     * Tạo một hàng hiển thị sản phẩm CHỜ DUYỆT với nút Duyệt / Từ chối.
     */
    private HBox buildPendingRow(AppContext.ProductRecord p, String sellerName) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");
        row.setPadding(new Insets(10, 20, 10, 20));

        Label name     = colLabel(p.name(), 200, true);
        Label category = colLabel(p.category(), 120, false);
        Label seller   = colLabel(
                "—".equals(sellerName) ? "Không rõ" : sellerName,
                140, false);

        // Hiển thị thêm giá khởi điểm trong cột seller để Admin dễ quyết định
        VBox sellerBox = new VBox(2);
        Label sellerLbl = new Label("👤 " + ("—".equals(sellerName) ? "Không rõ" : sellerName));
        sellerLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        Label priceLbl = new Label("Giá KĐ: " + formatVND(p.startPrice()));
        priceLbl.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px;");
        sellerBox.getChildren().addAll(sellerLbl, priceLbl);
        sellerBox.setMinWidth(160);
        HBox.setHgrow(sellerBox, Priority.ALWAYS);

        Label statusBadge = new Label("⏳ " + p.status());
        statusBadge.getStyleClass().addAll("history-badge", "badge-warn");
        statusBadge.setMinWidth(130);

        // Nút Duyệt
        Button approveBtn = new Button("✅ Duyệt");
        approveBtn.setStyle(
                "-fx-background-color: #14532d; -fx-text-fill: #4ade80; "
                + "-fx-background-radius: 6; -fx-cursor: hand; "
                + "-fx-font-size: 12px; -fx-padding: 5 12 5 12;");
        approveBtn.setOnAction(e -> handleApproveProduct(p, sellerName));

        // Nút Từ chối
        Button rejectBtn = new Button("❌ Từ chối");
        rejectBtn.getStyleClass().add("btn-danger");
        rejectBtn.setOnAction(e -> handleRejectProduct(p, sellerName));

        HBox actions = new HBox(8, approveBtn, rejectBtn);
        actions.setAlignment(Pos.CENTER);
        actions.setMinWidth(180);

        // Dùng sellerBox thay cho label seller đơn giản
        row.getChildren().addAll(name, category, sellerBox, statusBadge, actions);
        return row;
    }

    // =========================================================
    // TAB: PHIÊN ĐẤU GIÁ
    // =========================================================
    @FXML
    public void showSessions() {
        currentTab = "sessions";
        contentBox.getChildren().clear();

        HBox header = buildTableHeader(
                "ID Phiên", "Tên sản phẩm", "Giá hiện tại", "Trạng thái", "Hành động");
        contentBox.getChildren().add(header);

        List<AuctionSession> sessions = AppContext.getGlobalSessions();
        if (sessions.isEmpty()) {
            contentBox.getChildren().add(emptyLabel("Chưa có phiên đấu giá nào."));
            return;
        }

        for (AuctionSession s : sessions) {
            HBox row = new HBox(0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("history-row");
            row.setPadding(new Insets(10, 20, 10, 20));

            Label idLabel    = colLabel(s.getSessionId(), 140, false);
            Label nameLabel  = colLabel(s.getItemName(), 200, true);
            Label priceLabel = colLabel(formatVND(s.getCurrentPrice()), 140, false);
            priceLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");

            boolean isRunning = s.getStatus() == AuctionStatus.RUNNING;
            String statusTxt  = isRunning ? "ĐANG DIỄN RA" : "ĐÃ KẾT THÚC";
            Label statusBadge = new Label(statusTxt);
            statusBadge.getStyleClass().addAll("history-badge",
                    isRunning ? "badge-success" : "badge-neutral");
            statusBadge.setMinWidth(130);

            Button enterBtn = new Button("🔴 Vào phiên");
            enterBtn.getStyleClass().add("btn-primary");
            enterBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 10 5 10;");
            enterBtn.setDisable(!isRunning);
            enterBtn.setOnAction(e -> handleEnterSession(s));

            HBox actions = new HBox(8, enterBtn);
            actions.setAlignment(Pos.CENTER);

            row.getChildren().addAll(
                    idLabel, nameLabel, priceLabel, statusBadge, actions);
            contentBox.getChildren().add(row);
        }
    }

    // =========================================================
    // XỬ LÝ SỰ KIỆN
    // =========================================================
    private void handleDeleteUser(User u) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xóa người dùng");
        confirm.setHeaderText("Xóa: " + u.getUsername());
        confirm.setContentText("Bạn có chắc muốn xóa tài khoản này?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                authService.unregister(u.getUsername());
                showUsers();
                refreshStats();
            }
        });
    }

    /**
     * Duyệt sản phẩm: đổi status CHỜ DUYỆT → ĐÃ DUYỆT.
     * Seller sẽ thấy nút "Bắt đầu đấu giá" xuất hiện trong màn hình của họ.
     */
    private void handleApproveProduct(AppContext.ProductRecord p, String sellerName) {
        // Tra lại seller nếu cần (phòng trường hợp sellerName = "—")
        String resolved = resolvedSeller(p.id(), sellerName);
        if ("—".equals(resolved)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi",
                    "Không tìm được người bán.\nVui lòng thử lại.");
            return;
        }

        AppContext.updateProduct(resolved, new AppContext.ProductRecord(
                p.id(), p.name(), p.category(),
                p.startPrice(), p.currentPrice(), p.bidCount(),
                "ĐÃ DUYỆT",
                p.startTime(), p.endTime(), p.topBidder()
        ));

        showAlert(Alert.AlertType.INFORMATION, "Duyệt thành công",
                "✅ Đã duyệt sản phẩm:\n\"" + p.name() + "\"\n"
                + "Người bán: " + resolved + "\n\n"
                + "Seller có thể bắt đầu phiên đấu giá.");

        showProducts(); // reload — sản phẩm biến khỏi danh sách chờ
    }

    /**
     * Từ chối sản phẩm: đổi status CHỜ DUYỆT → TỪ CHỐI.
     * Seller sẽ thấy thông báo bị từ chối và có thể xóa rồi đăng lại.
     */
    private void handleRejectProduct(AppContext.ProductRecord p, String sellerName) {
        String resolved = resolvedSeller(p.id(), sellerName);
        if ("—".equals(resolved)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi",
                    "Không tìm được người bán.\nVui lòng thử lại.");
            return;
        }

        TextInputDialog reasonDialog = new TextInputDialog();
        reasonDialog.setTitle("Từ chối sản phẩm");
        reasonDialog.setHeaderText("Từ chối: \"" + p.name() + "\" của " + resolved);
        reasonDialog.setContentText("Lý do từ chối (tùy chọn):");

        reasonDialog.showAndWait().ifPresent(reason -> {
            AppContext.updateProduct(resolved, new AppContext.ProductRecord(
                    p.id(), p.name(), p.category(),
                    p.startPrice(), p.currentPrice(), p.bidCount(),
                    "TỪ CHỐI",
                    p.startTime(), p.endTime(), p.topBidder()
            ));

            System.out.printf("AdminController: Từ chối \"%s\" của %s. Lý do: %s%n",
                    p.name(), resolved, reason.isEmpty() ? "(không có)" : reason);

            showProducts(); // reload
        });
    }

    private void handleEnterSession(AuctionSession s) {
        AppContext.setActiveSession(s);
        if (autoRefreshTimeline != null) autoRefreshTimeline.pause();
        try {
            HelloApplication.showLiveAuctionView();
        } catch (Exception e) {
            e.printStackTrace();
            if (autoRefreshTimeline != null) autoRefreshTimeline.play();
        }
    }

    // =========================================================
    // HELPER — tìm seller chắc chắn
    // =========================================================

    /**
     * Trả về sellerName đã biết nếu hợp lệ,
     * ngược lại tra lại từ productMap.
     */
    private String resolvedSeller(String productId, String sellerName) {
        if (sellerName != null && !sellerName.isBlank()
                && !"—".equals(sellerName) && !"Không rõ".equals(sellerName)) {
            return sellerName;
        }
        return AppContext.getSellerForProduct(productId);
    }

    // =========================================================
    // TIỆN ÍCH UI
    // =========================================================
    private HBox buildTableHeader(String... cols) {
        HBox header = new HBox(0);
        header.getStyleClass().add("table-header-row");
        header.setPadding(new Insets(8, 20, 8, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        for (String col : cols) {
            Label l = new Label(col);
            l.getStyleClass().add("th-label");
            l.setMinWidth(col.equals("Hành động") ? 180 : 160);
            HBox.setHgrow(l, Priority.ALWAYS);
            header.getChildren().add(l);
        }
        return header;
    }

    private Label colLabel(String text, double minWidth, boolean bold) {
        Label l = new Label(text);
        l.setMinWidth(minWidth);
        HBox.setHgrow(l, Priority.ALWAYS);
        if (bold) {
            l.setStyle("-fx-text-fill: #e2e8f0; "
                    + "-fx-font-weight: bold; -fx-font-size: 13px;");
        } else {
            l.getStyleClass().add("history-item-meta");
        }
        return l;
    }

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #64748b; "
                + "-fx-font-size: 14px; -fx-padding: 40 0 40 0;");
        return l;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

    private String roleBadgeStyle(String role) {
        return switch (role) {
            case "ADMIN"  -> "badge-danger";
            case "SELLER" -> "badge-info";
            default       -> "badge-neutral";
        };
    }

    private String formatVND(double v) {
        return String.format("₫ %,.0f", v);
    }
}
