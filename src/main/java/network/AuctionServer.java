package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.example.util.JsonUtil;
import java.util.Map;

public class AuctionServer {

    private static final int PORT = 1234;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private static final List<ClientHandler> observers = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đang chạy trên Port: " + PORT);
            System.out.println("Đang chờ người tham gia...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Có thiết bị mới kết nối: "
                        + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                observers.add(handler);
                threadPool.execute(handler);

                // Broadcast số online ngay khi có người kết nối mới
                broadcastOnlineCount();
            }
        } catch (IOException e) {
            System.err.println("Lỗi Server: " + e.getMessage());
        }
    }

    /** Gửi tin nhắn đến tất cả client */
    public static void broadcast(String message, ClientHandler exclude) {
        System.out.println("Broadcast: " + message);
        for (ClientHandler handler : observers) {
            if (handler != exclude) {
                handler.sendMessage(message);
            }
        }
    }

    /** Broadcast đến TẤT CẢ kể cả sender (dùng cho CHAT & ONLINE_COUNT) */
    public static void broadcastAll(String message) {
        System.out.println("BroadcastAll: " + message);
        for (ClientHandler handler : observers) {
            handler.sendMessage(message);
        }
    }

    /** Gửi số người online thực đến tất cả client */
    public static void broadcastOnlineCount() {
        int count = observers.size();
        String msg = JsonUtil.toJson(Map.of(
                "type",  "ONLINE_COUNT",
                "count", String.valueOf(count)
        ));
        broadcastAll(msg);
        System.out.println("Online count: " + count);
    }

    /** Xóa client khỏi danh sách khi ngắt kết nối */
    public static void removeObserver(ClientHandler handler) {
        observers.remove(handler);
        System.out.println("Client đã bị xóa. Còn lại: " + observers.size());

        // Cập nhật số online sau khi có người thoát
        broadcastOnlineCount();
    }

    /** Lấy số người online hiện tại */
    public static int getOnlineCount() {
        return observers.size();
    }
}
