package com.nhom6.auctionsystem_nhom6;

import javafx.scene.image.Image;

import org.example.auction.AuctionSession;
import org.example.service.AuthService;
import org.example.user.Admin;
import org.example.user.Bidder;
import org.example.user.Seller;
import org.example.user.User;

/**
 * AppContext – giữ trạng thái toàn ứng dụng
 * (user đang đăng nhập, service, avatar, session...)
 */
public class AppContext {

    // ===== USER ĐANG ĐĂNG NHẬP =====
    private static User currentUser;

    // ===== AUTH SERVICE =====
    private static final AuthService authService = new AuthService();

    // ===== PHIÊN ĐẤU GIÁ HIỆN TẠI =====
    private static AuctionSession activeSession;

    // ===== AVATAR NGƯỜI DÙNG =====
    private static Image avatarImage;

    static {
        // Tạo dữ liệu mẫu
        seedData();

        // Kết nối server
        connectToServer();
    }

    // ======================================================
    // SERVER CONNECTION
    // ======================================================

    private static void connectToServer() {

        ServerConnection conn = ServerConnection.getInstance();

        boolean ok = conn.connect();

        if (ok) {
            System.out.println(
                    "AppContext: Kết nối server thành công."
            );
        } else {
            System.out.println(
                    "AppContext: Chạy offline."
            );
        }
    }

    // ======================================================
    // SEED DATA
    // ======================================================

    private static void seedData() {

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

        // ===== KHÔNG TẠO PHIÊN ĐẤU GIÁ MẪU =====
        activeSession = null;
    }

    // ======================================================
    // GETTER / SETTER
    // ======================================================

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static AuthService getAuthService() {
        return authService;
    }

    public static AuctionSession getActiveSession() {
        return activeSession;
    }

    public static void setActiveSession(
            AuctionSession session
    ) {
        activeSession = session;
    }

    // ===== AVATAR =====

    public static Image getAvatarImage() {
        return avatarImage;
    }

    public static void setAvatarImage(Image img) {
        avatarImage = img;
    }

    // ======================================================
    // LOGOUT
    // ======================================================

    public static void logout() {

        currentUser = null;

        avatarImage = null;
    }
}
