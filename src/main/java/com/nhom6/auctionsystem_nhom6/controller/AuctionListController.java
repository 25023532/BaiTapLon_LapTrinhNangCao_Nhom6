package com.nhom6.auctionsystem_nhom6.controller;

import com.nhom6.auctionsystem_nhom6.AppContext;
import com.nhom6.auctionsystem_nhom6.HelloApplication;
import com.nhom6.auctionsystem_nhom6.ServerConnection;

import javafx.animation.Animation;
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
import org.example.user.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuctionListController {

    @FXML private Label    totalLabel;
    @FXML private Label    runningLabel;
    @FXML private Label    upcomingLabel;
    @FXML private Label    endedLabel;
    @FXML private Button   tabAllBtn;
    @FXML private Button   tabRunBtn;
    @FXML private Button   tabUpBtn;
    @FXML private Button   tabEndBtn;
    @FXML private VBox     sessionListBox;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private TextField searchField;

    private int currentTab = 0;
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public record SessionRecord(
            String id, String itemName, String category,
            String sellerName, double startPrice, double currentPrice,
            int bidCount, String status,
            LocalDateTime startTime, LocalDateTime endTime,
            AuctionSession session
    ) {}

    private List<SessionRecord> allSessions = new ArrayList<>();

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        statusFilter.getItems().addAll(
                "Tất cả", "RUNNING", "UPCOMING", "ENDED");
        categoryFilter.getItems().addAll(
                "Tất cả", "Laptop", "Điện thoại", "Máy ảnh",
                "Điện tử", "Đồng hồ", "Xe cộ");

        loadSessions();
        refreshStats();
        renderList(allSessions);

        // Tự động cập nhật trạng thái mỗi 30 giây
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(30), e -> {
                    loadSessions();
                    refreshStats();
                    applyFilters();
                })
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        String pending = HelloApplication.getPendingCategory();
        if (pending != null && !pending.isEmpty()) {
            categoryFilter.setValue(pending);
            applyFilters();
        }
    }

    // =========================================================
    // LOAD
    // =========================================================
    /**
     * Logic xác định trạng thái hiển thị từ AuctionSession:
     *
     *  · OPEN                       → "UPCOMING"  (Sắp diễn ra — chưa được seller start)
     *  · RUNNING + endTime chưa qua → "RUNNING"   (Đang diễn ra)
     *  · RUNNING + endTime đã qua   → "ENDED"     (Đã kết thúc)
     *  · FINISHED / PAID / CANCELED → "ENDED"     (Đã kết thúc)
     *
     * Logic từ ProductRecord (chưa có AuctionSession thực):
     *  · now < startTime → "UPCOMING"
     *  · now > endTime   → "ENDED"
     *  · còn lại         → "RUNNING"
     */
    private void loadSessions() {
        allSessions = new ArrayList<>();
        User user        = AppContext.getCurrentUser();
        boolean isSeller = "SELLER".equalsIgnoreCase(user.getRole());

        // ── NGUỒN 1: globalSessions — MỌI user đều thấy ──────
        for (AuctionSession s : AppContext.getGlobalSessions()) {

            LocalDateTime now = LocalDateTime.now();
            String status;
            boolean skip = false;

            // Tìm startTime từ ProductRecord
            LocalDateTime startTime = s.getEndTime().minusHours(1); // fallback
            String category   = "Chung";
            String sellerName = AppContext.getSessionSeller(s.getSessionId());

            for (AppContext.ProductRecord p : AppContext.getAllProducts()) {
                if (p.id().equals(s.getSessionId())) {
                    category  = p.category();
                    startTime = p.startTime();
                    break;
                }
            }

            if (s.getStatus() == AuctionStatus.OPEN) {
                if (startTime.isBefore(now.plusHours(24))) {
                    status = "UPCOMING";
                } else {
                    continue;
                }
            } else if (s.getStatus() == AuctionStatus.RUNNING) {
                status = now.isAfter(s.getEndTime()) ? "ENDED" : "RUNNING";
            } else {
                status = "ENDED";
            }

            allSessions.add(new SessionRecord(
                    s.getSessionId(),
                    s.getItemName(),
                    category,
                    sellerName,
                    s.getStartingPrice(),
                    s.getCurrentPrice(),
                    s.getBidHistory().size(),
                    status,
                    startTime,
                    s.getEndTime(),
                    s
            ));
        }

        // ── NGUỒN 2: ProductRecord của Seller hiện tại ───────
        if (isSeller) {
            for (AppContext.ProductRecord p :
                    AppContext.getProducts(user.getUsername())) {

                boolean alreadyIn = allSessions.stream()
                        .anyMatch(s -> s.id().equals(p.id()));
                if (alreadyIn) continue;

                LocalDateTime now2 = LocalDateTime.now();
                String status;

                if ("CHỜ DUYỆT".equals(p.status())) continue;
                if (now2.isBefore(p.startTime())) {
                    if (p.startTime().isBefore(now2.plusHours(24))) {
                        status = "UPCOMING";
                    } else {
                        continue;
                    }
                } else if (now2.isAfter(p.endTime())
                    || "ĐÃ KẾT THÚC".equals(p.status())
                    || "ĐÃ BÁN".equals(p.status())
                    || "CANCELED".equals(p.status())) {
                    status = "ENDED";
                } else if ("ĐANG ĐẤU GIÁ".equals(p.status())) {
                    status = "RUNNING";
                } else {
                    status = "RUNNING";
                }

                allSessions.add(new SessionRecord(
                        p.id(), p.name(), p.category(),
                        user.getUsername(),
                        p.startPrice(), p.currentPrice(),
                        p.bidCount(), status,
                        p.startTime(), p.endTime(),
                        null
                ));
            }
        }
    }

    private void refreshStats() {
        totalLabel.setText(String.valueOf(allSessions.size()));
        runningLabel.setText(String.valueOf(
                allSessions.stream()
                        .filter(s -> "RUNNING".equals(s.status())).count()));
        upcomingLabel.setText(String.valueOf(
                allSessions.stream()
                        .filter(s -> "UPCOMING".equals(s.status())).count()));
        endedLabel.setText(String.valueOf(
                allSessions.stream()
                        .filter(s -> "ENDED".equals(s.status())).count()));
    }

    // =========================================================
    // RENDER
    // =========================================================
    private void renderList(List<SessionRecord> list) {
        sessionListBox.getChildren().clear();
        if (list.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(60, 0, 60, 0));
            Label icon = new Label("📭");
            icon.setStyle("-fx-font-size: 48px;");
            Label msg = new Label("Chưa có phiên đấu giá nào.");
            msg.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px; " +
                    "-fx-font-weight: bold;");
            Label hint = new Label(
                    "Seller cần bắt đầu phiên để hiển thị tại đây.");
            hint.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
            emptyBox.getChildren().addAll(icon, msg, hint);
            sessionListBox.getChildren().add(emptyBox);
            return;
        }
        for (SessionRecord s : list)
            sessionListBox.getChildren().add(buildSessionCard(s));
    }

    private HBox buildSessionCard(SessionRecord s) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("history-row");
        card.setPadding(new Insets(16, 20, 16, 20));

        // Status icon + badge
        VBox leftBar = new VBox(6);
        leftBar.setAlignment(Pos.CENTER);
        leftBar.setMinWidth(52);
        Label icon = new Label(statusEmoji(s.status()));
        icon.setStyle("-fx-font-size: 22px;");
        Label statusBadge = new Label(statusText(s.status()));
        statusBadge.getStyleClass().addAll(
                "history-badge", statusBadgeClass(s.status()));
        leftBar.getChildren().addAll(icon, statusBadge);

        // Info
        VBox info = new VBox(6);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(s.itemName());
        name.getStyleClass().add("history-item-name");

        HBox metaRow1 = new HBox(20);
        metaRow1.setAlignment(Pos.CENTER_LEFT);
        Label cat    = new Label("📂 " + s.category());
        Label seller = new Label("👤 " + s.sellerName());
        Label bids   = new Label("🔨 " + s.bidCount() + " lượt bid");
        cat.getStyleClass().add("history-item-meta");
        seller.getStyleClass().add("history-item-meta");
        bids.getStyleClass().add("history-item-meta");
        metaRow1.getChildren().addAll(cat, seller, bids);

        HBox metaRow2 = new HBox(20);
        metaRow2.setAlignment(Pos.CENTER_LEFT);
        Label startT = new Label("🕐 Bắt đầu: " + s.startTime().format(DT_FMT));
        Label endT   = new Label("⏰ Kết thúc: " + s.endTime().format(DT_FMT));
        Label id     = new Label("ID: " + s.id());
        startT.getStyleClass().add("history-item-meta");
        endT.getStyleClass().add("history-item-meta");
        id.getStyleClass().add("history-item-meta");
        metaRow2.getChildren().addAll(startT, endT, id);
        info.getChildren().addAll(name, metaRow1, metaRow2);

        // Price
        VBox priceBox = new VBox(4);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        priceBox.setMinWidth(160);
        Label startP = new Label("Khởi điểm: " + formatVND(s.startPrice()));
        startP.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        Label curP = new Label(formatVND(s.currentPrice()));
        curP.getStyleClass().add("history-item-price");
        double pct = s.startPrice() > 0
                ? (s.currentPrice() - s.startPrice()) / s.startPrice() : 0;
        ProgressBar bar = new ProgressBar(Math.min(pct, 1.0));
        bar.setPrefWidth(150);
        bar.setStyle("-fx-accent: #10b981;");
        Label pctLabel = new Label(String.format("+%.1f%%", pct * 100));
        pctLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px;");
        priceBox.getChildren().addAll(startP, curP, bar, pctLabel);

        // Actions
        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);
        actions.setMinWidth(120);

        Button viewBtn = new Button("👁 Xem chi tiết");
        viewBtn.getStyleClass().add("btn-secondary");
        viewBtn.setOnAction(e -> handleViewSession(s));

        Button joinBtn = new Button("⚡ Tham gia");
        joinBtn.getStyleClass().add("btn-primary");
        // Chỉ enable nếu RUNNING và có AuctionSession thực
        joinBtn.setDisable(!"RUNNING".equals(s.status()) || s.session() == null);
        joinBtn.setOnAction(e -> handleJoinSession(s));

        actions.getChildren().addAll(viewBtn, joinBtn);
        card.getChildren().addAll(leftBar, info, priceBox, actions);
        return card;
    }

    // =========================================================
    // TAB & FILTER
    // =========================================================
    @FXML private void handleTabAll()      { setTab(0); }
    @FXML private void handleTabRunning()  { setTab(1); }
    @FXML private void handleTabUpcoming() { setTab(2); }
    @FXML private void handleTabEnded()    { setTab(3); }

    private void setTab(int tab) {
        currentTab = tab;
        tabAllBtn.getStyleClass().setAll("tab-btn", tab == 0 ? "tab-active" : "");
        tabRunBtn.getStyleClass().setAll("tab-btn", tab == 1 ? "tab-active" : "");
        tabUpBtn .getStyleClass().setAll("tab-btn", tab == 2 ? "tab-active" : "");
        tabEndBtn.getStyleClass().setAll("tab-btn", tab == 3 ? "tab-active" : "");
        applyFilters();
    }

    @FXML private void handleFilter() { applyFilters(); }
    @FXML private void handleSearch() { applyFilters(); }

    private void applyFilters() {
        String keyword  = searchField.getText().trim().toLowerCase();
        String status   = statusFilter.getValue();
        String category = categoryFilter.getValue();

        List<SessionRecord> filtered = allSessions.stream()
            .filter(s -> {
                if (currentTab == 1 && !"RUNNING".equals(s.status()))  return false;
                if (currentTab == 2 && !"UPCOMING".equals(s.status())) return false;
                if (currentTab == 3 && !"ENDED".equals(s.status()))    return false;
                if (status != null && !"Tất cả".equals(status)
                        && !status.isEmpty()
                        && !status.equals(s.status())) return false;
                if (category != null && !"Tất cả".equals(category)
                        && !category.isEmpty()
                        && !category.equals(s.category())) return false;
                if (!keyword.isEmpty()
                        && !s.itemName().toLowerCase().contains(keyword)
                        && !s.sellerName().toLowerCase().contains(keyword)
                        && !s.id().toLowerCase().contains(keyword)) return false;
                return true;
            })
            .collect(Collectors.toList());

        renderList(filtered);
    }

    // =========================================================
    // ACTIONS
    // =========================================================
    private void handleViewSession(SessionRecord s) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi tiết phiên đấu giá");
        alert.setHeaderText(s.itemName());
        alert.setContentText(
                "ID             : " + s.id()                      + "\n" +
                "Danh mục      : " + s.category()                 + "\n" +
                "Người bán     : " + s.sellerName()               + "\n" +
                "Giá khởi điểm : " + formatVND(s.startPrice())    + "\n" +
                "Giá hiện tại  : " + formatVND(s.currentPrice())  + "\n" +
                "Số lượt bid   : " + s.bidCount()                 + "\n" +
                "Bắt đầu       : " + s.startTime().format(DT_FMT) + "\n" +
                "Kết thúc      : " + s.endTime().format(DT_FMT)   + "\n" +
                "Trạng thái    : " + statusText(s.status())
        );
        alert.showAndWait();
    }

    /** Bidder bấm Tham gia → set activeSession → vào LiveAuction */
    private void handleJoinSession(SessionRecord s) {
        if (s.session() == null) {
            showAlert("Chưa sẵn sàng",
                    "Phiên này chưa được Seller kích hoạt.\nVui lòng thử lại sau.");
            return;
        }
        AppContext.setActiveSession(s.session());
        try { HelloApplication.showLiveAuctionView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleBack() {
        try { HelloApplication.showMainView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleAuctionList() {
        loadSessions();
        refreshStats();
        applyFilters();
    }

    @FXML private void handleLiveAuction() {
        try { HelloApplication.showLiveAuctionView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleWallet() {
        try { HelloApplication.showWalletView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleRating() {
        try { HelloApplication.showRatingView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleHelp() {
        try { HelloApplication.showHelpView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleProfile() {
        try { HelloApplication.showProfileView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleHistory() {
        try { HelloApplication.showHistoryView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleLogout() {
        try { AppContext.logout(); HelloApplication.showLoginView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleMyProducts() {
        try { HelloApplication.showMyProductsView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleCategoryDienTu() {
        categoryFilter.setValue("Điện tử");
        applyFilters();
    }
    @FXML private void handleCategoryMayAnh() {
        categoryFilter.setValue("Máy ảnh");
        applyFilters();
    }
    @FXML private void handleCategoryLaptop() {
        categoryFilter.setValue("Laptop");
        applyFilters();
    }
    @FXML private void handleCategoryDienThoai() {
        categoryFilter.setValue("Điện thoại");
        applyFilters();
    }
    @FXML private void handleCategoryDongHo() {
        categoryFilter.setValue("Đồng hồ");
        applyFilters();
    }
    @FXML private void handleCategoryXeCo() {
        categoryFilter.setValue("Xe cộ");
        applyFilters();
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private String statusEmoji(String s) {
        return switch (s) {
            case "RUNNING"  -> "🟢";
            case "UPCOMING" -> "⏰";
            case "ENDED"    -> "🏁";
            default         -> "📋";
        };
    }

    private String statusText(String s) {
        return switch (s) {
            case "RUNNING"  -> "ĐANG DIỄN RA";
            case "UPCOMING" -> "SẮP DIỄN RA";
            case "ENDED"    -> "ĐÃ KẾT THÚC";
            default         -> s;
        };
    }

    private String statusBadgeClass(String s) {
        return switch (s) {
            case "RUNNING"  -> "badge-success";
            case "UPCOMING" -> "badge-warn";
            case "ENDED"    -> "badge-neutral";
            default         -> "badge-neutral";
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
