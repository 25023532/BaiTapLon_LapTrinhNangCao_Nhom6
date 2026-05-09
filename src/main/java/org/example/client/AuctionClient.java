package org.example.client;

import org.example.util.JsonUtil;

import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 * Client giao tiếp với Auction Server
 */
public class AuctionClient implements Closeable {

    private final String host;

    private final int port;

    private Socket socket;

    private BufferedReader in;

    private PrintWriter out;

    public AuctionClient(String host, int port) {

        this.host = host;
        this.port = port;
    }

    /**
     * Kết nối server
     */
    public void connect() throws IOException {

        socket = new Socket(host, port);

        in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
        );

        out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream()),
                true
        );

        System.out.println(
                "[CLIENT] Connected to " + host + ":" + port
        );

        // Đọc phản hồi đầu tiên từ server
        String response = in.readLine();

        System.out.println(
                "[SERVER] " + response
        );
    }

    // ════════════════════════════════════════════════
    // LOGIN
    // ════════════════════════════════════════════════

    public Map<String, Object> login(
            String username,
            String password
    ) {

        return send(Map.of(
                "action", "LOGIN",
                "username", username,
                "password", password
        ));
    }

    // ════════════════════════════════════════════════
    // PLACE BID
    // ════════════════════════════════════════════════

    public Map<String, Object> placeBid(
            String sessionId,
            double amount
    ) {

        return send(Map.of(
                "action", "PLACE_BID",
                "sessionId", sessionId,
                "amount", amount
        ));
    }

    // ════════════════════════════════════════════════
    // CHAT
    // ════════════════════════════════════════════════

    public Map<String, Object> sendChat(
            String message
    ) {

        return send(Map.of(
                "action", "CHAT",
                "message", message
        ));
    }

    // ════════════════════════════════════════════════
    // CORE SEND METHOD
    // ════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    public Map<String, Object> send(
            Map<String, ?> request
    ) {

        try {

            String json =
                    JsonUtil.toJson(request);

            out.println(json);

            String response =
                    in.readLine();

            return (Map<String, Object>)
                    JsonUtil.parse(response);

        } catch (Exception e) {

            return Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()
            );
        }
    }

    /**
     * Đóng kết nối
     */
    @Override
    public void close() throws IOException {

        if (socket != null) {
            socket.close();
        }
    }
}
