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
    // GLOBAL APP STATE
    // =========================================================

    private static User currentUser;

    private static final AuthService authService = new AuthService();

    private static AuctionSession activeSession = null;

    private static Image avatarImage;

    // =========================================================
    // WALLET  — số dư theo username
    // =========================================================

    private static final Map<String, Double> walletMap = new HashMap<>();

    // =========================================================
    // TRANSACTION STORAGE  — lịch sử giao dịch theo username
    // =========================================================

    private static final Map<String, List<TransactionRecord>>
            transactionMap = new HashMap<>();

    // =========================================================
    // HISTORY STORAGE
    // =========================================================

    private static final Map<String, List<HistoryRecord>>
            historyMap = new HashMap<>();

    // =========================================================
    // PRODUCT STORAGE
    // =========================================================

    private static final Map<String, List<ProductRecord>>
            productMap = new HashMap<>();

    // =========================================================
    // STATIC INIT
    // =========================================================

    static {
        seedData();
        connectToServer();
    }

    // =========================================================
    // SERVER CONNECTION
    // =========================================================

    private static void connectToServer() {
        try {
            ServerConnection conn = ServerConnection.getInstance();
            boolean connected = conn.connect();
            System.out.println(connected
                    ? "AppContext: Kết nối server thành công."
                    : "AppContext: Chạy offline.");
        } catch (Exception e) {
            System.out.println("AppContext: Lỗi kết nối server.");
            e.printStackTrace();
        }
    }

    // =========================================================
    // SAMPLE DATA
    // =========================================================

    private static void seedData() {
        try {
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

            // DEMO WALLET
            walletMap.put("bidder07",  5_000_000.0);
            walletMap.put("sellerlong", 12_000_000.0);
            walletMap.put("bidder03",  2_500_000.0);
            walletMap.put("bidder01",  0.0);

            // DEMO TRANSACTIONS
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

            // DEMO HISTORY
            addHistory("bidder07", new HistoryRecord(
                    "HD001", "iPhone 15 Pro Max", 28000000,
                    "SellerLong", "THÀNH CÔNG", true,
                    LocalDateTime.now().minusDays(1)));
            addHistory("sellerlong", new HistoryRecord(
                    "HD002", "MacBook Air M2", 22000000,
                    "Bidder07", "CHỜ XỬ LÝ", true,
                    LocalDateTime.now().minusHours(5)));

            // DEMO PRODUCT
            addProduct("sellerlong", new ProductRecord(
                    "PR001", "MacBook Pro M3", "Laptop",
                    22000000, 26500000, 12, "ĐANG ĐẤU GIÁ",
                    LocalDateTime.now().plusHours(2), "bidder07"));
            addProduct("sellerlong", new ProductRecord(
                    "PR002", "iPhone 15 Pro", "Điện thoại",
                    18000000, 21000000, 7, "ĐÃ BÁN",
                    LocalDateTime.now().minusDays(1), "bidder03"));

            System.out.println("AppContext: Seed data thành công.");
        } catch (Exception e) {
            System.out.println("AppContext: Lỗi seed data.");
            e.printStackTrace();
        }
    }

    // =========================================================
    // CURRENT USER
    // =========================================================

    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User user) { currentUser = user; }

    // =========================================================
    // AUTH SERVICE
    // =========================================================

    public static AuthService getAuthService() { return authService; }

    // =========================================================
    // AUCTION SESSION
    // =========================================================

    public static AuctionSession getActiveSession() { return activeSession; }
    public static void setActiveSession(AuctionSession session) { activeSession = session; }

    // =========================================================
    // AVATAR
    // =========================================================

    public static Image getAvatarImage() { return avatarImage; }
    public static void setAvatarImage(Image image) { avatarImage = image; }

    // =========================================================
    // WALLET
    // =========================================================

    /** Lấy số dư ví của user. Mặc định 0 nếu chưa có. */
    public static double getWalletBalance(String username) {
        return walletMap.getOrDefault(username, 0.0);
    }

    /** Nạp tiền vào ví — amount phải > 0 */
    public static boolean deposit(String username, double amount) {
        if (amount <= 0) return false;
        double current = walletMap.getOrDefault(username, 0.0);
        walletMap.put(username, current + amount);
        addTransaction(username, new TransactionRecord(
                "TX-" + System.currentTimeMillis(),
                "NẠP TIỀN",
                amount,
                "Nạp tiền vào ví",
                "THÀNH CÔNG",
                LocalDateTime.now()
        ));
        return true;
    }

    /** Thanh toán — trừ tiền khỏi ví, trả false nếu không đủ số dư */
    public static boolean payment(String username, double amount, String description) {
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
                LocalDateTime.now()
        ));
        return true;
    }

    // =========================================================
    // TRANSACTION RECORD
    // =========================================================

    public record TransactionRecord(
            String id,
            String type,        // "NẠP TIỀN" | "THANH TOÁN" | "HOÀN TIỀN"
            double amount,      // dương = nạp, âm = trừ
            String description,
            String status,
            LocalDateTime time
    ) {}

    public static List<TransactionRecord> getTransactions(String username) {
        return transactionMap.computeIfAbsent(username, k -> new ArrayList<>());
    }

    public static void addTransaction(String username, TransactionRecord record) {
        getTransactions(username).add(record);
    }

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
    // HISTORY RECORD
    // =========================================================

    public record HistoryRecord(
            String id, String itemName, double amount,
            String counterparty, String status, boolean wonBid,
            LocalDateTime time) {}

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
            String id, String name, String category,
            double startPrice, double currentPrice, int bidCount,
            String status, LocalDateTime endTime, String topBidder) {

        public ProductRecord withUpdated(
                double newPrice, int newBidCount,
                String newStatus, String newTopBidder) {
            return new ProductRecord(id, name, category, startPrice,
                    newPrice, newBidCount, newStatus, endTime, newTopBidder);
        }
    }

    // =========================================================
    // PRODUCT METHODS
    // =========================================================

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
