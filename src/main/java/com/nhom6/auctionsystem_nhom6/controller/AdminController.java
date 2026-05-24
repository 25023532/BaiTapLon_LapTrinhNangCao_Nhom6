package com.nhom6.auctionsystem_nhom6.controller;

import com.nhom6.auctionsystem_nhom6.AppContext;
import com.nhom6.auctionsystem_nhom6.HelloApplication;
import com.nhom6.auctionsystem_nhom6.ServerConnection;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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

    // ── FXML ──────────────────────────────────────────────────
    @FXML private Label      totalUsersLabel;
    @FXML private Label      totalProductsLabel;
    @FXML private Label      pendingLabel;
    @FXML private Label      activeSessionsLabel;
    @FXML private MenuButton adminMenuBtn;
    @FXML private Label      adminNameLabel;
    @FXML private Label      adminAvatarLabel;
    @FXML private VBox       contentBox;

    // ── Tab buttons — cần fx:id để cập nhật active style ──────
    @FXML private Button tabBtnUsers;
    @FXML private Button tabBtnProducts;
    @FXML private Button tabBtnSessions;

    private final AuthService authService = AppContext.getAuthService();
    private String currentTab = "users";
    private Timeline autoRefreshTimeline;

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        User me = AppContext.getCurrentUser();
        String name = me.getUsername();
        adminNameLabel.setText(name);
        adminAvatarLabel.setText(
                name.length() >= 2 ? name.substring(0, 2).toUpperCase()
                                   : name.toUpperCase());

        refreshStats();
        showUsers(); // tab mặc định — setActiveTab() gọi bên trong

        // Auto-refresh mỗi 2 giây
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e ->
                Platform.runLater(() -> {
                    refreshStats();
                    if ("products".equals(currentTab))
                        renderProductsContent();
                })
        ));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    // =========================================================
    // ACTIVE TAB HIGHLIGHT
    // =========================================================
    /**
     * Xóa tab-active khỏi tất cả nút, sau đó set vào nút được chọn.
     * Gọi ở đầu mỗi showXxx() để đồng bộ UI.
     */
    private void setActiveTab(Button activeBtn) {
        for (Button btn : List.of(tabBtnUsers, tabBtnProducts, tabBtnSessions)) {
            btn.getStyleClass().removeAll("tab-active");
            // Đảm bảo luôn có class tab-btn
            if (!btn.getStyleClass().contains("tab-btn"))
                btn.getStyleClass().add("tab-btn");
        }
        if (!activeBtn.getStyleClass().contains("tab-active"))
            activeBtn.getStyleClass().add("tab-active");
    }

    // =========================================================
    // PROFILE / LOGOUT
    // =========================================================
    @FXML private void handleProfile() {
        try { HelloApplication.showProfileView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleLogout() {
        if (autoRefreshTimeline != null) autoRefreshTimeline.stop();
        AppContext.logout();
        try { HelloApplication.showLoginView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================
    // STATS
    // =========================================================
    private void refreshStats() {
        List<User> users = new ArrayList<>(authService.getAllUsers().values());
        totalUsersLabel.setText(String.valueOf(users.size()));

        List<AppContext.ProductRecord> all = AppContext.getAllProducts();
        totalProductsLabel.setText(String.valueOf(all.size()));

        long pending = all.stream()
                .filter(p -> "CHỜ DUYỆT".equals(p.status())).count();
        pendingLabel.setText(String.valueOf(pending));

        long active = AppContext.getGlobalSessions().stream()
                .filter(s -> s.getStatus() == AuctionStatus.RUNNING).count();
        activeSessionsLabel.setText(String.valueOf(active));
    }

    // =========================================================
    // TAB: NGƯỜI DÙNG
    // =========================================================
    @FXML public void showUsers() {
        currentTab = "users";
        setActiveTab(tabBtnUsers); // ✅ highlight tab Người dùng
        contentBox.getChildren().clear();

        contentBox.getChildren().add(buildHeader(
                "Tên đăng nhập", "Họ tên", "Email", "Vai trò", "Hành động"));

        List<User> users = new ArrayList<>(authService.getAllUsers().values());
        if (users.isEmpty()) {
            contentBox.getChildren().add(emptyLabel("Chưa có người dùng nào."));
            return;
        }

        for (User u : users) {
            HBox row = makeRow();

            Label roleBadge = new Label(u.getRole());
            roleBadge.getStyleClass().addAll(
                    "history-badge", roleBadgeStyle(u.getRole()));
            roleBadge.setMinWidth(120);

            Button delBtn = new Button("🗑 Xóa");
            delBtn.getStyleClass().add("btn-danger");
            delBtn.setDisable(u.getUsername()
                    .equals(AppContext.getCurrentUser().getUsername()));
            delBtn.setOnAction(e -> handleDeleteUser(u));

            HBox actions = new HBox(8, delBtn);
            actions.setAlignment(Pos.CENTER);

            row.getChildren().addAll(
                    col(u.getUsername(), 160, true),
                    col(nvl(u.getFullName()), 180, false),
                    col(nvl(u.getEmail()), 200, false),
                    roleBadge, actions);
            contentBox.getChildren().add(row);
        }
    }

    // =========================================================
    // TAB: SẢN PHẨM
    // =========================================================
    @FXML public void showProducts() {
        currentTab = "products";
        setActiveTab(tabBtnProducts); // ✅ highlight tab Sản phẩm
        renderProductsContent();
    }

    private void renderProductsContent() {
        contentBox.getChildren().clear();

        contentBox.getChildren().add(buildHeader(
                "Tên sản phẩm", "Danh mục", "Người bán",
                "Trạng thái", "Hành động"));

        List<AppContext.ProductRecord> pending = AppContext.getAllPendingProducts()
            .stream()
            .sorted((a, b) -> b.startTime().compareTo(a.startTime()))
            .collect(java.util.stream.Collectors.toList());

        if (pending.isEmpty()) {
            contentBox.getChildren().add(
                    emptyLabel("✅  Không có sản phẩm nào đang chờ duyệt."));
            return;
        }

        for (AppContext.ProductRecord p : pending) {
            String seller = AppContext.getSellerForProduct(p.id());
            contentBox.getChildren().add(buildPendingRow(p, seller));
        }
    }

    private HBox buildPendingRow(AppContext.ProductRecord p, String sellerName) {
        HBox row = makeRow();

        Label name = col(p.name(), 200, true);
        Label cat  = col(p.category(), 120, false);

        VBox sellerBox = new VBox(3);
        sellerBox.setMinWidth(160);
        HBox.setHgrow(sellerBox, Priority.ALWAYS);
        Label sellerLbl = new Label("👤 " + ("—".equals(sellerName) ? "?" : sellerName));
        sellerLbl.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12px;");
        Label priceLbl = new Label("Giá KĐ: " + fmtVND(p.startPrice()));
        priceLbl.setStyle("-fx-text-fill:#38bdf8;-fx-font-size:11px;");
        sellerBox.getChildren().addAll(sellerLbl, priceLbl);

        Label badge = new Label("⏳ CHỜ DUYỆT");
        badge.getStyleClass().addAll("history-badge", "badge-warn");
        badge.setMinWidth(130);

        Button approveBtn = new Button("✅ Duyệt");
        approveBtn.setStyle(
                "-fx-background-color:#14532d;-fx-text-fill:#4ade80;"
                + "-fx-background-radius:6;-fx-cursor:hand;"
                + "-fx-font-size:12px;-fx-padding:5 12 5 12;");
        approveBtn.setOnAction(e -> handleApprove(p, sellerName));

        Button rejectBtn = new Button("❌ Từ chối");
        rejectBtn.getStyleClass().add("btn-danger");
        rejectBtn.setOnAction(e -> handleReject(p, sellerName));

        HBox actions = new HBox(8, approveBtn, rejectBtn);
        actions.setAlignment(Pos.CENTER);
        actions.setMinWidth(200);

        row.getChildren().addAll(name, cat, sellerBox, badge, actions);
        return row;
    }

    // =========================================================
    // TAB: PHIÊN ĐẤU GIÁ
    // =========================================================
    @FXML public void showSessions() {
        currentTab = "sessions";
        setActiveTab(tabBtnSessions); // ✅ highlight tab Phiên đấu giá
        contentBox.getChildren().clear();

        contentBox.getChildren().add(buildHeader(
                "ID Phiên", "Tên sản phẩm", "Giá hiện tại",
                "Trạng thái", "Hành động"));

        List<AuctionSession> sessions = AppContext.getGlobalSessions();
        if (sessions.isEmpty()) {
            contentBox.getChildren().add(
                    emptyLabel("Chưa có phiên đấu giá nào."));
            return;
        }

        for (AuctionSession s : sessions) {
            HBox row = makeRow();

            Label price = col(fmtVND(s.getCurrentPrice()), 140, false);
            price.setStyle("-fx-text-fill:#38bdf8;-fx-font-weight:bold;");

            boolean running = s.getStatus() == AuctionStatus.RUNNING;
            Label statusBadge = new Label(
                    running ? "ĐANG DIỄN RA" : "ĐÃ KẾT THÚC");
            statusBadge.getStyleClass().addAll("history-badge",
                    running ? "badge-success" : "badge-neutral");
            statusBadge.setMinWidth(130);

            Button enterBtn = new Button("🔴 Vào phiên");
            enterBtn.getStyleClass().add("btn-primary");
            enterBtn.setStyle("-fx-font-size:12px;-fx-padding:5 10 5 10;");
            enterBtn.setDisable(!running);
            enterBtn.setOnAction(e -> handleEnterSession(s));

            HBox actions = new HBox(8, enterBtn);
            actions.setAlignment(Pos.CENTER);

            row.getChildren().addAll(
                    col(s.getSessionId(), 140, false),
                    col(s.getItemName(), 200, true),
                    price, statusBadge, actions);
            contentBox.getChildren().add(row);
        }
    }

    // =========================================================
    // XỬ LÝ SỰ KIỆN
    // =========================================================
    private void handleDeleteUser(User u) {
        if (!confirm("Xóa người dùng", "Xóa: " + u.getUsername(),
                "Bạn có chắc muốn xóa tài khoản này?")) return;
        authService.unregister(u.getUsername());
        showUsers();
        refreshStats();
    }

    private void handleApprove(AppContext.ProductRecord p, String sellerName) {
        String seller = resolveSeller(p.id(), sellerName);
        if ("—".equals(seller)) {
            alert(Alert.AlertType.ERROR, "Lỗi", "Không tìm được người bán.");
            return;
        }
        AppContext.updateProduct(seller, new AppContext.ProductRecord(
                p.id(), p.name(), p.category(),
                p.startPrice(), p.currentPrice(), p.bidCount(),
                "ĐÃ DUYỆT", p.startTime(), p.endTime(), p.topBidder()));

        alert(Alert.AlertType.INFORMATION, "Duyệt thành công",
                "✅ Đã duyệt: \"" + p.name() + "\"\n"
                + "Người bán: " + seller + "\n\n"
                + "Seller có thể bắt đầu phiên đấu giá.");

        renderProductsContent();
        refreshStats();
    }

    private void handleReject(AppContext.ProductRecord p, String sellerName) {
        String seller = resolveSeller(p.id(), sellerName);
        if ("—".equals(seller)) {
            alert(Alert.AlertType.ERROR, "Lỗi", "Không tìm được người bán.");
            return;
        }
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Từ chối sản phẩm");
        dlg.setHeaderText("Từ chối: \"" + p.name() + "\"");
        dlg.setContentText("Lý do từ chối (tùy chọn):");
        dlg.showAndWait().ifPresent(reason -> {
            AppContext.updateProduct(seller, new AppContext.ProductRecord(
                    p.id(), p.name(), p.category(),
                    p.startPrice(), p.currentPrice(), p.bidCount(),
                    "TỪ CHỐI", p.startTime(), p.endTime(), p.topBidder()));

            System.out.printf("Admin từ chối \"%s\" của %s. Lý do: %s%n",
                    p.name(), seller,
                    reason.isEmpty() ? "(không có)" : reason);

            renderProductsContent();
            refreshStats();
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
    // HELPERS
    // =========================================================
    private String resolveSeller(String productId, String sellerName) {
        if (sellerName != null && !sellerName.isBlank()
                && !"—".equals(sellerName) && !"?".equals(sellerName))
            return sellerName;
        return AppContext.getSellerForProduct(productId);
    }

    private HBox makeRow() {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");
        row.setPadding(new Insets(10, 20, 10, 20));
        return row;
    }

    private HBox buildHeader(String... cols) {
        HBox h = new HBox(0);
        h.getStyleClass().add("table-header-row");
        h.setPadding(new Insets(8, 20, 8, 20));
        h.setAlignment(Pos.CENTER_LEFT);
        for (String c : cols) {
            Label l = new Label(c);
            l.getStyleClass().add("th-label");
            l.setMinWidth("Hành động".equals(c) ? 200 : 160);
            HBox.setHgrow(l, Priority.ALWAYS);
            h.getChildren().add(l);
        }
        return h;
    }

    private Label col(String text, double minW, boolean bold) {
        Label l = new Label(text);
        l.setMinWidth(minW);
        HBox.setHgrow(l, Priority.ALWAYS);
        if (bold) l.setStyle(
                "-fx-text-fill:#e2e8f0;-fx-font-weight:bold;-fx-font-size:13px;");
        else l.getStyleClass().add("history-item-meta");
        return l;
    }

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-text-fill:#64748b;-fx-font-size:14px;"
                + "-fx-padding:40 0 40 0;");
        return l;
    }

    private boolean confirm(String title, String header, String content) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(content);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void alert(Alert.AlertType type, String title, String content) {
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

    private String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private String fmtVND(double v) {
        return String.format("₫ %,.0f", v);
    }
}
