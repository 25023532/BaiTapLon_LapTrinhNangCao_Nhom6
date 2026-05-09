package org.example.service;

import org.example.user.User;

import java.util.*;

public class AuthService {

    private final Map<String, User> users = new LinkedHashMap<>();

    public AuthService() {
        // Load tài khoản đã lưu từ file khi khởi động
        Map<String, User> saved = UserStorage.loadAll();
        users.putAll(saved);
        System.out.println("[AuthService] Đã load " + saved.size() + " tài khoản từ file.");
    }

    // ── Đăng ký ──────────────────────────────────────────────
    public void register(User user) {
        if (user == null)
            throw new IllegalArgumentException("User không được null");
        if (users.containsKey(user.getUsername()))
            throw new IllegalStateException("Username '" + user.getUsername() + "' đã tồn tại");

        users.put(user.getUsername(), user);
        UserStorage.saveAll(users);  // ← lưu xuống file ngay lập tức
        System.out.println("[AuthService] Đăng ký thành công: " + user.getUsername());
    }

    // ── Đăng nhập ────────────────────────────────────────────
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

    // ── Kiểm tra username đã tồn tại chưa ───────────────────
    public boolean isRegistered(String username) {
        return users.containsKey(username);
    }

    // ── Xóa tài khoản ────────────────────────────────────────
    public void unregister(String username) {
        if (!users.containsKey(username))
            throw new IllegalArgumentException("Không tìm thấy user: " + username);
        users.remove(username);
        UserStorage.saveAll(users);
    }

    public Map<String, User> getAllUsers() {
        return Collections.unmodifiableMap(users);
    }
}
