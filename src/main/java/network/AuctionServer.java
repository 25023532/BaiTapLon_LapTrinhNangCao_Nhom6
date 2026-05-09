package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
            }
        } catch (IOException e) {
            System.err.println("Lỗi Server: " + e.getMessage());
        }
    }

    /** Gửi tin nhắn đến tất cả client (trừ sender nếu muốn bỏ qua) */
    public static void broadcast(String message, ClientHandler exclude) {
        System.out.println("Broadcast: " + message);
        for (ClientHandler handler : observers) {
            if (handler != exclude) {
                handler.sendMessage(message); // ✅ method này có trong ClientHandler bên dưới
            }
        }
    }

    /** Xóa client khỏi danh sách khi ngắt kết nối */
    public static void removeObserver(ClientHandler handler) {
        observers.remove(handler);
        System.out.println("Client đã bị xóa khỏi danh sách. Còn lại: " + observers.size());
    }
}
