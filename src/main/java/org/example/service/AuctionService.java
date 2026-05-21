package org.example.service;

import network.ClientManager;
import org.example.auction.AuctionSession;
import org.example.auction.Bid;
import org.example.dao.AuctionDAO;
import org.example.exception.AuctionClosedException;
import org.example.exception.InvalidBidException;
import org.example.util.JsonUtil;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AuctionService — xử lý nghiệp vụ đấu giá, thread-safe.
 *
 * Giải quyết:
 *   ✅ Race condition: ReentrantLock riêng từng phiên
 *   ✅ Lost update: kiểm tra giá trước khi ghi (optimistic + lock)
 *   ✅ Rollback: AuctionDAO dùng transaction
 *   ✅ Realtime notify: ClientManager.broadcastAll sau khi commit
 */
public class AuctionService {

    // Singleton
    private static final AuctionService INSTANCE = new AuctionService();
    public static AuctionService getInstance() { return INSTANCE; }
    private AuctionService() {}

    /** Lock riêng cho từng phiên — tạo tự động khi cần */
    private final ConcurrentHashMap<String, ReentrantLock> lockMap
            = new ConcurrentHashMap<>();

    /** Cache phiên đang active (sessionId → AuctionSession) */
    private final ConcurrentHashMap<String, AuctionSession> sessionCache
            = new ConcurrentHashMap<>();

    // =========================================================
    // QUẢN LÝ PHIÊN
    // =========================================================

    /** Đăng ký phiên mới (gọi khi SESSION_START) */
    public void registerSession(AuctionSession session) {
        sessionCache.put(session.getSessionId(), session);
        lockMap.computeIfAbsent(session.getSessionId(), id -> new ReentrantLock());
        System.out.println("[AuctionService] Session registered: "
                + session.getSessionId());
    }

    /** Xóa phiên (gọi khi SESSION_END) */
    public void removeSession(String sessionId) {
        sessionCache.remove(sessionId);
        lockMap.remove(sessionId);
    }

    public AuctionSession getSession(String sessionId) {
        return sessionCache.get(sessionId);
    }

    // =========================================================
    // ĐẶT GIÁ — THREAD-SAFE
    // =========================================================

    /**
     * Đặt giá an toàn cho một phiên.
     *
     * Luồng:
     *   1. Lấy lock của phiên (chỉ 1 thread/phiên chạy cùng lúc)
     *   2. Đọc trạng thái hiện tại
     *   3. Kiểm tra hợp lệ
     *   4. Ghi vào DB (có rollback nếu lỗi)
     *   5. Cập nhật in-memory
     *   6. Broadcast realtime tới tất cả client
     *   7. Mở khóa trong finally
     *
     * @param sessionId  ID phiên
     * @param bidderId   username người đặt
     * @param amount     số tiền đặt
     * @param dao        AuctionDAO (được inject từ ClientHandler)
     * @return BidResult chứa kết quả
     */
    public BidResult placeBid(String sessionId,
                               String bidderId,
                               double amount,
                               AuctionDAO dao) {

        // Lấy hoặc tạo lock cho phiên này
        ReentrantLock lock = lockMap.computeIfAbsent(
                sessionId, id -> new ReentrantLock());

        lock.lock();
        try {
            // 1. Kiểm tra phiên tồn tại
            AuctionSession session = sessionCache.get(sessionId);
            if (session == null) {
                return BidResult.fail("Phiên đấu giá không tồn tại: " + sessionId);
            }

            // 2. Kiểm tra phiên còn active
            if (!session.isActive()) {
                return BidResult.fail("Phiên đấu giá đã kết thúc.");
            }

            // 3. Kiểm tra số tiền hợp lệ
            double minRequired = session.getCurrentPrice() + session.getMinBidStep();
            if (amount < minRequired) {
                return BidResult.fail(String.format(
                        "Giá phải ≥ %.0f (hiện tại %.0f + bước %.0f)",
                        minRequired, session.getCurrentPrice(),
                        session.getMinBidStep()));
            }

            // 4. Tạo Bid object
            Bid bid = new Bid(
                    UUID.randomUUID().toString(),
                    bidderId,
                    amount
            );

            // 5. Ghi vào DB (có transaction + rollback)
            if (dao != null) {
                try {
                    // Optimistic check: chỉ update nếu giá chưa thay đổi
                    boolean updated = dao.updatePriceOptimistic(
                            sessionId, amount, session.getCurrentPrice());
                    if (!updated) {
                        return BidResult.fail(
                                "Giá vừa thay đổi, vui lòng đặt lại.");
                    }
                    dao.saveBid(bid, sessionId);
                } catch (SQLException e) {
                    System.err.println("[AuctionService] DB error: "
                            + e.getMessage());
                    return BidResult.fail("Lỗi hệ thống, vui lòng thử lại.");
                }
            }

            // 6. Cập nhật in-memory session
            try {
                session.placeBid(bid);
            } catch (Exception e) {
                return BidResult.fail(e.getMessage());
            }

            // 7. Broadcast NEW_BID tới TẤT CẢ client (realtime)
            String notification = JsonUtil.toJson(Map.of(
                    "type",      "NEW_BID",
                    "sessionId", sessionId,
                    "username",  bidderId,
                    "amount",    String.valueOf(amount)
            ));
            ClientManager.broadcastAll(notification);

            System.out.printf("[AuctionService] Bid OK: session=%s bidder=%s amount=%.0f%n",
                    sessionId, bidderId, amount);
            return BidResult.success(session, bid);

        } finally {
            lock.unlock(); // luôn mở khóa dù có exception
        }
    }

    // =========================================================
    // RESULT WRAPPER
    // =========================================================

    public static class BidResult {
        public final boolean success;
        public final String  message;
        public final AuctionSession session;
        public final Bid bid;

        private BidResult(boolean success, String message,
                          AuctionSession session, Bid bid) {
            this.success = success;
            this.message = message;
            this.session = session;
            this.bid     = bid;
        }

        public static BidResult success(AuctionSession s, Bid b) {
            return new BidResult(true, "Đặt giá thành công", s, b);
        }

        public static BidResult fail(String reason) {
            return new BidResult(false, reason, null, null);
        }
    }
}
