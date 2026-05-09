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
                        new InputStreamReader(socket.getInputStream())
                );

                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()),
                        true
                )

        ) {

            this.out = writer;

            // Gửi thông báo kết nối thành công
            sendJson(Map.of(
                    "status", "OK",
                    "message", "Connected to Auction Server"
            ));

            String inputLine;

            while ((inputLine = in.readLine()) != null) {

                System.out.println(
                        "[SERVER RECEIVED] " + inputLine
                );

                handleMessage(inputLine);
            }

        } catch (IOException e) {

            System.out.println(
                    "Client [" + username + "] disconnected."
            );

        } finally {

            AuctionServer.removeObserver(this);

            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Xử lý JSON message từ client
     */
    @SuppressWarnings("unchecked")
    private void handleMessage(String message) {

        if (message == null || message.isBlank()) {
            return;
        }

        try {

            Map<String, Object> data =
                    (Map<String, Object>) JsonUtil.parse(message);

            String action =
                    data.get("action")
                            .toString()
                            .toUpperCase();

            switch (action) {

                // ───────── LOGIN ─────────

                case "LOGIN" -> {

                    username =
                            data.get("username").toString();

                    sendJson(Map.of(
                            "status", "OK",
                            "message", "Login success",
                            "username", username
                    ));

                    AuctionServer.broadcast(
                            JsonUtil.toJson(
                                    Map.of(
                                            "type", "SYSTEM",
                                            "message",
                                            username + " joined auction."
                                    )
                            ),
                            this
                    );
                }

                // ───────── PLACE BID ─────────

                case "PLACE_BID" -> {

                    String sessionId =
                            data.get("sessionId").toString();

                    String amount =
                            data.get("amount").toString();

                    String bidJson = JsonUtil.toJson(
                            Map.of(
                                    "type", "NEW_BID",
                                    "username", username,
                                    "sessionId", sessionId,
                                    "amount", amount
                            )
                    );

                    AuctionServer.broadcast(
                            bidJson,
                            null
                    );

                    sendJson(Map.of(
                            "status", "OK",
                            "message", "Bid placed successfully"
                    ));
                }

                // ───────── CHAT ─────────

                case "CHAT" -> {

                    String chatMessage =
                            data.get("message").toString();

                    String chatJson = JsonUtil.toJson(
                            Map.of(
                                    "type", "CHAT",
                                    "username", username,
                                    "message", chatMessage
                            )
                    );

                    AuctionServer.broadcast(
                            chatJson,
                            null
                    );
                }

                // ───────── UNKNOWN ─────────

                default -> sendJson(Map.of(
                        "status", "ERROR",
                        "message", "Unsupported action"
                ));
            }

        } catch (Exception e) {

            sendJson(Map.of(
                    "status", "ERROR",
                    "message", "Invalid JSON format"
            ));
        }
    }

    /**
     * Gửi JSON về client
     */
    private void sendJson(Map<String, Object> data) {

        if (out != null) {

            out.println(
                    JsonUtil.toJson(data)
            );
        }
    }

    /**
     * Observer update
     */
    @Override
    public void update(Object message) {

        if (out != null) {
            out.println(message.toString());
        }
    }
}
