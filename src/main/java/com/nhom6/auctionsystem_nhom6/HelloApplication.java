package com.nhom6.auctionsystem_nhom6;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.Parent;

public class HelloApplication extends Application {

    private static Stage  primaryStage;
    private static String pendingCategory = null;
    private static String pendingKeyword  = null;   // ← THÊM MỚI

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        Image icon = new Image(
                getClass().getResourceAsStream(
                        "/com/nhom6/auctionsystem_nhom6/images/logo.png"));
        stage.getIcons().add(icon);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/nhom6/auctionsystem_nhom6/login-view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(
                getClass().getResource(
                        "/com/nhom6/auctionsystem_nhom6/styles/main.css")
                        .toExternalForm());

        stage.setTitle("AuctionSys – Hệ thống Đấu giá");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    // =========================================================
    // VIEWS
    // =========================================================
    public static void showMainView() throws Exception {
        load("main-view.fxml", "AuctionSys – Trang chủ");
    }

    public static void showLoginView() throws Exception {
        load("login-view.fxml", "AuctionSys – Đăng nhập");
    }

    public static void showRegisterView() throws Exception {
        load("register-view.fxml", "AuctionSys – Đăng ký tài khoản");
    }

    public static void showProfileView() throws Exception {
        load("profile-view.fxml", "AuctionSys – Hồ sơ cá nhân");
    }

    public static void showHistoryView() throws Exception {
        load("history-view.fxml", "AuctionSys – Lịch sử");
    }

    public static void showMyProductsView() throws Exception {
        load("my-products-view.fxml", "AuctionSys – Sản phẩm đăng bán");
    }

    public static void showWalletView() throws Exception {
        load("wallet-view.fxml", "AuctionSys – Ví & Giao dịch");
    }

    public static void showAuctionListView() throws Exception {
        pendingCategory = null;
        pendingKeyword  = null;   // ← reset khi xem toàn bộ
        load("auction-list-view.fxml", "AuctionSys – Danh sách phiên đấu giá");
    }

    public static void showAuctionListByCategory(String category) throws Exception {
        pendingCategory = category;
        pendingKeyword  = null;   // ← reset keyword khi lọc theo category
        load("auction-list-view.fxml", "AuctionSys – " + category);
    }

    // ← THÊM MỚI: tìm kiếm theo từ khóa
    public static void showAuctionListByKeyword(String keyword) throws Exception {
        pendingKeyword  = keyword;
        pendingCategory = null;   // reset category khi tìm theo keyword
        load("auction-list-view.fxml", "AuctionSys – Tìm kiếm: " + keyword);
    }

    public static void showRatingView() throws Exception {
        load("rating-view.fxml", "AuctionSys – Đánh giá");
    }

    public static void showHelpView() throws Exception {
        load("help-view.fxml", "AuctionSys – Trợ giúp");
    }

    public static void showProductManagementView() throws Exception {
        load("product-management-view.fxml", "AuctionSys – Quản lý sản phẩm");
    }

    public static void showLiveAuctionView() throws Exception {
        load("live-auction-view.fxml", "AuctionSys – Đấu giá trực tiếp");
    }

    public static void showAuctionSessionHistoryView() throws Exception {
        load("auction-session-history-view.fxml",
                "AuctionSys – Lịch sử phiên đấu giá");
    }

    // ← THÊM MỚI: màn hình Admin
    public static void showAdminView() throws Exception {
        load("admin-view.fxml", "AuctionSys – Quản trị hệ thống");
    }

    // =========================================================
    // PENDING CATEGORY / KEYWORD
    // =========================================================
    public static String getPendingCategory() {
        String cat = pendingCategory;
        pendingCategory = null;
        return cat;
    }

    // ← THÊM MỚI: getter cho keyword (gọi trong AuctionListController)
    public static String getPendingKeyword() {
        String kw = pendingKeyword;
        pendingKeyword = null;
        return kw;
    }

    // =========================================================
    // HELPER
    // =========================================================
    private static void load(String fxml, String title) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource(
                        "/com/nhom6/auctionsystem_nhom6/" + fxml));
        Parent root = loader.load();
        primaryStage.getScene().setRoot(root);
        primaryStage.setTitle(title);
    }

    public static Stage getPrimaryStage() { return primaryStage; }
}
