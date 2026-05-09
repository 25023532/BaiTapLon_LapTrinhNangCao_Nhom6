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

    /** Kết nối đến server. Trả về true nếu thành công. */
    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);
            out    = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Đã kết nối đến server " + HOST + ":" + PORT);

            // Lắng nghe tin nhắn từ server trên thread riêng
            Thread readerThread = new Thread(this::listenFromServer, "ServerReader");
            readerThread.setDaemon(true);
            readerThread.start();

            return true;
        } catch (IOException e) {
            System.err.println("Không thể kết nối server: " + e.getMessage());
            return false;
        }
    }

    /** Gửi lệnh lên server */
    public void send(String message) {
        if (out != null) {
            out.println(message);
            System.out.println("Client gửi: " + message);
        }
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

    /** Interface để nhận tin nhắn từ server */
    public interface MessageListener {
        void onMessage(String message);
    }
}
