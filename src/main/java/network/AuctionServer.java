package network;

import org.example.util.JsonUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AuctionServer — server TCP đa luồng cho hệ thống đấu giá.
 *
 * Tính năng:
 *  - Seller đăng sản phẩm → notify Admin
 *  - Admin duyệt/từ chối  → notify Seller
 *  - Seller bắt đầu phiên → notify tất cả Bidder
 *  - Đồng bộ bid realtime
 */
public class AuctionServer {

    private static final int PORT             = 1234;
    private static final int THREAD_POOL_SIZE = 10;

    // ── Danh sách client ──────────────────────────────────
    /** Tất cả client đang kết nối (thread-safe iterate) */
    private static final List<ClientHandler> observers =
            new CopyOnWriteArrayList<>();

    /** username → handler (để gửi đến 1 user cụ thể) */
    private static final Map<String, ClientHandler> userMap =
            new ConcurrentHashMap<>();

    // ── Dữ liệu cần giữ để gửi cho client mới login ──────
    /** Sản phẩm CHỜ DUYỆT → gửi cho Admin mới login */
    private static final List<String> pendingProducts =
            new CopyOnWriteArrayList<>();

    /** Phiên đang active → gửi cho client mới login */
    private static final Map<String, String> activeSessions =
            new ConcurrentHashMap<>();

    private static final ExecutorService threadPool =
            Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    private static final AtomicBoolean running = new AtomicBoolean(true);

    // =========================================================
    // MAIN
    // =========================================================
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(
                new Thread(AuctionServer::shutdown, "shutdown-hook"));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setReuseAddress(true);
            System.out.println("[Server] Đang chạy trên port " + PORT);
            System.out.println("[Server] Đang chờ kết nối...");

            while (running.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[Server] Kết nối mới: "
                            + clientSocket.getRemoteSocketAddress());

                    ClientHandler handler = new ClientHandler(clientSocket);
                    observers.add(handler);
                    threadPool.execute(handler);
                    broadcastOnlineCount();

                } catch (IOException e) {
                    if (running.get())
                        System.err.println("[Server] Lỗi accept: "
                                + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[Server] Lỗi khởi động: " + e.getMessage());
        }
    }

    // =========================================================
    // BROADCAST METHODS
    // =========================================================

    /** Gửi đến tất cả NGOẠI TRỪ sender */
    public static void broadcast(String message, ClientHandler exclude) {
        for (ClientHandler h : observers)
            if (h != exclude) h.sendMessage(message);
    }

    /** Gửi đến TẤT CẢ client */
    public static void broadcastAll(String message) {
        System.out.println("[BROADCAST_ALL] " + message);
        for (ClientHandler h : observers)
            h.sendMessage(message);
    }

    /** Gửi đến tất cả client có role cụ thể (ADMIN / SELLER / BIDDER) */
    public static void broadcastToRole(String role, String message) {
        System.out.println("[BROADCAST_ROLE:" + role + "] " + message);
        for (ClientHandler h : observers) {
            if (role.equalsIgnoreCase(h.getRole()))
                h.sendMessage(message);
        }
    }

    /** Gửi đến 1 user cụ thể theo username */
    public static void sendToUser(String username, String message) {
        ClientHandler handler = userMap.get(username);
        if (handler != null) {
            handler.sendMessage(message);
            System.out.println("[SEND_TO:" + username + "] " + message);
        } else {
            System.out.println("[SEND_TO:" + username + "] User offline, bỏ qua.");
        }
    }

    /** Broadcast số người online */
    public static void broadcastOnlineCount() {
        int count = observers.size();
        String msg = JsonUtil.toJson(Map.of(
                "type",  "ONLINE_COUNT",
                "count", String.valueOf(count)
        ));
        broadcastAll(msg);
        System.out.println("[ONLINE] " + count + " người đang online.");
    }

    // =========================================================
    // QUẢN LÝ CLIENT
    // =========================================================

    /** Đăng ký client sau khi LOGIN thành công */
    public static void registerUser(String username, ClientHandler handler) {
        userMap.put(username, handler);
        System.out.println("[Server] Đăng ký user: " + username
                + " | Role: " + handler.getRole()
                + " | Online: " + observers.size());
    }

    /** Xóa client khi ngắt kết nối */
    public static void removeObserver(ClientHandler handler) {
        boolean removed = observers.remove(handler);
        userMap.values().remove(handler);
        if (removed) {
            System.out.println("[DISCONNECT] [" + handler.getUsername()
                    + "] ngắt kết nối. Còn lại: " + observers.size());
            broadcastOnlineCount();
        }
    }

    public static int getOnlineCount() { return observers.size(); }

    // =========================================================
    // PENDING PRODUCTS (chờ Admin duyệt)
    // =========================================================

    /** Lưu sản phẩm CHỜ DUYỆT để gửi cho Admin mới login */
    public static void addPendingProduct(String notifJson) {
        pendingProducts.add(notifJson);
    }

    /** Xóa khi Admin đã xử lý (duyệt hoặc từ chối) */
    public static void removePendingProduct(String productId) {
        pendingProducts.removeIf(s -> s.contains(productId));
    }

    public static List<String> getPendingProducts() {
        return Collections.unmodifiableList(pendingProducts);
    }

    // =========================================================
    // ACTIVE SESSIONS
    // =========================================================

    /** Lưu phiên đang chạy để gửi cho client mới login */
    public static void addActiveSession(String sessionId, String notifJson) {
        activeSessions.put(sessionId, notifJson);
    }

    public static void removeActiveSession(String sessionId) {
        activeSessions.remove(sessionId);
    }

    public static Collection<String> getActiveSessions() {
        return activeSessions.values();
    }

    // =========================================================
    // SHUTDOWN
    // =========================================================
    private static void shutdown() {
        System.out.println("[SHUTDOWN] Đang tắt server...");
        running.set(false);
        for (ClientHandler h : observers) h.close();
        observers.clear();
        userMap.clear();
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS))
                threadPool.shutdownNow();
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[SHUTDOWN] Server đã tắt hoàn toàn.");
    }
}
