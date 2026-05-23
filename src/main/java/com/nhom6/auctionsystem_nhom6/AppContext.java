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

public class AppContext {

    // =========================================================
    // GLOBAL STATE
    // =========================================================
    private static User currentUser;
    private static AuctionSession activeSession;
    private static Image avatarImage;

    private static final AuthService authService =
            new AuthService();

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

        sessionSellerMap.put(
                session.getSessionId(),
                sellerUsername
        );
    }

    public static String getSessionSeller(String sessionId) {
        return sessionSellerMap.getOrDefault(
                sessionId,
                "Seller"
        );
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
    private static final Map<String, Double> walletMap =
            new ConcurrentHashMap<>();

    private static final Map<String,
            List<TransactionRecord>> transactionMap =
            new ConcurrentHashMap<>();

    private static final Map<String,
            List<HistoryRecord>> historyMap =
            new ConcurrentHashMap<>();

    private static final Map<String,
            List<AuctionSessionRecord>> sessionHistoryMap =
            new ConcurrentHashMap<>();

    private static final Map<String,
            List<ProductRecord>> productMap =
            new ConcurrentHashMap<>();

    // =========================================================
    // ONLINE USER TRACKING
    // =========================================================
    private static final Set<String> onlineUsers =
            Collections.synchronizedSet(new HashSet<>());

    public static void markUserOnline(String username) {

        if (username != null && !username.isBlank()) {
            onlineUsers.add(username);
        }
    }

    public static void markUserOffline(String username) {

        if (username != null) {
            onlineUsers.remove(username);
        }
    }

    public static int getOnlineUserCount() {
        return Math.max(1, onlineUsers.size());
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

            ServerConnection conn =
                    ServerConnection.getInstance();

            boolean ok = conn.connect();

            System.out.println(
                    ok
                            ? "AppContext: Kết nối server OK."
                            : "AppContext: Chạy offline."
            );

        } catch (Exception e) {

            System.out.println(
                    "AppContext: Lỗi kết nối server."
            );
        }
    }

    private static void seedData() {

        try {

            if (!authService.isRegistered("admin")) {
                authService.register(
                        new Admin(
                                "A001",
                                "admin",
                                "admin123"
                        )
                );
            }

            if (!authService.isRegistered("sellerlong")) {
                authService.register(
                        new Seller(
                                "S001",
                                "sellerlong",
                                "seller123"
                        )
                );
            }

            if (!authService.isRegistered("bidder07")) {
                authService.register(
                        new Bidder(
                                "B001",
                                "bidder07",
                                "bidder123"
                        )
                );
            }

            if (!authService.isRegistered("bidder03")) {
                authService.register(
                        new Bidder(
                                "B002",
                                "bidder03",
                                "bidder123"
                        )
                );
            }

            if (!authService.isRegistered("bidder01")) {
                authService.register(
                        new Bidder(
                                "B003",
                                "bidder01",
                                "bidder123"
                        )
                );
            }

            walletMap.put("bidder07", 5000000.0);
            walletMap.put("sellerlong", 12000000.0);
            walletMap.put("bidder03", 2500000.0);
            walletMap.put("bidder01", 0.0);

            addTransaction(
                    "bidder07",
                    new TransactionRecord(
                            "TX001",
                            "NẠP TIỀN",
                            5000000,
                            "Nạp qua ngân hàng VCB",
                            "THÀNH CÔNG",
                            LocalDateTime.now().minusDays(3)
                    )
            );

            addTransaction(
                    "sellerlong",
                    new TransactionRecord(
                            "TX003",
                            "NẠP TIỀN",
                            12000000,
                            "Nạp qua MoMo",
                            "THÀNH CÔNG",
                            LocalDateTime.now().minusDays(5)
                    )
            );

            addProductInternal(
                    "sellerlong",
                    new ProductRecord(
                            "PR001",
                            "MacBook Pro M3",
                            "Laptop",
                            22000000,
                            26500000,
                            12,
                            "ĐÃ BÁN",
                            LocalDateTime.now().minusHours(3),
                            LocalDateTime.now().minusHours(1),
                            "bidder07"
                    )
            );

            addProductInternal(
                    "sellerlong",
                    new ProductRecord(
                            "PR002",
                            "iPhone 15 Pro",
                            "Điện thoại",
                            18000000,
                            21000000,
                            7,
                            "ĐÃ BÁN",
                            LocalDateTime.now().minusDays(2),
                            LocalDateTime.now().minusDays(1),
                            "bidder03"
                    )
            );

            seedSessionHistory();

            System.out.println(
                    "AppContext: Seed data OK."
            );

        } catch (Exception e) {

            System.out.println(
                    "AppContext: Lỗi seed – "
                            + e.getMessage()
            );
        }
    }

    private static void seedSessionHistory() {

        LocalDateTime base = LocalDateTime.now();

        addSessionHistoryInternal(
                "sellerlong",
                new AuctionSessionRecord(
                        "SES-001",
                        "iPhone 15 Pro Max",
                        "sellerlong",
                        20000000,
                        28000000,
                        "bidder07",
                        5,
                        base.minusDays(2),
                        base.minusDays(1),
                        "THÀNH CÔNG",
                        "SELLER",
                        0,
                        false
                )
        );

        addSessionHistoryInternal(
                "bidder07",
                new AuctionSessionRecord(
                        "SES-001",
                        "iPhone 15 Pro Max",
                        "sellerlong",
                        20000000,
                        28000000,
                        "bidder07",
                        5,
                        base.minusDays(2),
                        base.minusDays(1),
                        "THẮNG GIÁ",
                        "BIDDER",
                        28000000,
                        true
                )
        );
    }

    // =========================================================
    // AUTH / USER
    // =========================================================
    public static AuthService getAuthService() {
        return authService;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {

        currentUser = user;

        if (user != null) {
            markUserOnline(user.getUsername());
        }
    }

    // =========================================================
    // SESSION / AVATAR
    // =========================================================
    public static AuctionSession getActiveSession() {
        return activeSession;
    }

    public static void setActiveSession(
            AuctionSession session) {

        activeSession = session;
    }

    public static Image getAvatarImage() {
        return avatarImage;
    }

    public static void setAvatarImage(Image image) {
        avatarImage = image;
    }

    // =========================================================
    // LOGOUT
    // =========================================================
    public static void logout() {

        if (currentUser != null) {
            markUserOffline(currentUser.getUsername());
        }

        currentUser = null;
        activeSession = null;
        avatarImage = null;
    }

    // =========================================================
    // WALLET
    // =========================================================
    public static double getWalletBalance(String username) {

        return walletMap.getOrDefault(
                username,
                0.0
        );
    }

    public static void putWallet(
            String username,
            double balance) {

        walletMap.put(username, balance);
    }

    public static boolean deposit(
            String username,
            double amount) {

        if (amount <= 0) {
            return false;
        }

        walletMap.put(
                username,
                walletMap.getOrDefault(username, 0.0)
                        + amount
        );

        addTransaction(
                username,
                new TransactionRecord(
                        "TX-" + System.currentTimeMillis(),
                        "NẠP TIỀN",
                        amount,
                        "Nạp tiền vào ví",
                        "THÀNH CÔNG",
                        LocalDateTime.now()
                )
        );

        ServerConnection conn =
                ServerConnection.getInstance();

        if (conn.isConnected()) {
            conn.sendDeposit(username, amount);
        }

        return true;
    }

    // =========================================================
    // RECORDS
    // =========================================================
    public record TransactionRecord(
            String id,
            String type,
            double amount,
            String description,
            String status,
            LocalDateTime time
    ) {}

    public static List<TransactionRecord>
    getTransactions(String username) {

        return transactionMap.computeIfAbsent(
                username,
                k -> new CopyOnWriteArrayList<>()
        );
    }

    public static void addTransaction(
            String username,
            TransactionRecord record) {

        getTransactions(username).add(record);
    }

    // =========================================================
    // HISTORY
    // =========================================================
    public record HistoryRecord(
            String id,
            String itemName,
            double amount,
            String counterparty,
            String status,
            boolean wonBid,
            LocalDateTime time
    ) {}

    public static List<HistoryRecord>
    getHistory(String username) {

        return historyMap.computeIfAbsent(
                username,
                k -> new CopyOnWriteArrayList<>()
        );
    }

    public static void addHistory(
            String username,
            HistoryRecord record) {

        getHistory(username).add(record);

        ServerConnection conn =
                ServerConnection.getInstance();

        if (conn.isConnected()) {
            conn.sendAddHistory(username, record);
        }
    }

    // =========================================================
    // PRODUCT RECORD
    // =========================================================
    public record ProductRecord(
            String id,
            String name,
            String category,
            double startPrice,
            double currentPrice,
            int bidCount,
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String topBidder
    ) {

        public ProductRecord withUpdated(
                double newPrice,
                int newBidCount,
                String newStatus,
                String newTopBidder
        ) {

            return new ProductRecord(
                    id,
                    name,
                    category,
                    startPrice,
                    newPrice,
                    newBidCount,
                    newStatus,
                    startTime,
                    endTime,
                    newTopBidder
            );
        }
    }

    // =========================================================
    // PRODUCT METHODS
    // =========================================================
    public static List<ProductRecord>
    getProducts(String username) {

        return productMap.computeIfAbsent(
                username,
                k -> new CopyOnWriteArrayList<>()
        );
    }

    private static void addProductInternal(
            String username,
            ProductRecord product) {

        getProducts(username).add(product);
    }

    public static void addProduct(
            String username,
            ProductRecord product) {

        ProductRecord enforced =
                new ProductRecord(
                        product.id(),
                        product.name(),
                        product.category(),
                        product.startPrice(),
                        product.currentPrice(),
                        product.bidCount(),
                        "CHỜ DUYỆT",
                        product.startTime(),
                        product.endTime(),
                        product.topBidder()
                );

        getProducts(username).add(enforced);

        ServerConnection conn =
                ServerConnection.getInstance();

        if (conn.isConnected()) {
            conn.sendAddProduct(username, enforced);
        }
    }

    // =========================================================
    // SESSION HISTORY
    // =========================================================
    public record AuctionSessionRecord(
            String sessionId,
            String itemName,
            String sellerName,
            double startPrice,
            double finalPrice,
            String winnerName,
            int totalBids,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String result,
            String myRole,
            double myFinalBid,
            boolean iWon
    ) {}

    private static void addSessionHistoryInternal(
            String username,
            AuctionSessionRecord record) {

        sessionHistoryMap
                .computeIfAbsent(
                        username,
                        k -> new CopyOnWriteArrayList<>()
                )
                .add(record);
    }

    public static void addSessionHistory(
            String username,
            AuctionSessionRecord record) {

        addSessionHistoryInternal(
                username,
                record
        );

        ServerConnection conn =
                ServerConnection.getInstance();

        if (conn.isConnected()) {
            conn.sendAddSessionHistory(
                    username,
                    record
            );
        }
    }

    public static List<AuctionSessionRecord>
    getSessionHistory(String username) {

        List<AuctionSessionRecord> list =
                sessionHistoryMap.getOrDefault(
                        username,
                        Collections.emptyList()
                );

        List<AuctionSessionRecord> sorted =
                new ArrayList<>(list);

        sorted.sort(
                Comparator.comparing(
                        AuctionSessionRecord::endTime
                ).reversed()
        );

        return Collections.unmodifiableList(sorted);
    }
}
