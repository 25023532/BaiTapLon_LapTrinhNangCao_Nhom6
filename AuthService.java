package org.example.service;

import org.example.user.User;

import java.util.HashMap;
import java.util.Map;

public class AuthService {

    private final Map<String, User> users = new HashMap<>();

    public void register(User user) {
        if (user == null)
            throw new IllegalArgumentException("User không được null");
        if (users.containsKey(user.getUsername()))
            throw new IllegalStateException("Username '" + user.getUsername() + "' đã tồn tại");

        users.put(user.getUsername(), user);
        System.out.println("Đăng ký thành công: " + user.getUsername());
    }

    public User login(String username, String password) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username không được rỗng");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password không được rỗng");

        User user = users.get(username);
        if (user != null && user.checkPassword(password)) {
            System.out.println("Đăng nhập thành công: " + username + " [" + user.getRole() + "]");
            return user;
        }

        System.out.println("Sai username hoặc password.");
        return null;
    }

    public void unregister(String username) {
        if (!users.containsKey(username))
            throw new IllegalArgumentException("Không tìm thấy user: " + username);
        users.remove(username);
        System.out.println("Đã xóa tài khoản: " + username);
    }

    public boolean isRegistered(String username) {
        return users.containsKey(username);
    }
}
