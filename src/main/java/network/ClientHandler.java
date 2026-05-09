package network;

import org.example.util.JsonUtil;

import java.io.*;
import java.net.Socket;
import java.util.Map;

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
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            this.out = writer;

            sendJson(Map.of(
                "status", "OK",
                "message", "Connected to Auction Server"
            ));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("[SERVER RECEIVED] " + inputLine);
                handleMessage(inputLine);
            }

        } catch (IOException e) {
            System.out.println("Client [" + username + "] disconnected.");
        } finally {
            AuctionServer.removeObserver(this);
            try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    // ✅ Method này được AuctionServer.broadcast() gọi
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMessage(String message) {
        if (message == null || message.isBlank()) return;

        try {
            Map<String, Object> data = (Map<String, Object>) JsonUtil.parse(message);
            String action = data.get("action").toString().toUpperCase();

            switch (action) {

                case "LOGIN" -> {
                    username = data.get("username").toString();
                    sendJson(Map.of(
                        "status", "OK",
                        "message", "Login success",
                        "username", username
                    ));
                    AuctionServer.broadcast(
                        JsonUtil.toJson(Map.of(
                            "type", "SYSTEM",
                            "message", username + " joined auction."
                        )), this
                    );
                }

                case "PLACE_BID" -> {
                    String sessionId = data.get("sessionId").toString();
                    String amount    = data.get("amount").toString();
                    AuctionServer.broadcast(
                        JsonUtil.toJson(Map.of(
                            "type",      "NEW_BID",
                            "username",  username,
                            "sessionId", sessionId,
                            "amount",    amount
                        )), null
                    );
                    sendJson(Map.of("status", "OK", "message", "Bid placed successfully"));
                }

                case "CHAT" -> {
                    String chatMessage = data.get("message").toString();
                    AuctionServer.broadcast(
                        JsonUtil.toJson(Map.of(
                            "type",     "CHAT",
                            "username", username,
                            "message",  chatMessage
                        )), null
                    );
                }

                default -> sendJson(Map.of("status", "ERROR", "message", "Unsupported action"));
            }

        } catch (Exception e) {
            sendJson(Map.of("status", "ERROR", "message", "Invalid JSON format"));
        }
    }

    private void sendJson(Map<String, Object> data) {
        if (out != null) out.println(JsonUtil.toJson(data));
    }

    // Observer pattern
    @Override
    public void update(Object message) {
        if (out != null) out.println(message.toString());
    }
}
