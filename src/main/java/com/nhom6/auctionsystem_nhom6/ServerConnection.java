package com.nhom6.auctionsystem_nhom6;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket client — kết nối qua internet (khác mạng LAN).
 * Thay thế hoàn toàn TCP Socket cũ.
 * API giữ nguyên để không cần sửa Controller.
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
                    if (listener != null)
                        javafx.application.Platform.runLater(
                                () -> listener.onMessage(message));
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

    public interface MessageListener {
        void onMessage(String message);
    }
}
