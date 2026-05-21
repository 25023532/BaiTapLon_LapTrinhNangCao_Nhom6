package com.nhom6.auctionsystem_nhom6;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Server — deploy lên Railway / Render / bất kỳ cloud nào.
 * Chạy: java -jar auction-server.jar [PORT]
 * Railway tự cung cấp biến môi trường PORT.
 *
 * Là source of truth: lưu trữ toàn bộ state trên filesystem qua ServerDatabase
 * và broadcast cập nhật real-time đến tất cả client đã kết nối.
 */
public class AuctionWebSocketServer extends WebSocketServer {

    // =========================================================
    // CLIENT REGISTRY
    // =========================================================
    record ClientInfo(String username, String role) {}

    private final Map<WebSocket, ClientInfo> clients   = new ConcurrentHashMap<>();
    private final Map<String, WebSocket>     userIndex = new ConcurrentHashMap<>();

    private static final DateTimeFormatter LOG_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    // =========================================================
    // SERVER DATABASE — source of truth
    // =========================================================
    private final ServerDatabase db = new ServerDatabase();

    // =========================================================
    // CONSTRUCTOR
    // =========================================================
    public AuctionWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        setReuseAddr(true);
    }

    // =========================================================
    // LIFECYCLE
    // =========================================================
    @Override
    public void onStart() {
        log("✅ AuctionServer WebSocket đang chạy trên port " + getPort());
        log("   Database loaded: " + db.getWallets().size() + " wallets, "
                + db.getProducts().size() + " sellers, "
                + db.getRunningSessions().size() + " running sessions");
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        log("🔌 Kết nối mới: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        ClientInfo info = clients.remove(conn);
        if (info != null) {
            userIndex.remove(info.username());
            log("❌ " + info.username() + " đã ngắt kết nối.");
            broadcastOnlineCount();
            broadcastAll(json("type", "SYSTEM",
                    "message", info.username() + " đã rời phiên."));
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log("⚠️  Lỗi: " + ex.getMessage());
    }

    // =========================================================
    // MESSAGE HANDLER
    // =========================================================
    @Override
    public void onMessage(WebSocket conn, String raw) {
        log("📨 [" + senderName(conn) + "] " + raw);

        String action = extract(raw, "action");
        switch (action) {

            // ── Đăng nhập ─────────────────────────────────────
            case "LOGIN" -> {
                String username = extract(raw, "username");
                String role     = extract(raw, "role").toUpperCase();
                clients.put(conn, new ClientInfo(username, role));
                userIndex.put(username, conn);
                log("👤 LOGIN: " + username + " [" + role + "]");
                broadcastOnlineCount();
                broadcastAll(json("type", "SYSTEM",
                        "message", username + " đã tham gia hệ thống."));

                // ✅ SYNC: gửi toàn bộ database state cho client mới
                sendSyncData(conn, username);
            }

            // ── Đăng ký tài khoản mới ─────────────────────────
            case "REGISTER" -> {
                String username = extract(raw, "username");
                String role     = extract(raw, "role");
                String fullName = extract(raw, "fullName");
                String email    = extract(raw, "email");
                log("📝 REGISTER: " + username + " [" + role + "]");
                broadcastAll(json("type", "USER_REGISTERED",
                        "username", username,
                        "role", role,
                        "fullName", fullName,
                        "email", email));
            }

            // ── Xóa tài khoản ─────────────────────────────────
            case "UNREGISTER" -> {
                String username = extract(raw, "username");
                log("🗑 UNREGISTER: " + username);
                broadcastAll(json("type", "USER_UNREGISTERED",
                        "username", username));
            }

            // ── Chat ──────────────────────────────────────────
            case "CHAT" -> {
                String username = extract(raw, "username");
                String message  = extract(raw, "message");
                broadcastAll("{\"type\":\"CHAT\","
                        + "\"username\":\"" + esc(username) + "\","
                        + "\"message\":\""  + esc(message)  + "\"}");
            }

            // ── Nạp tiền ──────────────────────────────────────
            case "DEPOSIT" -> {
                String username = extract(raw, "username");
                double amount   = Double.parseDouble(extract(raw, "amount"));
                double newBal   = db.getWallets().getOrDefault(username, 0.0) + amount;
                db.updateWallet(username, newBal);
                String txId = "TX-" + System.currentTimeMillis();
                db.addTransaction(username, new AppContext.TransactionRecord(
                        txId, "NẠP TIỀN", amount, "Nạp tiền vào ví",
                        "THÀNH CÔNG", LocalDateTime.now()));
                log("💰 DEPOSIT: " + username + " +" + amount);
                broadcastWalletUpdate(username, newBal);
            }

            // ── Thanh toán ────────────────────────────────────
            case "PAYMENT" -> {
                String username  = extract(raw, "username");
                double amount    = Double.parseDouble(extract(raw, "amount"));
                String desc      = extract(raw, "description");
                double curBal    = db.getWallets().getOrDefault(username, 0.0);
                if (curBal >= amount) {
                    double newBal = curBal - amount;
                    db.updateWallet(username, newBal);
                    String txId = "TX-" + System.currentTimeMillis();
                    db.addTransaction(username, new AppContext.TransactionRecord(
                            txId, "THANH TOÁN", -amount, desc,
                            "THÀNH CÔNG", LocalDateTime.now()));
                    log("💸 PAYMENT: " + username + " -" + amount + " (" + desc + ")");
                    broadcastWalletUpdate(username, newBal);
                } else {
                    log("⚠️ PAYMENT FAILED (insufficient): " + username);
                }
            }

            // ── Thêm sản phẩm ─────────────────────────────────
            case "ADD_PRODUCT" -> {
                String seller   = extract(raw, "seller");
                String id       = extract(raw, "id");
                String name     = extract(raw, "name");
                String category = extract(raw, "category");
                double startP   = Double.parseDouble(extract(raw, "startPrice"));
                String status   = extract(raw, "status");
                LocalDateTime startTime = LocalDateTime.parse(extract(raw, "startTime"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                LocalDateTime endTime   = LocalDateTime.parse(extract(raw, "endTime"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                db.putProduct(seller, new AppContext.ProductRecord(
                        id, name, category, startP, startP, 0,
                        status, startTime, endTime, "—"));
                log("📦 ADD_PRODUCT: " + seller + " → " + name);
                broadcastProductUpdate(seller, db.getProducts().get(seller)
                        .stream().filter(p -> p.id().equals(id)).findFirst().orElse(null));
            }

            // ── Cập nhật sản phẩm ─────────────────────────────
            case "UPDATE_PRODUCT" -> {
                String seller = extract(raw, "seller");
                String id     = extract(raw, "id");
                AppContext.ProductRecord existing = db.getProducts().getOrDefault(seller, List.of())
                        .stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
                if (existing != null) {
                    String name     = extract(raw, "name");
                    String category = extract(raw, "category");
                    String status   = extract(raw, "status");
                    double currentP = Double.parseDouble(extract(raw, "currentPrice"));
                    int bidCount    = Integer.parseInt(extract(raw, "bidCount"));
                    String topB     = extract(raw, "topBidder");
                    LocalDateTime endTime = LocalDateTime.parse(extract(raw, "endTime"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    db.putProduct(seller, new AppContext.ProductRecord(
                            id, name.isEmpty() ? existing.name() : name,
                            category.isEmpty() ? existing.category() : category,
                            existing.startPrice(), currentP, bidCount,
                            status.isEmpty() ? existing.status() : status,
                            existing.startTime(), endTime,
                            topB.isEmpty() ? existing.topBidder() : topB));
                    log("✏️ UPDATE_PRODUCT: " + seller + " → " + id);
                    broadcastProductUpdate(seller, db.getProducts().get(seller)
                            .stream().filter(p -> p.id().equals(id)).findFirst().orElse(null));
                }
            }

            // ── Thêm lịch sử đấu giá ──────────────────────────
            case "ADD_HISTORY" -> {
                String username = extract(raw, "username");
                String id       = extract(raw, "id");
                String itemName = extract(raw, "itemName");
                double amount   = Double.parseDouble(extract(raw, "amount"));
                String counter  = extract(raw, "counterparty");
                String status   = extract(raw, "status");
                boolean wonBid  = Boolean.parseBoolean(extract(raw, "wonBid"));
                LocalDateTime time = LocalDateTime.parse(extract(raw, "timestamp"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                db.addHistory(username, new AppContext.HistoryRecord(
                        id, itemName, amount, counter, status, wonBid, time));
                log("📜 ADD_HISTORY: " + username + " → " + itemName);
                broadcastAddHistory(username, new AppContext.HistoryRecord(
                        id, itemName, amount, counter, status, wonBid, time));
            }

            // ── Thêm lịch sử phiên ────────────────────────────
            case "ADD_SESSION_HISTORY" -> {
                String username  = extract(raw, "username");
                String sessionId = extract(raw, "sessionId");
                String itemName  = extract(raw, "itemName");
                String sellerName= extract(raw, "sellerName");
                double startP    = Double.parseDouble(extract(raw, "startPrice"));
                double finalP    = Double.parseDouble(extract(raw, "finalPrice"));
                String winner    = extract(raw, "winnerName");
                int totalBids    = Integer.parseInt(extract(raw, "totalBids"));
                LocalDateTime startT = LocalDateTime.parse(extract(raw, "startTime"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                LocalDateTime endT   = LocalDateTime.parse(extract(raw, "endTime"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                String result    = extract(raw, "result");
                String myRole    = extract(raw, "myRole");
                double myFinalBid= Double.parseDouble(extract(raw, "myFinalBid"));
                boolean iWon     = Boolean.parseBoolean(extract(raw, "iWon"));
                db.addSessionHistory(username, new AppContext.AuctionSessionRecord(
                        sessionId, itemName, sellerName, startP, finalP,
                        winner.equals("null") ? null : winner,
                        totalBids, startT, endT, result, myRole, myFinalBid, iWon));
                log("🏁 ADD_SESSION_HISTORY: " + username + " → " + sessionId);
                broadcastAddSessionHistory(username, new AppContext.AuctionSessionRecord(
                        sessionId, itemName, sellerName, startP, finalP,
                        winner.equals("null") ? null : winner,
                        totalBids, startT, endT, result, myRole, myFinalBid, iWon));
            }

            // ── Đặt giá ───────────────────────────────────────
            case "PLACE_BID" -> {
                String username  = extract(raw, "username");
                String amountStr = extract(raw, "amount");
                String sessionId = extract(raw, "sessionId");
                double amount    = Double.parseDouble(amountStr);

                // Update running session on server
                ServerDatabase.RunningSessionInfo rs = db.getRunningSession(sessionId);
                if (rs != null) {
                    List<ServerDatabase.BidEntry> bids = new ArrayList<>(rs.bidHistory());
                    bids.add(new ServerDatabase.BidEntry(username, amount,
                            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
                    ServerDatabase.RunningSessionInfo updated = new ServerDatabase.RunningSessionInfo(
                            rs.sessionId(), rs.itemName(), rs.sellerName(),
                            rs.startPrice(), rs.minStep(), rs.startTime(),
                            rs.endTime(), rs.category(), amount, bids.size(), username, bids);
                    db.addRunningSession(sessionId, updated);

                    // Update product current price in database
                    db.putProduct(rs.sellerName(), new AppContext.ProductRecord(
                            sessionId, rs.itemName(), rs.category(),
                            rs.startPrice(), amount, bids.size(),
                            "ĐANG ĐẤU GIÁ", rs.startTime(), rs.endTime(), username));
                }

                broadcastAll("{\"type\":\"NEW_BID\","
                        + "\"username\":\""  + esc(username)  + "\","
                        + "\"amount\":\""    + esc(amountStr)+ "\","
                        + "\"sessionId\":\"" + esc(sessionId) + "\"}");
            }

            // ── Bắt đầu phiên (Seller → tất cả Bidder + Admin) ──
            case "SESSION_START" -> {
                String sessionId  = extract(raw, "sessionId");
                String itemName   = extract(raw, "itemName");
                String sellerName = extract(raw, "sellerName");
                double startPrice = Double.parseDouble(extract(raw, "startPrice"));
                double minStep    = Double.parseDouble(extract(raw, "minStep"));
                LocalDateTime endTime = LocalDateTime.parse(extract(raw, "endTime"), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                String category   = extract(raw, "category");

                // Track running session on server
                db.addRunningSession(sessionId, new ServerDatabase.RunningSessionInfo(
                        sessionId, itemName, sellerName, startPrice, minStep,
                        LocalDateTime.now(), endTime, category, startPrice, 0, "—",
                        new ArrayList<>()));

                // Update product status
                db.putProduct(sellerName, new AppContext.ProductRecord(
                        sessionId, itemName, category, startPrice, startPrice, 0,
                        "ĐANG ĐẤU GIÁ",
                        LocalDateTime.now(), endTime, "—"));

                broadcastAll("{\"type\":\"SESSION_START\","
                        + "\"sessionId\":\""  + esc(sessionId)  + "\","
                        + "\"itemName\":\""   + esc(itemName)   + "\","
                        + "\"sellerName\":\"" + esc(sellerName) + "\","
                        + "\"startPrice\":\"" + startPrice + "\","
                        + "\"minStep\":\""    + minStep + "\","
                        + "\"endTime\":\""    + esc(endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)) + "\","
                        + "\"category\":\""   + esc(category)   + "\"}");
            }

            // ── Kết thúc phiên ────────────────────────────────
            case "SESSION_END" -> {
                String sessionId = extract(raw, "sessionId");
                String itemName  = extract(raw, "itemName");
                String winner    = extract(raw, "winner");
                double finalPrice= Double.parseDouble(extract(raw, "finalPrice"));

                ServerDatabase.RunningSessionInfo rs = db.getRunningSession(sessionId);
                if (rs != null) {
                    // Update product final state
                    db.putProduct(rs.sellerName(), new AppContext.ProductRecord(
                            sessionId, rs.itemName(), rs.category(),
                            rs.startPrice(), finalPrice, rs.bidCount(),
                            winner != null && !winner.isBlank() ? "ĐÃ BÁN" : "ĐÃ KẾT THÚC",
                            rs.startTime(), LocalDateTime.now(),
                            winner != null && !winner.isBlank() ? winner : "—"));
                }
                db.removeRunningSession(sessionId);

                broadcastAll("{\"type\":\"SESSION_END\","
                        + "\"sessionId\":\"" + esc(sessionId) + "\","
                        + "\"itemName\":\""  + esc(itemName)  + "\","
                        + "\"winner\":\""    + esc(winner)    + "\","
                        + "\"finalPrice\":\"" + finalPrice + "\"}");
            }

            // ── Seller gửi sản phẩm chờ duyệt → Admin ─────────
            case "PRODUCT_PENDING" -> {
                String productId   = extract(raw, "productId");
                String productName = extract(raw, "productName");
                String sellerName  = extract(raw, "sellerName");
                String category    = extract(raw, "category");
                String startPrice  = extract(raw, "startPrice");
                String startTime   = extract(raw, "startTime");
                String endTime     = extract(raw, "endTime");
                broadcastToRole("ADMIN", "{\"type\":\"PRODUCT_PENDING\","
                        + "\"productId\":\""   + esc(productId)   + "\","
                        + "\"productName\":\"" + esc(productName) + "\","
                        + "\"sellerName\":\""  + esc(sellerName)  + "\","
                        + "\"category\":\""    + esc(category)    + "\","
                        + "\"startPrice\":\""  + esc(startPrice)  + "\","
                        + "\"startTime\":\""   + esc(startTime)   + "\","
                        + "\"endTime\":\""     + esc(endTime)     + "\"}");
                log("📦 PRODUCT_PENDING → broadcast to ADMIN: " + productName);
            }

            // ── Admin duyệt sản phẩm → Seller cụ thể ──────────
            case "PRODUCT_APPROVED" -> {
                String productId      = extract(raw, "productId");
                String sellerUsername = extract(raw, "sellerUsername");
                String productName    = extract(raw, "productName");
                sendToUser(sellerUsername, "{\"type\":\"PRODUCT_APPROVED\","
                        + "\"productId\":\""   + esc(productId)   + "\","
                        + "\"productName\":\"" + esc(productName) + "\"}");
                log("✅ PRODUCT_APPROVED → " + sellerUsername + ": " + productName);
            }

            // ── Admin từ chối → Seller cụ thể ─────────────────
            case "PRODUCT_REJECTED" -> {
                String productId      = extract(raw, "productId");
                String sellerUsername = extract(raw, "sellerUsername");
                String productName    = extract(raw, "productName");
                String reason         = extract(raw, "reason");
                sendToUser(sellerUsername, "{\"type\":\"PRODUCT_REJECTED\","
                        + "\"productId\":\""   + esc(productId)   + "\","
                        + "\"sellerUsername\":\"" + esc(sellerUsername) + "\","
                        + "\"productName\":\"" + esc(productName)    + "\","
                        + "\"reason\":\""      + esc(reason)         + "\"}");
                log("❌ PRODUCT_REJECTED → " + sellerUsername + ": " + reason);
            }

            // ── Thanh toán thành công → broadcast ─────────────
            case "PAYMENT_SUCCESS" -> {
                String username  = extract(raw, "username");
                String itemName  = extract(raw, "itemName");
                String amount    = extract(raw, "amount");
                broadcastAll("{\"type\":\"PAYMENT_SUCCESS\","
                        + "\"username\":\"" + esc(username) + "\","
                        + "\"itemName\":\""  + esc(itemName) + "\","
                        + "\"amount\":\""    + esc(amount)   + "\"}");
            }

            // ── Yêu cầu số online ─────────────────────────────
            case "GET_ONLINE_COUNT" -> {
                conn.send("{\"type\":\"ONLINE_COUNT\","
                        + "\"count\":\"" + clients.size() + "\"}");
            }

            default -> log("⚠️  Không nhận dạng được action: " + action);
        }
    }

    // =========================================================
    // SYNC — gửi toàn bộ database state cho client mới đăng nhập
    // =========================================================
    private void sendSyncData(WebSocket conn, String username) {
        log("🔄 SYNC → " + username);

        // SYNC_USERS
        conn.send(syncMsg("SYNC_USERS", db.serializeUsers()));

        // SYNC_WALLETS
        conn.send(syncMsg("SYNC_WALLETS", db.serializeWallets()));

        // SYNC_TRANSACTIONS
        conn.send(syncMsg("SYNC_TRANSACTIONS", db.serializeTransactions()));

        // SYNC_PRODUCTS
        conn.send(syncMsg("SYNC_PRODUCTS", db.serializeProducts()));

        // SYNC_HISTORY
        conn.send(syncMsg("SYNC_HISTORY", db.serializeHistory()));

        // SYNC_SESSION_HISTORY
        conn.send(syncMsg("SYNC_SESSION_HISTORY", db.serializeSessionHistory()));

        // SYNC_RUNNING_SESSIONS
        conn.send(syncMsg("SYNC_RUNNING_SESSIONS", db.serializeRunningSessions()));

        log("✅ SYNC done → " + username);
    }

    private String syncMsg(String type, String data) {
        return "{\"type\":\"" + type + "\",\"data\":\"" + esc(data) + "\"}";
    }

    // =========================================================
    // BROADCAST HELPERS
    // =========================================================
    private void broadcastAll(String message) {
        for (WebSocket c : clients.keySet()) {
            try { if (c.isOpen()) c.send(message); }
            catch (Exception ignored) {}
        }
    }

    private void broadcastToRole(String role, String message) {
        for (Map.Entry<WebSocket, ClientInfo> e : clients.entrySet()) {
            if (role.equalsIgnoreCase(e.getValue().role()) && e.getKey().isOpen()) {
                try { e.getKey().send(message); }
                catch (Exception ignored) {}
            }
        }
    }

    private void sendToUser(String username, String message) {
        WebSocket c = userIndex.get(username);
        if (c != null && c.isOpen()) {
            try { c.send(message); }
            catch (Exception ignored) {}
        } else {
            log("⚠️  User offline, không gửi được: " + username);
        }
    }

    private void broadcastOnlineCount() {
        broadcastAll("{\"type\":\"ONLINE_COUNT\","
                + "\"count\":\"" + clients.size() + "\"}");
    }

    // ── Broadcast helpers for real-time mutations ─────────────
    private void broadcastWalletUpdate(String username, double balance) {
        broadcastAll("{\"type\":\"WALLET_UPDATE\","
                + "\"username\":\"" + esc(username) + "\","
                + "\"balance\":\"" + balance + "\"}");
    }

    private void broadcastProductUpdate(String seller, AppContext.ProductRecord p) {
        if (p == null) return;
        broadcastAll("{\"type\":\"UPDATE_PRODUCT\","
                + "\"seller\":\"" + esc(seller) + "\","
                + "\"id\":\"" + esc(p.id()) + "\","
                + "\"name\":\"" + esc(p.name()) + "\","
                + "\"category\":\"" + esc(p.category()) + "\","
                + "\"startPrice\":\"" + p.startPrice() + "\","
                + "\"currentPrice\":\"" + p.currentPrice() + "\","
                + "\"bidCount\":\"" + p.bidCount() + "\","
                + "\"status\":\"" + esc(p.status()) + "\","
                + "\"startTime\":\"" + p.startTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\","
                + "\"endTime\":\"" + p.endTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\","
                + "\"topBidder\":\"" + esc(p.topBidder()) + "\"}");
    }

    private void broadcastAddHistory(String username, AppContext.HistoryRecord r) {
        broadcastAll("{\"type\":\"ADD_HISTORY\","
                + "\"username\":\"" + esc(username) + "\","
                + "\"id\":\"" + esc(r.id()) + "\","
                + "\"itemName\":\"" + esc(r.itemName()) + "\","
                + "\"amount\":\"" + r.amount() + "\","
                + "\"counterparty\":\"" + esc(r.counterparty()) + "\","
                + "\"status\":\"" + esc(r.status()) + "\","
                + "\"wonBid\":\"" + r.wonBid() + "\","
                + "\"timestamp\":\"" + r.time().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\"}");
    }

    private void broadcastAddSessionHistory(String username, AppContext.AuctionSessionRecord r) {
        broadcastAll("{\"type\":\"ADD_SESSION_HISTORY\","
                + "\"username\":\"" + esc(username) + "\","
                + "\"sessionId\":\"" + esc(r.sessionId()) + "\","
                + "\"itemName\":\"" + esc(r.itemName()) + "\","
                + "\"sellerName\":\"" + esc(r.sellerName()) + "\","
                + "\"startPrice\":\"" + r.startPrice() + "\","
                + "\"finalPrice\":\"" + r.finalPrice() + "\","
                + "\"winnerName\":\"" + (r.winnerName() == null ? "null" : esc(r.winnerName())) + "\","
                + "\"totalBids\":\"" + r.totalBids() + "\","
                + "\"startTime\":\"" + r.startTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\","
                + "\"endTime\":\"" + r.endTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\","
                + "\"result\":\"" + esc(r.result()) + "\","
                + "\"myRole\":\"" + esc(r.myRole()) + "\","
                + "\"myFinalBid\":\"" + r.myFinalBid() + "\","
                + "\"iWon\":\"" + r.iWon() + "\"}");
    }

    // =========================================================
    // JSON / UTIL HELPERS
    // =========================================================
    private String extract(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? "" : json.substring(start, end);
    }

    private String json(String... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length - 1; i += 2) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(kv[i]).append("\":\"")
              .append(esc(kv[i + 1])).append("\"");
        }
        return sb.append("}").toString();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String senderName(WebSocket conn) {
        ClientInfo info = clients.get(conn);
        return info != null ? info.username() : conn.getRemoteSocketAddress().toString();
    }

    private void log(String msg) {
        System.out.println("[" + LocalDateTime.now().format(LOG_FMT) + "] " + msg);
    }

    // =========================================================
    // MAIN — chạy standalone hoặc deploy Railway/Render
    // =========================================================
    public static void main(String[] args) {
        String envPort = System.getenv("PORT");
        int port = 1234;
        if (envPort != null && !envPort.isBlank()) {
            try { port = Integer.parseInt(envPort.trim()); }
            catch (NumberFormatException ignored) {}
        } else if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); }
            catch (NumberFormatException ignored) {}
        }

        AuctionWebSocketServer server = new AuctionWebSocketServer(port);
        server.start();
        System.out.println("═══════════════════════════════════════");
        System.out.println("  AuctionSys WebSocket Server");
        System.out.println("  Port: " + port);
        System.out.println("  URL : ws://0.0.0.0:" + port);
        System.out.println("═══════════════════════════════════════");
    }
}
