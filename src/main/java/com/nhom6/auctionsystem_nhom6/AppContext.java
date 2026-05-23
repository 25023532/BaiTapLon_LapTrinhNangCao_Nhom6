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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class AppContext {

    // =========================================================
    // GLOBAL STATE
    // =========================================================
    private static User           currentUser;
    private static AuctionSession activeSession;
    private static Image          avatarImage;

    private static final AuthService authService = new AuthService();

    // =========================================================
    // AUCTION SESSION
    // =========================================================
    private static final List<AuctionSession> globalSessions =
            new CopyOnWriteArrayList<>();

    private static final Map<String, String> sessionSellerMap =
            new ConcurrentHashMap<>();

    public static void registerSession(AuctionSession session,
                                        String sellerUsername) {
        globalSessions.removeIf(s ->
                s.getSessionId().equals(session.getSessionId()));
        globalSessions.add(session);
        sessionSellerMap.put(session.getSessionId(), sellerUsername);
    }

    public static String getSessionSeller(String sessionId) {
        return sessionSellerMap.getOrDefault(sessionId, "Seller");
    }

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

    public static List<AuctionSession> getGlobalSessions() {
        return Collections.unmodifiableList(globalSessions);
    }

    // =========================================================
    // STORAGE MAPS
    // =========================================================
    private static final Map<String, Double>
            walletMap = new ConcurrentHashMap<>();

    private static final Map<String, List<TransactionRecord>>
            transactionMap = new ConcurrentHashMap<>();

    private static final Map<String, List<HistoryRecord>>
            historyMap = new ConcurrentHashMap<>();

    private static final Map<String, List<AuctionSessionRecord>>
            sessionHistoryMap = new ConcurrentHashMap<>();

    private static final Map<String, List<ProductRecord>>
            productMap = new ConcurrentHashMap<>();

    // =========================================================
    // ONLINE USER TRACKING
    // =========================================================
    private static final Set<String> onlineUsers =
            Collections.synchronizedSet(new HashSet<>());

    /** Số online từ server WebSocket — 0 = chưa nhận được */
    private static int serverOnlineCount = 0;

    public static void markUserOnline(String username) {
        if (username != null && !username.isBlank())
            onlineUsers.add(username);
    }

    public static void markUserOffline(String username) {
        if (username != null)
            onlineUsers.remove(username);
    }

    /**
     * Lấy số user online.
     * Ưu tiên số từ server; nếu offline dùng local tracking.
     */
    public static int getOnlineUserCount() {
        if (serverOnlineCount > 0) return serverOnlineCount;
        return Math.max(1, onlineUsers.size());
    }

    /**
     * Cập nhật số online từ server WebSocket.
     * ✅ Fix MainController: cannot find symbol setServerOnlineCount()
     */
    public static void setServerOnlineCount(int count) {
        serverOnlineCount = Math.max(0, count);
    }

    // =========================================================
    // INITIALIZATION
    // =========================================================
    static {
        seedData();
        connectToServer();
    }

    private static void connectToServer() {
        try {
            ServerConnection conn = ServerConnection.getInstance();
            boolean ok = conn.connect();
            System.out.println(ok
                    ? "AppContext: Kết nối server OK."
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
            addTransaction("sellerlong", new TransactionRecord(
                    "TX003", "NẠP TIỀN", 12_000_000,
                    "Nạp qua MoMo", "THÀNH CÔNG",
                    LocalDateTime.now().minusDays(5)));

            addProductInternal("sellerlong", new ProductRecord(
                    "PR001", "MacBook Pro M3", "Laptop",
                    22_000_000, 26_500_000, 12, "ĐÃ BÁN",
                    LocalDateTime.now().minusHours(3),
                    LocalDateTime.now().minusHours(1), "bidder07"));
            addProductInternal("sellerlong", new ProductRecord(
                    "PR002", "iPhone 15 Pro", "Điện thoại",
                    18_000_000, 21_000_000, 7, "ĐÃ BÁN",
                    LocalDateTime.now().minusDays(2),
                    LocalDateTime.now().minusDays(1), "bidder03"));

            seedSessionHistory();
            System.out.println("AppContext: Seed data OK.");
        } catch (Exception e) {
            System.out.println("AppContext: Lỗi seed – " + e.getMessage());
        }
    }

    private static void seedSessionHistory() {
        LocalDateTime base = LocalDateTime.now();
        addSessionHistoryInternal("sellerlong", new AuctionSessionRecord(
                "SES-001", "iPhone 15 Pro Max", "sellerlong",
                20_000_000, 28_000_000, "bidder07", 5,
                base.minusDays(2), base.minusDays(1),
                "THÀNH CÔNG", "SELLER", 0, false));
        addSessionHistoryInternal("bidder07", new AuctionSessionRecord(
                "SES-001", "iPhone 15 Pro Max", "sellerlong",
                20_000_000, 28_000_000, "bidder07", 5,
                base.minusDays(2), base.minusDays(1),
                "THẮNG GIÁ", "BIDDER", 28_000_000, true));
    }

    // =========================================================
    // AUTH / USER
    // =========================================================
    public static AuthService getAuthService() { return authService; }

    public static User getCurrentUser() { return currentUser; }

    public static void setCurrentUser(User user) {
        currentUser = user;
        if (user != null) markUserOnline(user.getUsername());
    }

    // =========================================================
    // SESSION / AVATAR
    // =========================================================
    public static AuctionSession getActiveSession()                 { return activeSession; }
    public static void           setActiveSession(AuctionSession s) { activeSession = s; }

    public static Image getAvatarImage()          { return avatarImage; }
    public static void  setAvatarImage(Image img) { avatarImage = img; }

    // =========================================================
    // LOGOUT
    // =========================================================
    public static void logout() {
        if (currentUser != null) markUserOffline(currentUser.getUsername());
        currentUser       = null;
        activeSession     = null;
        avatarImage       = null;
        serverOnlineCount = 0;
    }

    // =========================================================
    // WALLET
    // =========================================================
    public static double getWalletBalance(String username) {
        return walletMap.getOrDefault(username, 0.0);
    }

    public static void putWallet(String username, double balance) {
        walletMap.put(username, balance);
    }

    public static boolean deposit(String username, double amount) {
        if (amount <= 0) return false;
        walletMap.put(username, walletMap.getOrDefault(username, 0.0) + amount);
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
        return transactionMap.computeIfAbsent(
                username, k -> new CopyOnWriteArrayList<>());
    }
    public static void addTransaction(String username, TransactionRecord record) {
        getTransactions(username).add(record);
    }

    // =========================================================
    // HISTORY
    // =========================================================
    public record HistoryRecord(
            String id, String itemName, double amount,
            String counterparty, String status,
            boolean wonBid, LocalDateTime time) {}

    public static List<HistoryRecord> getHistory(String username) {
        return historyMap.computeIfAbsent(
                username, k -> new CopyOnWriteArrayList<>());
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
            String status, LocalDateTime startTime,
            LocalDateTime endTime, String topBidder) {

        public ProductRecord withUpdated(double newPrice, int newBidCount,
                                          String newStatus, String newTopBidder) {
            return new ProductRecord(id, name, category, startPrice,
                    newPrice, newBidCount, newStatus,
                    startTime, endTime, newTopBidder);
        }
    }

    // =========================================================
    // PRODUCT METHODS
    // =========================================================
    public static List<ProductRecord> getProducts(String username) {
        return productMap.computeIfAbsent(
                username, k -> new CopyOnWriteArrayList<>());
    }

    /** Thêm sản phẩm nội bộ (seed data) — không qua luồng duyệt */
    private static void addProductInternal(String username,
                                            ProductRecord product) {
        getProducts(username).add(product);
    }

    /**
     * Thêm sản phẩm mới từ Seller — tự động gán trạng thái CHỜ DUYỆT.
     */
    public static void addProduct(String username, ProductRecord product) {
        ProductRecord enforced = new ProductRecord(
                product.id(), product.name(), product.category(),
                product.startPrice(), product.currentPrice(),
                product.bidCount(), "CHỜ DUYỆT",
                product.startTime(), product.endTime(),
                product.topBidder());
        getProducts(username).add(enforced);
    }

    public static void removeProduct(String username, String productId) {
        getProducts(username).removeIf(p -> p.id().equals(productId));
    }

    /**
     * Cập nhật sản phẩm theo username seller.
     * ✅ Fix AdminController: cannot find symbol updateProduct()
     */
    public static void updateProduct(String username, ProductRecord updated) {
        List<ProductRecord> list = getProducts(username);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(updated.id())) {
                list.set(i, updated);
                return;
            }
        }
    }

    /**
     * Lấy tất cả sản phẩm của mọi seller.
     * ✅ Fix AdminController: cannot find symbol getAllProducts()
     */
    public static List<ProductRecord> getAllProducts() {
        List<ProductRecord> all = new ArrayList<>();
        productMap.values().forEach(all::addAll);
        return all;
    }

    /**
     * Lấy tất cả sản phẩm đang CHỜ DUYỆT.
     * ✅ Fix AdminController: cannot find symbol getAllPendingProducts()
     */
    public static List<ProductRecord> getAllPendingProducts() {
        return getAllProducts().stream()
                .filter(p -> "CHỜ DUYỆT".equals(p.status()))
                .collect(Collectors.toList());
    }

    /**
     * Tìm username seller sở hữu sản phẩm theo productId.
     * ✅ Fix AdminController: cannot find symbol getSellerForProduct()
     *
     * @return username seller, hoặc "—" nếu không tìm thấy
     */
    public static String getSellerForProduct(String productId) {
        for (Map.Entry<String, List<ProductRecord>> entry : productMap.entrySet()) {
            for (ProductRecord p : entry.getValue()) {
                if (p.id().equals(productId)) return entry.getKey();
            }
        }
        return "—";
    }

    // =========================================================
    // AUCTION SESSION HISTORY
    // =========================================================
    public record AuctionSessionRecord(
            String sessionId, String itemName, String sellerName,
            double startPrice, double finalPrice, String winnerName,
            int totalBids, LocalDateTime startTime, LocalDateTime endTime,
            String result, String myRole, double myFinalBid, boolean iWon) {}

    private static void addSessionHistoryInternal(String username,
                                                   AuctionSessionRecord record) {
        sessionHistoryMap
                .computeIfAbsent(username, k -> new CopyOnWriteArrayList<>())
                .add(record);
    }

    public static void addSessionHistory(String username,
                                          AuctionSessionRecord record) {
        addSessionHistoryInternal(username, record);
    }

    public static List<AuctionSessionRecord> getSessionHistory(String username) {
        List<AuctionSessionRecord> list =
                sessionHistoryMap.getOrDefault(username, Collections.emptyList());
        List<AuctionSessionRecord> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparing(AuctionSessionRecord::endTime).reversed());
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Gọi khi phiên kết thúc — tự động tạo bản ghi lịch sử
     * cho Seller và tất cả Bidder đã tham gia.
     */
    public static void finalizeSession(AuctionSession session,
                                        LocalDateTime  startTime) {
        if (session == null) return;

        var    bids       = session.getBidHistory();
        int    totalBids  = bids.size();
        double finalPrice = session.getCurrentPrice();
        String sellerId   = getSessionSeller(session.getSessionId());
        LocalDateTime endTime = LocalDateTime.now();

        String winnerName   = bids.isEmpty()
                ? null : bids.get(bids.size() - 1).getBidderId();
        String sellerResult = totalBids == 0
                ? "KHÔNG CÓ NGƯỜI ĐẤU" : "THÀNH CÔNG";

        // Lưu cho Seller
        addSessionHistoryInternal(sellerId, new AuctionSessionRecord(
                session.getSessionId(), session.getItemName(), sellerId,
                session.getStartingPrice(), finalPrice, winnerName,
                totalBids, startTime, endTime,
                sellerResult, "SELLER", 0, false));

        // Lưu cho từng Bidder
        Set<String> seen = new LinkedHashSet<>();
        for (var bid : bids) seen.add(bid.getBidderId());

        for (String bidderId : seen) {
            double myHighestBid = bids.stream()
                    .filter(b -> b.getBidderId().equals(bidderId))
                    .mapToDouble(b -> b.getAmount())
                    .max().orElse(0);
            boolean iWon   = bidderId.equals(winnerName);
            String  result = iWon ? "THẮNG GIÁ" : "THUA GIÁ";
            addSessionHistoryInternal(bidderId, new AuctionSessionRecord(
                    session.getSessionId(), session.getItemName(), sellerId,
                    session.getStartingPrice(), finalPrice, winnerName,
                    totalBids, startTime, endTime,
                    result, "BIDDER", myHighestBid, iWon));
        }

        System.out.printf("AppContext: Phiên \"%s\" đã lưu lịch sử (%d bidder).%n",
                session.getItemName(), seen.size());
    }
}
