package com.nhom6.auctionsystem_nhom6;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(
                        "/com/nhom6/auctionsystem_nhom6/login-view.fxml"));
        BorderPane root = loader.load();
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

    // ── Existing views ────────────────────────────────────────
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

    // ── NEW views ─────────────────────────────────────────────
    public static void showAuctionListView() throws Exception {
        load("auction-list-view.fxml", "AuctionSys – Danh sách phiên đấu giá");
    }

    public static void showProductManagementView() throws Exception {
        load("product-management-view.fxml", "AuctionSys – Quản lý sản phẩm");
    }

    public static void showLiveAuctionView() throws Exception {
        load("live-auction-view.fxml", "AuctionSys – Đấu giá trực tiếp");
    }

    // ── Helper ────────────────────────────────────────────────
    private static void load(String fxml, String title) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource(
                        "/com/nhom6/auctionsystem_nhom6/" + fxml));
        BorderPane root = loader.load();
        primaryStage.getScene().setRoot(root);
        primaryStage.setTitle(title);
    }

    public static Stage getPrimaryStage() { return primaryStage; }
}
