package com.nhom6.auctionsystem_nhom6.server;

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
 */
public class AuctionWebSocketServer extends WebSocketServer {

    // =========================================================
    // CLIENT REGISTRY
    // =========================================================
    /** Thông tin mỗi client đã đăng nhập */
    record ClientInfo(String username, String role) {}

    private final Map<WebSocket, ClientInfo> clients   = new ConcurrentHashMap<>();
    private final Map<String, WebSocket>     userIndex = new ConcurrentHashMap<>();

    private static final DateTimeFormatter LOG_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

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
        log("✅ AuctionServer WebSocket đang chạy trên port "
                + getPort());
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
            }

            // ── Chat ──────────────────────────────────────────
            case "CHAT" -> {
                String username = extract(raw, "username");
                String message  = extract(raw, "message");
                broadcastAll("{\"type\":\"CHAT\","
                        + "\"username\":\"" + esc(username) + "\","
                        + "\"message\":\""  + esc(message)  + "\"}");
            }

            // ── Đặt giá ───────────────────────────────────────
            case "PLACE_BID" -> {
                String username  = extract(raw, "username");
                String amount    = extract(raw, "amount");
                String sessionId = extract(raw, "sessionId");
                broadcastAll("{\"type\":\"NEW_BID\","
                        + "\"username\":\""  + esc(username)  + "\","
                        + "\"amount\":\""    + esc(amount)    + "\","
                        + "\"sessionId\":\"" + esc(sessionId) + "\"}");
            }

            // ── Bắt đầu phiên (Seller → tất cả Bidder + Admin) ──
            case "SESSION_START" -> {
                String sessionId  = extract(raw, "sessionId");
                String itemName   = extract(raw, "itemName");
                String sellerName = extract(raw, "sellerName");
                String startPrice = extract(raw, "startPrice");
                String minStep    = extract(raw, "minStep");
                String endTime    = extract(raw, "endTime");
                String category   = extract(raw, "category");
                broadcastAll("{\"type\":\"SESSION_START\","
                        + "\"sessionId\":\""  + esc(sessionId)  + "\","
                        + "\"itemName\":\""   + esc(itemName)   + "\","
                        + "\"sellerName\":\"" + esc(sellerName) + "\","
                        + "\"startPrice\":\"" + esc(startPrice) + "\","
                        + "\"minStep\":\""    + esc(minStep)    + "\","
                        + "\"endTime\":\""    + esc(endTime)    + "\","
                        + "\"category\":\""   + esc(category)   + "\"}");
            }

            // ── Kết thúc phiên ────────────────────────────────
            case "SESSION_END" -> {
                String sessionId = extract(raw, "sessionId");
                String itemName  = extract(raw, "itemName");
                String winner    = extract(raw, "winner");
                String finalPrice= extract(raw, "finalPrice");
                broadcastAll("{\"type\":\"SESSION_END\","
                        + "\"sessionId\":\"" + esc(sessionId) + "\","
                        + "\"itemName\":\""  + esc(itemName)  + "\","
                        + "\"winner\":\""    + esc(winner)    + "\","
                        + "\"finalPrice\":\"" + esc(finalPrice) + "\"}");
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
                // Gửi đến TẤT CẢ Admin đang online
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
                        + "\"productName\":\"" + esc(productName) + "\","
                        + "\"reason\":\""      + esc(reason)      + "\"}");
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
    // BROADCAST HELPERS
    // =========================================================
    private void broadcastAll(String message) {
        for (WebSocket conn : clients.keySet()) {
            try {
                if (conn.isOpen()) conn.send(message);
            } catch (Exception ignored) {}
        }
    }

    private void broadcastToRole(String role, String message) {
        for (Map.Entry<WebSocket, ClientInfo> e : clients.entrySet()) {
            if (role.equalsIgnoreCase(e.getValue().role())
                    && e.getKey().isOpen()) {
                try { e.getKey().send(message); }
                catch (Exception ignored) {}
            }
        }
    }

    private void sendToUser(String username, String message) {
        WebSocket conn = userIndex.get(username);
        if (conn != null && conn.isOpen()) {
            try { conn.send(message); }
            catch (Exception ignored) {}
        } else {
            log("⚠️  User offline, không gửi được: " + username);
        }
    }

    private void broadcastOnlineCount() {
        broadcastAll("{\"type\":\"ONLINE_COUNT\","
                + "\"count\":\"" + clients.size() + "\"}");
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
        // Railway cung cấp PORT qua biến môi trường
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
