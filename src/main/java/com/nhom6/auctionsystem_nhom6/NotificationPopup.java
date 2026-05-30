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
                -fx-background-color: #FFFFFF;
                -fx-background-radius: 0;
                -fx-border-color: #1A1A1A;
                -fx-border-radius: 0;
                -fx-border-width: 3;
                -fx-effect: dropshadow(gaussian, #1A1A1A, 8, 0, 0, 0);
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
                -fx-background-color: #1F0C40;
                -fx-background-radius: 0;
                -fx-border-color: transparent transparent #1A1A1A transparent;
                -fx-border-width: 3;
                """);

        Label title = new Label("🔔  THÔNG BÁO");
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: 900; -fx-letter-spacing: 1px;");
        HBox.setHgrow(title, Priority.ALWAYS);

        // Số unread
        long unread = items.stream().filter(i -> !i.read).count();
        if (unread > 0) {
            Label badge = new Label(String.valueOf(unread));
            badge.setStyle("""
                    -fx-background-color: #FF6B35;
                    -fx-text-fill: white;
                    -fx-font-size: 10px;
                    -fx-font-weight: bold;
                    -fx-background-radius: 0;
                    -fx-border-color: #1A1A1A;
                    -fx-border-width: 1;
                    -fx-padding: 1 6 1 6;
                    """);
            header.getChildren().addAll(title, badge);
        } else {
            header.getChildren().add(title);
        }

        Button close = new Button("✕");
        close.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #FFFFFF;
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-cursor: hand;
                -fx-padding: 2 8 2 8;
                -fx-background-radius: 0;
                """);
        close.setOnMouseEntered(e -> close.setStyle(close.getStyle()
                .replace("-fx-background-color: transparent", "-fx-background-color: #FF3B3B")));
        close.setOnMouseExited(e -> close.setStyle(close.getStyle()
                .replace("-fx-background-color: #FF3B3B", "-fx-background-color: transparent")));
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
            empty.setStyle("-fx-background-color: #FFF8F0;");
            Label ico = new Label("🔕");
            ico.setStyle("-fx-font-size: 36px; -fx-text-fill: #1F0C40;");
            Label msg = new Label("Chưa có thông báo mới");
            msg.setStyle("-fx-text-fill: #666666; -fx-font-size: 13px; -fx-font-weight: bold;");
            empty.getChildren().addAll(ico, msg);
            listBox.getChildren().add(empty);
        } else {
            for (int i = 0; i < items.size(); i++) {
                listBox.getChildren().add(buildRow(items.get(i)));
                if (i < items.size() - 1) {
                    Region line = new Region();
                    line.setPrefHeight(2);
                    line.setMaxHeight(2);
                    line.setStyle("-fx-background-color: #1A1A1A;");
                    listBox.getChildren().add(line);
                }
            }
        }

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("""
                -fx-background: #FFFFFF;
                -fx-background-color: #FFFFFF;
                -fx-border-color: transparent;
                """);
        double height = items.isEmpty() ? 140 : Math.min(items.size() * 78.0, 400);
        scroll.setPrefHeight(height);
        return scroll;
    }

    // ─── Một hàng thông báo ──────────────────────────────────
    private static HBox buildRow(NotificationManager.NotifItem item) {
        HBox row = new HBox(12);
        row.setPadding(new Insets(14, 16, 14, 16));
        row.setAlignment(Pos.TOP_LEFT);
        row.setMinHeight(74);

        String bgDefault = item.read ? "#FFFFFF" : "#FFF8F0";
        row.setStyle("-fx-background-color: " + bgDefault + ";");

        row.setOnMouseEntered(e ->
                row.setStyle("-fx-background-color: #F0E6FF; -fx-cursor: hand;"));
        row.setOnMouseExited(e ->
                row.setStyle("-fx-background-color: " + bgDefault + ";"));

        // Icon
        Label icon = new Label(resolveIcon(item.type));
        icon.setStyle("-fx-font-size: 22px; -fx-min-width: 32px; -fx-alignment: CENTER;");
        icon.setMinWidth(32);

        // Nội dung
        VBox content = new VBox(4);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label titleLbl = new Label(item.title);
        titleLbl.setStyle("-fx-text-fill: " + resolveColor(item.type) + "; -fx-font-size: 13px; -fx-font-weight: 900;");
        titleLbl.setWrapText(true);
        titleLbl.setMaxWidth(220);

        Label bodyLbl = new Label(item.body);
        bodyLbl.setStyle("-fx-text-fill: #444444; -fx-font-size: 12px; -fx-font-weight: bold;");
        bodyLbl.setWrapText(true);
        bodyLbl.setMaxWidth(220);

        Label timeLbl = new Label(item.time.format(FMT));
        timeLbl.setStyle("-fx-text-fill: #888888; -fx-font-size: 10px; -fx-font-weight: bold;");

        content.getChildren().addAll(titleLbl, bodyLbl, timeLbl);

        VBox dotBox = new VBox();
        dotBox.setAlignment(Pos.TOP_CENTER);
        dotBox.setPadding(new Insets(4, 0, 0, 0));

        // Unread indicator - Square instead of circle for neobrutalism
        Region unreadIndicator = new Region();
        unreadIndicator.setPrefSize(10, 10);
        unreadIndicator.setMinSize(10, 10);
        unreadIndicator.setStyle("-fx-background-color: #FF6B35; -fx-border-color: #1A1A1A; -fx-border-width: 1.5;");
        unreadIndicator.setVisible(!item.read);
        dotBox.getChildren().add(unreadIndicator);

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
            case BID_WON                  -> "#2DC653"; // Success
            case BID_LOST, AUCTION_ENDING_SOON -> "#FF3B3B"; // Danger
            case OUTBID, PAYMENT_REQUIRED -> "#FF6B35"; // Warning/Accent
            default                       -> "#1F0C40"; // Primary Deep Purple
        };
        }
}
