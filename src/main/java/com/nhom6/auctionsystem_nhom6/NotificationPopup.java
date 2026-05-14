package com.nhom6.auctionsystem_nhom6;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Xây dựng popup dropdown thông báo.
 * Màu sắc khớp với stylesheet hiện tại (#1e293b, #334155, v.v.)
 */
public class NotificationPopup {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm · dd/MM");

    // ─── Tạo và hiện popup ───────────────────────────────────
    public static Popup show(Button bellButton,
                             List<NotificationManager.NotifItem> items) {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setAutoFix(true);

        VBox root = buildRoot(items, popup);
        popup.getContent().add(root);

        // Căn vị trí: ngay dưới nút chuông, lệch trái để khung nằm trong màn hình
        Window window = bellButton.getScene().getWindow();
        var b = bellButton.localToScreen(bellButton.getBoundsInLocal());
        popup.show(window,
                b.getMaxX() - 340,
                b.getMaxY() + 8);

        return popup;
    }

    // ─── Root container ──────────────────────────────────────
    private static VBox buildRoot(List<NotificationManager.NotifItem> items,
                                  Popup popup) {
        VBox root = new VBox(0);
        root.setPrefWidth(340);
        root.setMaxWidth(340);
        root.setStyle("""
                -fx-background-color: #1e293b;
                -fx-background-radius: 12;
                -fx-border-color: #334155;
                -fx-border-radius: 12;
                -fx-border-width: 1;
                -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.55),20,0,0,6);
                """);

        root.getChildren().addAll(
                buildHeader(items, popup),
                buildList(items)
        );
        return root;
    }

    // ─── Header ──────────────────────────────────────────────
    private static HBox buildHeader(List<NotificationManager.NotifItem> items,
                                    Popup popup) {
        HBox header = new HBox();
        header.setPadding(new Insets(13, 16, 11, 16));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("""
                -fx-background-color: #0f172a;
                -fx-background-radius: 12 12 0 0;
                -fx-border-color: transparent transparent #334155 transparent;
                -fx-border-width: 1;
                """);

        Label title = new Label("🔔  Thông báo");
        title.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 14px; -fx-font-weight: bold;");
        HBox.setHgrow(title, Priority.ALWAYS);

        // Số unread
        long unread = items.stream().filter(i -> !i.read).count();
        if (unread > 0) {
            Label badge = new Label(String.valueOf(unread));
            badge.setStyle("""
                    -fx-background-color: #2563eb;
                    -fx-text-fill: white;
                    -fx-font-size: 10px;
                    -fx-font-weight: bold;
                    -fx-background-radius: 10;
                    -fx-padding: 1 6 1 6;
                    """);
            header.getChildren().addAll(title, badge);
        } else {
            header.getChildren().add(title);
        }

        Button close = new Button("✕");
        close.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #64748b;
                -fx-font-size: 13px;
                -fx-cursor: hand;
                -fx-padding: 2 6 2 6;
                -fx-background-radius: 4;
                """);
        close.setOnMouseEntered(e -> close.setStyle(close.getStyle()
                .replace("-fx-background-color: transparent", "-fx-background-color: #334155")));
        close.setOnMouseExited(e -> close.setStyle(close.getStyle()
                .replace("-fx-background-color: #334155", "-fx-background-color: transparent")));
        close.setOnAction(e -> popup.hide());
        header.getChildren().add(close);

        return header;
    }

    // ─── Danh sách thông báo ─────────────────────────────────
    private static ScrollPane buildList(List<NotificationManager.NotifItem> items) {
        VBox listBox = new VBox(0);

        if (items.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(48, 0, 48, 0));
            Label ico = new Label("🔕");
            ico.setStyle("-fx-font-size: 36px;");
            Label msg = new Label("Chưa có thông báo nào");
            msg.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
            empty.getChildren().addAll(ico, msg);
            listBox.getChildren().add(empty);
        } else {
            for (int i = 0; i < items.size(); i++) {
                listBox.getChildren().add(buildRow(items.get(i)));
                if (i < items.size() - 1) {
                    Separator sep = new Separator();
                    sep.setStyle("-fx-background-color: #1e293b; -fx-border-color: #1e293b;");
                    // Dùng custom line thay vì Separator mặc định
                    Region line = new Region();
                    line.setPrefHeight(1);
                    line.setMaxHeight(1);
                    line.setStyle("-fx-background-color: #334155;");
                    listBox.getChildren().add(line);
                }
            }
        }

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("""
                -fx-background: #1e293b;
                -fx-background-color: #1e293b;
                -fx-border-color: transparent;
                """);
        double height = items.isEmpty() ? 140 : Math.min(items.size() * 74.0, 400);
        scroll.setPrefHeight(height);
        return scroll;
    }

    // ─── Một hàng thông báo ──────────────────────────────────
    private static HBox buildRow(NotificationManager.NotifItem item) {
        HBox row = new HBox(12);
        row.setPadding(new Insets(12, 16, 12, 14));
        row.setAlignment(Pos.TOP_LEFT);
        row.setMinHeight(66);

        String bgDefault = item.read
                ? "#1e293b"
                : "#1a2d45";   // hơi xanh nhạt nếu chưa đọc
        row.setStyle("-fx-background-color: " + bgDefault + ";");

        row.setOnMouseEntered(e ->
                row.setStyle("-fx-background-color: #243447; -fx-cursor: hand;"));
        row.setOnMouseExited(e ->
                row.setStyle("-fx-background-color: " + bgDefault + ";"));

        // Icon
        Label icon = new Label(resolveIcon(item.type));
        icon.setStyle("-fx-font-size: 22px; -fx-min-width: 30px; -fx-alignment: CENTER;");
        icon.setMinWidth(30);

        // Nội dung
        VBox content = new VBox(3);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label titleLbl = new Label(item.title);
        titleLbl.setStyle("-fx-text-fill: " + resolveColor(item.type)
                + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        titleLbl.setWrapText(true);
        titleLbl.setMaxWidth(220);

        Label bodyLbl = new Label(item.body);
        bodyLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        bodyLbl.setWrapText(true);
        bodyLbl.setMaxWidth(220);

        Label timeLbl = new Label(item.time.format(FMT));
        timeLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

        content.getChildren().addAll(titleLbl, bodyLbl, timeLbl);

        VBox dotBox = new VBox();
        dotBox.setAlignment(Pos.TOP_CENTER);
        dotBox.setPadding(new Insets(4, 0, 0, 0));
        Circle dot = new Circle(5, Color.web("#2563eb"));
        dot.setVisible(!item.read);
        dotBox.getChildren().add(dot);

        row.getChildren().addAll(icon, content, dotBox);
        return row;
    }

    // ─── Helpers ─────────────────────────────────────────────
    private static String resolveIcon(NotificationManager.NotifType type) {
        return switch (type) {
            case OUTBID              -> "🔔";
            case BID_WON             -> "🏆";
            case BID_LOST            -> "😞";
            case BID_PLACED          -> "✅";
            case AUCTION_ENDING_SOON -> "⏳";
            case PAYMENT_REQUIRED    -> "💳";
            case SYSTEM              -> "📢";
        };
    }

    private static String resolveColor(NotificationManager.NotifType type) {
        return switch (type) {
            case OUTBID, PAYMENT_REQUIRED -> "#f97316";
            case BID_WON                  -> "#10b981";
            case BID_LOST                 -> "#ef4444";
            case AUCTION_ENDING_SOON      -> "#fbbf24";
            default                       -> "#f1f5f9";
        };
    }
}