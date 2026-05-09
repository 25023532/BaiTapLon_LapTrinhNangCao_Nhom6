package com.nhom6.auctionsystem_nhom6;

import org.example.auction.AuctionSession;
import org.example.service.AuthService;
import org.example.user.*;

import java.time.LocalDateTime;

/**
 * AppContext – giữ trạng thái toàn ứng dụng (user đang đăng nhập, service, dữ liệu mẫu).
 */
public class AppContext {

    private static User currentUser;
    private static final AuthService authService = new AuthService();
    private static AuctionSession activeSession;

    static {
        // Tạo dữ liệu mẫu để chạy thử
        seedData();
        // Kết nối đến AuctionServer
        connectToServer();
    }

    private static void connectToServer() {
        ServerConnection conn = ServerConnection.getInstance();
        boolean ok = conn.connect();
        if (ok) {
            System.out.println("AppContext: Kết nối server thành công.");
        } else {
            System.out.println("AppContext: Chạy offline (không kết nối được server).");
        }
    }

    private static void seedData() {
        // Chỉ tạo tài khoản mẫu nếu chưa có trong file
        if (!authService.isRegistered("admin"))
            authService.register(new Admin("A001", "admin", "admin123"));
        if (!authService.isRegistered("sellerlong"))
            authService.register(new Seller("S001", "sellerlong", "seller123"));
        if (!authService.isRegistered("bidder07"))
            authService.register(new Bidder("B001", "bidder07", "bidder123"));
        if (!authService.isRegistered("bidder03"))
            authService.register(new Bidder("B002", "bidder03", "bidder123"));
        if (!authService.isRegistered("bidder01"))
            authService.register(new Bidder("B003", "bidder01", "bidder123"));

        // Phiên đấu giá mẫu
        activeSession = new AuctionSession(
                "SESSION-001",
                "MacBook Pro M3 – 18GB RAM, 512GB SSD",
                22_000_000,
                500_000,
                LocalDateTime.now().plusHours(2)
        );
        activeSession.start();
    }

    public static User getCurrentUser()              { return currentUser; }
    public static void setCurrentUser(User user)     { currentUser = user; }
    public static AuthService getAuthService()       { return authService; }
    public static AuctionSession getActiveSession()  { return activeSession; }
    public static void setActiveSession(AuctionSession s) { activeSession = s; }

    public static void logout() { currentUser = null; }
}
