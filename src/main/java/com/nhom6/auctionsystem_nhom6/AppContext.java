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
    // CÁC MAP LƯU TRỮ
    // =========================================================
    private static final Map<String, Double>
            walletMap = new HashMap<>();

    private static final Map<String, List<TransactionRecord>>
            transactionMap = new HashMap<>();

    private static final Map<String, List<HistoryRecord>>
            historyMap = new HashMap<>();

    /**
     * KEY = sellerUsername, VALUE = danh sách sản phẩm của seller đó.
     * Mọi seller đăng sản phẩm đều được lưu vào đây với status CHỜ DUYỆT.
     * Admin gọi getAllProducts() để lấy tất cả, lọc CHỜ DUYỆT để duyệt.
     */
    private static final Map<String, List<ProductRecord>>
            productMap = new HashMap<>();

    // Map username → danh sách phiên đã kết thúc mà user liên quan
    private static final Map<String, List<AuctionSessionRecord>>
            sessionHistoryMap = new HashMap<>();

    // =========================================================
    // KHỞI TẠO TĨNH
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

    /**
     * Seed data khởi tạo:
     * - Chỉ tạo tài khoản mẫu (admin, seller, bidder).
     * - KHÔNG tạo sản phẩm sẵn để tránh bypass luồng duyệt.
     * - Chỉ seed lịch sử giao dịch / ví để demo.
     */
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

            // ── Ví ────────────────────────────────────────────
            walletMap.put("bidder07",   5_000_000.0);
            walletMap.put("sellerlong", 12_000_000.0);
            walletMap.put("bidder03",   2_500_000.0);
            walletMap.put("bidder01",   0.0);

            // ── Lịch sử giao dịch mẫu ─────────────────────────
            addTransaction("bidder07", new TransactionRecord(
                    "TX001", "NẠP TIỀN", 5_000_000,
                    "Nạp qua ngân hàng VCB", "THÀNH CÔNG",
                    LocalDateTime.now().minusDays(3)));
            addTransaction("sellerlong", new TransactionRecord(
                    "TX003", "NẠP TIỀN", 12_000_000,
                    "Nạp qua MoMo", "THÀNH CÔNG",
                    LocalDateTime.now().minusDays(5)));

            // ── Lịch sử phiên mẫu (đã kết thúc) ──────────────
            seedSessionHistory();

            // ── KHÔNG seed sản phẩm ───────────────────────────
            // Mọi sản phẩm phải được seller tự đăng qua giao diện
            // và đi qua luồng: CHỜ DUYỆT → ĐÃ DUYỆT → ĐANG ĐẤU GIÁ

            System.out.println("AppContext: Seed data thành công.");
        } catch (Exception e) {
            System.out.println("AppContext: Lỗi seed data – " + e.getMessage());
        }
    }

    private static void seedSessionHistory() {
        LocalDateTime base = LocalDateTime.now();

        AuctionSessionRecord s1seller = new AuctionSessionRecord(
                "SES-001", "iPhone 15 Pro Max", "sellerlong",
                20_000_000, 28_000_000, "bidder07", 5,
                base.minusDays(2), base.minusDays(1),
                "THÀNH CÔNG", "SELLER", 0, false);
        AuctionSessionRecord s1bidder = new AuctionSessionRecord(
                "SES-001", "iPhone 15 Pro Max", "sellerlong",
                20_000_000, 28_000_000, "bidder07", 5,
                base.minusDays(2), base.minusDays(1),
                "THẮNG GIÁ", "BIDDER", 28_000_000, true);
        AuctionSessionRecord s1loser = new AuctionSessionRecord(
                "SES-001", "iPhone 15 Pro Max", "sellerlong",
                20_000_000, 28_000_000, "bidder07", 5,
                base.minusDays(2), base.minusDays(1),
                "THUA GIÁ", "BIDDER", 25_000_000, false);

        addSessionHistory("sellerlong", s1seller);
        addSessionHistory("bidder07",   s1bidder);
        addSessionHistory("bidder03",   s1loser);

        AuctionSessionRecord s2seller = new AuctionSessionRecord(
                "SES-002", "MacBook Air M2", "sellerlong",
                18_000_000, 18_000_000, null, 0,
                base.minusDays(4), base.minusDays(3),
                "KHÔNG CÓ NGƯỜI ĐẤU", "SELLER", 0, false);
        addSessionHistory("sellerlong", s2seller);
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
    // PHIÊN ĐẤU GIÁ ĐANG HOẠT ĐỘNG
    // =========================================================
    public static AuctionSession getActiveSession()                 { return activeSession; }
    public static void           setActiveSession(AuctionSession s) { activeSession = s; }

    // =========================================================
    // ẢNH ĐẠI DIỆN
    // =========================================================
    public static Image getAvatarImage()          { return avatarImage; }
    public static void  setAvatarImage(Image img) { avatarImage = img; }

    // =========================================================
    // ĐĂNG XUẤT
    // =========================================================
    public static void logout() {
        currentUser   = null;
        activeSession = null;
        avatarImage   = null;
        System.out.println("AppContext: Đã đăng xuất.");
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
                amount, "Nạp tiền vào ví", "THÀNH CÔNG",
                LocalDateTime.now()));
        return true;
    }

    public static boolean payment(String username, double amount, String description) {
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

    // ── CRUD sản phẩm ─────────────────────────────────────────

    /**
     * Lấy danh sách sản phẩm của một seller cụ thể.
     * Dùng trong MyProductsController (seller xem sản phẩm của mình).
     */
    public static List<ProductRecord> getProducts(String username) {
        return productMap.computeIfAbsent(username, k -> new ArrayList<>());
    }

    /**
     * Thêm sản phẩm cho seller.
     * Luôn được gọi với status = CHỜ DUYỆT từ MyProductsController.
     * Admin sẽ thấy ngay qua getAllProducts().
     */
    public static void addProduct(String username, ProductRecord product) {
        // Đảm bảo status phải là CHỜ DUYỆT khi mới thêm
        ProductRecord enforced = "CHỜ DUYỆT".equals(product.status())
                ? product
                : new ProductRecord(
                        product.id(), product.name(), product.category(),
                        product.startPrice(), product.currentPrice(),
                        product.bidCount(), "CHỜ DUYỆT",
                        product.startTime(), product.endTime(),
                        product.topBidder());
        getProducts(username).add(enforced);

        System.out.printf("AppContext: Seller \"%s\" đăng sản phẩm \"%s\" → CHỜ DUYỆT%n",
                username, product.name());
    }

    public static void removeProduct(String username, String productId) {
        getProducts(username).removeIf(p -> p.id().equals(productId));
    }

    public static void updateProduct(String username, ProductRecord updated) {
        List<ProductRecord> list = getProducts(username);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(updated.id())) {
                list.set(i, updated);
                System.out.printf("AppContext: Cập nhật sản phẩm \"%s\" → %s%n",
                        updated.name(), updated.status());
                return;
            }
        }
        System.out.printf("AppContext: WARN – không tìm thấy sản phẩm id=%s của seller=%s%n",
                updated.id(), username);
    }

    /**
     * Lấy TẤT CẢ sản phẩm của mọi seller.
     * AdminController dùng hàm này để hiện danh sách CHỜ DUYỆT.
     */
    public static List<ProductRecord> getAllProducts() {
        List<ProductRecord> all = new ArrayList<>();
        productMap.values().forEach(all::addAll);
        return Collections.unmodifiableList(all);
    }

    /**
     * Lấy tất cả sản phẩm CHỜ DUYỆT của mọi seller.
     * Tiện dùng trong AdminController để tránh lọc lại.
     */
    public static List<ProductRecord> getAllPendingProducts() {
        List<ProductRecord> pending = new ArrayList<>();
        for (List<ProductRecord> sellerProducts : productMap.values()) {
            for (ProductRecord p : sellerProducts) {
                if ("CHỜ DUYỆT".equals(p.status())) {
                    pending.add(p);
                }
            }
        }
        return Collections.unmodifiableList(pending);
    }

    /**
     * Tìm tên seller của một sản phẩm theo productId.
     * Tra ngược productMap: id → key (sellerUsername).
     * AdminController dùng: AppContext.getSellerForProduct(p.id())
     */
    public static String getSellerForProduct(String productId) {
        for (Map.Entry<String, List<ProductRecord>> entry : productMap.entrySet()) {
            for (ProductRecord p : entry.getValue()) {
                if (p.id().equals(productId)) {
                    return entry.getKey();
                }
            }
        }
        return "—";
    }

    // =========================================================
    // LỊCH SỬ PHIÊN ĐẤU GIÁ
    // =========================================================
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

    public static void addSessionHistory(String username, AuctionSessionRecord record) {
        sessionHistoryMap
                .computeIfAbsent(username, k -> new CopyOnWriteArrayList<>())
                .add(record);
    }

    public static List<AuctionSessionRecord> getSessionHistory(String username) {
        List<AuctionSessionRecord> list =
                sessionHistoryMap.getOrDefault(username, Collections.emptyList());
        List<AuctionSessionRecord> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparing(AuctionSessionRecord::endTime).reversed());
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Gọi khi một phiên kết thúc.
     * Tự động tạo AuctionSessionRecord cho seller và tất cả bidder đã tham gia.
     */
    public static void finalizeSession(AuctionSession session,
                                        LocalDateTime  startTime) {
        if (session == null) return;

        var    bids       = session.getBidHistory();
        int    totalBids  = bids.size();
        double finalPrice = session.getCurrentPrice();
        String sellerId   = getSessionSeller(session.getSessionId());
        LocalDateTime endTime = LocalDateTime.now();

        String winnerName = null;
        if (!bids.isEmpty()) {
            winnerName = bids.get(bids.size() - 1).getBidderId();
        }

        String sellerResult = (totalBids == 0) ? "KHÔNG CÓ NGƯỜI ĐẤU" : "THÀNH CÔNG";

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

        Set<String> seen = new LinkedHashSet<>();
        for (var bid : bids) {
            seen.add(bid.getBidderId());
        }

        for (String bidderId : seen) {
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
