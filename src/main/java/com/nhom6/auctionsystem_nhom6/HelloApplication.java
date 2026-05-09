package com.nhom6.auctionsystem_nhom6;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class HelloApplication extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        try {
            primaryStage = stage;

            // Load login-view.fxml
            Parent root = loadFXML("login-view.fxml");

            Scene scene = new Scene(root, 1280, 800);

            // Load CSS
            URL cssUrl = getClass().getResource(
                    "/com/nhom6/auctionsystem_nhom6/styles/main.css"
            );

            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.out.println("Không tìm thấy file CSS.");
            }

            stage.setTitle("AuctionSys – Hệ thống Đấu giá");
            stage.setScene(scene);

            stage.setMinWidth(900);
            stage.setMinHeight(600);

            stage.show();

        } catch (Exception e) {

            System.out.println("========== LỖI KHỞI ĐỘNG ==========");
            e.printStackTrace();

            Throwable cause = e.getCause();
            while (cause != null) {
                System.out.println("CAUSE: " + cause.getMessage());
                cause.printStackTrace();
                cause = cause.getCause();
            }
        }
    }

    // =========================
    // LOAD FXML AN TOÀN
    // =========================
    private static Parent loadFXML(String fileName) throws Exception {

        String path = "/com/nhom6/auctionsystem_nhom6/" + fileName;

        URL fxmlUrl = HelloApplication.class.getResource(path);

        if (fxmlUrl == null) {
            throw new RuntimeException(
                    "Không tìm thấy file FXML: " + path
            );
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);

        return loader.load();
    }

    // =========================
    // CHUYỂN SANG MAIN VIEW
    // =========================
    public static void showMainView() {
        try {

            Parent root = loadFXML("main-view.fxml");

            primaryStage.getScene().setRoot(root);

            primaryStage.setTitle("AuctionSys – Trang chủ");

        } catch (Exception e) {

            System.out.println("Lỗi mở main-view.fxml");
            e.printStackTrace();
        }
    }

    // =========================
    // CHUYỂN SANG LOGIN VIEW
    // =========================
    public static void showLoginView() {
        try {

            Parent root = loadFXML("login-view.fxml");

            primaryStage.getScene().setRoot(root);

            primaryStage.setTitle("AuctionSys – Đăng nhập");

        } catch (Exception e) {

            System.out.println("Lỗi mở login-view.fxml");
            e.printStackTrace();
        }
    }

    // =========================
    // CHUYỂN SANG REGISTER VIEW
    // =========================
    public static void showRegisterView() {
        try {

            Parent root = loadFXML("register-view.fxml");

            primaryStage.getScene().setRoot(root);

            primaryStage.setTitle("AuctionSys – Đăng ký");

        } catch (Exception e) {

            System.out.println("Lỗi mở register-view.fxml");
            e.printStackTrace();
        }
    }

    // =========================
    // CHUYỂN SANG PROFILE VIEW
    // =========================
    public static void showProfileView() {
        try {

            Parent root = loadFXML("profile-view.fxml");

            primaryStage.getScene().setRoot(root);

            primaryStage.setTitle("AuctionSys – Hồ sơ cá nhân");

        } catch (Exception e) {

            System.out.println("Lỗi mở profile-view.fxml");
            e.printStackTrace();
        }
    }

    // =========================
    // GET PRIMARY STAGE
    // =========================
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch();
    }
}
