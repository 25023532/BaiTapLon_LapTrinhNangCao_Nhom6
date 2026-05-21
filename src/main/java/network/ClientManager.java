package network;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ClientManager — quản lý tập trung toàn bộ client đang kết nối.
 *
 * Thread-safe:
 *   - CopyOnWriteArrayList  cho vòng broadcast (đọc nhiều, ghi ít)
 *   - ConcurrentHashMap     cho tra cứu theo username / role
 */
public class ClientManager {

    /** Danh sách TẤT CẢ handler đang kết nối */
    private static final CopyOnWriteArrayList<ClientHandler> allClients
            = new CopyOnWriteArrayList<>();

    /** username → handler (dùng sendToUser) */
    private static final ConcurrentHashMap<String, ClientHandler> userMap
            = new ConcurrentHashMap<>();

    // =========================================================
    // ĐĂNG KÝ / HỦY
    // =========================================================

    /** Gọi khi client LOGIN thành công */
    public static void register(String username, ClientHandler handler) {
        userMap.put(username, handler);
        allClients.addIfAbsent(handler);
        System.out.println("[ClientManager] register: " + username
                + " | online=" + allClients.size());
    }

    /** Gọi khi client ngắt kết nối (trong finally của ClientHandler.run) */
    public static void unregister(ClientHandler handler) {
        userMap.values().remove(handler);
        allClients.remove(handler);
        System.out.println("[ClientManager] unregister: " + handler.getUsername()
                + " | online=" + allClients.size());
    }

    // =========================================================
    // GỬI TIN
    // =========================================================

    /** Broadcast tới TẤT CẢ client đang kết nối */
    public static void broadcastAll(String message) {
        for (ClientHandler c : allClients) {
            c.sendMessage(message);
        }
    }

    /**
     * Broadcast tới tất cả client có role khớp (không phân biệt hoa thường).
     * Ví dụ: broadcastToRole("ADMIN", json) → chỉ admin nhận.
     */
    public static void broadcastToRole(String role, String message) {
        for (ClientHandler c : allClients) {
            if (role.equalsIgnoreCase(c.getRole())) {
                c.sendMessage(message);
            }
        }
    }

    /** Gửi riêng cho một user theo username. Không làm gì nếu user offline. */
    public static void sendToUser(String username, String message) {
        ClientHandler handler = userMap.get(username);
        if (handler != null) {
            handler.sendMessage(message);
        } else {
            System.out.println("[ClientManager] sendToUser: '"
                    + username + "' không online, bỏ qua.");
        }
    }

    // =========================================================
    // THỐNG KÊ
    // =========================================================

    public static int getOnlineCount() {
        return allClients.size();
    }

    /** Kiểm tra user có đang online không */
    public static boolean isOnline(String username) {
        return userMap.containsKey(username);
    }
}
