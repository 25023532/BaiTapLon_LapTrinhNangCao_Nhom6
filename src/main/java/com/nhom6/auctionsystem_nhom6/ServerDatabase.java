package com.nhom6.auctionsystem_nhom6;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, file-backed database manager for the WebSocket server.
 * Persists all maps in the data/ directory using pipe-delimited text files.
 * Format: | as field separator, one record per line.
 */
public class ServerDatabase {

    private static final String DATA_DIR = "data";
    private static final DateTimeFormatter DT =
            new java.time.format.DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .optionalStart().appendFraction(java.time.temporal.ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
                    .toFormatter();
    private static final String SEP = "|";

    // ── File paths ────────────────────────────────────────────
    private final Path walletsFile       = Path.of(DATA_DIR, "server_wallets.txt");
    private final Path transactionsFile  = Path.of(DATA_DIR, "server_transactions.txt");
    private final Path productsFile      = Path.of(DATA_DIR, "server_products.txt");
    private final Path historyFile       = Path.of(DATA_DIR, "server_history.txt");
    private final Path sessionHistoryFile= Path.of(DATA_DIR, "server_session_history.txt");
    private final Path usersFile         = Path.of(DATA_DIR, "users.json");
    private final Path ratingsFile = Path.of(DATA_DIR, "server_ratings.txt");
    private final List<AppContext.RatingRecord> ratings = new java.util.concurrent.CopyOnWriteArrayList<>();

    // ── In-memory maps ────────────────────────────────────────
    private final Map<String, Double>                                    wallets       = new ConcurrentHashMap<>();
    private final Map<String, List<AppContext.TransactionRecord>>        transactions  = new ConcurrentHashMap<>();
    private final Map<String, List<AppContext.ProductRecord>>           products      = new ConcurrentHashMap<>();
    private final Map<String, List<AppContext.HistoryRecord>>           history       = new ConcurrentHashMap<>();
    private final Map<String, List<AppContext.AuctionSessionRecord>>    sessionHistory= new ConcurrentHashMap<>();

    // Running sessions on the server: sessionId → RunningSessionInfo
    private final Map<String, RunningSessionInfo> runningSessions = new ConcurrentHashMap<>();

    /** Holds live session state tracked by the server */
    public record RunningSessionInfo(
            String sessionId, String itemName, String sellerName,
            double startPrice, double minStep, LocalDateTime startTime,
            LocalDateTime endTime, String category, double currentPrice,
            int bidCount, String topBidder, List<BidEntry> bidHistory) {}

    public record BidEntry(String bidderId, double amount, String timestamp) {}

    // ── Constructor: load from disk ───────────────────────────
    public ServerDatabase() {
        try { Files.createDirectories(Path.of(DATA_DIR)); } catch (IOException ignored) {}
        loadAll();
    }

    // =========================================================
    // LOAD ALL
    // =========================================================
    public void loadAll() {
        loadWallets();
        loadTransactions();
        loadProducts();
        loadHistory();
        loadRatings();
        loadSessionHistory();
        fixExpiredProducts();
        System.out.println("[ServerDatabase] Loaded all data from " + DATA_DIR + "/");
    }

    // =========================================================
    // WALLETS  (username|balance)
    // =========================================================
    private void loadWallets() {
        wallets.clear();
        if (!Files.exists(walletsFile)) return;
        try (BufferedReader br = Files.newBufferedReader(walletsFile)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split("\\|", 2);
                if (p.length == 2) wallets.put(p[0].trim(), Double.parseDouble(p[1].trim()));
            }
        } catch (IOException e) { System.err.println("[ServerDB] loadWallets: " + e.getMessage()); }
    }

    public synchronized void saveWallets() {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(walletsFile))) {
            for (var e : wallets.entrySet()) pw.println(e.getKey() + SEP + String.format("%.2f", e.getValue()));
        } catch (IOException e) { System.err.println("[ServerDB] saveWallets: " + e.getMessage()); }
    }

    public Map<String, Double> getWallets() { return Collections.unmodifiableMap(wallets); }

    public void updateWallet(String username, double balance) {
        wallets.put(username, balance);
        saveWallets();
    }

    // =========================================================
    // TRANSACTIONS  (username|id|type|amount|description|status|timestamp)
    // =========================================================
    private void loadTransactions() {
        transactions.clear();
        if (!Files.exists(transactionsFile)) return;
        try (BufferedReader br = Files.newBufferedReader(transactionsFile)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split("\\|", 7);
                if (p.length < 7) continue;
                String user = p[0].trim();
                var rec = new AppContext.TransactionRecord(
                        p[1].trim(), p[2].trim(),
                        Double.parseDouble(p[3].trim()),
                        p[4].trim(), p[5].trim(),
                        LocalDateTime.parse(p[6].trim(), DT));
                transactions.computeIfAbsent(user, k -> new ArrayList<>()).add(rec);
            }
        } catch (IOException e) { System.err.println("[ServerDB] loadTransactions: " + e.getMessage()); }
    }

    public synchronized void saveTransactions() {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(transactionsFile))) {
            for (var entry : transactions.entrySet()) {
                for (var r : entry.getValue()) {
                    pw.println(entry.getKey() + SEP + r.id() + SEP + r.type() + SEP
                            + r.amount() + SEP + r.description() + SEP + r.status() + SEP
                            + r.time().format(DT));
                }
            }
        } catch (IOException e) { System.err.println("[ServerDB] saveTransactions: " + e.getMessage()); }
    }

    public Map<String, List<AppContext.TransactionRecord>> getTransactions() {
        return Collections.unmodifiableMap(transactions);
    }

    public void addTransaction(String username, AppContext.TransactionRecord rec) {
        transactions.computeIfAbsent(username, k -> new ArrayList<>()).add(rec);
        saveTransactions();
    }

    // =========================================================
    // PRODUCTS  (seller|id|name|category|startPrice|currentPrice|bidCount|status|startTime|endTime|topBidder)
    // =========================================================
    private void loadProducts() {
        products.clear();
        if (!Files.exists(productsFile)) return;
        try (BufferedReader br = Files.newBufferedReader(productsFile)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split("\\|", 11);
                if (p.length < 11) continue;
                String seller = p[0].trim();
                var rec = new AppContext.ProductRecord(
                        p[1].trim(), p[2].trim(), p[3].trim(),
                        Double.parseDouble(p[4].trim()),
                        Double.parseDouble(p[5].trim()),
                        Integer.parseInt(p[6].trim()),
                        p[7].trim(),
                        LocalDateTime.parse(p[8].trim(), DT),
                        LocalDateTime.parse(p[9].trim(), DT),
                        p[10].trim());
                products.computeIfAbsent(seller, k -> new ArrayList<>()).add(rec);
            }
        } catch (IOException e) { System.err.println("[ServerDB] loadProducts: " + e.getMessage()); }
    }

    public synchronized void saveProducts() {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(productsFile))) {
            for (var entry : products.entrySet()) {
                for (var r : entry.getValue()) {
                    pw.println(entry.getKey() + SEP + r.id() + SEP + r.name() + SEP
                            + r.category() + SEP + r.startPrice() + SEP + r.currentPrice() + SEP
                            + r.bidCount() + SEP + r.status() + SEP
                            + r.startTime().format(DT) + SEP + r.endTime().format(DT) + SEP
                            + r.topBidder());
                }
            }
        } catch (IOException e) { System.err.println("[ServerDB] saveProducts: " + e.getMessage()); }
    }

    public Map<String, List<AppContext.ProductRecord>> getProducts() {
        return Collections.unmodifiableMap(products);
    }

    public void putProduct(String seller, AppContext.ProductRecord rec) {
        var list = products.computeIfAbsent(seller, k -> new ArrayList<>());
        list.removeIf(p -> p.id().equals(rec.id()));
        list.add(rec);
        saveProducts();
    }

    public void removeProduct(String seller, String productId) {
        products.computeIfAbsent(seller, k -> new ArrayList<>()).removeIf(p -> p.id().equals(productId));
        saveProducts();
    }

    // =========================================================
    // HISTORY  (username|id|itemName|amount|counterparty|status|wonBid|timestamp)
    // =========================================================
    private void loadHistory() {
        history.clear();
        if (!Files.exists(historyFile)) return;
        try (BufferedReader br = Files.newBufferedReader(historyFile)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split("\\|", 8);
                if (p.length < 8) continue;
                String user = p[0].trim();
                var rec = new AppContext.HistoryRecord(
                        p[1].trim(), p[2].trim(),
                        Double.parseDouble(p[3].trim()),
                        p[4].trim(), p[5].trim(),
                        Boolean.parseBoolean(p[6].trim()),
                        LocalDateTime.parse(p[7].trim(), DT));
                history.computeIfAbsent(user, k -> new ArrayList<>()).add(rec);
            }
        } catch (IOException e) { System.err.println("[ServerDB] loadHistory: " + e.getMessage()); }
    }

    public synchronized void saveHistory() {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(historyFile))) {
            for (var entry : history.entrySet()) {
                for (var r : entry.getValue()) {
                    pw.println(entry.getKey() + SEP + r.id() + SEP + r.itemName() + SEP
                            + r.amount() + SEP + r.counterparty() + SEP + r.status() + SEP
                            + r.wonBid() + SEP + r.time().format(DT));
                }
            }
        } catch (IOException e) { System.err.println("[ServerDB] saveHistory: " + e.getMessage()); }
    }

    public Map<String, List<AppContext.HistoryRecord>> getHistory() {
        return Collections.unmodifiableMap(history);
    }

    public void addHistory(String username, AppContext.HistoryRecord rec) {
        history.computeIfAbsent(username, k -> new ArrayList<>()).add(rec);
        saveHistory();
    }

    // =========================================================
    // SESSION HISTORY  (username|sessionId|itemName|sellerName|startPrice|finalPrice|winnerName|totalBids|startTime|endTime|result|myRole|myFinalBid|iWon)
    // =========================================================
    private void loadSessionHistory() {
        sessionHistory.clear();
        if (!Files.exists(sessionHistoryFile)) return;
        try (BufferedReader br = Files.newBufferedReader(sessionHistoryFile)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split("\\|", 14);
                if (p.length < 14) continue;
                String user = p[0].trim();
                var rec = new AppContext.AuctionSessionRecord(
                        p[1].trim(), p[2].trim(), p[3].trim(),
                        Double.parseDouble(p[4].trim()),
                        Double.parseDouble(p[5].trim()),
                        p[6].trim().equals("null") ? null : p[6].trim(),
                        Integer.parseInt(p[7].trim()),
                        LocalDateTime.parse(p[8].trim(), DT),
                        LocalDateTime.parse(p[9].trim(), DT),
                        p[10].trim(), p[11].trim(),
                        Double.parseDouble(p[12].trim()),
                        Boolean.parseBoolean(p[13].trim()));
                sessionHistory.computeIfAbsent(user, k -> new ArrayList<>()).add(rec);
            }
        } catch (IOException e) { System.err.println("[ServerDB] loadSessionHistory: " + e.getMessage()); }
    }

    // =========================================================
// RATINGS  (id|username|avatar|stars|comment|likes|timestamp)
// =========================================================
    private void loadRatings() {
        ratings.clear();
        if (!Files.exists(ratingsFile)) return;
        try (BufferedReader br = Files.newBufferedReader(ratingsFile)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split("\\|", 7);
                if (p.length < 7) continue;
                var rec = new AppContext.RatingRecord(
                        p[0].trim(), p[1].trim(), p[2].trim(),
                        Integer.parseInt(p[3].trim()),
                        p[4].trim().replace("\\n", "\n"),
                        LocalDateTime.parse(p[5].trim(), DT),
                        Integer.parseInt(p[6].trim()));
                ratings.add(rec);
            }
        } catch (IOException e) { System.err.println("[ServerDB] loadRatings: " + e.getMessage()); }
    }

    public synchronized void saveRatings() {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(ratingsFile))) {
            for (var r : ratings) {
                pw.println(r.id() + SEP + r.username() + SEP + r.avatar() + SEP
                        + r.stars() + SEP + r.comment().replace("\n", "\\n") + SEP
                        + r.time().format(DT) + SEP + r.likes());
            }
        } catch (IOException e) { System.err.println("[ServerDB] saveRatings: " + e.getMessage()); }
    }

    public List<AppContext.RatingRecord> getRatings() {
        return Collections.unmodifiableList(ratings);
    }

    public void addRating(AppContext.RatingRecord rec) {
        boolean exists = ratings.stream().anyMatch(r -> r.id().equals(rec.id()));
        if (!exists) {
            ratings.add(0, rec);
            saveRatings();
        }
    }

    public String serializeRatings() {
        StringBuilder sb = new StringBuilder();
        for (var r : ratings)
            sb.append(r.id()).append(SEP).append(r.username()).append(SEP)
                    .append(r.avatar()).append(SEP).append(r.stars()).append(SEP)
                    .append(r.comment().replace("\n", "\\n")).append(SEP)
                    .append(r.time().format(DT)).append(SEP).append(r.likes()).append('\n');
        return sb.toString();
    }

    public synchronized void saveSessionHistory() {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(sessionHistoryFile))) {
            for (var entry : sessionHistory.entrySet()) {
                for (var r : entry.getValue()) {
                    pw.println(entry.getKey() + SEP + r.sessionId() + SEP + r.itemName() + SEP
                            + r.sellerName() + SEP + r.startPrice() + SEP + r.finalPrice() + SEP
                            + (r.winnerName() == null ? "null" : r.winnerName()) + SEP
                            + r.totalBids() + SEP + r.startTime().format(DT) + SEP
                            + r.endTime().format(DT) + SEP + r.result() + SEP + r.myRole() + SEP
                            + r.myFinalBid() + SEP + r.iWon());
                }
            }
        } catch (IOException e) { System.err.println("[ServerDB] saveSessionHistory: " + e.getMessage()); }
    }

    public Map<String, List<AppContext.AuctionSessionRecord>> getSessionHistory() {
        return Collections.unmodifiableMap(sessionHistory);
    }

    public void addSessionHistory(String username, AppContext.AuctionSessionRecord rec) {
        List<AppContext.AuctionSessionRecord> list =
                sessionHistory.computeIfAbsent(username, k -> new ArrayList<>());
        boolean exists = list.stream().anyMatch(r ->
                r.sessionId().equals(rec.sessionId()) &&
                        r.startTime().equals(rec.startTime()));
        if (!exists) {
            list.add(rec);
            saveSessionHistory();
        }
    }

    // =========================================================
    // RUNNING SESSIONS (in-memory only, rebuilt from products on restart)
    // =========================================================
    public Map<String, RunningSessionInfo> getRunningSessions() {
        return Collections.unmodifiableMap(runningSessions);
    }

    public void addRunningSession(String sessionId, RunningSessionInfo info) {
        runningSessions.put(sessionId, info);
    }

    public void removeRunningSession(String sessionId) {
        runningSessions.remove(sessionId);
    }

    public RunningSessionInfo getRunningSession(String sessionId) {
        return runningSessions.get(sessionId);
    }

    // =========================================================
    // BULK SERIALIZATION FOR SYNC
    // =========================================================

    /** Serialize all users from users.json — returns raw JSON string */
    public String serializeUsers() {
        try {
            if (Files.exists(usersFile)) return Files.readString(usersFile);
        } catch (IOException ignored) {}
        return "[]";
    }

    /** Serialize all wallets: "username|balance\n..." */
    public String serializeWallets() {
        StringBuilder sb = new StringBuilder();
        for (var e : wallets.entrySet()) sb.append(e.getKey()).append(SEP).append(String.format("%.2f", e.getValue())).append('\n');
        return sb.toString();
    }

    /** Serialize all transactions */
    public String serializeTransactions() {
        StringBuilder sb = new StringBuilder();
        for (var entry : transactions.entrySet())
            for (var r : entry.getValue())
                sb.append(entry.getKey()).append(SEP).append(r.id()).append(SEP)
                  .append(r.type()).append(SEP).append(r.amount()).append(SEP)
                  .append(r.description()).append(SEP).append(r.status()).append(SEP)
                  .append(r.time().format(DT)).append('\n');
        return sb.toString();
    }

    /** Serialize all products */
    public String serializeProducts() {
        StringBuilder sb = new StringBuilder();
        for (var entry : products.entrySet())
            for (var r : entry.getValue())
                sb.append(entry.getKey()).append(SEP).append(r.id()).append(SEP)
                  .append(r.name()).append(SEP).append(r.category()).append(SEP)
                  .append(r.startPrice()).append(SEP).append(r.currentPrice()).append(SEP)
                  .append(r.bidCount()).append(SEP).append(r.status()).append(SEP)
                  .append(r.startTime().format(DT)).append(SEP).append(r.endTime().format(DT))
                  .append(SEP).append(r.topBidder()).append('\n');
        return sb.toString();
    }

    /** Serialize all history */
    public String serializeHistory() {
        StringBuilder sb = new StringBuilder();
        for (var entry : history.entrySet())
            for (var r : entry.getValue())
                sb.append(entry.getKey()).append(SEP).append(r.id()).append(SEP)
                  .append(r.itemName()).append(SEP).append(r.amount()).append(SEP)
                  .append(r.counterparty()).append(SEP).append(r.status()).append(SEP)
                  .append(r.wonBid()).append(SEP).append(r.time().format(DT)).append('\n');
        return sb.toString();
    }

    /** Serialize all session history */
    public String serializeSessionHistory() {
        StringBuilder sb = new StringBuilder();
        for (var entry : sessionHistory.entrySet())
            for (var r : entry.getValue())
                sb.append(entry.getKey()).append(SEP).append(r.sessionId()).append(SEP)
                  .append(r.itemName()).append(SEP).append(r.sellerName()).append(SEP)
                  .append(r.startPrice()).append(SEP).append(r.finalPrice()).append(SEP)
                  .append(r.winnerName() == null ? "null" : r.winnerName()).append(SEP)
                  .append(r.totalBids()).append(SEP).append(r.startTime().format(DT)).append(SEP)
                  .append(r.endTime().format(DT)).append(SEP).append(r.result()).append(SEP)
                  .append(r.myRole()).append(SEP).append(r.myFinalBid()).append(SEP)
                  .append(r.iWon()).append('\n');
        return sb.toString();
    }

    /** Serialize running sessions */
    public String serializeRunningSessions() {
        StringBuilder sb = new StringBuilder();
        for (var entry : runningSessions.entrySet()) {
            RunningSessionInfo s = entry.getValue();
            sb.append(s.sessionId()).append(SEP).append(s.itemName()).append(SEP)
              .append(s.sellerName()).append(SEP).append(s.startPrice()).append(SEP)
              .append(s.minStep()).append(SEP).append(s.startTime().format(DT)).append(SEP)
              .append(s.endTime().format(DT)).append(SEP)
              .append(s.category()).append(SEP).append(s.currentPrice()).append(SEP)
              .append(s.bidCount()).append(SEP).append(s.topBidder()).append(SEP)
              .append(s.bidHistory().size());
            for (var b : s.bidHistory()) {
                sb.append(SEP).append(b.bidderId()).append(SEP).append(b.amount())
                  .append(SEP).append(b.timestamp());
            }
            sb.append('\n');
        }
        return sb.toString();
    }
    // =========================================================
    // TỰ ĐỘNG CẬP NHẬT STATUS SẢN PHẨM HẾT HẠN
    // =========================================================
    public void fixExpiredProducts() {
        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;
        for (var entry : products.entrySet()) {
            String seller = entry.getKey();
            List<AppContext.ProductRecord> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                AppContext.ProductRecord p = list.get(i);
                if (p.status().equals("ĐANG ĐẤU GIÁ") && now.isAfter(p.endTime())) {
                    // Phiên đã hết giờ nhưng status vẫn là ĐANG ĐẤU GIÁ → sửa lại
                    String newStatus = p.topBidder().equals("—") ? "ĐÃ KẾT THÚC" : "ĐÃ BÁN";
                    list.set(i, new AppContext.ProductRecord(
                        p.id(), p.name(), p.category(),
                        p.startPrice(), p.currentPrice(), p.bidCount(),
                        newStatus, p.startTime(), p.endTime(), p.topBidder()));
                    changed = true;
                    System.out.println("[ServerDB] fixExpiredProducts: " + p.name()
                        + " → " + newStatus);
                }
            }
        }
        if (changed) saveProducts();
    }
}
