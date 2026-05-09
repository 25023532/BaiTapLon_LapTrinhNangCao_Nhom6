package com.nhom6.auctionsystem_nhom6;

import org.example.auction.AuctionSession;
import org.example.service.AuthService;
import org.example.user.Admin;
import org.example.user.Bidder;
import org.example.user.Seller;
import org.example.user.User;

import java.time.LocalDateTime;

public class AppContext {

    // =========================================
    // GLOBAL APP STATE
    // =========================================

    private static User currentUser;

    private static final AuthService authService = new AuthService();

    private static AuctionSession activeSession;

    // ✅ Avatar toàn ứng dụng
    private static javafx.scene.image.Image avatarImage;

    // =========================================
    // STATIC INIT
    // =========================================

    static {

        // Tạo dữ liệu mẫu
        seedData();

        // Kết nối server
        connectToServer();
    }

    // =========================================
    // SERVER CONNECTION
    // =========================================

    private static void connectToServer() {

        try {

            ServerConnection conn = ServerConnection.getInstance();

            boolean connected = conn.connect();

            if (connected) {

                System.out.println(
                        "AppContext: Kết nối server thành công."
                );

            } else {

                System.out.println(
                        "AppContext: Chạy offline (không kết nối được server)."
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "AppContext: Lỗi khi kết nối server."
            );

            e.printStackTrace();
        }
    }

    // =========================================
    // SAMPLE DATA
    // =========================================

    private static void seedData() {

        try {

            // ===== ADMIN =====
            if (!authService.isRegistered("admin")) {

                authService.register(
                        new Admin(
                                "A001",
                                "admin",
                                "admin123"
                        )
                );
            }

            // ===== SELLER =====
            if (!authService.isRegistered("sellerlong")) {

                authService.register(
                        new Seller(
                                "S001",
                                "sellerlong",
                                "seller123"
                        )
                );
            }

            // ===== BIDDER =====
            if (!authService.isRegistered("bidder07")) {

                authService.register(
                        new Bidder(
                                "B001",
                                "bidder07",
                                "bidder123"
                        )
                );
            }

            if (!authService.isRegistered("bidder03")) {

                authService.register(
                        new Bidder(
                                "B002",
                                "bidder03",
                                "bidder123"
                        )
                );
            }

            if (!authService.isRegistered("bidder01")) {

                authService.register(
                        new Bidder(
                                "B003",
                                "bidder01",
                                "bidder123"
                        )
                );
            }

            // =====================================
            // AUCTION SESSION DEMO
            // =====================================

            activeSession = new AuctionSession(
                    "SESSION-001",
                    "MacBook Pro M3 – 18GB RAM, 512GB SSD",
                    22_000_000,
                    500_000,
                    LocalDateTime.now().plusHours(2)
            );

            activeSession.start();

            System.out.println(
                    "AppContext: Tạo dữ liệu mẫu thành công."
            );

        } catch (Exception e) {

            System.out.println(
                    "AppContext: Lỗi khi tạo dữ liệu mẫu."
            );

            e.printStackTrace();
        }
    }

    // =========================================
    // CURRENT USER
    // =========================================

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    // =========================================
    // AUTH SERVICE
    // =========================================

    public static AuthService getAuthService() {
        return authService;
    }

    // =========================================
    // AUCTION SESSION
    // =========================================

    public static AuctionSession getActiveSession() {
        return activeSession;
    }

    public static void setActiveSession(
            AuctionSession session
    ) {
        activeSession = session;
    }

    // =========================================
    // AVATAR IMAGE
    // =========================================

    public static javafx.scene.image.Image getAvatarImage() {
        return avatarImage;
    }

    public static void setAvatarImage(
            javafx.scene.image.Image image
    ) {
        avatarImage = image;
    }

    // =========================================
    // LOGOUT
    // =========================================

    public static void logout() {

        currentUser = null;

        avatarImage = null;

        System.out.println(
                "AppContext: Đã đăng xuất."
        );
    }
}
