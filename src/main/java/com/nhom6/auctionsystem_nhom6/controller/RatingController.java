package com.nhom6.auctionsystem_nhom6.controller;

import com.nhom6.auctionsystem_nhom6.AppContext;
import com.nhom6.auctionsystem_nhom6.HelloApplication;
import com.nhom6.auctionsystem_nhom6.ServerConnection;
import com.nhom6.auctionsystem_nhom6.NotificationService;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.user.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class RatingController {

    // =========================================================
    // HEADER
    // =========================================================
    @FXML private Label userAvatarLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    // =========================================================
    // SUMMARY STATS
    // =========================================================
    @FXML private Label avgRatingLabel;
    @FXML private Label avgStarsLabel;
    @FXML private Label totalReviewsLabel;

    @FXML private Label count10Label;
    @FXML private Label count9Label;
    @FXML private Label count8Label;
    @FXML private Label count7Label;
    @FXML private Label count6Label;
    @FXML private Label count5Label;
    @FXML private Label count4Label;
    @FXML private Label count3Label;
    @FXML private Label count2Label;
    @FXML private Label count1Label;

    @FXML private ProgressBar bar10;
    @FXML private ProgressBar bar9;
    @FXML private ProgressBar bar8;
    @FXML private ProgressBar bar7;
    @FXML private ProgressBar bar6;
    @FXML private ProgressBar bar5;
    @FXML private ProgressBar bar4;
    @FXML private ProgressBar bar3;
    @FXML private ProgressBar bar2;
    @FXML private ProgressBar bar1;

    // =========================================================
    // WRITE REVIEW
    // =========================================================
    @FXML private HBox      starRow;
    @FXML private Label     selectedStarLabel;
    @FXML private TextArea  commentArea;
    @FXML private Label     charCountLabel;
    @FXML private Label     writeResultLabel;

    // =========================================================
    // FILTER
    // =========================================================
    @FXML private ComboBox<String> sortBox;
    @FXML private ComboBox<String> filterStarBox;
    @FXML private TextField        searchField;

    // =========================================================
    // LIST
    // =========================================================
    @FXML private VBox  reviewListBox;
    @FXML private Label reviewCountLabel;

    @FXML private Button btnSellerProducts;
    @FXML private Button btnAdminProducts;

    // =========================================================
    // DATA — dùng AppContext thay vì static list
    // =========================================================
    public record Review(
        String id,
        String username,
        String avatar,
        int    stars,
        String comment,
        LocalDateTime time,
        int    likes
    ) {}

    private int    selectedStars = 0;
    private String username;

    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("HH:mm  dd/MM/yyyy");
    private static final int MAX_CHARS = 500;

    // =========================================================
    // HELPER — chuyển RatingRecord → Review
    // =========================================================
    private List<Review> toReviewList(java.util.Collection<AppContext.RatingRecord> recs) {
        return recs.stream()
            .map(r -> new Review(r.id(), r.username(), r.avatar(),
                r.stars(), r.comment(), r.time(), r.likes()))
            .collect(Collectors.toList());
    }

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        User user = AppContext.getCurrentUser();
        if (user == null) return;
        applyRoleMenu(user);

        username = user.getUsername();
        userNameLabel.setText(username);
        userRoleLabel.setText(user.getRole());
        userAvatarLabel.setText(username.substring(0, Math.min(2, username.length())).toUpperCase());

        sortBox.getItems().addAll("Mới nhất", "Cũ nhất", "Sao cao nhất", "Sao thấp nhất", "Nhiều like nhất");
        sortBox.getSelectionModel().selectFirst();

        filterStarBox.getItems().addAll(
            "Tất cả", "10 ★", "9 ★ trở lên", "8 ★ trở lên",
            "7 ★ trở lên", "6 ★ trở lên", "5 ★ trở xuống",
            "4 ★ trở xuống", "3 ★ trở xuống"
        );
        filterStarBox.getSelectionModel().selectFirst();

        buildStarButtons();

        commentArea.textProperty().addListener((obs, o, n) -> {
            int len = n.length();
            if (len > MAX_CHARS) {
                commentArea.setText(o);
                return;
            }
            charCountLabel.setText(len + "/" + MAX_CHARS);
        });

        writeResultLabel.setVisible(false);
        refreshStats();
        renderReviews(toReviewList(AppContext.getRatings()));
    }

    // =========================================================
    // STAR BUTTONS
    // =========================================================
    private void buildStarButtons() {
        starRow.getChildren().clear();
        starRow.setSpacing(6);
        starRow.setAlignment(Pos.CENTER_LEFT);
        for (int i = 1; i <= 10; i++) {
            final int star = i;
            Button btn = new Button("★");
            btn.setPrefWidth(36);
            btn.setPrefHeight(36);
            btn.setStyle(starBtnStyle(false));
            btn.setOnMouseEntered(e -> highlightStars(star));
            btn.setOnMouseExited(e  -> highlightStars(selectedStars));
            btn.setOnAction(e -> {
                selectedStars = star;
                highlightStars(star);
                selectedStarLabel.setText("Bạn chọn: " + star + " ★");
                selectedStarLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold;");
            });
            starRow.getChildren().add(btn);
        }
    }

    private void highlightStars(int upTo) {
        for (int i = 0; i < starRow.getChildren().size(); i++) {
            Button btn = (Button) starRow.getChildren().get(i);
            btn.setStyle(starBtnStyle(i < upTo));
        }
    }

    private String starBtnStyle(boolean active) {
        if (active) return """
            -fx-background-color: transparent;
            -fx-text-fill: #fbbf24;
            -fx-font-size: 22px;
            -fx-cursor: hand;
            -fx-padding: 0;
            -fx-border-width: 0;
            """;
        return """
            -fx-background-color: transparent;
            -fx-text-fill: #334155;
            -fx-font-size: 22px;
            -fx-cursor: hand;
            -fx-padding: 0;
            -fx-border-width: 0;
            """;
    }

    // =========================================================
    // SUBMIT REVIEW
    // =========================================================
    @FXML
    private void handleSubmitReview() {
        writeResultLabel.setVisible(false);

        if (selectedStars == 0) {
            showWriteResult("⚠ Vui lòng chọn số sao.", false);
            return;
        }
        String comment = commentArea.getText().trim();

        AppContext.RatingRecord rec = new AppContext.RatingRecord(
            UUID.randomUUID().toString(),
            username,
            username.substring(0, Math.min(2, username.length())).toUpperCase(),
            selectedStars,
            comment,
            LocalDateTime.now(),
            0
        );

        // Lưu vào AppContext và gửi lên server
        AppContext.addRating(rec);
        ServerConnection conn = ServerConnection.getInstance();
        if (conn.isConnected()) conn.sendAddRating(rec);

        // Reset form
        selectedStars = 0;
        highlightStars(0);
        selectedStarLabel.setText("Chưa chọn sao");
        selectedStarLabel.setStyle("-fx-text-fill: #999999;");
        commentArea.clear();
        charCountLabel.setText("0/" + MAX_CHARS);

        showWriteResult("✅ Cảm ơn bạn đã đánh giá!", true);
        refreshStats();
        applyFilters();

        NotificationService.getInstance().add(
            NotificationService.Type.SYSTEM,
            "⭐ Đánh giá của bạn đã được ghi nhận",
            "Bạn đã đánh giá " + rec.stars() + " sao"
        );
    }

    // =========================================================
    // STATS
    // =========================================================
    private void refreshStats() {
        List<Review> allReviews = toReviewList(AppContext.getRatings());

        if (allReviews.isEmpty()) {
            avgRatingLabel.setText("—");
            avgStarsLabel.setText("Chưa có đánh giá");
            totalReviewsLabel.setText("0 đánh giá");
            return;
        }

        double avg = allReviews.stream()
            .mapToInt(Review::stars).average().orElse(0);
        avgRatingLabel.setText(String.format("%.1f", avg));
        avgStarsLabel.setText(starString((int) Math.round(avg)));
        totalReviewsLabel.setText(allReviews.size() + " đánh giá");

        int total = allReviews.size();
        int[] counts = new int[11];
        for (Review r : allReviews) counts[r.stars()]++;

        Label[] labels = {null, count1Label, count2Label, count3Label,
            count4Label, count5Label, count6Label, count7Label,
            count8Label, count9Label, count10Label};
        ProgressBar[] bars = {null, bar1, bar2, bar3, bar4, bar5,
            bar6, bar7, bar8, bar9, bar10};

        for (int i = 1; i <= 10; i++) {
            if (labels[i] != null) labels[i].setText(String.valueOf(counts[i]));
            if (bars[i]   != null) bars[i].setProgress(total > 0 ? (double) counts[i] / total : 0);
        }
    }

    private String starString(int stars) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 10; i++)
            sb.append(i <= stars ? "★" : "☆");
        return sb.toString();
    }

    // =========================================================
    // FILTER & SORT
    // =========================================================
    @FXML private void handleFilter() { applyFilters(); }
    @FXML private void handleSearch() { applyFilters(); }

    private void applyFilters() {
        String keyword = searchField.getText().trim().toLowerCase();
        String sortVal = sortBox.getValue();
        String starVal = filterStarBox.getValue();

        List<Review> filtered = toReviewList(AppContext.getRatings()).stream()
            .filter(r -> {
                if (!keyword.isEmpty()
                    && !r.comment().toLowerCase().contains(keyword)
                    && !r.username().toLowerCase().contains(keyword))
                    return false;
                if (starVal != null) {
                    return switch (starVal) {
                        case "10 ★"          -> r.stars() == 10;
                        case "9 ★ trở lên"   -> r.stars() >= 9;
                        case "8 ★ trở lên"   -> r.stars() >= 8;
                        case "7 ★ trở lên"   -> r.stars() >= 7;
                        case "6 ★ trở lên"   -> r.stars() >= 6;
                        case "5 ★ trở xuống" -> r.stars() <= 5;
                        case "4 ★ trở xuống" -> r.stars() <= 4;
                        case "3 ★ trở xuống" -> r.stars() <= 3;
                        default              -> true;
                    };
                }
                return true;
            })
            .collect(Collectors.toList());

        if (sortVal != null) {
            filtered.sort(switch (sortVal) {
                case "Cũ nhất"         -> Comparator.comparing(Review::time);
                case "Sao cao nhất"    -> Comparator.comparingInt(Review::stars).reversed();
                case "Sao thấp nhất"   -> Comparator.comparingInt(Review::stars);
                case "Nhiều like nhất" -> Comparator.comparingInt(Review::likes).reversed();
                default                -> Comparator.comparing(Review::time).reversed();
            });
        }

        renderReviews(filtered);
    }

    // =========================================================
    // RENDER
    // =========================================================
    private void renderReviews(List<Review> list) {
        reviewListBox.getChildren().clear();
        reviewCountLabel.setText(list.size() + " đánh giá");

        if (list.isEmpty()) {
            Label empty = new Label("Chưa có đánh giá nào phù hợp.");
            empty.setStyle("-fx-text-fill: #999999; -fx-font-size: 14px; -fx-padding: 40 0 40 0;");
            reviewListBox.getChildren().add(empty);
            return;
        }

        for (Review r : list)
            reviewListBox.getChildren().add(buildReviewCard(r));
    }

    private VBox buildReviewCard(Review r) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("""
                -fx-background-color: #FFFFFF;
                -fx-background-radius: 0;
                -fx-border-width: 3;
                """ + starBorderColor(r.stars()));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label(r.avatar());
        avatar.setStyle("""
                -fx-background-color: #1F0C40;
                -fx-text-fill: #FFFFFF;
                -fx-font-weight: bold;
                -fx-font-size: 13px;
                -fx-background-radius: 0;
                -fx-border-color: #1A1A1A;
                -fx-border-width: 2;
                -fx-padding: 8 10 8 10;
                -fx-min-width: 36;
                -fx-alignment: CENTER;
                """);

        VBox nameBox = new VBox(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(r.username());
        name.setStyle("-fx-text-fill: #1F0C40; -fx-font-weight: bold; -fx-font-size: 14px;");

        if (r.username().equals(username)) {
            Label meBadge = new Label("Bạn");
            meBadge.setStyle("""
                    -fx-background-color: #FF6B35;
                    -fx-text-fill: #FFFFFF;
                    -fx-font-size: 10px;
                    -fx-font-weight: bold;
                    -fx-background-radius: 0;
                    -fx-border-color: #1A1A1A;
                    -fx-border-width: 1;
                    -fx-padding: 2 6 2 6;
                    """);
            nameRow.getChildren().addAll(name, meBadge);
        } else {
            nameRow.getChildren().add(name);
        }

        Label time = new Label(r.time().format(DT_FMT));
        time.setStyle("-fx-text-fill: #999999; -fx-font-size: 11px;");
        nameBox.getChildren().addAll(nameRow, time);

        VBox starBox = new VBox(2);
        starBox.setAlignment(Pos.CENTER_RIGHT);
        Label starDisplay = new Label(starString(r.stars()));
        starDisplay.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 14px;");
        Label starNum = new Label(r.stars() + "/10");
        starNum.setStyle("-fx-text-fill: " + starColor(r.stars()) + "; "
            + "-fx-font-weight: bold; -fx-font-size: 18px;");
        starBox.getChildren().addAll(starNum, starDisplay);

        header.getChildren().addAll(avatar, nameBox, starBox);

        Label comment = new Label(r.comment());
        comment.setStyle("-fx-text-fill: #333333; -fx-font-size: 13px; -fx-line-spacing: 2;");
        comment.setWrapText(true);

        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);

        Button likeBtn = new Button("👍  " + r.likes());
        likeBtn.setStyle("""
                -fx-background-color: #FFF8F0;
                -fx-text-fill: #666666;
                -fx-font-size: 12px;
                -fx-cursor: hand;
                -fx-border-color: #1A1A1A;
                -fx-border-radius: 0;
                -fx-border-width: 2;
                -fx-padding: 4 10 4 10;
                """);
        likeBtn.setOnAction(e -> {
            likeBtn.setText("👍  " + (r.likes() + 1));
            likeBtn.setStyle("""
                    -fx-background-color: #1F0C40;
                    -fx-text-fill: #FFFFFF;
                    -fx-font-size: 12px;
                    -fx-cursor: hand;
                    -fx-border-color: #1A1A1A;
                    -fx-border-radius: 0;
                    -fx-border-width: 2;
                    -fx-padding: 4 10 4 10;
                    """);
            likeBtn.setDisable(true);
        });

        Label starTag = new Label(starTagText(r.stars()));
        starTag.setStyle("-fx-background-color: " + starTagBg(r.stars()) + "; "
            + "-fx-text-fill: " + starTagColor(r.stars()) + "; "
            + "-fx-font-size: 11px; -fx-background-radius: 4; "
            + "-fx-padding: 3 8 3 8; -fx-font-weight: bold;");

        footer.getChildren().addAll(likeBtn, starTag);
        card.getChildren().addAll(header, comment, footer);
        return card;
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private String starColor(int stars) {
        if (stars >= 9) return "#10b981";
        if (stars >= 7) return "#fbbf24";
        if (stars >= 5) return "#f97316";
        return "#ef4444";
    }

    private String starBorderColor(int stars) {
        if (stars >= 9) return "-fx-border-color: #FF6B35;";
        if (stars >= 7) return "-fx-border-color: #1F0C40;";
        if (stars >= 5) return "-fx-border-color: #1A1A1A;";
        return "-fx-border-color: #1A1A1A;";
    }

    private String starTagText(int stars) {
        if (stars == 10) return "⭐ Xuất sắc";
        if (stars >= 9)  return "🌟 Rất tốt";
        if (stars >= 7)  return "👍 Tốt";
        if (stars >= 5)  return "😐 Bình thường";
        if (stars >= 3)  return "👎 Không tốt";
        return "💔 Rất tệ";
    }

    private String starTagBg(int stars) {
        if (stars >= 9) return "#FF6B35";
        if (stars >= 7) return "#1F0C40";
        if (stars >= 5) return "#FFF8F0";
        return "#FFF8F0";
    }

    private String starTagColor(int stars) {
        if (stars >= 9) return "#FFFFFF";
        if (stars >= 7) return "#FFFFFF";
        if (stars >= 5) return "#1A1A1A";
        return "#1A1A1A";
    }

    private void showWriteResult(String msg, boolean success) {
        writeResultLabel.setText(msg);
        writeResultLabel.setStyle(success
            ? "-fx-text-fill: #FF6B35; -fx-font-weight: bold; -fx-font-size: 13px;"
            : "-fx-text-fill: #CC0000; -fx-font-weight: bold; -fx-font-size: 13px;");
        writeResultLabel.setVisible(true);
    }

    private void applyRoleMenu(User user) {
        String role = user.getRole() == null ? "" : user.getRole().toUpperCase();
        boolean isSeller = "SELLER".equals(role);
        boolean isAdmin  = "ADMIN".equals(role);
        if (btnSellerProducts != null) { btnSellerProducts.setVisible(isSeller); btnSellerProducts.setManaged(isSeller); }
        if (btnAdminProducts != null) { btnAdminProducts.setVisible(isAdmin); btnAdminProducts.setManaged(isAdmin); }
    }

    // =========================================================
    // NAVIGATION
    // =========================================================
    @FXML private void handleBack() {
        try { HelloApplication.showMainView(); }
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

    @FXML private void handleAuctionList() {
        try { HelloApplication.showAuctionListView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleLiveAuction() {
        try { HelloApplication.showLiveAuctionView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleSellerProducts() {
        try { HelloApplication.showMyProductsView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleAdminProducts() {
        try { HelloApplication.showProductManagementView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleWallet() {
        try { HelloApplication.showWalletView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleRating() {
        applyFilters();
    }

    @FXML private void handleHelp() {
        try { HelloApplication.showHelpView(); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleCategoryDienTu() {
        try { HelloApplication.showAuctionListByCategory("Điện tử"); }
        catch (Exception e) { e.printStackTrace(); }
    }
    @FXML private void handleCategoryMayAnh() {
        try { HelloApplication.showAuctionListByCategory("Máy ảnh"); }
        catch (Exception e) { e.printStackTrace(); }
    }
    @FXML private void handleCategoryLaptop() {
        try { HelloApplication.showAuctionListByCategory("Laptop"); }
        catch (Exception e) { e.printStackTrace(); }
    }
    @FXML private void handleCategoryDienThoai() {
        try { HelloApplication.showAuctionListByCategory("Điện thoại"); }
        catch (Exception e) { e.printStackTrace(); }
    }
    @FXML private void handleCategoryDongHo() {
        try { HelloApplication.showAuctionListByCategory("Đồng hồ"); }
        catch (Exception e) { e.printStackTrace(); }
    }
    @FXML private void handleCategoryXeCo() {
        try { HelloApplication.showAuctionListByCategory("Xe cộ"); }
        catch (Exception e) { e.printStackTrace(); }
    }
}
