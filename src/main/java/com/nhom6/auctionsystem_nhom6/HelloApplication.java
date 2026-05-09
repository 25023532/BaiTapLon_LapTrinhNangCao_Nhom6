package com.nhom6.auctionsystem_nhom6;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/nhom6/auctionsystem_nhom6/login-view.fxml"));
        BorderPane root = loader.load();
        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(
                getClass().getResource("/com/nhom6/auctionsystem_nhom6/styles/main.css")
                        .toExternalForm());
        stage.setTitle("AuctionSys – Hệ thống Đấu giá");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void showMainView() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource(
                        "/com/nhom6/auctionsystem_nhom6/main-view.fxml"));
        BorderPane root = loader.load();
        primaryStage.getScene().setRoot(root);
        primaryStage.setTitle("AuctionSys – Trang chủ");
    }

    public static void showLoginView() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource(
                        "/com/nhom6/auctionsystem_nhom6/login-view.fxml"));
        BorderPane root = loader.load();
        primaryStage.getScene().setRoot(root);
        primaryStage.setTitle("AuctionSys – Đăng nhập");
    }

    // ← MỚI: mở màn hình đăng ký
    public static void showRegisterView() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                HelloApplication.class.getResource(
                        "/com/nhom6/auctionsystem_nhom6/register-view.fxml"));
        BorderPane root = loader.load();
        primaryStage.getScene().setRoot(root);
        primaryStage.setTitle("AuctionSys – Đăng ký tài khoản");
    }

    public static Stage getPrimaryStage() { return primaryStage; }
}
