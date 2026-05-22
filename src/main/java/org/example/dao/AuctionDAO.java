package org.example.dao;

import org.example.auction.AuctionSession;
import org.example.auction.Bid;
import com.nhom6.auctionsystem_nhom6.AppContext;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AuctionDAO — truy cập DB cho AuctionSession.
 *
 * Mỗi write-operation đều dùng transaction + rollback.
 * Inject Connection từ ngoài để dễ test và quản lý pool.
 *
 * Bảng cần có (ví dụ MySQL/SQLite):
 *   CREATE TABLE auction_sessions (
 *       session_id   VARCHAR(64)  PRIMARY KEY,
 *       item_name    VARCHAR(255) NOT NULL,
 *       start_price  DOUBLE       NOT NULL,
 *       current_price DOUBLE      NOT NULL,
 *       min_bid_step DOUBLE       NOT NULL,
 *       seller_name  VARCHAR(128) NOT NULL,
 *       category     VARCHAR(64),
 *       start_time   DATETIME     NOT NULL,
 *       end_time     DATETIME     NOT NULL,
 *       status       VARCHAR(32)  NOT NULL DEFAULT 'PENDING'
 *   );
 *
 *   CREATE TABLE bids (
 *       bid_id       VARCHAR(64) PRIMARY KEY,
 *       session_id   VARCHAR(64) NOT NULL REFERENCES auction_sessions(session_id),
 *       bidder_id    VARCHAR(128) NOT NULL,
 *       amount       DOUBLE       NOT NULL,
 *       bid_time     DATETIME     NOT NULL
 *   );
 */
public class AuctionDAO {

    private final Connection conn;

    public AuctionDAO(Connection conn) {
        this.conn = conn;
    }

    // =========================================================
    // READ
    // =========================================================

    /**
     * Lấy AuctionSession theo sessionId.
     * @return AuctionSession hoặc null nếu không tồn tại.
     */
    public AuctionSession findById(String sessionId) throws SQLException {
        String sql = "SELECT * FROM auction_sessions WHERE session_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            return mapRow(rs);
        }
    }

    /** Lấy tất cả phiên đang RUNNING */
    public List<AuctionSession> findActiveSessions() throws SQLException {
        String sql = "SELECT * FROM auction_sessions WHERE status = 'RUNNING'";
        List<AuctionSession> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // =========================================================
    // WRITE (có transaction + rollback)
    // =========================================================

    /**
     * Lưu phiên mới vào DB.
     * @throws SQLException nếu lỗi — tự động rollback.
     */
    public void save(AuctionSession session) throws SQLException {
        conn.setAutoCommit(false);
        try {
            String sql = """
                INSERT INTO auction_sessions
                  (session_id, item_name, start_price, current_price,
                   min_bid_step, seller_name, category,
                   start_time, end_time, status)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1,    session.getSessionId());
                ps.setString(2,    session.getItemName());
                ps.setDouble(3,    session.getStartingPrice());
                ps.setDouble(4,    session.getCurrentPrice());
                ps.setDouble(5,    session.getMinBidStep());
                ps.setString(6,    AppContext.getSessionSeller(session.getSessionId()));
                ps.setString(7,   AppContext.getAllProducts().stream()
                        .filter(p -> p.id().equals(session.getSessionId()))
                        .map(p -> p.category())
                        .findFirst().orElse("Chung"));
                ps.setTimestamp(8, Timestamp.valueOf(
                        AppContext.getAllProducts().stream()
                                .filter(p -> p.id().equals(session.getSessionId()))
                                .map(p -> p.startTime())
                                .findFirst()
                                .orElse(session.getEndTime().minusHours(1))
                ));
                ps.setTimestamp(9, Timestamp.valueOf(session.getEndTime()));
                ps.setString(10,   session.getStatus().name());
                ps.executeUpdate();
            }
            conn.commit();
            System.out.println("[AuctionDAO] save OK: " + session.getSessionId());
        } catch (SQLException e) {
            conn.rollback();   // ← rollback khi lỗi
            System.err.println("[AuctionDAO] save ROLLBACK: " + e.getMessage());
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Cập nhật current_price và status sau khi có bid mới.
     * Dùng optimistic lock: chỉ update nếu current_price chưa thay đổi.
     *
     * @return true nếu update thành công, false nếu bị race condition.
     */
    public boolean updatePriceOptimistic(String sessionId,
                                         double newPrice,
                                         double expectedOldPrice) throws SQLException {
        conn.setAutoCommit(false);
        try {
            String sql = """
                UPDATE auction_sessions
                SET current_price = ?
                WHERE session_id = ? AND current_price = ?
                """;
            int rows;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, newPrice);
                ps.setString(2, sessionId);
                ps.setDouble(3, expectedOldPrice);
                rows = ps.executeUpdate();
            }
            if (rows == 0) {
                conn.rollback();
                return false; // race condition — giá đã bị thay đổi bởi bid khác
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Lưu một lượt bid vào bảng bids (trong cùng transaction với update giá).
     */
    public void saveBid(Bid bid, String sessionId) throws SQLException {
        conn.setAutoCommit(false);
        try {
            String sql = """
                INSERT INTO bids (bid_id, session_id, bidder_id, amount, bid_time)
                VALUES (?,?,?,?,?)
                """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1,    bid.getBidId());
                ps.setString(2,    sessionId);
                ps.setString(3,    bid.getBidderId());
                ps.setDouble(4,    bid.getAmount());
                ps.setTimestamp(5, Timestamp.valueOf(bid.getTimestamp()));
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /** Cập nhật status của phiên (PENDING / RUNNING / ENDED) */
    public void updateStatus(String sessionId, String status) throws SQLException {
        conn.setAutoCommit(false);
        try {
            String sql = "UPDATE auction_sessions SET status = ? WHERE session_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setString(2, sessionId);
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // =========================================================
    // HELPER
    // =========================================================

    private AuctionSession mapRow(ResultSet rs) throws SQLException {
        return new AuctionSession(
                rs.getString("session_id"),
                rs.getString("item_name"),
                rs.getDouble("start_price"),
                rs.getDouble("min_bid_step"),
                rs.getTimestamp("end_time").toLocalDateTime()
        );
    }
}
