package org.example.service;

import org.example.user.User;

import java.util.*;

public class AuthService {

    private final Map<String, User> users = new LinkedHashMap<>();

    public AuthService() {
        // Tải danh sách tài khoản đã lưu từ file khi khởi động
        Map<String, User> saved = UserStorage.loadAll();
        users.putAll(saved);
        System.out.println("[AuthService] Đã load " + saved.size() + " tài khoản từ file.");
    }

    // =========================================================
    // ĐĂNG KÝ
    // =========================================================
    public void register(User user) {
        if (user == null)
            throw new IllegalArgumentException("User không được null");
        if (users.containsKey(user.getUsername()))
            throw new IllegalStateException("Username '" + user.getUsername() + "' đã tồn tại");

        users.put(user.getUsername(), user);
        UserStorage.saveAll(users); // Lưu ngay xuống file
        System.out.println("[AuthService] Đăng ký thành công: " + user.getUsername());
    }

    // =========================================================
    // ĐĂNG NHẬP
    // =========================================================
    public User login(String username, String password) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username không được rỗng");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password không được rỗng");

        User user = users.get(username);
        if (user != null && user.checkPassword(password)) {
            System.out.println("[AuthService] Đăng nhập thành công: " + username);
            return user;
        }

        System.out.println("[AuthService] Sai username hoặc password.");
        return null;
    }

    // =========================================================
    // KIỂM TRA USERNAME ĐÃ TỒN TẠI
    // =========================================================
    public boolean isRegistered(String username) {
        return users.containsKey(username);
    }

    // =========================================================
    // XÓA TÀI KHOẢN
    // AdminController gọi phương thức này (tên cũ: deleteUser → đổi thành unregister)
    // =========================================================
    public void unregister(String username) {
        if (!users.containsKey(username))
            throw new IllegalArgumentException("Không tìm thấy user: " + username);

        users.remove(username);
        UserStorage.saveAll(users); // Lưu lại sau khi xóa
        System.out.println("[AuthService] Đã xóa tài khoản: " + username);
    }

    // =========================================================
    // LẤY TOÀN BỘ NGƯỜI DÙNG
    // Trả về Map<String, User> — username là key
    // AdminController cần dùng: new ArrayList<>(authService.getAllUsers().values())
    // để lấy dạng List<User>
    // =========================================================
    public Map<String, User> getAllUsers() {
        return Collections.unmodifiableMap(users);
    }
}
