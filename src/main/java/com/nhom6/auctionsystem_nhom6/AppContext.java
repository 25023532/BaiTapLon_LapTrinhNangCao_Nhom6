package com.nhom6.auctionsystem_nhom6;

import javafx.scene.image.Image;
import org.example.auction.AuctionSession;
import org.example.service.AuthService;
import org.example.user.Admin;
import org.example.user.Bidder;
import org.example.user.Seller;
import org.example.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppContext {

    // =========================================================
    // GLOBAL APP STATE
    // =========================================================

    private static User currentUser;

    private static final AuthService authService =
            new AuthService();

    private static AuctionSession activeSession;

    // Avatar toàn ứng dụng
    private static Image avatarImage;

    // =========================================================
    // HISTORY STORAGE
    // =========================================================

    // username -> history list
    private static final Map<String, List<HistoryRecord>>
            historyMap = new HashMap<>();

    // =========================================================
    // PRODUCT STORAGE
    // =========================================================

    // username -> product list
    private static final Map<String, List<ProductRecord>>
            productMap = new HashMap<>();

    // =========================================================
    // STATIC INIT
    // =========================================================

    static {

        // Tạo dữ liệu mẫu
        seedData();

        // Kết nối server
        connectToServer();
    }

    // =========================================================
    // SERVER CONNECTION
    // =========================================================

    private static void connectToServer() {

        try {

            ServerConnection conn =
                    ServerConnection.getInstance();

            boolean connected = conn.connect();

            if (connected) {

                System.out.println(
                        "AppContext: Kết nối server thành công."
                );

            } else {

                System.out.println(
                        "AppContext: Chạy offline."
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "AppContext: Lỗi kết nối server."
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // ✅ FIX: Tách riêng method tạo demo session để tái sử dụng
    // =========================================================

    private static AuctionSession createDemoSession() {

        AuctionSession session = new AuctionSession(
                "SESSION-001",
                "MacBook Pro M3 – 18GB RAM, 512GB SSD",
                22_000_000,
                500_000,
                LocalDateTime.now().plusHours(2)
        );

        session.start();

        return session;
    }

    // =========================================================
    // SAMPLE DATA
    // =========================================================

    private static void seedData() {

        try {

            // ================= ADMIN =================

            if (!authService.isRegistered("admin")) {

                authService.register(
                        new Admin(
                                "A001",
                                "admin",
                                "admin123"
                        )
                );
            }

            // ================= SELLER =================

            if (!authService.isRegistered("sellerlong")) {

                authService.register(
                        new Seller(
                                "S001",
                                "sellerlong",
                                "seller123"
                        )
                );
            }

            // ================= BIDDER =================

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

            // =================================================
            // ✅ FIX: Dùng createDemoSession() thay vì tạo inline
            // =================================================

            activeSession = createDemoSession();

            // =================================================
            // DEMO HISTORY
            // =================================================

            addHistory(
                    "bidder07",
                    new HistoryRecord(
                            "HD001",
                            "iPhone 15 Pro Max",
                            28000000,
                            "SellerLong",
                            "THÀNH CÔNG",
                            true,
                            LocalDateTime.now().minusDays(1)
                    )
            );

            addHistory(
                    "sellerlong",
                    new HistoryRecord(
                            "HD002",
                            "MacBook Air M2",
                            22000000,
                            "Bidder07",
                            "CHỜ XỬ LÝ",
                            true,
                            LocalDateTime.now().minusHours(5)
                    )
            );

            // =================================================
            // DEMO PRODUCT
            // =================================================

            addProduct(
                    "sellerlong",
                    new ProductRecord(
                            "PR001",
                            "MacBook Pro M3",
                            "Laptop",
                            22000000,
                            26500000,
                            12,
                            "ĐANG ĐẤU GIÁ",
                            LocalDateTime.now().plusHours(2),
                            "bidder07"
                    )
            );

            addProduct(
                    "sellerlong",
                    new ProductRecord(
                            "PR002",
                            "iPhone 15 Pro",
                            "Điện thoại",
                            18000000,
                            21000000,
                            7,
                            "ĐÃ BÁN",
                            LocalDateTime.now().minusDays(1),
                            "bidder03"
                    )
            );

            System.out.println(
                    "AppContext: Seed data thành công."
            );

        } catch (Exception e) {

            System.out.println(
                    "AppContext: Lỗi seed data."
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // CURRENT USER
    // =========================================================

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    // =========================================================
    // AUTH SERVICE
    // =========================================================

    public static AuthService getAuthService() {
        return authService;
    }

    // =========================================================
    // AUCTION SESSION
    // =========================================================

    public static AuctionSession getActiveSession() {
        return activeSession;
    }

    public static void setActiveSession(
            AuctionSession session
    ) {
        activeSession = session;
    }

    // =========================================================
    // AVATAR
    // =========================================================

    public static Image getAvatarImage() {
        return avatarImage;
    }

    public static void setAvatarImage(Image image) {
        avatarImage = image;
    }

    // =========================================================
    // ✅ FIX: logout() tạo lại session thay vì set null
    // Nguyên nhân lỗi gốc: activeSession = null sau logout
    // → login lại → MainController.initialize() → session null → alert lỗi
    // =========================================================

    public static void logout() {

        currentUser = null;

        // ✅ Tạo lại session mới thay vì set null
        activeSession = createDemoSession();

        avatarImage = null;

        System.out.println(
                "AppContext: Đã đăng xuất."
        );
    }

    // =========================================================
    // HISTORY RECORD
    // =========================================================

    public record HistoryRecord(

            String id,
            String itemName,
            double amount,
            String counterparty,
            String status,
            boolean wonBid,
            LocalDateTime time

    ) {}

    public static List<HistoryRecord>
    getHistory(String username) {

        return historyMap.computeIfAbsent(
                username,
                k -> new ArrayList<>()
        );
    }

    public static void addHistory(
            String username,
            HistoryRecord record
    ) {

        getHistory(username).add(record);
    }

    // =========================================================
    // PRODUCT RECORD
    // =========================================================

    public record ProductRecord(

            String id,
            String name,
            String category,
            double startPrice,
            double currentPrice,
            int bidCount,
            String status,
            LocalDateTime endTime,
            String topBidder

    ) {

        public ProductRecord withUpdated(

                double newPrice,
                int newBidCount,
                String newStatus,
                String newTopBidder

        ) {

            return new ProductRecord(
                    id,
                    name,
                    category,
                    startPrice,
                    newPrice,
                    newBidCount,
                    newStatus,
                    endTime,
                    newTopBidder
            );
        }
    }

    // =========================================================
    // PRODUCT METHODS
    // =========================================================

    public static List<ProductRecord>
    getProducts(String username) {

        return productMap.computeIfAbsent(
                username,
                k -> new ArrayList<>()
        );
    }

    public static void addProduct(
            String username,
            ProductRecord product
    ) {

        getProducts(username).add(product);
    }

    public static void removeProduct(
            String username,
            String productId
    ) {

        getProducts(username)
                .removeIf(p -> p.id().equals(productId));
    }

    public static void updateProduct(
            String username,
            ProductRecord updated
    ) {

        List<ProductRecord> list =
                getProducts(username);

        for (int i = 0; i < list.size(); i++) {

            if (list.get(i).id().equals(updated.id())) {

                list.set(i, updated);

                return;
            }
        }
    }
}
