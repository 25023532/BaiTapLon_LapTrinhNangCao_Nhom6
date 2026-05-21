package com.nhom6.auctionsystem_nhom6.controller;

import com.nhom6.auctionsystem_nhom6.AppContext;
import com.nhom6.auctionsystem_nhom6.HelloApplication;
import com.nhom6.auctionsystem_nhom6.ServerConnection;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.user.User;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller cho màn hình "Lịch sử phiên đấu giá".
 * File FXML: auction-session-history-view.fxml
 */
public class AuctionSessionHistoryController {

    // ── Stats ────────────────────────────────────────────────
    @FXML private Label totalSessionsLabel;
    @FXML private Label wonSessionsLabel;
    @FXML private Label totalSpentLabel;
    @FXML private Label totalSoldLabel;     // chỉ hiện với SELLER

    @FXML private Label totalSessionsDesc;
    @FXML private Label wonSessionsDesc;
    @FXML private Label totalSpentDesc;
    @FXML private Label totalSoldDesc;

    // ── Filter ───────────────────────────────────────────────
    @FXML private TextField              searchField;
    @FXML private ComboBox<String>       resultFilter;
    @FXML private ComboBox<String>       roleFilter;

    // ── List ─────────────────────────────────────────────────
    @FXML private VBox sessionListBox;

    // ── Data ─────────────────────────────────────────────────
    private boolean isSeller;
    private List<AppContext.AuctionSessionRecord> allRecords;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("HH:mm  dd/MM/yyyy");

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        User user = AppContext.getCurrentUser();
        isSeller  = "SELLER".equalsIgnoreCase(user.getRole());

        // ── Nhãn theo role ────────────────────────────────────
        if (isSeller) {
            totalSessionsDesc.setText("Tổng phiên tạo");
            wonSessionsDesc.setText("Phiên thành công");
            totalSpentDesc.setText("Doanh thu");
            if (totalSoldDesc != null) totalSoldDesc.setText("Sản phẩm đã bán");
        } else {
            totalSessionsDesc.setText("Phiên tham gia");
            wonSessionsDesc.setText("Phiên thắng giá");
            totalSpentDesc.setText("Tổng tiền đặt giá");
            if (totalSoldDesc  != null) totalSoldDesc.setVisible(false);
            if (totalSoldLabel != null) totalSoldLabel.setVisible(false);
        }

        // ── Filter options ────────────────────────────────────
        resultFilter.getItems().addAll(
                "Tất cả", "THÀNH CÔNG", "THẮNG GIÁ", "THUA GIÁ",
                "KHÔNG CÓ NGƯỜI ĐẤU", "ĐÃ HỦY");
        resultFilter.setValue("Tất cả");

        roleFilter.getItems().addAll("Tất cả", "SELLER", "BIDDER");
        roleFilter.setValue("Tất cả");

        // ── Load data ─────────────────────────────────────────
        allRecords = AppContext.getSessionHistory(user.getUsername());

        refreshStats();
        renderList(allRecords);
    }

    // =========================================================
    // STATS
    // =========================================================
    private void refreshStats() {
        long total = allRecords.size();

        long won = allRecords.stream()
                .filter(r -> "THẮNG GIÁ".equals(r.result())
                          || "THÀNH CÔNG".equals(r.result()))
                .count();

        double spent = allRecords.stream()
                .filter(r -> "BIDDER".equals(r.myRole()))
                .mapToDouble(AppContext.AuctionSessionRecord::myFinalBid)
                .sum();

        double revenue = allRecords.stream()
                .filter(r -> "SELLER".equals(r.myRole())
                          && "THÀNH CÔNG".equals(r.result()))
                .mapToDouble(AppContext.AuctionSessionRecord::finalPrice)
                .sum();

        totalSessionsLabel.setText(String.valueOf(total));
        wonSessionsLabel.setText(String.valueOf(won));
        totalSpentLabel.setText(formatVND(isSeller ? revenue : spent));
        if (totalSoldLabel != null) totalSoldLabel.setText(String.valueOf(won));
    }

    // =========================================================
    // RENDER LIST
    // =========================================================
    private void renderList(List<AppContext.AuctionSessionRecord> records) {
        sessionListBox.getChildren().clear();

        if (records.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60, 0, 60, 0));
            Label icon = new Label("🏛️");
            icon.setStyle("-fx-font-size: 48px;");
            Label msg = new Label("Chưa có phiên đấu giá nào được ghi lại.\n" +
                    "Khi phiên kết thúc, lịch sử sẽ hiển thị ở đây.");
            msg.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-text-alignment: center;");
            msg.setWrapText(true);
            msg.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            empty.getChildren().addAll(icon, msg);
            sessionListBox.getChildren().add(empty);
            return;
        }

        for (AppContext.AuctionSessionRecord r : records) {
            sessionListBox.getChildren().add(buildRow(r));
        }
    }

    private HBox buildRow(AppContext.AuctionSessionRecord r) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");
        row.setPadding(new Insets(14, 20, 14, 20));

        // Biểu tượng kết quả
        Label icon = new Label(resultIcon(r.result()));
        icon.setMinWidth(32);
        icon.setStyle("-fx-font-size: 22px;");

        // Thông tin chính
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(r.itemName());
        name.getStyleClass().add("history-item-name");

        // Dòng meta
        String winnerTxt = r.winnerName() != null
                ? "Người thắng: " + r.winnerName() : "Không có người thắng";
        Label meta = new Label(
                "Người bán: " + r.sellerName()
                + "   •   " + winnerTxt
                + "   •   " + r.endTime().format(DT_FMT)
                + "   •   Lượt bid: " + r.totalBids()
                + "   •   Role: " + r.myRole());
        meta.getStyleClass().add("history-item-meta");
        meta.setWrapText(true);

        info.getChildren().addAll(name, meta);

        // Giá chốt + badge
        VBox priceBox = new VBox(4);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        priceBox.setMinWidth(130);

        Label priceLabel = new Label(formatVND(r.finalPrice()));
        priceLabel.getStyleClass().add("history-item-price");

        Label badge = new Label(r.result());
        badge.getStyleClass().addAll("history-badge", badgeStyle(r.result()));

        // Nếu là bidder, hiển thị giá mình đã đặt
        if ("BIDDER".equals(r.myRole()) && r.myFinalBid() > 0) {
            Label myBid = new Label("Giá tôi đặt: " + formatVND(r.myFinalBid()));
            myBid.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
            priceBox.getChildren().addAll(priceLabel, myBid, badge);
        } else {
            priceBox.getChildren().addAll(priceLabel, badge);
        }

        // Nút chi tiết
        Button detailBtn = new Button("Chi tiết →");
        detailBtn.getStyleClass().add("btn-secondary");
        detailBtn.setOnAction(e -> showDetail(r));

        row.getChildren().addAll(icon, info, priceBox, detailBtn);
        return row;
    }

    // =========================================================
    // FILTERS
    // =========================================================
    @FXML private void handleFilter() { applyFilters(); }
    @FXML private void handleSearch() { applyFilters(); }

    private void applyFilters() {
        String keyword = searchField.getText().trim().toLowerCase();
        String result  = resultFilter.getValue();
        String role    = roleFilter.getValue();

        List<AppContext.AuctionSessionRecord> filtered = allRecords.stream()
                .filter(r -> {
                    if (result != null && !"Tất cả".equals(result)
                            && !result.equals(r.result())) return false;
                    if (role != null && !"Tất cả".equals(role)
                            && !role.equals(r.myRole())) return false;
                    if (!keyword.isEmpty()) {
                        boolean match = r.itemName().toLowerCase().contains(keyword)
                                || r.sellerName().toLowerCase().contains(keyword)
                                || (r.winnerName() != null
                                    && r.winnerName().toLowerCase().contains(keyword))
                                || r.sessionId().toLowerCase().contains(keyword);
                        if (!match) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        renderList(filtered);
    }

    // =========================================================
    // DETAIL POPUP
    // =========================================================
    private void showDetail(AppContext.AuctionSessionRecord r) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi tiết phiên đấu giá");
        alert.setHeaderText(r.itemName());
        alert.setContentText(
                "Mã phiên      : " + r.sessionId()              + "\n" +
                "Người bán     : " + r.sellerName()             + "\n" +
                "Giá khởi điểm : " + formatVND(r.startPrice())  + "\n" +
                "Giá chốt      : " + formatVND(r.finalPrice())  + "\n" +
                "Người thắng   : " + (r.winnerName() != null
                                        ? r.winnerName() : "—") + "\n" +
                "Tổng lượt bid : " + r.totalBids()              + "\n" +
                "Bắt đầu       : " + r.startTime().format(DT_FMT) + "\n" +
                "Kết thúc      : " + r.endTime().format(DT_FMT)   + "\n" +
                "Role của tôi  : " + r.myRole()                 + "\n" +
                (r.myFinalBid() > 0
                    ? "Giá tôi đặt   : " + formatVND(r.myFinalBid()) + "\n" : "") +
                "Kết quả       : " + r.result()
        );
        alert.showAndWait();
    }

    // =========================================================
    // BACK
    // =========================================================
    @FXML private void handleBack() {
        try { HelloApplication.showMainView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private String resultIcon(String result) {
        return switch (result) {
            case "THÀNH CÔNG"          -> "✅";
            case "THẮNG GIÁ"           -> "🏆";
            case "THUA GIÁ"            -> "❌";
            case "KHÔNG CÓ NGƯỜI ĐẤU" -> "😶";
            case "ĐÃ HỦY"              -> "🚫";
            default                     -> "📋";
        };
    }

    private String badgeStyle(String result) {
        return switch (result) {
            case "THÀNH CÔNG", "THẮNG GIÁ" -> "badge-success";
            case "THUA GIÁ"                -> "badge-danger";
            case "KHÔNG CÓ NGƯỜI ĐẤU"     -> "badge-warn";
            case "ĐÃ HỦY"                  -> "badge-neutral";
            default                         -> "badge-neutral";
        };
    }

    private String formatVND(double v) {
        return String.format("₫ %,.0f", v);
    }
}
