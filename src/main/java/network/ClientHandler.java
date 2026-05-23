package network;

import org.example.dao.AuctionDAO;
import org.example.service.AuctionService;
import org.example.util.JsonUtil;

import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 * ClientHandler — xử lý 1 client trên 1 luồng riêng.
 *
 * THAY ĐỔI SO VỚI BẢN CŨ:
 *   ✅ Dùng ClientManager thay AuctionServer cho register/broadcast/sendToUser
 *   ✅ Dùng AuctionService.placeBid() cho PLACE_BID → thread-safe, có lock
 *   ✅ Inject AuctionDAO vào AuctionService để ghi DB với rollback
 *   ✅ Kết quả PLACE_BID trả về success/fail rõ ràng từ BidResult
 */
public class ClientHandler implements Runnable, Observer {

    private final Socket socket;

    private volatile PrintWriter out;
    private volatile boolean     isClosed = false;

    private final Object writeLock = new Object();

    private String username = "unknown";
    private String role     = "BIDDER";

    // AuctionDAO — inject khi cần (null = không dùng DB, chỉ in-memory)
    private AuctionDAO auctionDAO = null;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        // TODO: thay bằng DBConnection.getConnection() nếu có DB
        // try { this.auctionDAO = new AuctionDAO(DBConnection.getConnection()); }
        // catch (Exception e) { System.err.println("DB unavailable: " + e.getMessage()); }
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
            // ✅ Dùng ClientManager thay AuctionServer.removeObserver
            ClientManager.unregister(this);
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

                    // ✅ Đăng ký vào ClientManager (thay AuctionServer.registerUser)
                    ClientManager.register(username, this);
                    AuctionServer.registerUser(username, this); // giữ cho tương thích

                    sendJson(Map.of(
                            "status",   "OK",
                            "message",  "Login success",
                            "username", username,
                            "role",     role
                    ));

                    // ✅ Dùng ClientManager.broadcastAll
                    ClientManager.broadcastAll(JsonUtil.toJson(Map.of(
                            "type",    "SYSTEM",
                            "message", username + " đã tham gia."
                    )));

                    // Gửi số online
                    ClientManager.broadcastAll(JsonUtil.toJson(Map.of(
                            "type",  "ONLINE_COUNT",
                            "count", String.valueOf(ClientManager.getOnlineCount())
                    )));

                    // Gửi pending products cho Admin mới login
                    if ("ADMIN".equals(role)) {
                        for (String pending : AuctionServer.getPendingProducts()) {
                            sendMessage(pending);
                        }
                    }

                    // Gửi các phiên đang active
                    for (String sessionData : AuctionServer.getActiveSessions()) {
                        sendMessage(sessionData);
                    }
                }

                // ── Đặt giá (✅ THAY ĐỔI CHÍNH) ───────────────
                case "PLACE_BID" -> {
                    String sessionId = safe(data, "sessionId");
                    double amount;
                    try {
                        amount = Double.parseDouble(safe(data, "amount"));
                    } catch (NumberFormatException e) {
                        sendJson(Map.of("status", "ERROR",
                                "message", "Số tiền không hợp lệ"));
                        return;
                    }

                    // ✅ Gọi AuctionService.placeBid — thread-safe, có lock
                    AuctionService.BidResult result =
                            AuctionService.getInstance()
                                    .placeBid(sessionId, username, amount, auctionDAO);

                    if (result.success) {
                        sendJson(Map.of(
                                "status",  "OK",
                                "message", "Bid placed successfully",
                                "amount",  String.valueOf(amount)
                        ));
                        // Broadcast đã được AuctionService thực hiện
                    } else {
                        sendJson(Map.of(
                                "status",  "ERROR",
                                "message", result.message
                        ));
                    }
                }

                // ── Chat ──────────────────────────────────────
                case "CHAT" -> {
                    String chatMsg = data.get("message").toString();
                    ClientManager.broadcastAll(JsonUtil.toJson(Map.of(
                            "type",     "CHAT",
                            "username", username,
                            "message",  chatMsg
                    )));
                }

                // ── Admin duyệt → notify Seller ───────────────
                case "PRODUCT_APPROVED" -> {
                    String productId   = safe(data, "productId");
                    String sellerUser  = safe(data, "sellerUsername");
                    String productName = safe(data, "productName");

                    AuctionServer.removePendingProduct(productId);
                    // ✅ Dùng ClientManager.sendToUser
                    ClientManager.sendToUser(sellerUser, JsonUtil.toJson(Map.of(
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
                    ClientManager.sendToUser(sellerUser, JsonUtil.toJson(Map.of(
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
                    AuctionServer.addActiveSession(sessionId, notif);
                    // ✅ Broadcast cho TẤT CẢ
                    ClientManager.broadcastAll(notif);
                    sendJson(Map.of("status", "OK",
                            "message", "Session start broadcasted"));
                }

                // ── Kết thúc phiên ────────────────────────────
                case "SESSION_END" -> {
                    String sessionId = safe(data, "sessionId");
                    String itemName  = safe(data, "itemName");

                    AuctionServer.removeActiveSession(sessionId);
                    AuctionService.getInstance().removeSession(sessionId);
                    ClientManager.broadcastAll(JsonUtil.toJson(Map.of(
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
                            // ✅ Lấy từ ClientManager thay AuctionServer
                            "count", String.valueOf(ClientManager.getOnlineCount())
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
