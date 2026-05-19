package network;

import org.example.util.JsonUtil;

import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 * ClientHandler — xử lý 1 client trên 1 luồng riêng.
 *
 * Giao thức (JSON):
 *
 * Client → Server:
 *   {action:"LOGIN",          username, role}
 *   {action:"PLACE_BID",      username, sessionId, amount}
 *   {action:"CHAT",           username, message}
 *   {action:"PRODUCT_PENDING",productId, productName, sellerName,
 *                              category, startPrice, startTime, endTime}
 *   {action:"PRODUCT_APPROVED",productId, sellerUsername, productName}
 *   {action:"PRODUCT_REJECTED",productId, sellerUsername, productName, reason}
 *   {action:"SESSION_START",  sessionId, itemName, startPrice, minStep,
 *                              endTime, sellerName, category}
 *   {action:"SESSION_END",    sessionId, itemName}
 *   {action:"GET_ONLINE_COUNT"}
 *
 * Server → Client:
 *   {type:"ONLINE_COUNT",               count}
 *   {type:"NEW_BID",                    username, sessionId, amount}
 *   {type:"CHAT",                       username, message}
 *   {type:"SYSTEM",                     message}
 *   {type:"NOTIFY_ADMIN_NEW_PRODUCT",   productId, productName, sellerName,
 *                                        category, startPrice, startTime, endTime}
 *   {type:"NOTIFY_SELLER_APPROVED",     productId, productName}
 *   {type:"NOTIFY_SELLER_REJECTED",     productId, productName, reason}
 *   {type:"NOTIFY_BIDDER_SESSION_START",sessionId, itemName, startPrice,
 *                                        minStep, endTime, sellerName, category}
 *   {type:"NOTIFY_BIDDER_SESSION_END",  sessionId, itemName}
 */
public class ClientHandler implements Runnable, Observer {

    private final Socket socket;

    private volatile PrintWriter out;
    private volatile boolean     isClosed = false;

    /** Lock ghi riêng — không dùng "this" để tránh deadlock */
    private final Object writeLock = new Object();

    private String username = "unknown";
    private String role     = "BIDDER"; // mặc định

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // =========================================================
    // RUN
    // =========================================================
    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true);

            // Gửi welcome
            sendJson(Map.of(
                    "status",  "OK",
                    "message", "Connected to Auction Server"
            ));

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[RECV from " + username + "] " + line);
                handleMessage(line);
            }

        } catch (IOException e) {
            if (!isClosed)
                System.out.println("[DISCONNECT] " + username
                        + " mất kết nối: " + e.getMessage());
        } finally {
            AuctionServer.removeObserver(this);
            close();
        }
    }

    // =========================================================
    // GỬI TIN — THREAD-SAFE
    // =========================================================
    public void sendMessage(String message) {
        if (isClosed || out == null) return;
        synchronized (writeLock) {
            if (isClosed || out == null) return;
            out.println(message);
        }
    }

    private void sendJson(Map<String, Object> data) {
        sendMessage(JsonUtil.toJson(data));
    }

    // =========================================================
    // XỬ LÝ MESSAGE
    // =========================================================
    @SuppressWarnings("unchecked")
    private void handleMessage(String message) {
        if (message == null || message.isBlank()) return;
        try {
            Map<String, Object> data =
                    (Map<String, Object>) JsonUtil.parse(message);
            String action = data.get("action").toString().toUpperCase();

            switch (action) {

                // ── Đăng nhập ─────────────────────────────────
                case "LOGIN" -> {
                    username = data.get("username").toString();
                    role     = data.getOrDefault("role", "BIDDER")
                                   .toString().toUpperCase();

                    // Đăng ký vào userMap để server có thể sendToUser
                    AuctionServer.registerUser(username, this);

                    sendJson(Map.of(
                            "status",   "OK",
                            "message",  "Login success",
                            "username", username,
                            "role",     role
                    ));

                    AuctionServer.broadcastAll(JsonUtil.toJson(Map.of(
                            "type",    "SYSTEM",
                            "message", username + " đã tham gia."
                    )));
                    AuctionServer.broadcastOnlineCount();

                    // Gửi pending products cho Admin mới login
                    if ("ADMIN".equals(role)) {
                        for (String pending : AuctionServer.getPendingProducts()) {
                            sendMessage(pending);
                        }
                    }

                    // Gửi các phiên đang active cho client mới
                    for (String sessionData : AuctionServer.getActiveSessions()) {
                        sendMessage(sessionData);
                    }
                }

                // ── Đặt giá ───────────────────────────────────
                case "PLACE_BID" -> {
                    String sessionId = data.get("sessionId").toString();
                    String amount    = data.get("amount").toString();
                    AuctionServer.broadcastAll(JsonUtil.toJson(Map.of(
                            "type",      "NEW_BID",
                            "username",  username,
                            "sessionId", sessionId,
                            "amount",    amount
                    )));
                    sendJson(Map.of("status", "OK",
                            "message", "Bid placed successfully"));
                }

                // ── Chat ──────────────────────────────────────
                case "CHAT" -> {
                    String chatMsg = data.get("message").toString();
                    AuctionServer.broadcast(JsonUtil.toJson(Map.of(
                            "type",     "CHAT",
                            "username", username,
                            "message",  chatMsg
                    )), this);
                }

                // ── Seller đăng sản phẩm → notify Admin ───────
                case "PRODUCT_PENDING" -> {
                    String notif = JsonUtil.toJson(Map.of(
                            "type",        "NOTIFY_ADMIN_NEW_PRODUCT",
                            "productId",   safe(data, "productId"),
                            "productName", safe(data, "productName"),
                            "sellerName",  safe(data, "sellerName"),
                            "category",    safe(data, "category"),
                            "startPrice",  safe(data, "startPrice"),
                            "startTime",   safe(data, "startTime"),
                            "endTime",     safe(data, "endTime")
                    ));
                    // Lưu để gửi cho Admin mới login sau này
                    AuctionServer.addPendingProduct(notif);
                    // Broadcast đến tất cả Admin đang online ngay
                    AuctionServer.broadcastToRole("ADMIN", notif);
                    sendJson(Map.of("status", "OK",
                            "message", "Product pending sent to admins"));
                }

                // ── Admin duyệt → notify Seller ───────────────
                case "PRODUCT_APPROVED" -> {
                    String productId     = safe(data, "productId");
                    String sellerUser    = safe(data, "sellerUsername");
                    String productName   = safe(data, "productName");

                    // Xóa khỏi pending
                    AuctionServer.removePendingProduct(productId);

                    // Gửi riêng cho Seller
                    AuctionServer.sendToUser(sellerUser, JsonUtil.toJson(Map.of(
                            "type",        "NOTIFY_SELLER_APPROVED",
                            "productId",   productId,
                            "productName", productName
                    )));
                    sendJson(Map.of("status", "OK",
                            "message", "Approved notification sent"));
                }

                // ── Admin từ chối → notify Seller ─────────────
                case "PRODUCT_REJECTED" -> {
                    String productId   = safe(data, "productId");
                    String sellerUser  = safe(data, "sellerUsername");
                    String productName = safe(data, "productName");
                    String reason      = safe(data, "reason");

                    AuctionServer.removePendingProduct(productId);

                    AuctionServer.sendToUser(sellerUser, JsonUtil.toJson(Map.of(
                            "type",        "NOTIFY_SELLER_REJECTED",
                            "productId",   productId,
                            "productName", productName,
                            "reason",      reason
                    )));
                    sendJson(Map.of("status", "OK",
                            "message", "Rejected notification sent"));
                }

                // ── Seller bắt đầu phiên → notify tất cả ─────
                case "SESSION_START" -> {
                    String sessionId = safe(data, "sessionId");
                    String notif     = JsonUtil.toJson(Map.of(
                            "type",       "NOTIFY_BIDDER_SESSION_START",
                            "sessionId",  sessionId,
                            "itemName",   safe(data, "itemName"),
                            "startPrice", safe(data, "startPrice"),
                            "minStep",    safe(data, "minStep"),
                            "endTime",    safe(data, "endTime"),
                            "sellerName", safe(data, "sellerName"),
                            "category",   safe(data, "category")
                    ));
                    // Lưu để gửi cho client mới login
                    AuctionServer.addActiveSession(sessionId, notif);
                    // Broadcast cho TẤT CẢ (Bidder + Seller khác)
                    AuctionServer.broadcastAll(notif);
                    sendJson(Map.of("status", "OK",
                            "message", "Session start broadcasted"));
                }

                // ── Kết thúc phiên ────────────────────────────
                case "SESSION_END" -> {
                    String sessionId = safe(data, "sessionId");
                    String itemName  = safe(data, "itemName");

                    AuctionServer.removeActiveSession(sessionId);
                    AuctionServer.broadcastAll(JsonUtil.toJson(Map.of(
                            "type",      "NOTIFY_BIDDER_SESSION_END",
                            "sessionId", sessionId,
                            "itemName",  itemName
                    )));
                    sendJson(Map.of("status", "OK",
                            "message", "Session end broadcasted"));
                }

                // ── Số online ─────────────────────────────────
                case "GET_ONLINE_COUNT" -> {
                    sendJson(Map.of(
                            "type",  "ONLINE_COUNT",
                            "count", String.valueOf(AuctionServer.getOnlineCount())
                    ));
                }

                default -> sendJson(Map.of(
                        "status",  "ERROR",
                        "message", "Unsupported action: " + action
                ));
            }

        } catch (Exception e) {
            System.err.println("[ERROR] handleMessage: " + e.getMessage());
            sendJson(Map.of(
                    "status",  "ERROR",
                    "message", "Invalid JSON or error: " + e.getMessage()
            ));
        }
    }

    /** Lấy giá trị an toàn từ map, trả "" nếu null */
    private String safe(Map<String, Object> data, String key) {
        Object v = data.get(key);
        return v == null ? "" : v.toString();
    }

    // =========================================================
    // ĐÓNG KẾT NỐI
    // =========================================================
    public synchronized void close() {
        if (isClosed) return;
        isClosed = true;
        try {
            if (out != null) out.flush();
            socket.close();
        } catch (IOException e) {
            System.err.println("[CLOSE] " + username + ": " + e.getMessage());
        }
    }

    // =========================================================
    // OBSERVER
    // =========================================================
    @Override
    public void update(Object message) {
        sendMessage(message.toString());
    }

    // =========================================================
    // GETTERS
    // =========================================================
    public String getUsername() { return username; }
    public String getRole()     { return role; }
}
