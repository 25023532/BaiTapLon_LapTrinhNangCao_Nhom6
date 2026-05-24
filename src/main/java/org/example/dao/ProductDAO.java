package org.example.dao;

import org.example.auction.AuctionSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProductDAO — Data Access Object cho sản phẩm đấu giá.
 *
 * <p>Lưu trữ in-memory danh sách các {@link AuctionSession} đại diện
 * cho sản phẩm đang được đấu giá. Trong môi trường production, lớp
 * này sẽ được thay thế bằng implementation kết nối database.</p>
 *
 * <p>Thread-safe nhờ sử dụng {@link ConcurrentHashMap}.</p>
 *
 * <p>Fix: getProductName() → getItemName()
 * (AuctionSession không có getProductName()).</p>
 */
public class ProductDAO {

    /** Kho lưu trữ: sessionId → AuctionSession */
    private final Map<String, AuctionSession> store = new ConcurrentHashMap<>();

    // ── Create / Update ──────────────────────────────────────────────────

    /**
     * Lưu một phiên đấu giá mới.
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
     * Cập nhật phiên đấu giá đã tồn tại.
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
     * @return {@link Optional} chứa phiên nếu tìm thấy, rỗng nếu không có
     */
    public Optional<AuctionSession> findById(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId không được null/rỗng");
        }
        return Optional.ofNullable(store.get(sessionId));
    }

    /**
     * Trả về tất cả phiên đấu giá (bản sao chỉ đọc).
     */
    public List<AuctionSession> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    /**
     * Tìm các phiên theo tên sản phẩm (không phân biệt hoa thường).
     *
     * FIX: dùng getItemName() thay vì getProductName()
     * (AuctionSession không có getProductName()).
     *
     * @param keyword từ khóa cần tìm trong tên sản phẩm
     * @return danh sách phiên có tên sản phẩm chứa keyword
     */
    public List<AuctionSession> findByProductName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("keyword không được null/rỗng");
        }
        String lower = keyword.toLowerCase();
        List<AuctionSession> result = new ArrayList<>();
        for (AuctionSession s : store.values()) {
            if (s.getItemName().toLowerCase().contains(lower)) { // FIX: getItemName()
                result.add(s);
            }
        }
        return Collections.unmodifiableList(result);
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
