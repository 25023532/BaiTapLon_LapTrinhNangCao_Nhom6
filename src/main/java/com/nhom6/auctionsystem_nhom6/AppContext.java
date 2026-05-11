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

    // ✅ AuthService singleton — dùng chung toàn app
    private static final AuthService authService = new AuthService();

    private static final Map<String, List<HistoryRecord>> historyMap = new HashMap<>();
    private static final Map<String, List<ProductRecord>> productMap = new HashMap<>();

    // ── AuthService ───────────────────────────────────────────
    public static AuthService getAuthService() {
        return authService;
    }

    // ── User ──────────────────────────────────────────────────
    public static void setCurrentUser(User u)             { currentUser  = u; }
    public static User getCurrentUser()                   { return currentUser; }
    public static void setActiveSession(AuctionSession s) { activeSession = s; }
    public static AuctionSession getActiveSession()       { return activeSession; }

    public static void logout() {
        currentUser   = null;
        activeSession = null;
    }

    // ── History Record ────────────────────────────────────────
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

    // ── Product Record ────────────────────────────────────────
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
