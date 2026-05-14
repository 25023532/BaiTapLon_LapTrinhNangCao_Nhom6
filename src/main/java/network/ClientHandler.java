package network;

import org.example.util.JsonUtil;

import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 * ClientHandler — xử lý 1 client trên 1 luồng riêng.
 *
 * Vấn đề đồng bộ đã xử lý:
 *  1. sendMessage / sendJson dùng synchronized(writeLock) → tránh race condition
 *     khi nhiều luồng broadcast ghi vào cùng 1 PrintWriter đồng thời.
 *     (PrintWriter bản thân KHÔNG thread-safe dù auto-flush=true)
 *
 *  2. volatile out → đảm bảo mọi thread thấy giá trị mới nhất khi kiểm tra null.
 *     Không dùng volatile đơn thuần để serialize write — đó là việc của writeLock.
 *
 *  3. close() synchronized → tránh race giữa luồng đang broadcast và luồng đang
 *     đóng socket (double-close socket ném IOException).
 *
 *  4. isClosed flag (volatile) → sendMessage/sendJson kiểm tra trước khi ghi,
 *     tránh ghi vào socket đã đóng và spam log lỗi.
 *
 *  5. out được gán TRƯỚC khi gửi welcome message → không bao giờ null khi dùng.
 *
 *  6. Phân tách sendMessage (String thô) và sendJson (Map) để rõ ràng hơn.
 *     Cả hai đều đi qua cùng 1 writeLock.
 */
public class ClientHandler implements Runnable, Observer {

    private final Socket socket;

    // volatile: đảm bảo visibility giữa các thread (luồng run() gán, luồng khác đọc)
    private volatile PrintWriter out;
    private volatile boolean isClosed = false;

    // Lock riêng cho việc ghi — không dùng "this" để tránh deadlock với synchronized method
    private final Object writeLock = new Object();

    private String username = "unknown";

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // =========================================================
    // VÒNG LẶP ĐỌC CHÍNH
    // =========================================================

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            // Gán out TRƯỚC khi gửi bất kỳ tin nào
            // Không đặt trong try-with-resources vì close() cần gọi out.flush() thủ công
            out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true);

            sendJson(Map.of(
                    "status",  "OK",
                    "message", "Connected to Auction Server"
            ));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("[RECV from " + username + "] " + inputLine);
                handleMessage(inputLine);
            }

        } catch (IOException e) {
            if (!isClosed) {
                // Lỗi thật (không phải do close() chủ động)
                System.out.println("[DISCONNECT] Client [" + username
                        + "] mất kết nối: " + e.getMessage());
            }
        } finally {
            // removeObserver sẽ gọi broadcastOnlineCount() sau khi xóa khỏi list
            AuctionServer.removeObserver(this);
            close();
        }
    }

    // =========================================================
    // GỬI TIN — THREAD-SAFE
    // =========================================================

    /**
     * Gửi chuỗi String thô (JSON đã được serialize bên ngoài).
     * synchronized(writeLock) đảm bảo chỉ 1 thread ghi tại 1 thời điểm,
     * tránh các dòng JSON bị trộn lẫn nhau trên socket.
     */
    public void sendMessage(String message) {
        if (isClosed || out == null) return;

        synchronized (writeLock) {
            // Kiểm tra lại bên trong lock vì isClosed có thể thay đổi
            // giữa lần kiểm tra ngoài và lúc vào được lock
            if (isClosed || out == null) return;
            out.println(message);
        }
    }

    /**
     * Serialize Map thành JSON rồi gửi qua sendMessage.
     * Không cần synchronized riêng — sendMessage đã lo.
     */
    private void sendJson(Map<String, Object> data) {
        sendMessage(JsonUtil.toJson(data));
    }

    // =========================================================
    // XỬ LÝ MESSAGE TỪ CLIENT
    // =========================================================

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
                            "status",   "OK",
                            "message",  "Login success",
                            "username", username
                    ));
                    AuctionServer.broadcastAll(JsonUtil.toJson(Map.of(
                            "type",    "SYSTEM",
                            "message", username + " đã tham gia."
                    )));
                    AuctionServer.broadcastOnlineCount();
                }

                case "PLACE_BID" -> {
                    String sessionId = data.get("sessionId").toString();
                    String amount    = data.get("amount").toString();
                    AuctionServer.broadcastAll(JsonUtil.toJson(Map.of(
                            "type",      "NEW_BID",
                            "username",  username,
                            "sessionId", sessionId,
                            "amount",    amount
                    )));
                    sendJson(Map.of("status", "OK", "message", "Bid placed successfully"));
                }

                case "CHAT" -> {
                    String chatMessage = data.get("message").toString();
                    AuctionServer.broadcast(JsonUtil.toJson(Map.of(
                            "type",     "CHAT",
                            "username", username,
                            "message",  chatMessage
                    )), this);
                }

                case "GET_ONLINE_COUNT" -> {
                    sendJson(Map.of(
                            "type",  "ONLINE_COUNT",
                            "count", String.valueOf(AuctionServer.getOnlineCount())
                    ));
                }

                default -> sendJson(Map.of(
                        "status",  "ERROR",
                        "message", "Unsupported action: " + action
                ));
            }

        } catch (Exception e) {
            sendJson(Map.of(
                    "status",  "ERROR",
                    "message", "Invalid JSON: " + e.getMessage()
            ));
        }
    }

    // =========================================================
    // ĐÓNG KẾT NỐI — THREAD-SAFE
    // =========================================================

    /**
     * Đóng socket và đánh dấu handler là đã đóng.
     * synchronized đảm bảo socket.close() chỉ được gọi đúng 1 lần
     * dù nhiều luồng (shutdown hook, finally trong run()) cùng gọi close().
     */
    public synchronized void close() {
        if (isClosed) return;     // đã đóng rồi, không làm gì thêm
        isClosed = true;

        try {
            if (out != null) {
                out.flush();      // đẩy hết dữ liệu còn trong buffer trước khi đóng
            }
            socket.close();
        } catch (IOException e) {
            System.err.println("[CLOSE] Lỗi đóng socket của [" + username + "]: "
                    + e.getMessage());
        }
    }

    // =========================================================
    // OBSERVER INTERFACE
    // =========================================================

    /**
     * Nếu dự án dùng Observer pattern để push event,
     * update() cũng phải đi qua sendMessage để đảm bảo thread-safety.
     */
    @Override
    public void update(Object message) {
        sendMessage(message.toString());
    }

    // =========================================================
    // GETTER
    // =========================================================

    public String getUsername() {
        return username;
    }
}
