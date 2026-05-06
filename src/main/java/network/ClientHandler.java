package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String inputLine;
            // Đọc dữ liệu từ khách hàng gửi lên
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Server nhận được: " + inputLine + " từ " + socket.getRemoteSocketAddress());
            }
        } catch (IOException e) {
            System.out.println("Một khách hàng đã ngắt kết nối.");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}