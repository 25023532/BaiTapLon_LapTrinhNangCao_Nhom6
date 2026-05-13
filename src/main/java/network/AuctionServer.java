package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.example.util.JsonUtil;

/**
 * AuctionServer — server TCP đa luồng cho hệ thống đấu giá.
 *
 * Vấn đề đồng bộ đã xử lý:
 *  1. CopyOnWriteArrayList  → add/remove thread-safe; iterator snapshot không bị
 *     ConcurrentModificationException khi broadcast đang chạy.
 *  2. AtomicBoolean running → cờ dừng server không cần synchronized.
 *  3. broadcastOnlineCount  → đọc size() và gửi tin trong cùng 1 snapshot để tránh
 *     race giữa "đọc count" và "gửi tin".
 *  4. broadcast / broadcastAll → dùng snapshot list (COWL tự cung cấp) nên thread
 *     khác add/remove trong lúc broadcast không gây lỗi.
 *  5. Graceful shutdown → đóng ServerSocket và shutdown threadPool khi JVM thoát.
 *  6. sendMessage được xử lý trong ClientHandler (synchronized trên outputStream).
 */
public class AuctionServer {

    private static final int PORT = 1234;
    private static final int THREAD_POOL_SIZE = 10;

    // =========================================================
    // STATE CHIA SẺ GIỮA CÁC LUỒNG
    // =========================================================

    /**
     * CopyOnWriteArrayList: mỗi lần write (add/remove) tạo bản sao mảng mới.
     * Iterator/for-each luôn nhìn thấy snapshot tại thời điểm bắt đầu vòng lặp
     * → broadcast không bị ConcurrentModificationException dù có thread khác
     * đang add/remove đồng thời.
     *
     * Trade-off: write chậm hơn ArrayList (O(n) copy), nhưng read/iterate rất nhanh.
     * Phù hợp vì broadcast thường xuyên hơn connect/disconnect.
     */
    private static final List<ClientHandler> observers = new CopyOnWriteArrayList<>();

    private static final ExecutorService threadPool =
            Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    /**
     * AtomicBoolean: cờ dừng vòng lặp accept() mà không cần synchronized.
     * Volatile đảm bảo mọi thread thấy giá trị mới nhất ngay lập tức.
     */
    private static final AtomicBoolean running = new AtomicBoolean(true);

    // =========================================================
    // ENTRY POINT
    // =========================================================

    public static void main(String[] args) {
        // Đăng ký hook dọn dẹp khi JVM tắt (Ctrl+C, kill, v.v.)
        Runtime.getRuntime().addShutdownHook(new Thread(AuctionServer::shutdown,
                "shutdown-hook"));

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            // Cho phép tái sử dụng cổng ngay sau khi khởi động lại server
            serverSocket.setReuseAddress(true);

            System.out.println("Server đang chạy trên Port: " + PORT);
            System.out.println("Đang chờ người tham gia...");

            while (running.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Có thiết bị mới kết nối: "
                            + clientSocket.getRemoteSocketAddress());

                    ClientHandler handler = new ClientHandler(clientSocket);

                    /*
                     * QUAN TRỌNG: add vào observers TRƯỚC khi submit vào threadPool.
                     * Nếu add sau khi submit, có thể xảy ra race:
                     *   - Thread handler đã gửi tin trước khi được thêm vào list
                     *   - broadcastOnlineCount() đọc list khi handler chưa có trong đó
                     */
                    observers.add(handler);

                    threadPool.execute(handler);

                    // Broadcast số online sau khi đã thêm handler vào list
                    broadcastOnlineCount();

                } catch (IOException e) {
                    // Khi shutdown() đóng serverSocket, accept() ném IOException
                    // → kiểm tra cờ running để phân biệt lỗi thật vs tắt có chủ đích
                    if (running.get()) {
                        System.err.println("Lỗi khi chấp nhận kết nối: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Lỗi khởi động Server: " + e.getMessage());
        }
    }

    // =========================================================
    // BROADCAST METHODS
    // =========================================================

    /**
     * Gửi tin đến tất cả client TRỪ sender.
     *
     * COWL cung cấp snapshot list tại thời điểm for-each bắt đầu.
     * Thread khác add/remove trong lúc này không ảnh hưởng vòng lặp hiện tại.
     */
    public static void broadcast(String message, ClientHandler exclude) {
        System.out.println("[BROADCAST] " + message);
        for (ClientHandler handler : observers) {          // snapshot-safe
            if (handler != exclude) {
                handler.sendMessage(message);
            }
        }
    }

    /**
     * Gửi tin đến TẤT CẢ client kể cả sender (CHAT, ONLINE_COUNT, v.v.)
     */
    public static void broadcastAll(String message) {
        System.out.println("[BROADCAST_ALL] " + message);
        for (ClientHandler handler : observers) {          // snapshot-safe
            handler.sendMessage(message);
        }
    }

    /**
     * Gửi số người online đến tất cả client.
     *
     * Lấy snapshot size() VÀ gửi tin trong cùng 1 lần gọi để đảm bảo
     * count khớp với trạng thái observers tại thời điểm đọc.
     * (Không cần synchronized vì COWL đảm bảo tính nhất quán của size())
     */
    public static void broadcastOnlineCount() {
        // Snapshot count tại đây — mọi thao tác sau đều dùng giá trị này
        int count = observers.size();

        String msg = JsonUtil.toJson(Map.of(
                "type",  "ONLINE_COUNT",
                "count", String.valueOf(count)
        ));

        broadcastAll(msg);
        System.out.println("[ONLINE] Số người đang online: " + count);
    }

    // =========================================================
    // QUẢN LÝ OBSERVER
    // =========================================================

    /**
     * Xóa client khỏi danh sách và cập nhật số online.
     * Gọi từ ClientHandler khi socket đóng.
     */
    public static void removeObserver(ClientHandler handler) {
        boolean removed = observers.remove(handler);
        if (removed) {
            System.out.println("[DISCONNECT] Client bị xóa. Còn lại: "
                    + observers.size());
            broadcastOnlineCount();
        }
    }

    /**
     * Trả về số người online hiện tại (snapshot tức thời).
     */
    public static int getOnlineCount() {
        return observers.size();
    }

    // =========================================================
    // GRACEFUL SHUTDOWN
    // =========================================================

    /**
     * Dọn dẹp tài nguyên khi server tắt.
     * Được gọi tự động bởi JVM shutdown hook.
     */
    private static void shutdown() {
        System.out.println("[SHUTDOWN] Đang tắt server...");
        running.set(false);

        // Đóng tất cả kết nối client
        for (ClientHandler handler : observers) {
            handler.close();   // ClientHandler cần có method close()
        }
        observers.clear();

        // Dừng thread pool: chờ tối đa 5 giây cho các task đang chạy hoàn thành
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
                System.out.println("[SHUTDOWN] Buộc dừng thread pool.");
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("[SHUTDOWN] Server đã tắt hoàn toàn.");
    }
}
