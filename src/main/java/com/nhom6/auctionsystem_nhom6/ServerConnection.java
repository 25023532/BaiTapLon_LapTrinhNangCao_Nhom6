package com.nhom6.auctionsystem_nhom6;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ServerConnection – quản lý kết nối socket đến AuctionServer.
 * Singleton, dùng chung trong toàn app.
 *
 * Hỗ trợ:
 *  - Seller đăng sản phẩm → sendProductPending() → Admin nhận notify
 *  - Admin duyệt/từ chối  → sendProductApproved/Rejected() → Seller nhận notify
 *  - Seller bắt đầu phiên → sendSessionStart() → Bidder nhận notify
 *  - Bid realtime         → sendBid()
 *  - Chat                 → sendChat()
 */
public class ServerConnection {

    private static final String HOST = "yamabiko.proxy.rlwy.net";
    private static final int    PORT = 10654;

    private static final DateTimeFormatter DT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static ServerConnection instance;

    private Socket         socket;
    private PrintWriter    out;
    private BufferedReader in;
    private MessageListener listener;

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) instance = new ServerConnection();
        return instance;
    }

    // =========================================================
    // KẾT NỐI
    // =========================================================

    /**
     * Kết nối đến server và gửi LOGIN kèm role.
     * Server dùng role để phân loại (Admin/Seller/Bidder).
     */
    public boolean connect(String username) {
        try {
            socket = new Socket(HOST, PORT);
            out    = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true);
            in     = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            System.out.println("[Client] Đã kết nối server "
                    + HOST + ":" + PORT);

            // Lắng nghe server trên thread nền
            Thread reader = new Thread(this::listenFromServer, "ServerReader");
            reader.setDaemon(true);
            reader.start();

            // Lấy role từ AppContext
            String role = "BIDDER";
            if (AppContext.getCurrentUser() != null)
                role = AppContext.getCurrentUser().getRole().toUpperCase();

            // Gửi LOGIN với role để server phân loại
            sendJson("{\"action\":\"LOGIN\","
                    + "\"username\":\"" + esc(username) + "\","
                    + "\"role\":\"" + esc(role) + "\"}");

            return true;
        } catch (IOException e) {
            System.err.println("[Client] Offline: " + e.getMessage());
            return false;
        }
    }

    /** Giữ lại để không break code cũ */
    public boolean connect() {
        return connect("guest_" + System.currentTimeMillis());
    }

    // =========================================================
    // GỬI MESSAGE
    // =========================================================

    /** Chat */
    public void sendChat(String username, String message) {
        sendJson("{\"action\":\"CHAT\","
                + "\"username\":\"" + esc(username) + "\","
                + "\"message\":\""  + esc(message)  + "\"}");
    }

    /** Đặt giá */
    public void sendBid(String username, String sessionId, double amount) {
        sendJson("{\"action\":\"PLACE_BID\","
                + "\"username\":\""  + esc(username)  + "\","
                + "\"sessionId\":\"" + esc(sessionId) + "\","
                + "\"amount\":\""    + amount          + "\"}");
    }

    /**
     * Seller gọi khi thêm sản phẩm mới.
     * Server → broadcastToRole("ADMIN") → Admin nhận NOTIFY_ADMIN_NEW_PRODUCT
     */
    public void sendProductPending(String productId,
                                    String productName,
                                    String sellerName,
                                    String category,
                                    double startPrice,
                                    LocalDateTime startTime,
                                    LocalDateTime endTime) {
        sendJson("{\"action\":\"PRODUCT_PENDING\","
                + "\"productId\":\""   + esc(productId)           + "\","
                + "\"productName\":\"" + esc(productName)         + "\","
                + "\"sellerName\":\""  + esc(sellerName)          + "\","
                + "\"category\":\""    + esc(category)            + "\","
                + "\"startPrice\":\""  + startPrice               + "\","
                + "\"startTime\":\""   + startTime.format(DT)     + "\","
                + "\"endTime\":\""     + endTime.format(DT)       + "\"}");
    }

    /**
     * Admin gọi khi duyệt sản phẩm.
     * Server → sendToUser(sellerUsername) → Seller nhận NOTIFY_SELLER_APPROVED
     */
    public void sendProductApproved(String productId,
                                     String sellerUsername,
                                     String productName) {
        sendJson("{\"action\":\"PRODUCT_APPROVED\","
                + "\"productId\":\""      + esc(productId)      + "\","
                + "\"sellerUsername\":\"" + esc(sellerUsername) + "\","
                + "\"productName\":\""    + esc(productName)    + "\"}");
    }

    /**
     * Admin gọi khi từ chối sản phẩm.
     * Server → sendToUser(sellerUsername) → Seller nhận NOTIFY_SELLER_REJECTED
     */
    public void sendProductRejected(String productId,
                                     String sellerUsername,
                                     String productName,
                                     String reason) {
        sendJson("{\"action\":\"PRODUCT_REJECTED\","
                + "\"productId\":\""      + esc(productId)      + "\","
                + "\"sellerUsername\":\"" + esc(sellerUsername) + "\","
                + "\"productName\":\""    + esc(productName)    + "\","
                + "\"reason\":\""         + esc(reason)         + "\"}");
    }

    /**
     * Seller gọi khi bắt đầu phiên đấu giá.
     * Server → broadcastAll() → Bidder nhận NOTIFY_BIDDER_SESSION_START
     */
    public void sendSessionStart(String sessionId,
                                  String itemName,
                                  double startPrice,
                                  double minStep,
                                  LocalDateTime endTime,
                                  String sellerName,
                                  String category) {
        sendJson("{\"action\":\"SESSION_START\","
                + "\"sessionId\":\""  + esc(sessionId)       + "\","
                + "\"itemName\":\""   + esc(itemName)         + "\","
                + "\"startPrice\":\"" + startPrice            + "\","
                + "\"minStep\":\""    + minStep               + "\","
                + "\"endTime\":\""    + endTime.format(DT)   + "\","
                + "\"sellerName\":\"" + esc(sellerName)       + "\","
                + "\"category\":\""   + esc(category)         + "\"}");
    }

    /** Gửi khi phiên kết thúc */
    public void sendSessionEnd(String sessionId, String itemName) {
        sendJson("{\"action\":\"SESSION_END\","
                + "\"sessionId\":\"" + esc(sessionId) + "\","
                + "\"itemName\":\""  + esc(itemName)  + "\"}");
    }

    /** Gửi raw JSON */
    public void sendJson(String json) {
        if (out != null) {
            out.println(json);
            System.out.println("[Client gửi] " + json);
        }
    }

    /** Giữ tương thích với code cũ dùng send("CHAT:...") hoặc send("BID:...") */
    public void send(String message) {
        if (message.startsWith("CHAT:")) {
            String[] p = message.split(":", 3);
            if (p.length == 3) { sendChat(p[1], p[2]); return; }
        }
        if (message.startsWith("BID:")) {
            String[] p = message.split(":", 3);
            if (p.length == 3) {
                try {
                    sendBid(p[1], "default-session",
                            Double.parseDouble(p[2]));
                } catch (NumberFormatException e) { sendJson(message); }
                return;
            }
        }
        sendJson(message);
    }

    // =========================================================
    // LISTENER & ĐỌC TIN
    // =========================================================

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    private void listenFromServer() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[Client nhận] " + line);
                if (listener != null) {
                    final String msg = line;
                    javafx.application.Platform.runLater(
                            () -> listener.onMessage(msg));
                }
            }
        } catch (IOException e) {
            System.out.println("[Client] Mất kết nối server.");
        }
    }

    // =========================================================
    // UTIL
    // =========================================================

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void disconnect() {
        try { if (socket != null) socket.close(); }
        catch (IOException e) { e.printStackTrace(); }
    }

    /** Escape ký tự đặc biệt trong JSON string */
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public interface MessageListener {
        void onMessage(String message);
    }
}
