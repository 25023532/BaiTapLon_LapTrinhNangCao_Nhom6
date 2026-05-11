package com.nhom6.auctionsystem_nhom6;

import javafx.scene.image.Image;
import org.example.auction.AuctionSession;
import org.example.service.AuthService;
import org.example.user.Admin;
import org.example.user.Bidder;
import org.example.user.Seller;
import org.example.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppContext {

    // =========================================================
    // GLOBAL STATE
    // =========================================================

    private static User           currentUser;
    private static AuctionSession activeSession = null;
    private static Image          avatarImage;

    // ── Singleton services ────────────────────────────────────
    private static final AuthService authService = new AuthService();

    // =========================================================
    // STORAGE MAPS
    // =========================================================

    private static final Map<String, Double>
            walletMap      = new HashMap<>();

    private static final Map<String, List<TransactionRecord>>
            transactionMap = new HashMap<>();

    private static final Map<String, List<HistoryRecord>>
            historyMap     = new HashMap<>();

    private static final Map<String, List<ProductRecord>>
            productMap     = new HashMap<>();

    // =========================================================
    // STATIC INITIALIZER  —  seed data + server
    // =========================================================

    static {
        seedData();
        connectToServer();
    }

    // ── Kết nối server (offline-safe) ─────────────────────────
    private static void connectToServer() {
        try {
            ServerConnection conn = ServerConnection.getInstance();
            boolean connected = conn.connect();
            System.out.println(connected
                    ? "AppContext: Kết nối server thành công."
                    : "AppContext: Chạy offline.");
        } catch (Exception e) {
            System.out.println("AppContext: Lỗi kết nối server.");
        }
    }

    // ── Seed tài khoản mẫu + dữ liệu demo ────────────────────
    private static void seedData() {
        try {

            // ── Tài khoản mặc định ────────────────────────────
            if (!authService.isRegistered("admin"))
                authService.register(new Admin("A001", "admin", "admin123"));

            if (!authService.isRegistered("sellerlong"))
                authService.register(new Seller("S001", "sellerlong", "seller123"));

            if (!authService.isRegistered("bidder07"))
                authService.register(new Bidder("B001", "bidder07", "bidder123"));

            if (!authService.isRegistered("bidder03"))
                authService.register(new Bidder("B002", "bidder03", "bidder123"));

            if (!authService.isRegistered("bidder01"))
                authService.register(new Bidder("B003", "bidder01", "bidder123"));

            // ── Demo ví ───────────────────────────────────────
            walletMap.put("bidder07",   5_000_000.0);
            walletMap.put("sellerlong", 12_000_000.0);
            walletMap.put("bidder03",   2_500_000.0);
            walletMap.put("bidder01",   0.0);

            // ── Demo giao dịch ────────────────────────────────
            addTransaction("bidder07", new TransactionRecord(
                    "TX001", "NẠP TIỀN", 5_000_000,
                    "Nạp qua ngân hàng VCB", "THÀNH CÔNG",
                    LocalDateTime.now().minusDays(3)));

            addTransaction("bidder07", new TransactionRecord(
                    "TX002", "THANH TOÁN", -28_000_000,
                    "iPhone 15 Pro Max – HD001", "THÀNH CÔNG",
                    LocalDateTime.now().minusDays(1)));

            addTransaction("sellerlong", new TransactionRecord(
                    "TX003", "NẠP TIỀN", 12_000_000,
                    "Nạp qua MoMo", "THÀNH CÔNG",
                    LocalDateTime.now().minusDays(5)));

            // ── Demo lịch sử mua/bán ──────────────────────────
            addHistory("bidder07", new HistoryRecord(
                    "HD001", "iPhone 15 Pro Max", 28_000_000,
                    "SellerLong", "THÀNH CÔNG", true,
                    LocalDateTime.now().minusDays(1)));

            addHistory("sellerlong", new HistoryRecord(
                    "HD002", "MacBook Air M2", 22_000_000,
                    "Bidder07", "CHỜ XỬ LÝ", true,
                    LocalDateTime.now().minusHours(6)));

            // ── Demo sản phẩm seller ──────────────────────────
            addProduct("sellerlong", new ProductRecord(
                    "PR001", "MacBook Pro M3", "Laptop",
                    22_000_000, 26_500_000, 12,
                    "ĐANG ĐẤU GIÁ",
                    LocalDateTime.now().minusHours(1),
                    LocalDateTime.now().plusHours(2),
                    "bidder07"));

            addProduct("sellerlong", new ProductRecord(
                    "PR002", "iPhone 15 Pro", "Điện thoại",
                    18_000_000, 21_000_000, 7,
                    "ĐÃ BÁN",
                    LocalDateTime.now().minusDays(2),
                    LocalDateTime.now().minusDays(1),
                    "bidder03"));

            System.out.println("AppContext: Seed data thành công.");

        } catch (Exception e) {
            System.out.println("AppContext: Lỗi seed data – " + e.getMessage());
        }
    }

    // =========================================================
    // AUTH SERVICE
    // =========================================================

    public static AuthService getAuthService() { return authService; }

    // =========================================================
    // USER
    // =========================================================

    public static User getCurrentUser()            { return currentUser; }
    public static void setCurrentUser(User user)   { currentUser = user; }

    // =========================================================
    // AUCTION SESSION
    // =========================================================

    public static AuctionSession getActiveSession()              { return activeSession; }
    public static void setActiveSession(AuctionSession session)  { activeSession = session; }

    // =========================================================
    // AVATAR
    // =========================================================

    public static Image getAvatarImage()         { return avatarImage; }
    public static void  setAvatarImage(Image img){ avatarImage = img; }

    // =========================================================
    // LOGOUT
    // =========================================================

    public static void logout() {
        currentUser   = null;
        activeSession = null;
        avatarImage   = null;
        System.out.println("AppContext: Đã đăng xuất.");
    }

    // =========================================================
    // WALLET
    // =========================================================

    /** Lấy số dư ví. Mặc định 0 nếu chưa có. */
    public static double getWalletBalance(String username) {
        return walletMap.getOrDefault(username, 0.0);
    }

    /** Nạp tiền vào ví — amount phải > 0 */
    public static boolean deposit(String username, double amount) {
        if (amount <= 0) return false;
        walletMap.put(username,
                walletMap.getOrDefault(username, 0.0) + amount);
        addTransaction(username, new TransactionRecord(
                "TX-" + System.currentTimeMillis(),
                "NẠP TIỀN",
                amount,
                "Nạp tiền vào ví",
                "THÀNH CÔNG",
                LocalDateTime.now()));
        return true;
    }

    /** Thanh toán — trừ tiền, trả false nếu không đủ số dư */
    public static boolean payment(String username, double amount,
                                   String description) {
        if (amount <= 0) return false;
        double current = walletMap.getOrDefault(username, 0.0);
        if (current < amount) return false;
        walletMap.put(username, current - amount);
        addTransaction(username, new TransactionRecord(
                "TX-" + System.currentTimeMillis(),
                "THANH TOÁN",
                -amount,
                description,
                "THÀNH CÔNG",
                LocalDateTime.now()));
        return true;
    }

    // =========================================================
    // TRANSACTION RECORD
    // =========================================================

    public record TransactionRecord(
            String        id,
            String        type,          // "NẠP TIỀN" | "THANH TOÁN" | "HOÀN TIỀN"
            double        amount,        // dương = nạp vào, âm = trừ ra
            String        description,
            String        status,        // "THÀNH CÔNG" | "THẤT BẠI" | "CHỜ XỬ LÝ"
            LocalDateTime time
    ) {}

    public static List<TransactionRecord> getTransactions(String username) {
        return transactionMap.computeIfAbsent(username, k -> new ArrayList<>());
    }

    public static void addTransaction(String username, TransactionRecord record) {
        getTransactions(username).add(record);
    }

    // =========================================================
    // HISTORY RECORD
    // =========================================================

    public record HistoryRecord(
            String        id,
            String        itemName,
            double        amount,
            String        counterparty,
            String        status,        // "THÀNH CÔNG" | "CHỜ XỬ LÝ" | "THẤT BẠI" | "ĐÃ HỦY"
            boolean       wonBid,
            LocalDateTime time
    ) {}

    public static List<HistoryRecord> getHistory(String username) {
        return historyMap.computeIfAbsent(username, k -> new ArrayList<>());
    }

    public static void addHistory(String username, HistoryRecord record) {
        getHistory(username).add(record);
    }

    // =========================================================
    // PRODUCT RECORD
    // =========================================================

    public record ProductRecord(
            String        id,
            String        name,
            String        category,
            double        startPrice,
            double        currentPrice,
            int           bidCount,
            String        status,        // "ĐANG ĐẤU GIÁ" | "ĐÃ BÁN" | "HẾT HẠN" | "ĐÃ HỦY"
            LocalDateTime startTime,     // ✅ thời gian bắt đầu đấu giá
            LocalDateTime endTime,       // thời gian kết thúc
            String        topBidder      // người đang dẫn đầu / đã thắng
    ) {
        /** Tạo bản sao với các trường được cập nhật */
        public ProductRecord withUpdated(double newPrice, int newBidCount,
                                          String newStatus, String newTopBidder) {
            return new ProductRecord(
                    id, name, category, startPrice,
                    newPrice, newBidCount, newStatus,
                    startTime, endTime, newTopBidder);
        }
    }

    public static List<ProductRecord> getProducts(String username) {
        return productMap.computeIfAbsent(username, k -> new ArrayList<>());
    }

    public static void addProduct(String username, ProductRecord product) {
        getProducts(username).add(product);
    }

    public static void removeProduct(String username, String productId) {
        getProducts(username).removeIf(p -> p.id().equals(productId));
    }

    public static void updateProduct(String username, ProductRecord updated) {
        List<ProductRecord> list = getProducts(username);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(updated.id())) {
                list.set(i, updated);
                return;
            }
        }
    }
}
