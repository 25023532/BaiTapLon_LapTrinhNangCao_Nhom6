package com.nhom6.auctionsystem_nhom6;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerConnection {

    // ── KHÔNG hardcode HOST/PORT nữa — đọc từ ServerConfig ──
    // private static final String HOST = "yamabiko.proxy.rlwy.net"; // XÓA
    // private static final int    PORT = 10654;                      // XÓA

    private static final DateTimeFormatter DT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static ServerConnection instance;

    private Socket          socket;
    private PrintWriter     out;
    private BufferedReader  in;
    private MessageListener listener;

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) instance = new ServerConnection();
        return instance;
    }

    // =========================================================
    // KẾT NỐI — đọc HOST/PORT động từ ServerConfig
    // =========================================================
    public boolean connect(String username) {
        // Đảm bảo file config tồn tại
        ServerConfig.createDefaultIfMissing();

        String host = ServerConfig.getHost();
        int    port = ServerConfig.getPort();

        try {
            System.out.println("[Client] Đang kết nối "
                    + host + ":" + port + " ...");

            socket = new Socket(host, port);
            out    = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true);
            in     = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            // Lắng nghe server trên thread nền
            Thread reader = new Thread(
                    this::listenFromServer, "ServerReader");
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

            System.out.println("[Client] Kết nối thành công: "
                    + username + " [" + role + "] → "
                    + host + ":" + port);
            return true;

        } catch (IOException e) {
            System.err.println("[Client] Không kết nối được "
                    + host + ":" + port
                    + " — Chạy offline. (" + e.getMessage() + ")");
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
                + "\"productId\":\""   + esc(productId)       + "\","
                + "\"productName\":\"" + esc(productName)      + "\","
                + "\"sellerName\":\""  + esc(sellerName)       + "\","
                + "\"category\":\""    + esc(category)         + "\","
                + "\"startPrice\":\""  + startPrice            + "\","
                + "\"startTime\":\""   + startTime.format(DT)  + "\","
                + "\"endTime\":\""     + endTime.format(DT)    + "\"}");
    }

    public void sendProductApproved(String productId,
                                     String sellerUsername,
                                     String productName) {
        sendJson("{\"action\":\"PRODUCT_APPROVED\","
                + "\"productId\":\""      + esc(productId)      + "\","
                + "\"sellerUsername\":\"" + esc(sellerUsername)  + "\","
                + "\"productName\":\""    + esc(productName)     + "\"}");
    }

    public void sendProductRejected(String productId,
                                     String sellerUsername,
                                     String productName,
                                     String reason) {
        sendJson("{\"action\":\"PRODUCT_REJECTED\","
                + "\"productId\":\""      + esc(productId)      + "\","
                + "\"sellerUsername\":\"" + esc(sellerUsername)  + "\","
                + "\"productName\":\""    + esc(productName)     + "\","
                + "\"reason\":\""         + esc(reason)          + "\"}");
    }

    public void sendSessionStart(String sessionId,
                                  String itemName,
                                  double startPrice,
                                  double minStep,
                                  LocalDateTime endTime,
                                  String sellerName,
                                  String category) {
        sendJson("{\"action\":\"SESSION_START\","
                + "\"sessionId\":\""  + esc(sessionId)      + "\","
                + "\"itemName\":\""   + esc(itemName)        + "\","
                + "\"startPrice\":\"" + startPrice           + "\","
                + "\"minStep\":\""    + minStep              + "\","
                + "\"endTime\":\""    + endTime.format(DT)   + "\","
                + "\"sellerName\":\"" + esc(sellerName)      + "\","
                + "\"category\":\""   + esc(category)        + "\"}");
    }

    public void sendSessionEnd(String sessionId, String itemName) {
        sendJson("{\"action\":\"SESSION_END\","
                + "\"sessionId\":\"" + esc(sessionId) + "\","
                + "\"itemName\":\""  + esc(itemName)  + "\"}");
    }

    public void sendJson(String json) {
        if (out != null) {
            out.println(json);
            System.out.println("[Client gửi] " + json);
        }
    }

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
