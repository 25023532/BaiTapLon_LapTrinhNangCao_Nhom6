package com.nhom6.auctionsystem_nhom6;

import javafx.scene.image.Image;
import org.example.auction.AuctionSession;
import org.example.auction.AuctionStatus;
import org.example.service.AuthService;
import org.example.user.Admin;
import org.example.user.Bidder;
import org.example.user.Seller;
import org.example.user.User;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class AppContext {

    // =========================================================
    // GLOBAL STATE
    // =========================================================
    private static User           currentUser;
    private static AuctionSession activeSession = null;
    private static Image          avatarImage;

    private static final AuthService authService = new AuthService();

    // =========================================================
    // DANH SÁCH PHIÊN CHUNG
    // =========================================================
    private static final List<AuctionSession> globalSessions =
            new CopyOnWriteArrayList<>();

    // Map sessionId → sellerUsername (để hiển thị tên seller)
    private static final Map<String, String> sessionSellerMap =
            new HashMap<>();

    /**
     * Seller gọi khi bắt đầu phiên.
     * Lưu cả sellerUsername để Bidder biết ai tạo phiên.
     */
    public static void registerSession(AuctionSession session,
                                        String sellerUsername) {
        globalSessions.removeIf(s ->
                s.getSessionId().equals(session.getSessionId()));
        globalSessions.add(session);
        sessionSellerMap.put(session.getSessionId(), sellerUsername);
    }

    /** Tên seller của một session */
    public static String getSessionSeller(String sessionId) {
        return sessionSellerMap.getOrDefault(sessionId, "Seller");
    }

    /** Lấy tất cả phiên đang RUNNING */
    public static List<AuctionSession> getRunningSessions() {
        List<AuctionSession> result = new ArrayList<>();
        for (AuctionSession s : globalSessions) {
            if (s.getStatus() == AuctionStatus.RUNNING
                    && LocalDateTime.now().isBefore(s.getEndTime())) {
                result.add(s);
            }
        }
        return result;
    }

    /** Lấy tất cả phiên (mọi trạng thái) */
    public static List<AuctionSession> getGlobalSessions() {
        return Collections.unmodifiableList(globalSessions);
    }

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
    // STATIC INITIALIZER
    // =========================================================
    static {
        seedData();
        connectToServer();
    }

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

            walletMap.put("bidder07",   5_000_000.0);
            walletMap.put("sellerlong", 12_000_000.0);
            walletMap.put("bidder03",   2_500_000.0);
            walletMap.put("bidder01",   0.0);

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

            addHistory("bidder07", new HistoryRecord(
                    "HD001", "iPhone 15 Pro Max", 28_000_000,
                    "SellerLong", "THÀNH CÔNG", true,
                    LocalDateTime.now().minusDays(1)));
            addHistory("sellerlong", new HistoryRecord(
                    "HD002", "MacBook Air M2", 22_000_000,
                    "Bidder07", "CHỜ XỬ LÝ", true,
                    LocalDateTime.now().minusHours(6)));

            // Seed product — KHÔNG registerSession ở đây
            // vì chưa có AuctionSession thực
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
    // AUTH
    // =========================================================
    public static AuthService getAuthService() { return authService; }

    // =========================================================
    // USER
    // =========================================================
    public static User getCurrentUser()          { return currentUser; }
    public static void setCurrentUser(User user) { currentUser = user; }

    // =========================================================
    // AUCTION SESSION
    // =========================================================
    public static AuctionSession getActiveSession() {
        return activeSession;
    }
    public static void setActiveSession(AuctionSession session) {
        activeSession = session;
    }

    // =========================================================
    // AVATAR
    // =========================================================
    public static Image getAvatarImage()          { return avatarImage; }
    public static void  setAvatarImage(Image img) { avatarImage = img; }

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
    public static double getWalletBalance(String username) {
        return walletMap.getOrDefault(username, 0.0);
    }

    public static boolean deposit(String username, double amount) {
        if (amount <= 0) return false;
        walletMap.put(username,
                walletMap.getOrDefault(username, 0.0) + amount);
        addTransaction(username, new TransactionRecord(
                "TX-" + System.currentTimeMillis(), "NẠP TIỀN",
                amount, "Nạp tiền vào ví", "THÀNH CÔNG",
                LocalDateTime.now()));
        return true;
    }

    public static boolean payment(String username, double amount,
                                   String description) {
        if (amount <= 0) return false;
        double current = walletMap.getOrDefault(username, 0.0);
        if (current < amount) return false;
        walletMap.put(username, current - amount);
        addTransaction(username, new TransactionRecord(
                "TX-" + System.currentTimeMillis(), "THANH TOÁN",
                -amount, description, "THÀNH CÔNG",
                LocalDateTime.now()));
        return true;
    }

    // =========================================================
    // RECORDS
    // =========================================================
    public record TransactionRecord(
            String id, String type, double amount,
            String description, String status, LocalDateTime time) {}

    public static List<TransactionRecord> getTransactions(String username) {
        return transactionMap.computeIfAbsent(username, k -> new ArrayList<>());
    }
    public static void addTransaction(String username,
                                       TransactionRecord record) {
        getTransactions(username).add(record);
    }

    public record HistoryRecord(
            String id, String itemName, double amount,
            String counterparty, String status,
            boolean wonBid, LocalDateTime time) {}

    public static List<HistoryRecord> getHistory(String username) {
        return historyMap.computeIfAbsent(username, k -> new ArrayList<>());
    }
    public static void addHistory(String username, HistoryRecord record) {
        getHistory(username).add(record);
    }

    public record ProductRecord(
            String id, String name, String category,
            double startPrice, double currentPrice, int bidCount,
            String status, LocalDateTime startTime,
            LocalDateTime endTime, String topBidder) {

        public ProductRecord withUpdated(double newPrice, int newBidCount,
                                          String newStatus,
                                          String newTopBidder) {
            return new ProductRecord(id, name, category, startPrice,
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
    public static void updateProduct(String username,
                                      ProductRecord updated) {
        List<ProductRecord> list = getProducts(username);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(updated.id())) {
                list.set(i, updated);
                return;
            }
        }
    }

    /** Lấy tất cả sản phẩm của mọi seller — dùng để tra category */
    public static List<ProductRecord> getAllProducts() {
        List<ProductRecord> all = new ArrayList<>();
        productMap.values().forEach(all::addAll);
        return all;
    }
}
