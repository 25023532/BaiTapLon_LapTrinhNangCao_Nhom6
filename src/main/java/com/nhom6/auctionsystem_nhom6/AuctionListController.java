package com.nhom6.auctionsystem_nhom6;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
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

    private int currentTab = 0; // 0=all,1=running,2=upcoming,3=ended
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    // ── Model ─────────────────────────────────────────────────
    public record SessionRecord(
            String id, String itemName, String category,
            String sellerName, double startPrice, double currentPrice,
            int bidCount, String status,
            LocalDateTime startTime, LocalDateTime endTime
    ) {}

    private List<SessionRecord> allSessions = new ArrayList<>();

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
    }

    // ── Load: chỉ lấy dữ liệu thực từ AppContext ─────────────
    private void loadSessions() {
        allSessions = new ArrayList<>();

        // Active session từ AppContext nếu có
        if (AppContext.getActiveSession() != null) {
            var s = AppContext.getActiveSession();
            allSessions.add(new SessionRecord(
                    "S-001", s.getItemName(), "Laptop",
                    "SellerLong", s.getStartingPrice(), s.getCurrentPrice(),
                    s.getBidHistory().size(), s.getStatus().name(),
                    LocalDateTime.now().minusHours(1), s.getEndTime()
            ));
        }

        // Sản phẩm seller đã đăng
        User user = AppContext.getCurrentUser();
        if ("SELLER".equalsIgnoreCase(user.getRole())) {
            for (AppContext.ProductRecord p : AppContext.getProducts(user.getUsername())) {
                String status = switch (p.status()) {
                    case "ĐANG ĐẤU GIÁ" -> "RUNNING";
                    case "CHỜ DUYỆT"    -> "UPCOMING";
                    case "ĐÃ BÁN"       -> "ENDED";
                    default             -> "ENDED";
                };
                allSessions.add(new SessionRecord(
                        p.id(), p.name(), p.category(),
                        user.getUsername(),
                        p.startPrice(), p.currentPrice(),
                        p.bidCount(), status,
                        LocalDateTime.now(), p.endTime()
                ));
            }
        }

        // ── Dữ liệu mẫu đã được XÓA ──────────────────────────
        // Danh sách chỉ hiển thị khi có sản phẩm thực được thêm vào
    }

    // ── Stats ─────────────────────────────────────────────────
    private void refreshStats() {
        totalLabel.setText(String.valueOf(allSessions.size()));
        runningLabel.setText(String.valueOf(
                allSessions.stream().filter(s -> "RUNNING".equals(s.status())).count()));
        upcomingLabel.setText(String.valueOf(
                allSessions.stream().filter(s -> "UPCOMING".equals(s.status())).count()));
        endedLabel.setText(String.valueOf(
                allSessions.stream().filter(s -> "ENDED".equals(s.status())).count()));
    }

    // ── Render ────────────────────────────────────────────────
    private void renderList(List<SessionRecord> list) {
        sessionListBox.getChildren().clear();
        if (list.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(60, 0, 60, 0));

            Label icon = new Label("📭");
            icon.setStyle("-fx-font-size: 48px;");

            Label msg = new Label("Chưa có phiên đấu giá nào.");
            msg.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px; -fx-font-weight: bold;");

            Label hint = new Label("Hãy thêm sản phẩm để tạo phiên đấu giá.");
            hint.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");

            emptyBox.getChildren().addAll(icon, msg, hint);
            sessionListBox.getChildren().add(emptyBox);
            return;
        }
        for (SessionRecord s : list) {
            sessionListBox.getChildren().add(buildSessionCard(s));
        }
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
        statusBadge.getStyleClass().addAll("history-badge", statusBadgeClass(s.status()));

        leftBar.getChildren().addAll(icon, statusBadge);

        // Center info
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

        // Price box
        VBox priceBox = new VBox(4);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        priceBox.setMinWidth(160);

        Label startP = new Label("Khởi điểm: " + formatVND(s.startPrice()));
        startP.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        Label curP = new Label(formatVND(s.currentPrice()));
        curP.getStyleClass().add("history-item-price");

        double progress = s.startPrice() > 0
                ? Math.min((s.currentPrice() - s.startPrice()) / s.startPrice(), 1.0)
                : 0;
        ProgressBar bar = new ProgressBar(progress);
        bar.setPrefWidth(150);
        bar.setStyle("-fx-accent: #10b981; -fx-background-color: #1e293b;");

        Label progressLabel = new Label(String.format("+%.1f%%",
                s.startPrice() > 0
                ? (s.currentPrice() - s.startPrice()) / s.startPrice() * 100
                : 0));
        progressLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px;");

        priceBox.getChildren().addAll(startP, curP, bar, progressLabel);

        // Action buttons
        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);
        actions.setMinWidth(110);

        Button viewBtn = new Button("👁 Xem chi tiết");
        viewBtn.getStyleClass().add("btn-secondary");
        viewBtn.setOnAction(e -> handleViewSession(s));

        Button joinBtn = new Button("⚡ Tham gia");
        joinBtn.getStyleClass().add("btn-primary");
        joinBtn.setDisable(!"RUNNING".equals(s.status()));
        joinBtn.setOnAction(e -> handleJoinSession(s));

        actions.getChildren().addAll(viewBtn, joinBtn);
        card.getChildren().addAll(leftBar, info, priceBox, actions);
        return card;
    }

    // ── Tab handlers ──────────────────────────────────────────
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
                if (status != null && !status.isEmpty()
                        && !"Tất cả".equals(status)
                        && !status.equals(s.status())) return false;
                if (category != null && !category.isEmpty()
                        && !"Tất cả".equals(category)
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

    private void handleJoinSession(SessionRecord s) {
        try { HelloApplication.showLiveAuctionView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleBack() {
        try { HelloApplication.showMainView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // ── Helpers ───────────────────────────────────────────────
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
    private String formatVND(double v) {
        return String.format("₫ %,.0f", v);
    }
}
