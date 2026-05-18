package com.nhom6.auctionsystem_nhom6;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.auction.AuctionSession;
import org.example.auction.AuctionStatus;
import org.example.service.AuthService;
import org.example.user.User;

import java.util.ArrayList;
import java.util.List;

public class AdminController {

    @FXML private Label totalUsersLabel;
    @FXML private Label totalProductsLabel;
    @FXML private Label pendingLabel;
    @FXML private Label activeSessionsLabel;
    @FXML private Label adminNameLabel;
    @FXML private VBox  contentBox;

    private final AuthService authService = AppContext.getAuthService();

    private String currentTab = "users";

    @FXML
    public void initialize() {
        User me = AppContext.getCurrentUser();
        adminNameLabel.setText("👤 " + me.getUsername());
        refreshStats();
        showUsers();
    }

    // =========================================================
    // THỐNG KÊ
    // =========================================================
    private void refreshStats() {
        // getAllUsers() trả về Map → chuyển sang List
        List<User> users = new ArrayList<>(authService.getAllUsers().values());
        totalUsersLabel.setText(String.valueOf(users.size()));

        long totalProducts = AppContext.getAllProducts().size();
        totalProductsLabel.setText(String.valueOf(totalProducts));

        long pending = AppContext.getAllProducts().stream()
                .filter(p -> "CHỜ DUYỆT".equals(p.status())).count();
        pendingLabel.setText(String.valueOf(pending));

        // FIX: AuctionStatus không có ENDED
        // → đếm phiên đang RUNNING (mọi trạng thái khác đều là đã kết thúc)
        long activeSessions = AppContext.getGlobalSessions().stream()
                .filter(s -> s.getStatus() == AuctionStatus.RUNNING).count();
        activeSessionsLabel.setText(String.valueOf(activeSessions));
    }

    // =========================================================
    // TAB: NGƯỜI DÙNG
    // =========================================================
    @FXML
    public void showUsers() {
        currentTab = "users";
        contentBox.getChildren().clear();

        HBox header = buildTableHeader("Tên đăng nhập", "Họ tên", "Email", "Vai trò", "Hành động");
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
            Label fullName = colLabel(u.getFullName().isEmpty() ? "—" : u.getFullName(), 180, false);
            Label email    = colLabel(u.getEmail().isEmpty() ? "—" : u.getEmail(), 200, false);

            Label roleBadge = new Label(u.getRole());
            roleBadge.getStyleClass().addAll("history-badge", roleBadgeStyle(u.getRole()));
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
    // TAB: SẢN PHẨM
    // =========================================================
    @FXML
    public void showProducts() {
        currentTab = "products";
        contentBox.getChildren().clear();

        HBox header = buildTableHeader("Tên sản phẩm", "Danh mục", "Người bán", "Trạng thái", "Hành động");
        contentBox.getChildren().add(header);

        List<AppContext.ProductRecord> products = AppContext.getAllProducts();
        if (products.isEmpty()) {
            contentBox.getChildren().add(emptyLabel("Chưa có sản phẩm nào."));
            return;
        }

        for (AppContext.ProductRecord p : products) {
            HBox row = new HBox(0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("history-row");
            row.setPadding(new Insets(10, 20, 10, 20));

            Label name     = colLabel(p.name(), 200, true);
            Label category = colLabel(p.category(), 120, false);

            // FIX: ProductRecord không có field sellerUsername vì seller được lưu
            //      làm KEY trong productMap. Dùng helper getSellerForProduct() để tra cứu.
            String sellerName = AppContext.getSellerForProduct(p.id());
            Label seller = colLabel(sellerName, 140, false);

            Label statusBadge = new Label(p.status());
            statusBadge.getStyleClass().addAll("history-badge", productBadgeStyle(p.status()));
            statusBadge.setMinWidth(130);

            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER);

            if ("CHỜ DUYỆT".equals(p.status())) {
                Button approveBtn = new Button("✅ Duyệt");
                approveBtn.setStyle(
                        "-fx-background-color: #14532d; -fx-text-fill: #4ade80; "
                                + "-fx-background-radius: 6; -fx-cursor: hand; "
                                + "-fx-font-size: 12px; -fx-padding: 5 10 5 10;");
                approveBtn.setOnAction(e -> handleApproveProduct(p, sellerName));

                Button rejectBtn = new Button("❌ Từ chối");
                rejectBtn.getStyleClass().add("btn-danger");
                rejectBtn.setOnAction(e -> handleRejectProduct(p, sellerName));

                actions.getChildren().addAll(approveBtn, rejectBtn);
            } else {
                Button viewBtn = new Button("👁 Xem");
                viewBtn.getStyleClass().add("btn-secondary");
                viewBtn.setOnAction(e -> handleViewProduct(p, sellerName));
                actions.getChildren().add(viewBtn);
            }

            row.getChildren().addAll(name, category, seller, statusBadge, actions);
            contentBox.getChildren().add(row);
        }

        refreshStats();
    }

    // =========================================================
    // TAB: PHIÊN ĐẤU GIÁ
    // =========================================================
    @FXML
    public void showSessions() {
        currentTab = "sessions";
        contentBox.getChildren().clear();

        HBox header = buildTableHeader("ID Phiên", "Tên sản phẩm", "Giá hiện tại", "Trạng thái", "Hành động");
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

            // FIX: không dùng AuctionStatus.ENDED vì không tồn tại
            // → phiên đang chạy khi status == RUNNING, mọi trạng thái khác = kết thúc
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

            row.getChildren().addAll(idLabel, nameLabel, priceLabel, statusBadge, actions);
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
                // AuthService dùng unregister() thay vì deleteUser()
                authService.unregister(u.getUsername());
                showUsers();
                refreshStats();
            }
        });
    }

    private void handleApproveProduct(AppContext.ProductRecord p, String sellerName) {
        AppContext.updateProduct(sellerName, new AppContext.ProductRecord(
                p.id(), p.name(), p.category(),
                p.startPrice(), p.currentPrice(), p.bidCount(),
                "ĐÃ DUYỆT", p.startTime(), p.endTime(), p.topBidder()
        ));
        showProducts();
    }

    private void handleRejectProduct(AppContext.ProductRecord p, String sellerName) {
        AppContext.updateProduct(sellerName, new AppContext.ProductRecord(
                p.id(), p.name(), p.category(),
                p.startPrice(), p.currentPrice(), p.bidCount(),
                "TỪ CHỐI", p.startTime(), p.endTime(), p.topBidder()
        ));
        showProducts();
    }

    private void handleViewProduct(AppContext.ProductRecord p, String sellerName) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Chi tiết sản phẩm");
        a.setHeaderText(p.name());
        a.setContentText(
                "ID         : " + p.id()
                        + "\nDanh mục  : " + p.category()
                        + "\nGiá KĐ   : " + formatVND(p.startPrice())
                        + "\nGiá hiện  : " + formatVND(p.currentPrice())
                        + "\nNgười bán : " + sellerName
                        + "\nTrạng thái: " + p.status()
        );
        a.showAndWait();
    }

    private void handleEnterSession(AuctionSession s) {
        AppContext.setActiveSession(s);
        try { HelloApplication.showLiveAuctionView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout() {
        AppContext.setCurrentUser(null);
        try { HelloApplication.showLoginView(); }
        catch (Exception e) { e.printStackTrace(); }
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
            l.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold; -fx-font-size: 13px;");
        } else {
            l.getStyleClass().add("history-item-meta");
        }
        return l;
    }

    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-padding: 40 0 40 0;");
        return l;
    }

    private String roleBadgeStyle(String role) {
        return switch (role) {
            case "ADMIN"  -> "badge-danger";
            case "SELLER" -> "badge-info";
            default       -> "badge-neutral";
        };
    }

    private String productBadgeStyle(String status) {
        return switch (status) {
            case "ĐÃ DUYỆT"       -> "badge-info";
            case "ĐANG ĐẤU GIÁ"  -> "badge-success";
            case "CHỜ DUYỆT"      -> "badge-warn";
            case "ĐÃ BÁN"         -> "badge-success";
            case "TỪ CHỐI"        -> "badge-danger";
            default               -> "badge-neutral";
        };
    }

    private String formatVND(double v) {
        return String.format("₫ %,.0f", v);
    }
}
