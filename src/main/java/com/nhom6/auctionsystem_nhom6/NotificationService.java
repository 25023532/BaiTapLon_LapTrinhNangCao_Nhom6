package com.nhom6.auctionsystem_nhom6;

import javafx.application.Platform;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class NotificationService {

    // ── Singleton ─────────────────────────────────────────────
    private static NotificationService instance;
    public static NotificationService getInstance() {
        if (instance == null) instance = new NotificationService();
        return instance;
    }
    private NotificationService() {}

    // ── Notification types ────────────────────────────────────
    public enum Type {
        OUTBID,       // Bị người khác vượt giá
        WIN,          // Thắng phiên đấu giá
        LOSE,         // Thua phiên đấu giá
        ANTI_SNIPE,   // Phiên được gia hạn do anti-sniping
        NEW_BID,      // Có lượt bid mới trong phiên đang xem
        SESSION_END,  // Phiên sắp kết thúc (còn 5 phút)
        SYSTEM,       // Thông báo hệ thống
        CHAT,         // Tin nhắn chat mới khi đang ở màn hình khác
        PAYMENT,      // Thanh toán / giao dịch
        SELLER_BID,   // Seller: có người đặt giá sản phẩm của mình
    }

    // ── Notification record ───────────────────────────────────
    public record Notification(
            String id,
            Type type,
            String title,
            String message,
            LocalDateTime time,
            boolean read
    ) {
        public Notification markRead() {
            return new Notification(id, type, title, message, time, true);
        }

        public String timeFormatted() {
            return time.format(DateTimeFormatter.ofPattern("HH:mm dd/MM"));
        }

        public String emoji() {
            return switch (type) {
                case OUTBID      -> "⚠️";
                case WIN         -> "🏆";
                case LOSE        -> "😔";
                case ANTI_SNIPE  -> "⚡";
                case NEW_BID     -> "🔨";
                case SESSION_END -> "⏰";
                case SYSTEM      -> "🔔";
                case CHAT        -> "💬";
                case PAYMENT     -> "💳";
                case SELLER_BID  -> "🛒";
            };
        }

        public String colorStyle() {
            return switch (type) {
                case OUTBID      -> "-fx-border-color: #ef4444;";
                case WIN         -> "-fx-border-color: #10b981;";
                case LOSE        -> "-fx-border-color: #64748b;";
                case ANTI_SNIPE  -> "-fx-border-color: #fbbf24;";
                case NEW_BID     -> "-fx-border-color: #3b82f6;";
                case SESSION_END -> "-fx-border-color: #f97316;";
                case PAYMENT     -> "-fx-border-color: #8b5cf6;";
                case SELLER_BID  -> "-fx-border-color: #06b6d4;";
                default          -> "-fx-border-color: #475569;";
            };
        }
    }

    // ── Storage ───────────────────────────────────────────────
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();
    private final List<Consumer<List<Notification>>> listeners = new CopyOnWriteArrayList<>();

    // ── Public API ────────────────────────────────────────────

    /** Thêm thông báo mới và notify tất cả listener */
    public void add(Type type, String title, String message) {
        Notification n = new Notification(
                "N-" + System.currentTimeMillis(),
                type, title, message,
                LocalDateTime.now(), false
        );
        notifications.add(0, n); // mới nhất lên đầu

        // Giữ tối đa 50 thông báo
        if (notifications.size() > 50)
            notifications.subList(50, notifications.size()).clear();

        notifyListeners();
    }

    /** Lấy tất cả thông báo */
    public List<Notification> getAll() {
        return new ArrayList<>(notifications);
    }

    /** Số thông báo chưa đọc */
    public int getUnreadCount() {
        return (int) notifications.stream().filter(n -> !n.read()).count();
    }

    /** Đánh dấu tất cả đã đọc */
    public void markAllRead() {
        notifications.replaceAll(Notification::markRead);
        notifyListeners();
    }

    /** Đánh dấu 1 thông báo đã đọc */
    public void markRead(String id) {
        notifications.replaceAll(n ->
                n.id().equals(id) ? n.markRead() : n);
        notifyListeners();
    }

    /** Xóa tất cả */
    public void clearAll() {
        notifications.clear();
        notifyListeners();
    }

    /** Đăng ký listener — gọi mỗi khi có thay đổi */
    public void addListener(Consumer<List<Notification>> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<List<Notification>> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        List<Notification> snapshot = getAll();
        Platform.runLater(() ->
                listeners.forEach(l -> l.accept(snapshot)));
    }

    // ── Convenience methods ───────────────────────────────────

    public void notifyOutbid(String itemName, double newPrice) {
        add(Type.OUTBID,
                "⚠️ Bị vượt giá!",
                "Bạn vừa bị vượt giá trong phiên \"" + itemName + "\". "
                        + "Giá mới: ₫ " + String.format("%,.0f", newPrice));
    }

    public void notifyWin(String itemName, double finalPrice) {
        add(Type.WIN,
                "🏆 Bạn đã thắng!",
                "Chúc mừng! Bạn thắng phiên \"" + itemName + "\" "
                        + "với giá ₫ " + String.format("%,.0f", finalPrice));
    }

    public void notifyLose(String itemName, String winner) {
        add(Type.LOSE,
                "😔 Bạn đã thua",
                "Phiên \"" + itemName + "\" kết thúc. "
                        + "Người thắng: " + winner);
    }

    public void notifyAntiSnipe(String itemName, String newEndTime, int count) {
        add(Type.ANTI_SNIPE,
                "⚡ Phiên được gia hạn!",
                "\"" + itemName + "\" tự động kéo dài do có bid vào phút chót. "
                        + "Kết thúc mới: " + newEndTime + " (lần " + count + "/5)");
    }

    public void notifySessionEndingSoon(String itemName, int minutesLeft) {
        add(Type.SESSION_END,
                "⏰ Phiên sắp kết thúc!",
                "\"" + itemName + "\" còn " + minutesLeft + " phút. Đừng bỏ lỡ!");
    }

    public void notifyNewBid(String itemName, String bidder, double amount) {
        add(Type.NEW_BID,
                "🔨 Có lượt bid mới",
                bidder + " vừa đặt ₫ " + String.format("%,.0f", amount)
                        + " cho \"" + itemName + "\"");
    }

    public void notifySellerBid(String itemName, String bidder, double amount) {
        add(Type.SELLER_BID,
                "🛒 Sản phẩm có người đặt giá",
                bidder + " đặt ₫ " + String.format("%,.0f", amount)
                        + " cho \"" + itemName + "\" của bạn");
    }

    public void notifyPayment(String description, double amount) {
        add(Type.PAYMENT,
                "💳 Giao dịch mới",
                description + ": ₫ " + String.format("%,.0f", amount));
    }

    public void notifySystem(String message) {
        add(Type.SYSTEM, "🔔 Thông báo hệ thống", message);
    }
}
