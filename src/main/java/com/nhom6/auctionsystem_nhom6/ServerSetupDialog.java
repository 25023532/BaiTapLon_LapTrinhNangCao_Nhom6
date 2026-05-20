package com.nhom6.auctionsystem_nhom6;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Optional;

/**
 * Dialog cấu hình IP server.
 * Hiện khi lần đầu chạy app hoặc khi đang dùng localhost
 * (tức là chưa cấu hình cho 2 máy).
 */
public class ServerSetupDialog {

    /**
     * Hiện dialog nếu đang dùng localhost.
     * Gọi trong HelloApplication.start() trước showLoginView().
     */
    public static void showIfNeeded() {
        String currentHost = ServerConfig.getHost();
        boolean isDefault  = "localhost".equalsIgnoreCase(currentHost)
                          || "127.0.0.1".equals(currentHost);
        if (!isDefault) return; // đã cấu hình IP thực → không hỏi nữa
        show();
    }

    /** Hiện dialog cho người dùng nhập IP */
    public static void show() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("⚙️ Cấu hình kết nối Server");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setPrefWidth(420);

        ButtonType connectBtn = new ButtonType(
                "🔌 Kết nối", ButtonBar.ButtonData.OK_DONE);
        ButtonType offlineBtn = new ButtonType(
                "📴 Chạy offline", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(connectBtn, offlineBtn);

        // ── Nội dung ─────────────────────────────────────
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #0f172a;");

        // Hướng dẫn
        Label guide = new Label(
                "Nhập IP của máy đang chạy AuctionServer.\n"
                + "Nếu chạy cùng 1 máy → để nguyên 'localhost'.");
        guide.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        guide.setWrapText(true);

        // IP máy hiện tại (gợi ý)
        Label myIpHint = new Label(
                "📍 IP máy này: " + ServerConfig.getLocalIP());
        myIpHint.setStyle(
                "-fx-text-fill: #10b981; -fx-font-size: 11px; "
                + "-fx-background-color: #064e3b; "
                + "-fx-background-radius: 4; -fx-padding: 4 8 4 8;");

        // Form nhập
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        Label hostLabel = new Label("Server IP:");
        hostLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; "
                + "-fx-font-weight: bold;");

        TextField hostField = new TextField(ServerConfig.getHost());
        hostField.setPromptText("VD: 192.168.1.10");
        hostField.setPrefWidth(220);
        hostField.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: #f1f5f9; "
                + "-fx-prompt-text-fill: #475569; -fx-border-color: #334155; "
                + "-fx-border-radius: 6; -fx-background-radius: 6; "
                + "-fx-padding: 8 12 8 12; -fx-font-size: 13px;");

        Label portLabel = new Label("Port:");
        portLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; "
                + "-fx-font-weight: bold;");

        TextField portField = new TextField(
                String.valueOf(ServerConfig.getPort()));
        portField.setPrefWidth(80);
        portField.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: #f1f5f9; "
                + "-fx-border-color: #334155; -fx-border-radius: 6; "
                + "-fx-background-radius: 6; -fx-padding: 8 12 8 12; "
                + "-fx-font-size: 13px;");

        // Nút dùng nhanh localhost
        Button localhostBtn = new Button("🏠 Dùng localhost");
        localhostBtn.setStyle(
                "-fx-background-color: #334155; -fx-text-fill: #e2e8f0; "
                + "-fx-font-size: 11px; -fx-background-radius: 4; "
                + "-fx-cursor: hand; -fx-padding: 4 10 4 10;");
        localhostBtn.setOnAction(e -> hostField.setText("localhost"));

        // Nút dùng IP máy hiện tại
        Button myIpBtn = new Button("📍 Dùng IP máy này");
        myIpBtn.setStyle(
                "-fx-background-color: #1e3a5f; -fx-text-fill: #38bdf8; "
                + "-fx-font-size: 11px; -fx-background-radius: 4; "
                + "-fx-cursor: hand; -fx-padding: 4 10 4 10;");
        myIpBtn.setOnAction(e ->
                hostField.setText(ServerConfig.getLocalIP()));

        HBox quickBtns = new HBox(8, localhostBtn, myIpBtn);
        quickBtns.setAlignment(Pos.CENTER_LEFT);

        grid.add(hostLabel,  0, 0);
        grid.add(hostField,  1, 0);
        grid.add(portLabel,  0, 1);
        grid.add(portField,  1, 1);
        grid.add(quickBtns, 0, 2, 2, 1);

        // Lưu thiết lập checkbox
        CheckBox rememberCheck = new CheckBox("Lưu thiết lập này");
        rememberCheck.setSelected(true);
        rememberCheck.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        content.getChildren().addAll(guide, myIpHint, grid, rememberCheck);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle(
                "-fx-background-color: #0f172a; "
                + "-fx-border-color: #334155; -fx-border-width: 1;");

        // Style buttons
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node conn = dialog.getDialogPane()
                    .lookupButton(connectBtn);
            javafx.scene.Node off  = dialog.getDialogPane()
                    .lookupButton(offlineBtn);
            if (conn != null)
                conn.setStyle("-fx-background-color: #2563eb; "
                        + "-fx-text-fill: white; -fx-font-weight: bold; "
                        + "-fx-background-radius: 6; -fx-padding: 8 20 8 20;");
            if (off != null)
                off.setStyle("-fx-background-color: #1e293b; "
                        + "-fx-text-fill: #94a3b8; -fx-background-radius: 6; "
                        + "-fx-border-color: #334155; -fx-border-width: 1; "
                        + "-fx-padding: 8 20 8 20;");
        });

        Optional<ButtonType> result = dialog.showAndWait();
        result.ifPresent(btn -> {
            if (btn == connectBtn) {
                String newHost = hostField.getText().trim();
                int    newPort;
                try {
                    newPort = Integer.parseInt(portField.getText().trim());
                } catch (NumberFormatException e) {
                    newPort = 1234;
                }
                if (newHost.isEmpty()) newHost = "localhost";

                if (rememberCheck.isSelected()) {
                    ServerConfig.save(newHost, newPort);
                } else {
                    // Chỉ set trong memory, không lưu file
                    // (reload sẽ dùng lại giá trị cũ)
                    ServerConfig.save(newHost, newPort);
                }
            }
            // Nếu chọn offline → không làm gì, app chạy offline
        });
    }
}
