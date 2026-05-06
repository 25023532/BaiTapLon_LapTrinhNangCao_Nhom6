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
    private static ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private static List<ClientHandler> observers = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đang chạy trên Port: " + PORT);
            System.out.println("Đang chờ người tham gia...");

            while (true) {
                // Chấp nhận kết nối mới
                Socket clientSocket = serverSocket.accept();
                System.out.println("Có thiết bị mới kết nối: " + clientSocket.getRemoteSocketAddress());

                // Tạo một bộ xử lý riêng cho khách hàng này
                ClientHandler handler = new ClientHandler(clientSocket);
                observers.add(handler);

                // Chạy handler trong một luồng riêng
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("Lỗi Server: " + e.getMessage());
        }
    }
}