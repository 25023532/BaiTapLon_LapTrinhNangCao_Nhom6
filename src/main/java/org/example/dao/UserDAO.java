package org.example.dao;

import org.example.user.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UserDAO — Data Access Object cho người dùng.
 *
 * <p>Lưu trữ in-memory các {@link User}. Trong môi trường production,
 * lớp này sẽ được thay thế bằng implementation kết nối database.</p>
 *
 * <p>Thread-safe nhờ {@link ConcurrentHashMap}.</p>
 */
public class UserDAO {

    /** Kho lưu trữ: username → User */
    private final Map<String, User> store = new ConcurrentHashMap<>();

    // ── Create / Update ──────────────────────────────────────────────────

    /**
     * Lưu một user mới.
     *
     * @param user user cần lưu
     * @throws IllegalArgumentException nếu user null hoặc username đã tồn tại
     */
    public void save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User không được null");
        }
        if (store.containsKey(user.getUsername())) {
            throw new IllegalArgumentException(
                    "Username đã tồn tại: " + user.getUsername());
        }
        store.put(user.getUsername(), user);
    }

    /**
     * Cập nhật thông tin user.
     *
     * @param user user cần cập nhật
     * @throws IllegalArgumentException nếu user null hoặc username không tồn tại
     */
    public void update(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User không được null");
        }
        if (!store.containsKey(user.getUsername())) {
            throw new IllegalArgumentException(
                    "Username không tồn tại: " + user.getUsername());
        }
        store.put(user.getUsername(), user);
    }

    // ── Read ─────────────────────────────────────────────────────────────

    /**
     * Tìm user theo username.
     *
     * @param username username cần tìm
     * @return {@link Optional} chứa User nếu tìm thấy
     * @throws IllegalArgumentException nếu username null hoặc rỗng
     */
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username không được null/rỗng");
        }
        return Optional.ofNullable(store.get(username));
    }

    /**
     * Trả về toàn bộ danh sách user (bản sao chỉ đọc).
     */
    public List<User> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    /**
     * Tìm user theo loại (Admin / Bidder).
     *
     * @param type class type cần lọc (vd: {@code Admin.class})
     * @param <T>  kiểu user
     * @return danh sách user thuộc loại đó
     */
    @SuppressWarnings("unchecked")
    public <T extends User> List<T> findByType(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type không được null");
        }
        List<T> result = new ArrayList<>();
        for (User u : store.values()) {
            if (type.isInstance(u)) {
                result.add((T) u);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Kiểm tra username đã tồn tại chưa.
     */
    public boolean existsByUsername(String username) {
        return username != null && store.containsKey(username);
    }

    /**
     * Tổng số user đang lưu.
     */
    public int count() {
        return store.size();
    }

    // ── Delete ───────────────────────────────────────────────────────────

    /**
     * Xóa user theo username.
     *
     * @param username username cần xóa
     * @throws IllegalArgumentException nếu username không tồn tại
     */
    public void deleteByUsername(String username) {
        if (!store.containsKey(username)) {
            throw new IllegalArgumentException(
                    "Không tìm thấy user để xóa: " + username);
        }
        store.remove(username);
    }

    /**
     * Xóa toàn bộ dữ liệu (dùng trong unit test / reset).
     */
    public void clear() {
        store.clear();
    }
}
