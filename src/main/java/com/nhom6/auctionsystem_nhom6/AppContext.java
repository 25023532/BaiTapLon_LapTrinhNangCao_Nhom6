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

    // Map sessionId → sellerUsername
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
            walletMap = new HashMap<>();

    private static final Map<String, List<TransactionRecord>>
            transactionMap = new HashMap<>();

    private static final Map<String, List<HistoryRecord>>
            historyMap = new HashMap<>();

    private static final Map<String, List<ProductRecord>>
            productMap = new HashMap<>();

    // Map username → danh sách phiên đã kết thúc mà user liên quan
    private static final Map<String, List<AuctionSessionRecord>>
            sessionHistoryMap = new HashMap<>();

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

            // ── Seed lịch sử phiên mẫu ────────────────────────────────────
            seedSessionHistory();

            System.out.println("AppContext: Seed data thành công.");
        } catch (Exception e) {
            System.out.println("AppContext: Lỗi seed data – " + e.getMessage());
        }
    }

    /**
     * Tạo một vài bản ghi phiên mẫu để màn hình lịch sử
     * không trống khi chạy lần đầu.
     */
    private static void seedSessionHistory() {
        LocalDateTime base = LocalDateTime.now();

        // Phiên 1 — bidder07 thắng, sellerlong bán
        AuctionSessionRecord s1seller = new AuctionSessionRecord(
                "SES-001", "iPhone 15 Pro Max", "sellerlong",
                20_000_000, 28_000_000, "bidder07",
                5,
                base.minusDays(2),
                base.minusDays(1),
                "THÀNH CÔNG",
                "SELLER", 0, false);

        AuctionSessionRecord s1bidder = new AuctionSessionRecord(
                "SES-001", "iPhone 15 Pro Max", "sellerlong",
                20_000_000, 28_000_000, "bidder07",
                5,
                base.minusDays(2),
                base.minusDays(1),
                "THẮNG GIÁ",
                "BIDDER", 28_000_000, true);

        AuctionSessionRecord s1loser = new AuctionSessionRecord(
                "SES-001", "iPhone 15 Pro Max", "sellerlong",
                20_000_000, 28_000_000, "bidder07",
                5,
                base.minusDays(2),
                base.minusDays(1),
                "THUA GIÁ",
                "BIDDER", 25_000_000, false);

        addSessionHistory("sellerlong", s1seller);
        addSessionHistory("bidder07",   s1bidder);
        addSessionHistory("bidder03",   s1loser);

        // Phiên 2 — không có người đặt giá
        AuctionSessionRecord s2seller = new AuctionSessionRecord(
                "SES-002", "MacBook Air M2", "sellerlong",
                18_000_000, 18_000_000, null,
                0,
                base.minusDays(4),
                base.minusDays(3),
                "KHÔNG CÓ NGƯỜI ĐẤU",
                "SELLER", 0, false);

        addSessionHistory("sellerlong", s2seller);

        // Phiên 3 — bidder03 thắng
        AuctionSessionRecord s3seller = new AuctionSessionRecord(
                "SES-003", "Sony Alpha A7 IV", "sellerlong",
                15_000_000, 17_500_000, "bidder03",
                3,
                base.minusDays(6),
                base.minusDays(5),
                "THÀNH CÔNG",
                "SELLER", 0, false);

        AuctionSessionRecord s3winner = new AuctionSessionRecord(
                "SES-003", "Sony Alpha A7 IV", "sellerlong",
                15_000_000, 17_500_000, "bidder03",
                3,
                base.minusDays(6),
                base.minusDays(5),
                "THẮNG GIÁ",
                "BIDDER", 17_500_000, true);

        addSessionHistory("sellerlong", s3seller);
        addSessionHistory("bidder03",   s3winner);
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
    public static AuctionSession getActiveSession()                    { return activeSession; }
    public static void           setActiveSession(AuctionSession s)    { activeSession = s; }

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
        return transactionMap.computeIfAbsent(username, k -> new ArrayList<>());
    }
    public static void addTransaction(String username, TransactionRecord record) {
        getTransactions(username).add(record);
    }

    // ─────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────
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

    /** Lấy tất cả sản phẩm của mọi seller — dùng để tra category */
    public static List<ProductRecord> getAllProducts() {
        List<ProductRecord> all = new ArrayList<>();
        productMap.values().forEach(all::addAll);
        return all;
    }

    // =========================================================
    // AUCTION SESSION HISTORY  ←← TÍNH NĂNG MỚI
    // =========================================================

    /**
     * Bản ghi một phiên đấu giá đã kết thúc từ góc nhìn của một user cụ thể.
     *
     * @param sessionId   ID phiên
     * @param itemName    Tên sản phẩm
     * @param sellerName  Tên người bán
     * @param startPrice  Giá khởi điểm
     * @param finalPrice  Giá chốt cuối cùng
     * @param winnerName  Người thắng (null nếu không ai đấu)
     * @param totalBids   Tổng số lượt đặt giá
     * @param startTime   Thời điểm bắt đầu phiên
     * @param endTime     Thời điểm kết thúc phiên
     * @param result      Kết quả từ góc nhìn user:
     *                    "THÀNH CÔNG" (seller bán được) |
     *                    "THẮNG GIÁ" (bidder thắng) |
     *                    "THUA GIÁ" (bidder thua) |
     *                    "KHÔNG CÓ NGƯỜI ĐẤU" (seller) |
     *                    "ĐÃ HỦY"
     * @param myRole      Role của user trong phiên: "SELLER" | "BIDDER"
     * @param myFinalBid  Giá cao nhất mà user đặt (0 nếu là seller / không đặt)
     * @param iWon        true nếu user là người thắng phiên này
     */
    public record AuctionSessionRecord(
            String        sessionId,
            String        itemName,
            String        sellerName,
            double        startPrice,
            double        finalPrice,
            String        winnerName,
            int           totalBids,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String        result,
            String        myRole,
            double        myFinalBid,
            boolean       iWon
    ) {}

    /**
     * Lưu thủ công một bản ghi phiên vào lịch sử của user.
     * Dùng khi cần ghi một chiều (ví dụ seed data).
     */
    public static void addSessionHistory(String username,
                                          AuctionSessionRecord record) {
        sessionHistoryMap
                .computeIfAbsent(username, k -> new CopyOnWriteArrayList<>())
                .add(record);
    }

    /**
     * Trả về lịch sử phiên của user, sắp xếp mới nhất trước.
     */
    public static List<AuctionSessionRecord> getSessionHistory(String username) {
        List<AuctionSessionRecord> list =
                sessionHistoryMap.getOrDefault(username, Collections.emptyList());
        List<AuctionSessionRecord> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparing(AuctionSessionRecord::endTime).reversed());
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Gọi khi một phiên kết thúc (countdown về 0 hoặc admin đóng phiên).
     * Tự động tạo AuctionSessionRecord phù hợp cho:
     *   - Seller sở hữu phiên
     *   - Từng Bidder đã đặt giá trong phiên
     *
     * Ví dụ sử dụng trong MainController:
     *   AppContext.finalizeSession(session, sessionStartTime);
     */
    public static void finalizeSession(AuctionSession session,
                                        LocalDateTime  startTime) {
        if (session == null) return;

        var    bids       = session.getBidHistory();
        int    totalBids  = bids.size();
        double finalPrice = session.getCurrentPrice();
        String sellerId   = getSessionSeller(session.getSessionId());
        LocalDateTime endTime = LocalDateTime.now();

        // Xác định người thắng = người có bid cao nhất (bid cuối trong list đã sort)
        String winnerName = null;
        if (!bids.isEmpty()) {
            winnerName = bids.get(bids.size() - 1).getBidderId();
        }

        String sellerResult = (totalBids == 0) ? "KHÔNG CÓ NGƯỜI ĐẤU" : "THÀNH CÔNG";

        // ── Lưu cho Seller ──────────────────────────────────
        addSessionHistory(sellerId, new AuctionSessionRecord(
                session.getSessionId(),
                session.getItemName(),
                sellerId,
                session.getStartingPrice(),
                finalPrice,
                winnerName,
                totalBids,
                startTime,
                endTime,
                sellerResult,
                "SELLER",
                0,
                false
        ));

        // ── Lưu cho từng Bidder (không trùng lặp) ───────────
        Set<String> seen = new LinkedHashSet<>();
        for (var bid : bids) {
            seen.add(bid.getBidderId());
        }

        for (String bidderId : seen) {
            // Giá cao nhất mà bidder này từng đặt trong phiên
            double myHighestBid = bids.stream()
                    .filter(b -> b.getBidderId().equals(bidderId))
                    .mapToDouble(b -> b.getAmount())
                    .max()
                    .orElse(0);

            boolean iWon   = bidderId.equals(winnerName);
            String  result = iWon ? "THẮNG GIÁ" : "THUA GIÁ";

            addSessionHistory(bidderId, new AuctionSessionRecord(
                    session.getSessionId(),
                    session.getItemName(),
                    sellerId,
                    session.getStartingPrice(),
                    finalPrice,
                    winnerName,
                    totalBids,
                    startTime,
                    endTime,
                    result,
                    "BIDDER",
                    myHighestBid,
                    iWon
            ));
        }

        System.out.printf("AppContext: Phiên \"%s\" đã được lưu vào lịch sử " +
                "(%d bidder liên quan).%n", session.getItemName(), seen.size());
    }
}
