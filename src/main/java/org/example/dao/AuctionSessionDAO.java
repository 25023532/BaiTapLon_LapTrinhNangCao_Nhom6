package org.example.dao;

import org.example.auction.AuctionSession;
import org.example.auction.AuctionStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AuctionSessionDAO — Data Access Object cho phiên đấu giá.
 *
 * <p>Quản lý toàn bộ vòng đời của {@link AuctionSession}: tạo mới,
 * cập nhật trạng thái, truy vấn theo nhiều tiêu chí khác nhau.</p>
 *
 * <p>Thread-safe nhờ {@link ConcurrentHashMap}.</p>
 *
 * <p>Fix:
 *   - getProductName()      → getItemName()         (đúng tên method trong AuctionSession)
 *   - getLeadingBidderId()  → đọc từ getBidHistory() (AuctionSession không có getter này)
 * </p>
 */
public class AuctionSessionDAO {

    /** Kho lưu trữ: sessionId → AuctionSession */
    private final Map<String, AuctionSession> store = new ConcurrentHashMap<>();

    // ── Create / Update ──────────────────────────────────────────────────

    /**
     * Lưu phiên đấu giá mới vào kho.
     *
     * @param session phiên cần lưu
     * @throws IllegalArgumentException nếu session null hoặc ID đã tồn tại
     */
    public void save(AuctionSession session) {
        if (session == null) {
            throw new IllegalArgumentException("AuctionSession không được null");
        }
        if (store.containsKey(session.getSessionId())) {
            throw new IllegalArgumentException(
                    "Session ID đã tồn tại: " + session.getSessionId());
        }
        store.put(session.getSessionId(), session);
    }

    /**
     * Cập nhật trạng thái / giá của phiên đã tồn tại.
     *
     * @param session phiên cần cập nhật
     * @throws IllegalArgumentException nếu session null hoặc ID không tồn tại
     */
    public void update(AuctionSession session) {
        if (session == null) {
            throw new IllegalArgumentException("AuctionSession không được null");
        }
        if (!store.containsKey(session.getSessionId())) {
            throw new IllegalArgumentException(
                    "Session ID không tồn tại: " + session.getSessionId());
        }
        store.put(session.getSessionId(), session);
    }

    // ── Read ─────────────────────────────────────────────────────────────

    /**
     * Tìm phiên theo ID.
     *
     * @param sessionId ID cần tìm
     * @return {@link Optional} chứa phiên nếu tìm thấy
     * @throws IllegalArgumentException nếu sessionId null/rỗng
     */
    public Optional<AuctionSession> findById(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId không được null/rỗng");
        }
        return Optional.ofNullable(store.get(sessionId));
    }

    /**
     * Trả về toàn bộ phiên đấu giá (bản sao chỉ đọc).
     */
    public List<AuctionSession> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    /**
     * Tìm phiên theo trạng thái.
     *
     * @param status trạng thái cần lọc
     * @return danh sách phiên có trạng thái tương ứng
     */
    public List<AuctionSession> findByStatus(AuctionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status không được null");
        }
        return store.values().stream()
                .filter(s -> s.getStatus() == status)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Tìm phiên đang hoạt động (RUNNING).
     */
    public List<AuctionSession> findActive() {
        return findByStatus(AuctionStatus.RUNNING);
    }

    /**
     * Tìm phiên theo tên sản phẩm (không phân biệt hoa thường).
     *
     * FIX: dùng getItemName() thay vì getProductName()
     * (AuctionSession không có getProductName()).
     *
     * @param keyword từ khóa tìm kiếm
     * @return danh sách phiên khớp
     */
    public List<AuctionSession> findByProductName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("keyword không được null/rỗng");
        }
        String lower = keyword.toLowerCase();
        return store.values().stream()
                .filter(s -> s.getItemName().toLowerCase().contains(lower)) // FIX: getItemName()
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Tìm phiên có bidder là người dẫn đầu (bid cao nhất hiện tại).
     *
     * FIX: AuctionSession không có getLeadingBidderId().
     * Thay bằng đọc bid cuối cùng từ getBidHistory() —
     * bid cuối = bid cao nhất = người đang dẫn đầu.
     * Nếu chưa có bid nào → phiên không có người dẫn đầu, bỏ qua.
     *
     * @param bidderId ID bidder cần tìm
     * @return danh sách phiên mà bidder đang dẫn đầu
     */
    public List<AuctionSession> findByLeadingBidder(String bidderId) {
        if (bidderId == null || bidderId.isBlank()) {
            throw new IllegalArgumentException("bidderId không được null/rỗng");
        }
        return store.values().stream()
                .filter(s -> {
                    var history = s.getBidHistory();
                    if (history == null || history.isEmpty()) return false;
                    // Bid cuối cùng = người đang dẫn đầu
                    String leaderId = history.get(history.size() - 1).getBidderId();
                    return bidderId.equals(leaderId);
                })
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Kiểm tra sự tồn tại của phiên theo ID.
     */
    public boolean existsById(String sessionId) {
        return sessionId != null && store.containsKey(sessionId);
    }

    /**
     * Tổng số phiên đang lưu.
     */
    public int count() {
        return store.size();
    }

    /**
     * Đếm số phiên theo trạng thái.
     */
    public long countByStatus(AuctionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status không được null");
        }
        return store.values().stream()
                .filter(s -> s.getStatus() == status)
                .count();
    }

    // ── Delete ───────────────────────────────────────────────────────────

    /**
     * Xóa phiên theo ID.
     *
     * @param sessionId ID cần xóa
     * @throws IllegalArgumentException nếu ID không tồn tại
     */
    public void deleteById(String sessionId) {
        if (!store.containsKey(sessionId)) {
            throw new IllegalArgumentException(
                    "Không tìm thấy session để xóa: " + sessionId);
        }
        store.remove(sessionId);
    }

    /**
     * Xóa toàn bộ dữ liệu (dùng trong unit test / reset).
     */
    public void clear() {
        store.clear();
    }
}
