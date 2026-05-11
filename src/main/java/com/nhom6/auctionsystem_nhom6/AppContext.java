package com.nhom6.auctionsystem_nhom6;

import org.example.auction.AuctionSession;
import org.example.service.AuthService;
import org.example.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppContext {

    private static User           currentUser;
    private static AuctionSession activeSession;

    // ── AuthService singleton ─────────────────────────────────
    private static final AuthService authService = new AuthService();

    // ── Storage ───────────────────────────────────────────────
    private static final Map<String, List<HistoryRecord>>     historyMap     = new HashMap<>();
    private static final Map<String, List<ProductRecord>>     productMap     = new HashMap<>();
    private static final Map<String, Double>                  walletMap      = new HashMap<>();
    private static final Map<String, List<TransactionRecord>> transactionMap = new HashMap<>();

    // =========================================================
    // AUTH SERVICE
    // =========================================================
    public static AuthService getAuthService() { return authService; }

    // =========================================================
    // USER / SESSION
    // =========================================================
    public static void setCurrentUser(User u)             { currentUser  = u; }
    public static User getCurrentUser()                   { return currentUser; }
    public static void setActiveSession(AuctionSession s) { activeSession = s; }
    public static AuctionSession getActiveSession()       { return activeSession; }

    public static void logout() {
        currentUser   = null;
        activeSession = null;
    }

    // =========================================================
    // WALLET
    // =========================================================

    /** Lấy số dư ví, mặc định 0 nếu chưa có */
    public static double getWalletBalance(String username) {
        return walletMap.getOrDefault(username, 0.0);
    }

    /**
     * Nạp tiền vào ví.
     * @return true nếu thành công
     */
    public static boolean deposit(String username, double amount) {
        if (amount <= 0) return false;
        double current = walletMap.getOrDefault(username, 0.0);
        walletMap.put(username, current + amount);
        addTransaction(username, new TransactionRecord(
                "TX-" + System.currentTimeMillis(),
                "Nạp tiền",
                amount,               // dương = tiền vào
                "Nạp tiền vào ví",
                "THÀNH CÔNG",
                LocalDateTime.now()
        ));
        return true;
    }

    /**
     * Thanh toán / rút tiền từ ví.
     * @return true nếu đủ số dư và thành công
     */
    public static boolean payment(String username, double amount, String description) {
        if (amount <= 0) return false;
        double current = walletMap.getOrDefault(username, 0.0);
        if (current < amount) return false;
        walletMap.put(username, current - amount);
        addTransaction(username, new TransactionRecord(
                "TX-" + System.currentTimeMillis(),
                "Thanh toán",
                -amount,              // âm = tiền ra
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
            String        id,
            String        type,         // "Nạp tiền" | "Thanh toán" | "Hoàn tiền" ...
            double        amount,       // dương = vào, âm = ra
            String        description,
            String        status,       // "THÀNH CÔNG" | "THẤT BẠI" | "CHỜ XỬ LÝ"
            LocalDateTime time
    ) {}

    public static List<TransactionRecord> getTransactions(String username) {
        return transactionMap.computeIfAbsent(username, k -> new ArrayList<>());
    }

    public static void addTransaction(String username, TransactionRecord tx) {
        getTransactions(username).add(tx);
    }

    // =========================================================
    // HISTORY RECORD
    // =========================================================

    public record HistoryRecord(
            String        id,
            String        itemName,
            double        amount,
            String        counterparty,
            String        status,
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
            String        status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String        topBidder
    ) {
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
}
