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
    // TRẠNG THÁI TOÀN CỤC
    // =========================================================
    private static User           currentUser;
    private static AuctionSession activeSession = null;
    private static Image          avatarImage;

    private static final AuthService authService = new AuthService();

    // =========================================================
    // PHIÊN ĐẤU GIÁ
    // =========================================================
    private static final List<AuctionSession> globalSessions =
            new CopyOnWriteArrayList<>();
    private static final Map<String, String> sessionSellerMap = new HashMap<>();

    public static void registerSession(AuctionSession session, String sellerUsername) {
        globalSessions.removeIf(s -> s.getSessionId().equals(session.getSessionId()));
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
                    && LocalDateTime.now().isBefore(s.getEndTime()))
                result.add(s);
        }
        return result;
    }

    public static List<AuctionSession> getGlobalSessions() {
        return Collections.unmodifiableList(globalSessions);
    }

    // =========================================================
    // MAP LƯU TRỮ
    // =========================================================
    private static final Map<String, Double>
            walletMap = new HashMap<>();
    private static final Map<String, List<TransactionRecord>>
            transactionMap = new HashMap<>();
    private static final Map<String, List<HistoryRecord>>
            historyMap = new HashMap<>();
    private static final Map<String, List<AuctionSessionRecord>>
            sessionHistoryMap = new HashMap<>();
    private static final Map<String, List<ProductRecord>>
            productMap = new HashMap<>();

    // =========================================================
    // KHỞI TẠO
    // =========================================================
    static {
        seedData();
        connectToServer();
    }

    private static void connectToServer() {
        try {
            ServerConnection conn = ServerConnection.getInstance();
            boolean ok = conn.connect();
            System.out.println(ok ? "AppContext: Kết nối server OK."
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

            seedSessionHistory();
            System.out.println("AppContext: Seed data OK.");
        } catch (Exception e) {
            System.out.println("AppContext: Lỗi seed – " + e.getMessage());
        }
    }

    private static void seedSessionHistory() {
        LocalDateTime base = LocalDateTime.now();
        addSessionHistory("sellerlong", new AuctionSessionRecord(
                "SES-001","iPhone 15 Pro Max","sellerlong",
                20_000_000,28_000_000,"bidder07",5,
                base.minusDays(2),base.minusDays(1),
                "THÀNH CÔNG","SELLER",0,false));
        addSessionHistory("bidder07", new AuctionSessionRecord(
                "SES-001","iPhone 15 Pro Max","sellerlong",
                20_000_000,28_000_000,"bidder07",5,
                base.minusDays(2),base.minusDays(1),
                "THẮNG GIÁ","BIDDER",28_000_000,true));
        addSessionHistory("bidder03", new AuctionSessionRecord(
                "SES-001","iPhone 15 Pro Max","sellerlong",
                20_000_000,28_000_000,"bidder07",5,
                base.minusDays(2),base.minusDays(1),
                "THUA GIÁ","BIDDER",25_000_000,false));
        addSessionHistory("sellerlong", new AuctionSessionRecord(
                "SES-002","MacBook Air M2","sellerlong",
                18_000_000,18_000_000,null,0,
                base.minusDays(4),base.minusDays(3),
                "KHÔNG CÓ NGƯỜI ĐẤU","SELLER",0,false));
    }

    // =========================================================
    // AUTH / USER
    // =========================================================
    public static AuthService getAuthService() { return authService; }
    public static User getCurrentUser()          { return currentUser; }
    public static void setCurrentUser(User user) { currentUser = user; }

    // =========================================================
    // SESSION / AVATAR
    // =========================================================
    public static AuctionSession getActiveSession()                 { return activeSession; }
    public static void           setActiveSession(AuctionSession s) { activeSession = s; }
    public static Image getAvatarImage()          { return avatarImage; }
    public static void  setAvatarImage(Image img) { avatarImage = img; }

    // =========================================================
    // ĐĂNG XUẤT
    // =========================================================
    public static void logout() {
        currentUser   = null;
        activeSession = null;
        avatarImage   = null;
    }

    // =========================================================
    // VÍ
    // =========================================================
    public static double getWalletBalance(String username) {
        return walletMap.getOrDefault(username, 0.0);
    }

    public static boolean deposit(String username, double amount) {
        if (amount <= 0) return false;
        walletMap.put(username, walletMap.getOrDefault(username, 0.0) + amount);
        addTransaction(username, new TransactionRecord(
                "TX-" + System.currentTimeMillis(), "NẠP TIỀN",
                amount, "Nạp tiền vào ví", "THÀNH CÔNG", LocalDateTime.now()));
        return true;
    }

    public static boolean payment(String username, double amount, String description) {
        if (amount <= 0) return false;
        double cur = walletMap.getOrDefault(username, 0.0);
        if (cur < amount) return false;
        walletMap.put(username, cur - amount);
        addTransaction(username, new TransactionRecord(
                "TX-" + System.currentTimeMillis(), "THANH TOÁN",
                -amount, description, "THÀNH CÔNG", LocalDateTime.now()));
        return true;
    }

    // =========================================================
    // RECORDS
    // =========================================================
    public record TransactionRecord(
            String id, String type, double amount,
            String description, String status, LocalDateTime time) {}

    public static List<TransactionRecord> getTransactions(String u) {
        return transactionMap.computeIfAbsent(u, k -> new ArrayList<>());
    }
    public static void addTransaction(String u, TransactionRecord r) {
        getTransactions(u).add(r);
    }

    public record HistoryRecord(
            String id, String itemName, double amount,
            String counterparty, String status,
            boolean wonBid, LocalDateTime time) {}

    public static List<HistoryRecord> getHistory(String u) {
        return historyMap.computeIfAbsent(u, k -> new ArrayList<>());
    }
    public static void addHistory(String u, HistoryRecord r) {
        getHistory(u).add(r);
    }

    // ─── ProductRecord ────────────────────────────────────────
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
    // TÍNH STATUS HIỂN THỊ THEO THỜI GIAN THỰC
    // =========================================================
    /**
     * Sau khi Admin duyệt (ĐÃ DUYỆT), status hiển thị tự động
     * chuyển theo thời gian thực:
     *
     *   now < startTime             → "SẮP DIỄN RA"
     *   startTime <= now < endTime  → "ĐANG ĐẤU GIÁ"
     *   now >= endTime              → "ĐÃ KẾT THÚC"
     *
     * Các status khác (CHỜ DUYỆT, TỪ CHỐI, ĐÃ BÁN, ĐÃ HỦY) giữ nguyên.
     */
    public static String computeDisplayStatus(ProductRecord p) {
        String raw = p.status();
        // Chỉ tính lại với sản phẩm đã qua tay Admin
        if ("CHỜ DUYỆT".equals(raw) || "TỪ CHỐI".equals(raw)
                || "ĐÃ BÁN".equals(raw) || "ĐÃ HỦY".equals(raw)) {
            return raw;
        }
        // ĐÃ DUYỆT hoặc đang trong vòng đời → tính theo giờ
        if ("ĐÃ DUYỆT".equals(raw)
                || "SẮP DIỄN RA".equals(raw)
                || "ĐANG ĐẤU GIÁ".equals(raw)
                || "ĐÃ KẾT THÚC".equals(raw)) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(p.startTime()))    return "SẮP DIỄN RA";
            else if (now.isBefore(p.endTime())) return "ĐANG ĐẤU GIÁ";
            else                                return "ĐÃ KẾT THÚC";
        }
        return raw;
    }

    // =========================================================
    // KIỂM TRA TRÙNG THỜI GIAN TOÀN HỆ THỐNG
    // =========================================================
    /**
     * Kiểm tra [newStart, newEnd] có trùng với bất kỳ sản phẩm nào
     * của seller đã được duyệt (ĐÃ DUYỆT / SẮP DIỄN RA / ĐANG ĐẤU GIÁ).
     * Bỏ qua CHỜ DUYỆT và TỪ CHỐI vì chưa chắc được duyệt.
     *
     * @param sellerUsername  seller đang đăng
     * @param newStart        thời gian bắt đầu mới
     * @param newEnd          thời gian kết thúc mới
     * @param excludeId       id sản phẩm bỏ qua khi sửa chính nó
     * @return tên sản phẩm bị trùng, hoặc null nếu không trùng
     */
    public static String findTimeConflictForSeller(String sellerUsername,
                                                    LocalDateTime newStart,
                                                    LocalDateTime newEnd,
                                                    String excludeId) {
        for (ProductRecord p : getProducts(sellerUsername)) {
            if (excludeId != null && excludeId.equals(p.id())) continue;
            String ds = computeDisplayStatus(p);
            // Chỉ kiểm tra với sản phẩm đã được duyệt và còn trong vòng đời
            if (!"ĐÃ DUYỆT".equals(ds)
                    && !"SẮP DIỄN RA".equals(ds)
                    && !"ĐANG ĐẤU GIÁ".equals(ds)) continue;
            boolean overlap = newStart.isBefore(p.endTime())
                           && newEnd.isAfter(p.startTime());
            if (overlap) return p.name();
        }
        return null;
    }

    // =========================================================
    // PRODUCT MAP
    // =========================================================
    public static List<ProductRecord> getProducts(String username) {
        return productMap.computeIfAbsent(username, k -> new ArrayList<>());
    }

    /**
     * Seller đăng sản phẩm mới.
     * Status luôn được enforce là CHỜ DUYỆT — không thể bypass.
     */
    public static void addProduct(String username, ProductRecord product) {
        ProductRecord enforced = new ProductRecord(
                product.id(), product.name(), product.category(),
                product.startPrice(), product.currentPrice(),
                product.bidCount(), "CHỜ DUYỆT",
                product.startTime(), product.endTime(), product.topBidder());
        productMap.computeIfAbsent(username, k -> new ArrayList<>()).add(enforced);
        System.out.printf("AppContext: [ADD] seller=%s product=\"%s\" → CHỜ DUYỆT%n",
                username, product.name());
    }

    public static void removeProduct(String username, String productId) {
        productMap.computeIfAbsent(username, k -> new ArrayList<>())
                  .removeIf(p -> p.id().equals(productId));
    }

    public static void updateProduct(String username, ProductRecord updated) {
        List<ProductRecord> list =
                productMap.computeIfAbsent(username, k -> new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(updated.id())) {
                list.set(i, updated);
                System.out.printf("AppContext: [UPDATE] seller=%s product=\"%s\" → %s%n",
                        username, updated.name(), updated.status());
                return;
            }
        }
        System.out.printf("AppContext: [UPDATE-WARN] id=%s không tìm thấy " +
                "trong list của seller=%s%n", updated.id(), username);
    }

    /** Tất cả sản phẩm của mọi seller — dùng cho Admin thống kê */
    public static List<ProductRecord> getAllProducts() {
        List<ProductRecord> all = new ArrayList<>();
        productMap.values().forEach(all::addAll);
        return Collections.unmodifiableList(all);
    }

    /**
     * Tất cả sản phẩm CHỜ DUYỆT của mọi seller.
     * AdminController gọi hàm này để render danh sách cần duyệt.
     */
    public static List<ProductRecord> getAllPendingProducts() {
        List<ProductRecord> pending = new ArrayList<>();
        for (List<ProductRecord> sellerList : productMap.values())
            for (ProductRecord p : sellerList)
                if ("CHỜ DUYỆT".equals(p.status())) pending.add(p);
        System.out.println("AppContext: getAllPendingProducts() → " + pending.size());
        return Collections.unmodifiableList(pending);
    }

    /** Tra ngược productMap: productId → sellerUsername */
    public static String getSellerForProduct(String productId) {
        for (Map.Entry<String, List<ProductRecord>> entry : productMap.entrySet())
            for (ProductRecord p : entry.getValue())
                if (p.id().equals(productId)) return entry.getKey();
        return "—";
    }

    // =========================================================
    // LỊCH SỬ PHIÊN
    // =========================================================
    public record AuctionSessionRecord(
            String sessionId, String itemName, String sellerName,
            double startPrice, double finalPrice, String winnerName,
            int totalBids, LocalDateTime startTime, LocalDateTime endTime,
            String result, String myRole, double myFinalBid, boolean iWon) {}

    public static void addSessionHistory(String u, AuctionSessionRecord r) {
        sessionHistoryMap.computeIfAbsent(u, k -> new CopyOnWriteArrayList<>()).add(r);
    }

    public static List<AuctionSessionRecord> getSessionHistory(String username) {
        List<AuctionSessionRecord> list =
                sessionHistoryMap.getOrDefault(username, Collections.emptyList());
        List<AuctionSessionRecord> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparing(AuctionSessionRecord::endTime).reversed());
        return Collections.unmodifiableList(sorted);
    }

    public static void finalizeSession(AuctionSession session,
                                        LocalDateTime startTime) {
        if (session == null) return;
        var    bids       = session.getBidHistory();
        int    totalBids  = bids.size();
        double finalPrice = session.getCurrentPrice();
        String sellerId   = getSessionSeller(session.getSessionId());
        LocalDateTime endTime = LocalDateTime.now();
        String winnerName = bids.isEmpty() ? null
                : bids.get(bids.size() - 1).getBidderId();

        addSessionHistory(sellerId, new AuctionSessionRecord(
                session.getSessionId(), session.getItemName(), sellerId,
                session.getStartingPrice(), finalPrice, winnerName, totalBids,
                startTime, endTime,
                totalBids == 0 ? "KHÔNG CÓ NGƯỜI ĐẤU" : "THÀNH CÔNG",
                "SELLER", 0, false));

        Set<String> seen = new LinkedHashSet<>();
        bids.forEach(b -> seen.add(b.getBidderId()));
        for (String bidderId : seen) {
            double myMax = bids.stream()
                    .filter(b -> b.getBidderId().equals(bidderId))
                    .mapToDouble(b -> b.getAmount()).max().orElse(0);
            boolean iWon = bidderId.equals(winnerName);
            addSessionHistory(bidderId, new AuctionSessionRecord(
                    session.getSessionId(), session.getItemName(), sellerId,
                    session.getStartingPrice(), finalPrice, winnerName, totalBids,
                    startTime, endTime, iWon ? "THẮNG GIÁ" : "THUA GIÁ",
                    "BIDDER", myMax, iWon));
        }
    }
}
