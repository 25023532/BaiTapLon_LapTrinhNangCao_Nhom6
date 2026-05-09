package network;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable, Observer {
    private final Socket socket;
    private PrintWriter out;
    private String username = "unknown";

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            this.out = writer;
            // Gửi thông báo chào mừng
            out.println("SERVER:Kết nối thành công đến AuctionServer!");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Server nhận: " + inputLine + " từ " + socket.getRemoteSocketAddress());
                handleMessage(inputLine);
            }
        } catch (IOException e) {
            System.out.println("Client [" + username + "] đã ngắt kết nối.");
        } finally {
            AuctionServer.removeObserver(this);
            try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    /**
     * Xử lý các lệnh từ client:
     *   LOGIN:<username>:<password>
     *   BID:<username>:<amount>
     *   CHAT:<username>:<message>
     */
    private void handleMessage(String message) {
        if (message == null || message.isBlank()) return;
        String[] parts = message.split(":", 3);
        String command = parts[0].toUpperCase();

        switch (command) {
            case "LOGIN" -> {
                if (parts.length >= 3) {
                    username = parts[1];
                    out.println("LOGIN_OK:" + username);
                    AuctionServer.broadcast("SERVER:" + username + " đã tham gia phiên đấu giá.", this);
                } else {
                    out.println("ERROR:Lệnh LOGIN không hợp lệ");
                }
            }
            case "BID" -> {
                if (parts.length >= 3) {
                    AuctionServer.broadcast("BID:" + parts[1] + ":" + parts[2], null);
                } else {
                    out.println("ERROR:Lệnh BID không hợp lệ");
                }
            }
            case "CHAT" -> {
                if (parts.length >= 3) {
                    AuctionServer.broadcast("CHAT:" + parts[1] + ":" + parts[2], null);
                } else {
                    out.println("ERROR:Lệnh CHAT không hợp lệ");
                }
            }
            default -> out.println("ERROR:Lệnh không được hỗ trợ: " + command);
        }
    }

    /** Gửi tin nhắn đến client này */
    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    @Override
    public void update(Object message) {
        sendMessage(message.toString());
    }
}