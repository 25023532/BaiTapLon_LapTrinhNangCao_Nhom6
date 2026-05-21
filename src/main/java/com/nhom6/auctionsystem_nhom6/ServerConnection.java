package com.nhom6.auctionsystem_nhom6;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket client — kết nối qua internet (khác mạng LAN).
 * Thay thế hoàn toàn TCP Socket cũ.
 * API giữ nguyên để không cần sửa Controller.
 *
 * Xử lý SYNC_* messages từ server để đồng bộ local cache,
 * và gửi mutation messages (DEPOSIT, PAYMENT, ADD_PRODUCT, ...)
 * lên server để broadcast đến các client khác.
 */
public class ServerConnection {

    private static final DateTimeFormatter DT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static ServerConnection instance;

    private WebSocketClient wsClient;
    private MessageListener listener;
    private String          lastUsername;

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) instance = new ServerConnection();
        return instance;
    }

    // =========================================================
    // KẾT NỐI
    // =========================================================
    public boolean connect(String username) {
        this.lastUsername = username;

        ServerConfig.createDefaultIfMissing();
        String url = ServerConfig.getWebSocketUrl();

        // Đóng kết nối cũ nếu còn
        disconnect();

        try {
            System.out.println("[WS] Đang kết nối: " + url);

            wsClient = new WebSocketClient(new URI(url)) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("[WS] Kết nối thành công → " + url);
                    // Gửi LOGIN ngay khi kết nối
                    String role = "BIDDER";
                    if (AppContext.getCurrentUser() != null)
                        role = AppContext.getCurrentUser().getRole().toUpperCase();
                    sendJson("{\"action\":\"LOGIN\","
                            + "\"username\":\"" + esc(username) + "\","
                            + "\"role\":\""     + esc(role)     + "\"}");
                    // Yêu cầu số online
                    sendJson("{\"action\":\"GET_ONLINE_COUNT\"}");
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("[WS nhận] " + message);
                    // ✅ Intercept SYNC_* and broadcast messages before forwarding to UI
                    if (!handleSyncMessage(message)) {
                        if (listener != null)
                            javafx.application.Platform.runLater(
                                    () -> listener.onMessage(message));
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("[WS] Đóng kết nối: " + reason
                            + " (code=" + code + ")");
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("[WS] Lỗi: " + ex.getMessage());
                }
            };

            // connectBlocking() chờ tối đa 5 giây
            // Chạy trong background thread để không block JavaFX
            final boolean[] result = {false};
            Thread t = new Thread(() -> {
                try {
                    result[0] = wsClient.connectBlocking(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "WS-Connect");
            t.setDaemon(true);
            t.start();
            t.join(6000); // Chờ tối đa 6 giây

            if (wsClient.isOpen()) {
                System.out.println("[WS] ✅ Kết nối OK: " + username);
                return true;
            } else {
                System.out.println("[WS] ❌ Kết nối thất bại → Chạy offline.");
                return false;
            }

        } catch (Exception e) {
            System.err.println("[WS] Không kết nối được " + url
                    + " — " + e.getMessage());
            return false;
        }
    }

    /** Giữ lại để không break code cũ */
    public boolean connect() {
        return connect("guest_" + System.currentTimeMillis());
    }

    // =========================================================
    // SYNC MESSAGE HANDLER
    // Returns true if the message was handled as a sync/broadcast message
    // =========================================================
    private boolean handleSyncMessage(String raw) {
        try {
            if (!raw.contains("\"type\"")) return false;
            String type = extractJson(raw, "type");

            switch (type) {
                // ── Bulk sync from server ─────────────────────
                case "SYNC_USERS" -> {
                    String data = extractJson(raw, "data");
                    Map<String, org.example.user.User> users = parseUsersFromJson(data);
                    org.example.service.AuthService auth = AppContext.getAuthService();
                    if (auth != null) auth.syncUsers(users);
                    return true;
                }
                case "SYNC_WALLETS" -> {
                    String data = extractJson(raw, "data");
                    AppContext.syncWallets(parseWallets(data));
                    return true;
                }
                case "SYNC_TRANSACTIONS" -> {
                    String data = extractJson(raw, "data");
                    AppContext.syncTransactions(parseTransactions(data));
                    return true;
                }
                case "SYNC_PRODUCTS" -> {
                    String data = extractJson(raw, "data");
                    AppContext.syncProducts(parseProducts(data));
                    return true;
                }
                case "SYNC_HISTORY" -> {
                    String data = extractJson(raw, "data");
                    AppContext.syncHistory(parseHistory(data));
                    return true;
                }
                case "SYNC_SESSION_HISTORY" -> {
                    String data = extractJson(raw, "data");
                    AppContext.syncSessionHistory(parseSessionHistory(data));
                    return true;
                }
                case "SYNC_RUNNING_SESSIONS" -> {
                    String data = extractJson(raw, "data");
                    parseRunningSessions(data);
                    return true;
                }

                // ── Real-time broadcast mutations from other clients ──
                case "USER_REGISTERED" -> {
                    String username = extractJson(raw, "username");
                    String role     = extractJson(raw, "role");
                    String fullName = extractJson(raw, "fullName");
                    String email    = extractJson(raw, "email");
                    // Add to local auth cache
                    org.example.user.User newUser;
                    String id = "U-" + UUID.randomUUID().toString().substring(0, 6);
                    if ("SELLER".equalsIgnoreCase(role))
                        newUser = new org.example.user.Seller(id, username, "", email, fullName);
                    else if ("ADMIN".equalsIgnoreCase(role))
                        newUser = new org.example.user.Admin(id, username, "", email, fullName);
                    else
                        newUser = new org.example.user.Bidder(id, username, "", email, fullName);
                    try {
                        AppContext.getAuthService().register(newUser);
                    } catch (IllegalStateException e) {
                        // Already registered — ignore
                    }
                    return false; // also forward to UI
                }
                case "USER_UNREGISTERED" -> {
                    String username = extractJson(raw, "username");
                    try {
                        AppContext.getAuthService().unregister(username);
                    } catch (IllegalArgumentException ignored) {}
                    return false; // also forward to UI
                }
                case "WALLET_UPDATE" -> {
                    String username = extractJson(raw, "username");
                    double balance  = Double.parseDouble(extractJson(raw, "balance"));
                    AppContext.putWallet(username, balance);
                    return false; // also forward to UI
                }
                case "UPDATE_PRODUCT" -> {
                    String seller   = extractJson(raw, "seller");
                    String id       = extractJson(raw, "id");
                    String name     = extractJson(raw, "name");
                    String category = extractJson(raw, "category");
                    double startP   = Double.parseDouble(extractJson(raw, "startPrice"));
                    double currentP = Double.parseDouble(extractJson(raw, "currentPrice"));
                    int bidCount    = Integer.parseInt(extractJson(raw, "bidCount"));
                    String status   = extractJson(raw, "status");
                    LocalDateTime startTime = LocalDateTime.parse(extractJson(raw, "startTime"), DT);
                    LocalDateTime endTime   = LocalDateTime.parse(extractJson(raw, "endTime"), DT);
                    String topBidder = extractJson(raw, "topBidder");
                    AppContext.updateProduct(seller, new AppContext.ProductRecord(
                            id, name, category, startP, currentP, bidCount,
                            status, startTime, endTime, topBidder));
                    return false; // also forward to UI
                }
                case "ADD_HISTORY" -> {
                    String username = extractJson(raw, "username");
                    String id       = extractJson(raw, "id");
                    String itemName = extractJson(raw, "itemName");
                    double amount   = Double.parseDouble(extractJson(raw, "amount"));
                    String counter  = extractJson(raw, "counterparty");
                    String status   = extractJson(raw, "status");
                    boolean wonBid  = Boolean.parseBoolean(extractJson(raw, "wonBid"));
                    LocalDateTime time = LocalDateTime.parse(extractJson(raw, "timestamp"), DT);
                    AppContext.addHistory(username, new AppContext.HistoryRecord(
                            id, itemName, amount, counter, status, wonBid, time));
                    return false; // also forward to UI
                }
                case "ADD_SESSION_HISTORY" -> {
                    String username  = extractJson(raw, "username");
                    String sessionId = extractJson(raw, "sessionId");
                    String itemName  = extractJson(raw, "itemName");
                    String sellerName= extractJson(raw, "sellerName");
                    double startP    = Double.parseDouble(extractJson(raw, "startPrice"));
                    double finalP    = Double.parseDouble(extractJson(raw, "finalPrice"));
                    String winner    = extractJson(raw, "winnerName");
                    int totalBids    = Integer.parseInt(extractJson(raw, "totalBids"));
                    LocalDateTime startT = LocalDateTime.parse(extractJson(raw, "startTime"), DT);
                    LocalDateTime endT   = LocalDateTime.parse(extractJson(raw, "endTime"), DT);
                    String result    = extractJson(raw, "result");
                    String myRole    = extractJson(raw, "myRole");
                    double myFinalBid= Double.parseDouble(extractJson(raw, "myFinalBid"));
                    boolean iWon     = Boolean.parseBoolean(extractJson(raw, "iWon"));
                    AppContext.addSessionHistory(username, new AppContext.AuctionSessionRecord(
                            sessionId, itemName, sellerName, startP, finalP,
                            "null".equals(winner) ? null : winner,
                            totalBids, startT, endT, result, myRole, myFinalBid, iWon));
                    return false; // also forward to UI
                }
                default -> {
                    return false; // not a sync message, forward to UI
                }
            }
        } catch (Exception e) {
            System.err.println("[WS] handleSyncMessage error: " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    // PARSING HELPERS for pipe-delimited sync data
    // =========================================================

    private Map<String, org.example.user.User> parseUsersFromJson(String json) {
        Map<String, org.example.user.User> map = new LinkedHashMap<>();
        if (json == null || json.isBlank() || "[]".equals(json.trim())) return map;
        // Simple JSON array parse — same format as UserStorage
        String content = json.trim();
        if (content.startsWith("[")) content = content.substring(1);
        if (content.endsWith("]")) content = content.substring(0, content.length() - 1);
        String[] objects = content.split("\\},\\s*\\{");
        for (String obj : objects) {
            obj = obj.replaceAll("[\\[\\]\\{\\}]", "").trim();
            if (obj.isEmpty()) continue;
            Map<String, String> fields = new HashMap<>();
            for (String line : obj.split("\n")) {
                line = line.trim();
                if (!line.contains(":")) continue;
                String[] parts = line.split(":", 2);
                String key = parts[0].replaceAll("\"", "").trim();
                String val = parts.length > 1
                        ? parts[1].replaceAll("\"", "").replaceAll(",\\s*$", "").trim() : "";
                fields.put(key, val);
            }
            String username = fields.getOrDefault("username", "");
            String role = fields.getOrDefault("role", "BIDDER");
            String id = fields.getOrDefault("id", "");
            String email = fields.getOrDefault("email", "");
            String fullName = fields.getOrDefault("fullName", "");
            String hashed = fields.getOrDefault("hashedPassword", "");
            if (username.isEmpty()) continue;
            org.example.user.User user;
            if ("ADMIN".equalsIgnoreCase(role))
                user = new org.example.user.Admin(id, username, hashed, email, fullName, true);
            else if ("SELLER".equalsIgnoreCase(role))
                user = new org.example.user.Seller(id, username, hashed, email, fullName, true);
            else
                user = new org.example.user.Bidder(id, username, hashed, email, fullName, true);
            map.put(username, user);
        }
        return map;
    }

    private Map<String, Double> parseWallets(String data) {
        Map<String, Double> map = new LinkedHashMap<>();
        if (data == null || data.isBlank()) return map;
        for (String line : data.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] p = line.split("\\|", 2);
            if (p.length == 2) map.put(p[0].trim(), Double.parseDouble(p[1].trim()));
        }
        return map;
    }

    private Map<String, List<AppContext.TransactionRecord>> parseTransactions(String data) {
        Map<String, List<AppContext.TransactionRecord>> map = new LinkedHashMap<>();
        if (data == null || data.isBlank()) return map;
        for (String line : data.split("\n")) {
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
            map.computeIfAbsent(user, k -> new ArrayList<>()).add(rec);
        }
        return map;
    }

    private Map<String, List<AppContext.ProductRecord>> parseProducts(String data) {
        Map<String, List<AppContext.ProductRecord>> map = new LinkedHashMap<>();
        if (data == null || data.isBlank()) return map;
        for (String line : data.split("\n")) {
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
            map.computeIfAbsent(seller, k -> new ArrayList<>()).add(rec);
        }
        return map;
    }

    private Map<String, List<AppContext.HistoryRecord>> parseHistory(String data) {
        Map<String, List<AppContext.HistoryRecord>> map = new LinkedHashMap<>();
        if (data == null || data.isBlank()) return map;
        for (String line : data.split("\n")) {
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
            map.computeIfAbsent(user, k -> new ArrayList<>()).add(rec);
        }
        return map;
    }

    private Map<String, List<AppContext.AuctionSessionRecord>> parseSessionHistory(String data) {
        Map<String, List<AppContext.AuctionSessionRecord>> map = new LinkedHashMap<>();
        if (data == null || data.isBlank()) return map;
        for (String line : data.split("\n")) {
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
            map.computeIfAbsent(user, k -> new ArrayList<>()).add(rec);
        }
        return map;
    }

    private void parseRunningSessions(String data) {
        if (data == null || data.isBlank()) return;
        for (String line : data.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] p = line.split("\\|");
            if (p.length < 11) continue;
            String sessionId  = p[0].trim();
            String itemName   = p[1].trim();
            String sellerName = p[2].trim();
            double startPrice = Double.parseDouble(p[3].trim());
            double minStep    = Double.parseDouble(p[4].trim());
            LocalDateTime startTime = LocalDateTime.parse(p[5].trim(), DT);
            LocalDateTime endTime   = LocalDateTime.parse(p[6].trim(), DT);
            String category   = p[7].trim();
            double currentPrice = Double.parseDouble(p[8].trim());
            int bidCount      = Integer.parseInt(p[9].trim());
            String topBidder  = p[10].trim();
            int bidEntryCount = Integer.parseInt(p[11].trim());
            // Reconstruct running session locally
            try {
                AppContext.syncRunningSession(sessionId, itemName, startPrice,
                        minStep, endTime, sellerName);
            } catch (Exception e) {
                System.err.println("parseRunningSessions error: " + e.getMessage());
            }
        }
    }

    // =========================================================
    // OUTGOING MUTATION METHODS
    // =========================================================

    public void sendRegister(org.example.user.User user) {
        sendJson("{\"action\":\"REGISTER\","
                + "\"username\":\"" + esc(user.getUsername()) + "\","
                + "\"role\":\"" + esc(user.getRole()) + "\","
                + "\"fullName\":\"" + esc(user.getFullName()) + "\","
                + "\"email\":\"" + esc(user.getEmail()) + "\"}");
    }

    public void sendUnregister(String username) {
        sendJson("{\"action\":\"UNREGISTER\","
                + "\"username\":\"" + esc(username) + "\"}");
    }

    public void sendDeposit(String username, double amount) {
        sendJson("{\"action\":\"DEPOSIT\","
                + "\"username\":\"" + esc(username) + "\","
                + "\"amount\":\"" + amount + "\"}");
    }

    public void sendPayment(String username, double amount, String description) {
        sendJson("{\"action\":\"PAYMENT\","
                + "\"username\":\"" + esc(username) + "\","
                + "\"amount\":\"" + amount + "\","
                + "\"description\":\"" + esc(description) + "\"}");
    }

    public void sendAddProduct(String username, AppContext.ProductRecord product) {
        sendJson("{\"action\":\"ADD_PRODUCT\","
                + "\"seller\":\"" + esc(username) + "\","
                + "\"id\":\"" + esc(product.id()) + "\","
                + "\"name\":\"" + esc(product.name()) + "\","
                + "\"category\":\"" + esc(product.category()) + "\","
                + "\"startPrice\":\"" + product.startPrice() + "\","
                + "\"status\":\"" + esc(product.status()) + "\","
                + "\"startTime\":\"" + product.startTime().format(DT) + "\","
                + "\"endTime\":\"" + product.endTime().format(DT) + "\"}");
    }

    public void sendUpdateProduct(String username, AppContext.ProductRecord product) {
        sendJson("{\"action\":\"UPDATE_PRODUCT\","
                + "\"seller\":\"" + esc(username) + "\","
                + "\"id\":\"" + esc(product.id()) + "\","
                + "\"name\":\"" + esc(product.name()) + "\","
                + "\"category\":\"" + esc(product.category()) + "\","
                + "\"currentPrice\":\"" + product.currentPrice() + "\","
                + "\"bidCount\":\"" + product.bidCount() + "\","
                + "\"status\":\"" + esc(product.status()) + "\","
                + "\"endTime\":\"" + product.endTime().format(DT) + "\","
                + "\"topBidder\":\"" + esc(product.topBidder()) + "\"}");
    }

    public void sendAddHistory(String username, AppContext.HistoryRecord r) {
        sendJson("{\"action\":\"ADD_HISTORY\","
                + "\"username\":\"" + esc(username) + "\","
                + "\"id\":\"" + esc(r.id()) + "\","
                + "\"itemName\":\"" + esc(r.itemName()) + "\","
                + "\"amount\":\"" + r.amount() + "\","
                + "\"counterparty\":\"" + esc(r.counterparty()) + "\","
                + "\"status\":\"" + esc(r.status()) + "\","
                + "\"wonBid\":\"" + r.wonBid() + "\","
                + "\"timestamp\":\"" + r.time().format(DT) + "\"}");
    }

    public void sendAddSessionHistory(String username, AppContext.AuctionSessionRecord r) {
        sendJson("{\"action\":\"ADD_SESSION_HISTORY\","
                + "\"username\":\"" + esc(username) + "\","
                + "\"sessionId\":\"" + esc(r.sessionId()) + "\","
                + "\"itemName\":\"" + esc(r.itemName()) + "\","
                + "\"sellerName\":\"" + esc(r.sellerName()) + "\","
                + "\"startPrice\":\"" + r.startPrice() + "\","
                + "\"finalPrice\":\"" + r.finalPrice() + "\","
                + "\"winnerName\":\"" + (r.winnerName() == null ? "null" : esc(r.winnerName())) + "\","
                + "\"totalBids\":\"" + r.totalBids() + "\","
                + "\"startTime\":\"" + r.startTime().format(DT) + "\","
                + "\"endTime\":\"" + r.endTime().format(DT) + "\","
                + "\"result\":\"" + esc(r.result()) + "\","
                + "\"myRole\":\"" + esc(r.myRole()) + "\","
                + "\"myFinalBid\":\"" + r.myFinalBid() + "\","
                + "\"iWon\":\"" + r.iWon() + "\"}");
    }

    // =========================================================
    // GỬI MESSAGE — API giữ nguyên, Controller không cần sửa
    // =========================================================

    public void sendChat(String username, String message) {
        sendJson("{\"action\":\"CHAT\","
                + "\"username\":\"" + esc(username) + "\","
                + "\"message\":\""  + esc(message)  + "\"}");
    }

    public void sendBid(String username, String sessionId, double amount) {
        sendJson("{\"action\":\"PLACE_BID\","
                + "\"username\":\""  + esc(username)  + "\","
                + "\"sessionId\":\"" + esc(sessionId) + "\","
                + "\"amount\":\""    + amount          + "\"}");
    }

    public void sendProductPending(String productId,
                                    String productName,
                                    String sellerName,
                                    String category,
                                    double startPrice,
                                    LocalDateTime startTime,
                                    LocalDateTime endTime) {
        sendJson("{\"action\":\"PRODUCT_PENDING\","
                + "\"productId\":\""   + esc(productId)      + "\","
                + "\"productName\":\"" + esc(productName)     + "\","
                + "\"sellerName\":\""  + esc(sellerName)      + "\","
                + "\"category\":\""    + esc(category)        + "\","
                + "\"startPrice\":\""  + startPrice           + "\","
                + "\"startTime\":\""   + startTime.format(DT) + "\","
                + "\"endTime\":\""     + endTime.format(DT)   + "\"}");
    }

    public void sendProductApproved(String productId,
                                     String sellerUsername,
                                     String productName) {
        sendJson("{\"action\":\"PRODUCT_APPROVED\","
                + "\"productId\":\""      + esc(productId)     + "\","
                + "\"sellerUsername\":\"" + esc(sellerUsername) + "\","
                + "\"productName\":\""    + esc(productName)    + "\"}");
    }

    public void sendProductRejected(String productId,
                                     String sellerUsername,
                                     String productName,
                                     String reason) {
        sendJson("{\"action\":\"PRODUCT_REJECTED\","
                + "\"productId\":\""      + esc(productId)     + "\","
                + "\"sellerUsername\":\"" + esc(sellerUsername) + "\","
                + "\"productName\":\""    + esc(productName)    + "\","
                + "\"reason\":\""         + esc(reason)         + "\"}");
    }

    public void sendSessionStart(String sessionId,
                                  String itemName,
                                  double startPrice,
                                  double minStep,
                                  LocalDateTime endTime,
                                  String sellerName,
                                  String category) {
        sendJson("{\"action\":\"SESSION_START\","
                + "\"sessionId\":\""  + esc(sessionId)    + "\","
                + "\"itemName\":\""   + esc(itemName)      + "\","
                + "\"startPrice\":\"" + startPrice         + "\","
                + "\"minStep\":\""    + minStep            + "\","
                + "\"endTime\":\""    + endTime.format(DT) + "\","
                + "\"sellerName\":\"" + esc(sellerName)    + "\","
                + "\"category\":\""   + esc(category)      + "\"}");
    }

    public void sendSessionEnd(String sessionId, String itemName,
                                String winner, double finalPrice) {
        sendJson("{\"action\":\"SESSION_END\","
                + "\"sessionId\":\"" + esc(sessionId) + "\","
                + "\"itemName\":\""  + esc(itemName)  + "\","
                + "\"winner\":\""    + esc(winner)    + "\","
                + "\"finalPrice\":\"" + finalPrice    + "\"}");
    }

    public void sendPaymentSuccess(String username,
                                    String itemName, double amount) {
        sendJson("{\"action\":\"PAYMENT_SUCCESS\","
                + "\"username\":\"" + esc(username) + "\","
                + "\"itemName\":\""  + esc(itemName) + "\","
                + "\"amount\":\""    + amount        + "\"}");
    }

    /** Gửi JSON thô — dùng khi cần gửi custom message */
    public void sendJson(String json) {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.send(json);
            System.out.println("[WS gửi] " + json);
        } else {
            System.out.println("[WS] Offline — bỏ qua: " + json);
        }
    }

    /** Legacy support */
    public void send(String message) {
        if (message.startsWith("CHAT:")) {
            String[] p = message.split(":", 3);
            if (p.length == 3) { sendChat(p[1], p[2]); return; }
        }
        sendJson(message);
    }

    // =========================================================
    // LISTENER
    // =========================================================
    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    // =========================================================
    // STATUS
    // =========================================================
    public boolean isConnected() {
        return wsClient != null && wsClient.isOpen();
    }

    public void disconnect() {
        try {
            if (wsClient != null && !wsClient.isClosed())
                wsClient.close();
        } catch (Exception ignored) {}
    }

    // =========================================================
    // UTIL
    // =========================================================
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n",  "\\n")
                .replace("\r",  "\\r")
                .replace("\t",  "\\t");
    }

    private String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? "" : json.substring(start, end);
    }

    public interface MessageListener {
        void onMessage(String message);
    }
}
