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
    // ONLINE USER TRACKING  (từ doc 5)
    // =========================================================
    private static final Set<String> onlineUsers =
            Collections.synchronizedSet(new HashSet<>());

    /** Số online nhận từ server WebSocket — 0 = chưa nhận */
    private static int serverOnlineCount = 0;

    public static void markUserOnline(String username) {
        if (username != null && !username.isBlank())
            onlineUsers.add(username);
    }

    public static void markUserOffline(String username) {
        if (username != null) onlineUsers.remove(username);
    }

    /**
     * Lấy số user online.
     * Ưu tiên số từ server; nếu offline dùng local tracking (ít nhất 1).
     */
    public static int getOnlineUserCount() {
        if (serverOnlineCount > 0) return serverOnlineCount;
        return Math.max(1, onlineUsers.size());
    }

    /** Cập nhật số online từ server WebSocket (ONLINE_COUNT message). */
    public static void setServerOnlineCount(int count) {
        serverOnlineCount = Math.max(0, count);
    }

    // =========================================================
    // PHIÊN ĐẤU GIÁ
    // =========================================================
    private static final List<AuctionSession> globalSessions =
            new CopyOnWriteArrayList<>();
    private static final Map<String, String> sessionSellerMap =
            new HashMap<>();

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
                    && LocalDateTime.now().isBefore(s.getEndTime()))
                result.add(s);
        }
        return result;
    }

    public static List<AuctionSession> getGlobalSessions() {
        return Collections.unmodifiableList(globalSessions);
    }

    public static boolean applyBidToSession(String sessionId,
                                            org.example.auction.Bid bid) {
        if (sessionId == null || bid == null) return false;
        for (AuctionSession session : globalSessions) {
            if (!session.getSessionId().equals(sessionId)) continue;
            if (hasBid(session, bid)) return false;
            try {
                session.placeBid(bid);
                return true;
            } catch (Exception e) {
                System.err.println("AppContext: applyBidToSession error: "
                        + e.getMessage());
                return false;
            }
        }
        return false;
    }

    private static boolean hasBid(AuctionSession session,
                                  org.example.auction.Bid bid) {
        return session.getBidHistory().stream().anyMatch(existing ->
                existing.getBidderId().equals(bid.getBidderId())
                        && Double.compare(existing.getAmount(),
                                bid.getAmount()) == 0);
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

    /**
     * KEY = sellerUsername → VALUE = list sản phẩm.
     * Vì cùng 1 JVM / 1 Stage (HelloApplication.setRoot),
     * static Map này được share trực tiếp giữa mọi controller.
     */
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

    /**
     * Seed data khởi tạo:
     * – Tài khoản + ví mẫu.
     * – Sản phẩm mẫu (ĐÃ BÁN) dùng addProductInternal để bypass CHỜ DUYỆT.
     * – Lịch sử phiên đấu giá mẫu.
     */
    private static void seedData() {
        try {
            // ── Tài khoản ────────────────────────────────────
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

            // ── Ví ───────────────────────────────────────────
            walletMap.put("bidder07",   5_000_000.0);
            walletMap.put("sellerlong", 12_000_000.0);
            walletMap.put("bidder03",   2_500_000.0);
            walletMap.put("bidder01",   0.0);

            // ── Giao dịch mẫu ────────────────────────────────
            addTransaction("bidder07", new TransactionRecord(
                    "TX001", "NẠP TIỀN", 5_000_000,
                    "Nạp qua ngân hàng VCB", "THÀNH CÔNG",
                    LocalDateTime.now().minusDays(3)));
            addTransaction("sellerlong", new TransactionRecord(
                    "TX003", "NẠP TIỀN", 12_000_000,
                    "Nạp qua MoMo", "THÀNH CÔNG",
                    LocalDateTime.now().minusDays(5)));

            // ── Sản phẩm mẫu (ĐÃ BÁN — bypass CHỜ DUYỆT) ───
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

            // ── Lịch sử phiên ────────────────────────────────
            seedSessionHistory();

            System.out.println("AppContext: Seed data OK.");
        } catch (Exception e) {
            System.out.println("AppContext: Lỗi seed – " + e.getMessage());
        }
    }

    private static void seedSessionHistory() {
        LocalDateTime base = LocalDateTime.now();

        addSessionHistory("sellerlong", new AuctionSessionRecord(
                "SES-001", "iPhone 15 Pro Max", "sellerlong",
                20_000_000, 28_000_000, "bidder07", 5,
                base.minusDays(2), base.minusDays(1),
                "THÀNH CÔNG", "SELLER", 0, false));
        addSessionHistory("bidder07", new AuctionSessionRecord(
                "SES-001", "iPhone 15 Pro Max", "sellerlong",
                20_000_000, 28_000_000, "bidder07", 5,
                base.minusDays(2), base.minusDays(1),
                "THẮNG GIÁ", "BIDDER", 28_000_000, true));
        addSessionHistory("bidder03", new AuctionSessionRecord(
                "SES-001", "iPhone 15 Pro Max", "sellerlong",
                20_000_000, 28_000_000, "bidder07", 5,
                base.minusDays(2), base.minusDays(1),
                "THUA GIÁ", "BIDDER", 25_000_000, false));
        addSessionHistory("sellerlong", new AuctionSessionRecord(
                "SES-002", "MacBook Air M2", "sellerlong",
                18_000_000, 18_000_000, null, 0,
                base.minusDays(4), base.minusDays(3),
                "KHÔNG CÓ NGƯỜI ĐẤU", "SELLER", 0, false));
    }

    // =========================================================
    // AUTH
    // =========================================================
    public static AuthService getAuthService() { return authService; }

    // =========================================================
    // USER — markUserOnline khi setCurrentUser
    // =========================================================
    public static User getCurrentUser() { return currentUser; }

    public static void setCurrentUser(User user) {
        currentUser = user;
        if (user != null) markUserOnline(user.getUsername());
    }

    // =========================================================
    // SESSION ĐANG HOẠT ĐỘNG / AVATAR
    // =========================================================
    public static AuctionSession getActiveSession()                 { return activeSession; }
    public static void           setActiveSession(AuctionSession s) { activeSession = s; }
    public static Image          getAvatarImage()                   { return avatarImage; }
    public static void           setAvatarImage(Image img)          { avatarImage = img; }

    // =========================================================
    // ĐĂNG XUẤT
    // =========================================================
    public static void logout() {
        if (currentUser != null) markUserOffline(currentUser.getUsername());
        currentUser       = null;
        activeSession     = null;
        avatarImage       = null;
        serverOnlineCount = 0;
    }

    // =========================================================
    // VÍ
    // =========================================================
    public static double getWalletBalance(String username) {
        return walletMap.getOrDefault(username, 0.0);
    }

    /** Ghi thẳng balance — dùng cho WALLET_UPDATE sync từ server */
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
        ServerConnection conn = ServerConnection.getInstance();
        if (conn.isConnected()) conn.sendDeposit(username, amount);
        return true;
    }

    public static boolean payment(String username, double amount,
                                   String description) {
        if (amount <= 0) return false;
        double cur = walletMap.getOrDefault(username, 0.0);
        if (cur < amount) return false;
        walletMap.put(username, cur - amount);
        addTransaction(username, new TransactionRecord(
                "TX-" + System.currentTimeMillis(), "THANH TOÁN",
                -amount, description, "THÀNH CÔNG",
                LocalDateTime.now()));
        ServerConnection conn = ServerConnection.getInstance();
        if (conn.isConnected()) conn.sendPayment(username, amount, description);
        return true;
    }

    // =========================================================
    // RECORDS — TRANSACTION
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

    // =========================================================
    // RECORDS — HISTORY
    // =========================================================
    public record HistoryRecord(
            String id, String itemName, double amount,
            String counterparty, String status,
            boolean wonBid, LocalDateTime time) {}

    public static List<HistoryRecord> getHistory(String u) {
        return historyMap.computeIfAbsent(u, k -> new ArrayList<>());
    }
    public static void addHistory(String u, HistoryRecord r) {
        getHistory(u).add(r);
        ServerConnection conn = ServerConnection.getInstance();
        if (conn.isConnected()) conn.sendAddHistory(u, r);
    }

    // =========================================================
    // RECORDS — PRODUCT
    // =========================================================
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

    // =========================================================
    // TÍNH STATUS HIỂN THỊ THEO THỜI GIAN THỰC  (từ doc 6)
    // =========================================================
    /**
     * Sau khi Admin duyệt → status = "ĐÃ DUYỆT".
     * Hàm này tự tính trạng thái hiển thị theo giờ thực:
     *
     *   now < startTime           → "SẮP DIỄN RA"
     *   startTime ≤ now < endTime → "ĐANG ĐẤU GIÁ"
     *   now ≥ endTime             → "ĐÃ KẾT THÚC"
     *
     * Các status cố định (CHỜ DUYỆT / TỪ CHỐI / ĐÃ BÁN / ĐÃ HỦY)
     * được trả về nguyên giá trị.
     */
    public static String computeDisplayStatus(ProductRecord p) {
        switch (p.status()) {
            case "CHỜ DUYỆT":
            case "TỪ CHỐI":
            case "ĐÃ BÁN":
            case "ĐÃ HỦY":
                return p.status();
        }
        // ĐÃ DUYỆT hoặc trạng thái vòng đời → tính theo giờ
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(p.startTime()))    return "SẮP DIỄN RA";
        else if (now.isBefore(p.endTime())) return "ĐANG ĐẤU GIÁ";
        else                                return "ĐÃ KẾT THÚC";
    }

    // =========================================================
    // KIỂM TRA TRÙNG THỜI GIAN  (từ doc 6)
    // =========================================================
    /**
     * Kiểm tra [newStart, newEnd] có chồng thời gian với sản phẩm nào
     * của seller đang ở CHỜ DUYỆT / ĐÃ DUYỆT / SẮP DIỄN RA / ĐANG ĐẤU GIÁ.
     *
     * Chặn cả CHỜ DUYỆT vì nếu admin duyệt sau sẽ trùng giờ.
     * Bỏ qua: TỪ CHỐI / ĐÃ BÁN / ĐÃ KẾT THÚC / ĐÃ HỦY.
     *
     * @param sellerUsername seller đang đăng / sửa
     * @param newStart       thời gian bắt đầu cần kiểm tra
     * @param newEnd         thời gian kết thúc cần kiểm tra
     * @param excludeId      id sản phẩm bỏ qua (null khi thêm mới)
     * @return tên + [trạng thái] sản phẩm bị trùng, hoặc null
     */
    public static String findTimeConflictForSeller(String sellerUsername,
                                                    LocalDateTime newStart,
                                                    LocalDateTime newEnd,
                                                    String excludeId) {
        for (ProductRecord p : getProducts(sellerUsername)) {
            if (excludeId != null && excludeId.equals(p.id())) continue;
            String ds = computeDisplayStatus(p);
            if ("TỪ CHỐI".equals(ds) || "ĐÃ BÁN".equals(ds)
                    || "ĐÃ KẾT THÚC".equals(ds) || "ĐÃ HỦY".equals(ds))
                continue;
            boolean overlap = newStart.isBefore(p.endTime())
                           && newEnd.isAfter(p.startTime());
            if (overlap) return p.name() + " [" + ds + "]";
        }
        return null;
    }

    // =========================================================
    // PRODUCT MAP — CRUD
    // =========================================================
    public static List<ProductRecord> getProducts(String username) {
        return productMap.computeIfAbsent(username, k -> new ArrayList<>());
    }

    /**
     * Dùng nội bộ: seed data, server sync.
     * KHÔNG enforce status → cho phép truyền bất kỳ status nào.
     */
    static void addProductInternal(String username, ProductRecord product) {
        productMap.computeIfAbsent(username, k -> new ArrayList<>())
                  .add(product);
    }

    /**
     * Seller đăng sản phẩm mới qua UI.
     * Status luôn được enforce là CHỜ DUYỆT — không thể bypass từ UI.
     * Sau khi thêm vào productMap, gửi lên server để đồng bộ.
     */
    public static void addProduct(String username, ProductRecord product) {
        ProductRecord enforced = new ProductRecord(
                product.id(), product.name(), product.category(),
                product.startPrice(), product.currentPrice(),
                product.bidCount(),
                "CHỜ DUYỆT",   // ← luôn enforce
                product.startTime(), product.endTime(),
                product.topBidder());
        productMap.computeIfAbsent(username, k -> new ArrayList<>())
                  .add(enforced);
        System.out.printf(
                "AppContext: [ADD] seller=%s product=\"%s\" → CHỜ DUYỆT%n",
                username, product.name());
        ServerConnection conn = ServerConnection.getInstance();
        if (conn.isConnected()) conn.sendAddProduct(username, enforced);
    }

    public static void removeProduct(String username, String productId) {
        productMap.computeIfAbsent(username, k -> new ArrayList<>())
                  .removeIf(p -> p.id().equals(productId));
    }

    /**
     * Cập nhật sản phẩm — Admin duyệt/từ chối, Seller sửa thông tin.
     * Không enforce status ở đây vì Admin cần đổi sang ĐÃ DUYỆT / TỪ CHỐI.
     * Gửi lên server để đồng bộ các client khác.
     */
    public static void updateProduct(String username, ProductRecord updated) {
        List<ProductRecord> list =
                productMap.computeIfAbsent(username, k -> new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(updated.id())) {
                list.set(i, updated);
                System.out.printf(
                        "AppContext: [UPDATE] seller=%s product=\"%s\" → %s%n",
                        username, updated.name(), updated.status());
                ServerConnection conn = ServerConnection.getInstance();
                if (conn.isConnected()) conn.sendUpdateProduct(username, updated);
                return;
            }
        }
        System.out.printf(
                "AppContext: [UPDATE-WARN] id=%s không tìm thấy seller=%s%n",
                updated.id(), username);
    }

    /**
     * Cập nhật sản phẩm mà KHÔNG gửi lên server.
     * Dùng khi nhận sync từ server để tránh vòng lặp gửi–nhận.
     */
    public static void updateProductSilent(String username,
                                            ProductRecord updated) {
        List<ProductRecord> list =
                productMap.computeIfAbsent(username, k -> new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(updated.id())) {
                list.set(i, updated);
                System.out.printf(
                        "AppContext: [UPDATE-SILENT] seller=%s product=\"%s\" → %s%n",
                        username, updated.name(), updated.status());
                return;
            }
        }
    }

    /** Tất cả sản phẩm của mọi seller — Admin dùng để thống kê */
    public static List<ProductRecord> getAllProducts() {
        List<ProductRecord> all = new ArrayList<>();
        productMap.values().forEach(all::addAll);
        return Collections.unmodifiableList(all);
    }

    /**
     * Tất cả sản phẩm CHỜ DUYỆT của mọi seller.
     * AdminController gọi hàm này để render danh sách cần duyệt.
     * Vì cùng JVM, kết quả luôn phản ánh đúng thời điểm thực.
     */
    public static List<ProductRecord> getAllPendingProducts() {
        List<ProductRecord> pending = new ArrayList<>();
        for (List<ProductRecord> sellerList : productMap.values())
            for (ProductRecord p : sellerList)
                if ("CHỜ DUYỆT".equals(p.status()))
                    pending.add(p);
        System.out.println("AppContext: getAllPendingProducts() → "
                + pending.size() + " sản phẩm");
        return Collections.unmodifiableList(pending);
    }

    /** Tra ngược productMap: productId → sellerUsername */
    public static String getSellerForProduct(String productId) {
        for (Map.Entry<String, List<ProductRecord>> entry : productMap.entrySet())
            for (ProductRecord p : entry.getValue())
                if (p.id().equals(productId))
                    return entry.getKey();
        return "—";
    }

    // =========================================================
    // RECORDS — RATING / REVIEW  (từ doc 5)
    // =========================================================
    /**
     * Bản ghi đánh giá hệ thống của người dùng.
     *
     * @param id       ID duy nhất
     * @param username Người đánh giá
     * @param avatar   Chữ viết tắt (2 ký tự đầu username)
     * @param stars    Số sao (1–5)
     * @param comment  Nội dung nhận xét
     * @param time     Thời điểm đánh giá
     * @param likes    Số lượt thích
     */
    public record RatingRecord(
            String        id,
            String        username,
            String        avatar,
            int           stars,
            String        comment,
            LocalDateTime time,
            int           likes) {}

    private static final List<RatingRecord> ratingList =
            new CopyOnWriteArrayList<>();

    static {
        // Seed đánh giá mẫu
        ratingList.add(new RatingRecord(
                "R001", "bidder07", "BI", 5,
                "Hệ thống đấu giá rất mượt, giao dịch nhanh chóng!",
                LocalDateTime.now().minusDays(2), 12));
        ratingList.add(new RatingRecord(
                "R002", "sellerlong", "SE", 4,
                "Dễ đăng bán sản phẩm, Admin duyệt nhanh.",
                LocalDateTime.now().minusDays(1), 8));
        ratingList.add(new RatingRecord(
                "R003", "bidder03", "BI", 5,
                "Chat trực tiếp trong phiên rất tiện lợi.",
                LocalDateTime.now().minusHours(6), 5));
    }

    /** Lấy tất cả đánh giá, mới nhất trước. */
    public static List<RatingRecord> getRatings() {
        List<RatingRecord> sorted = new ArrayList<>(ratingList);
        sorted.sort(Comparator.comparing(RatingRecord::time).reversed());
        return Collections.unmodifiableList(sorted);
    }

    /** Thêm đánh giá mới từ user. */
    public static void addRating(RatingRecord record) {
        ratingList.add(record);
    }

    /** Cập nhật số like của một đánh giá. */
    public static void likeRating(String ratingId) {
        for (int i = 0; i < ratingList.size(); i++) {
            RatingRecord r = ratingList.get(i);
            if (r.id().equals(ratingId)) {
                ratingList.set(i, new RatingRecord(
                        r.id(), r.username(), r.avatar(),
                        r.stars(), r.comment(), r.time(),
                        r.likes() + 1));
                return;
            }
        }
    }

    /**
     * Sync toàn bộ danh sách đánh giá từ server.
     * Dùng khi nhận SYNC_RATINGS message — overwrite local cache.
     * Giữ lại các rating seed nếu server trả về list rỗng.
     */
    public static void syncRatings(List<RatingRecord> newRatings) {
        if (newRatings == null || newRatings.isEmpty()) return;
        ratingList.clear();
        ratingList.addAll(newRatings);
        System.out.println("AppContext: syncRatings() → "
                + ratingList.size() + " đánh giá");
    }

    // =========================================================
    // RECORDS — AUCTION SESSION HISTORY
    // =========================================================
    public record AuctionSessionRecord(
            String sessionId, String itemName, String sellerName,
            double startPrice, double finalPrice, String winnerName,
            int totalBids, LocalDateTime startTime, LocalDateTime endTime,
            String result, String myRole, double myFinalBid, boolean iWon) {}

    public static void addSessionHistory(String u, AuctionSessionRecord r) {
        sessionHistoryMap
                .computeIfAbsent(u, k -> new CopyOnWriteArrayList<>())
                .add(r);
        ServerConnection conn = ServerConnection.getInstance();
        if (conn.isConnected()) conn.sendAddSessionHistory(u, r);
    }

    public static List<AuctionSessionRecord> getSessionHistory(String username) {
        List<AuctionSessionRecord> list =
                sessionHistoryMap.getOrDefault(username, Collections.emptyList());
        List<AuctionSessionRecord> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparing(AuctionSessionRecord::endTime).reversed());
        return Collections.unmodifiableList(sorted);
    }

    // =========================================================
    // SYNC METHODS — overwrite local cache với state từ server
    // =========================================================
    public static void syncWallets(Map<String, Double> newWallets) {
        walletMap.clear();
        walletMap.putAll(newWallets);
    }

    public static void syncTransactions(
            Map<String, List<TransactionRecord>> newTx) {
        transactionMap.clear();
        newTx.forEach((k, v) -> transactionMap.put(k, new ArrayList<>(v)));
    }

    public static void syncProducts(
            Map<String, List<ProductRecord>> newProducts) {
        productMap.clear();
        newProducts.forEach((k, v) -> productMap.put(k, new ArrayList<>(v)));
    }

    public static void syncHistory(
            Map<String, List<HistoryRecord>> newHistory) {
        historyMap.clear();
        newHistory.forEach((k, v) -> historyMap.put(k, new ArrayList<>(v)));
    }

    public static void syncSessionHistory(
            Map<String, List<AuctionSessionRecord>> newSH) {
        sessionHistoryMap.clear();
        newSH.forEach((k, v) ->
                sessionHistoryMap.put(k, new CopyOnWriteArrayList<>(v)));
    }

    /**
     * Sync một phiên đấu giá đang chạy từ server.
     * Nếu phiên chưa tồn tại trong globalSessions → tạo mới và start.
     */
    public static void syncRunningSession(String sessionId, String itemName,
                                          double startPrice, double minStep,
                                          LocalDateTime endTime, String sellerName,
                                          java.util.List<org.example.auction.Bid> bids) {
        try {
            org.example.auction.AuctionSession existing =
                globalSessions.stream()
                    .filter(s -> s.getSessionId().equals(sessionId))
                    .findFirst().orElse(null);
            if (existing == null) {
                org.example.auction.AuctionSession s =
                    new org.example.auction.AuctionSession(
                        sessionId, itemName, startPrice, minStep, endTime);
                s.start();
                for (org.example.auction.Bid b : bids) {
                    try { s.placeBid(b); } catch (Exception ignored) {}
                }
                registerSession(s, sellerName);
// Notify MainController để re-resolve session
                javafx.application.Platform.runLater(() -> {
                    if (AppContext.getActiveSession() == null) {
                        List<AuctionSession> running = getRunningSessions();
                        if (!running.isEmpty()) setActiveSession(running.get(0));
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("syncRunningSession error: " + e.getMessage());
        }
    }

    // =========================================================
    // FINALIZE SESSION
    // =========================================================
    public static void finalizeSession(AuctionSession session,
                                        LocalDateTime startTime) {
        if (session == null) return;
        var    bids       = session.getBidHistory();
        int    totalBids  = bids.size();
        double finalPrice = session.getCurrentPrice();
        String sellerId   = getSessionSeller(session.getSessionId());
        LocalDateTime endTime = LocalDateTime.now();
        String winnerName = bids.isEmpty()
                ? null : bids.get(bids.size() - 1).getBidderId();

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
                    .mapToDouble(b -> b.getAmount())
                    .max().orElse(0);
            boolean iWon = bidderId.equals(winnerName);
            addSessionHistory(bidderId, new AuctionSessionRecord(
                    session.getSessionId(), session.getItemName(), sellerId,
                    session.getStartingPrice(), finalPrice, winnerName,
                    totalBids, startTime, endTime,
                    iWon ? "THẮNG GIÁ" : "THUA GIÁ",
                    "BIDDER", myMax, iWon));
        }

        System.out.printf(
                "AppContext: Phiên \"%s\" đã kết thúc (%d bidder).%n",
                session.getItemName(), seen.size());
    }
}
