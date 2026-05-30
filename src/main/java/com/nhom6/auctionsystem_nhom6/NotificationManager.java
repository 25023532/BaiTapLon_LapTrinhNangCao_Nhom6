package com.nhom6.auctionsystem_nhom6;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Quản lý danh sách thông báo trong phiên hiện tại (Singleton).
 * Cho phép các controller khác nhau cùng truy cập và nhận thông báo.
 */
public class NotificationManager {

    private static NotificationManager instance;
    public static synchronized NotificationManager getInstance() {
        if (instance == null) instance = new NotificationManager();
        return instance;
    }

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    public void addListener(Runnable l) { listeners.add(l); }
    public void removeListener(Runnable l) { listeners.remove(l); }
    private void notifyListeners() { listeners.forEach(l -> { try { l.run(); } catch(Exception e) {} }); }

    // ─── Loại thông báo ──────────────────────────────────────
    public enum NotifType {
        OUTBID,              // Bị vượt giá
        BID_WON,             // Thắng đấu giá
        BID_LOST,            // Thua đấu giá
        BID_PLACED,          // Đặt giá thành công
        AUCTION_ENDING_SOON, // Sắp kết thúc
        PAYMENT_REQUIRED,    // Cần thanh toán
        SYSTEM               // Thông báo hệ thống
    }

    // ─── Model ───────────────────────────────────────────────
    public static class NotifItem {
        public final String        id;
        public final NotifType     type;
        public final String        title;
        public final String        body;
        public final LocalDateTime time;
        public       boolean       read = false;

        public NotifItem(NotifType type, String title, String body,
                         LocalDateTime time) {
            this.id    = java.util.UUID.randomUUID().toString();
            this.type  = type;
            this.title = title;
            this.body  = body;
            this.time  = time;
        }
    }

    // ─── Storage ─────────────────────────────────────────────
    private final List<NotifItem> items = new ArrayList<>();
    private static final int MAX_SIZE = 50;

    /** Thêm thông báo mới (mới nhất ở đầu). */
    public synchronized void add(NotifItem item) {
        items.add(0, item);
        if (items.size() > MAX_SIZE) {
            items.remove(items.size() - 1);
        }
        notifyListeners();
    }

    /** Lấy toàn bộ danh sách (bất biến). */
    public synchronized List<NotifItem> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    /** Số lượng chưa đọc. */
    public synchronized int countUnread() {
        return (int) items.stream().filter(i -> !i.read).count();
    }

    /** Đánh dấu tất cả là đã đọc. */
    public synchronized void markAllRead() {
        items.forEach(i -> i.read = true);
        notifyListeners();
    }

    /** Xóa toàn bộ. */
    public synchronized void clear() {
        items.clear();
        notifyListeners();
    }
}
