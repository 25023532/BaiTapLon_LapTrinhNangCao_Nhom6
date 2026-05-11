package com.nhom6.auctionsystem_nhom6;

import java.io.*;
import java.net.Socket;

/**
 * ServerConnection – quản lý kết nối socket đến AuctionServer.
 * Singleton, dùng chung trong toàn app.
 */
public class ServerConnection {

    private static final String HOST = "127.0.0.1";
    private static final int    PORT = 1234;

    private static ServerConnection instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private MessageListener listener;

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) instance = new ServerConnection();
        return instance;
    }

    /** Kết nối đến server và đăng ký username */
    public boolean connect(String username) {
        try {
            socket = new Socket(HOST, PORT);
            out    = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Đã kết nối đến server " + HOST + ":" + PORT);

            // Lắng nghe tin nhắn từ server trên thread riêng
            Thread readerThread = new Thread(this::listenFromServer, "ServerReader");
            readerThread.setDaemon(true);
            readerThread.start();

            // ✅ Gửi LOGIN để server biết username và cập nhật online count
            sendJson("{\"action\":\"LOGIN\",\"username\":\"" + username + "\"}");

            return true;
        } catch (IOException e) {
            System.err.println("Không thể kết nối server: " + e.getMessage());
            return false;
        }
    }

    /** Giữ lại method cũ để không break code cũ */
    public boolean connect() {
        return connect("guest_" + System.currentTimeMillis());
    }

    /** Gửi tin nhắn chat */
    public void sendChat(String username, String message) {
        sendJson("{\"action\":\"CHAT\","
                + "\"username\":\"" + username + "\","
                + "\"message\":\"" + escapeJson(message) + "\"}");
    }

    /** Gửi bid */
    public void sendBid(String username, String sessionId, double amount) {
        sendJson("{\"action\":\"PLACE_BID\","
                + "\"username\":\"" + username + "\","
                + "\"sessionId\":\"" + sessionId + "\","
                + "\"amount\":\"" + amount + "\"}");
    }

    /** Gửi raw JSON string lên server */
    public void sendJson(String json) {
        if (out != null) {
            out.println(json);
            System.out.println("Client gửi: " + json);
        }
    }

    /** Gửi lệnh thô (giữ tương thích cũ) */
    public void send(String message) {
        // Parse "CHAT:user:msg" và "BID:user:amount" thành JSON đúng format
        if (message.startsWith("CHAT:")) {
            String[] parts = message.split(":", 3);
            if (parts.length == 3) {
                sendChat(parts[1], parts[2]);
                return;
            }
        }
        if (message.startsWith("BID:")) {
            String[] parts = message.split(":", 3);
            if (parts.length == 3) {
                try {
                    sendBid(parts[1], "default-session", Double.parseDouble(parts[2]));
                } catch (NumberFormatException e) {
                    sendJson(message);
                }
                return;
            }
        }
        // Fallback: gửi thẳng
        sendJson(message);
    }

    /** Đặt listener để nhận tin nhắn từ server */
    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    /** Liên tục đọc tin nhắn từ server */
    private void listenFromServer() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Client nhận: " + line);
                if (listener != null) {
                    final String msg = line;
                    javafx.application.Platform.runLater(() -> listener.onMessage(msg));
                }
            }
        } catch (IOException e) {
            System.out.println("Mất kết nối với server.");
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    /** Interface để nhận tin nhắn từ server */
    public interface MessageListener {
        void onMessage(String message);
    }
}