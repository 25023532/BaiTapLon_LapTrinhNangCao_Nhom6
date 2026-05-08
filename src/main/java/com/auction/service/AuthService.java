package com.auction.service;

import com.auction.exception.AuthenticationException;
import com.auction.exception.DataException;
import com.auction.exception.GlobalExceptionHandler;

import java.util.HashMap;
import java.util.Map;

public class AuthService {

    // Giả lập database người dùng
    // TODO: thay bằng kết nối database thực tế
    private static final Map<String, String> USER_DB = new HashMap<>();

    static {
        USER_DB.put("admin",  "admin123");
        USER_DB.put("bidder1", "pass123");
        USER_DB.put("seller1", "pass456");
    }

    // ── Đăng nhập ─────────────────────────────────────────
    public String login(String username, String password) {
        try {
            // Validate dữ liệu đầu vào
            if (username == null || username.isBlank())
                throw new DataException(
                    "Tên đăng nhập không được để trống", "username", username);

            if (password == null || password.isBlank())
                throw new DataException(
                    "Mật khẩu không được để trống", "password", password);

            // Kiểm tra tài khoản tồn tại
            if (!USER_DB.containsKey(username))
                throw new AuthenticationException(
                    "Tài khoản không tồn tại!", username);

            // Kiểm tra mật khẩu
            if (!USER_DB.get(username).equals(password))
                throw new AuthenticationException(
                    "Sai mật khẩu!", username);

            System.out.println("Đăng nhập thành công: " + username);
            return "Đăng nhập thành công! Xin chào " + username;

        } catch (AuthenticationException e) {
            return GlobalExceptionHandler.handle(e);

        } catch (DataException e) {
            return GlobalExceptionHandler.handle(e);
        }
    }

    // ── Đăng ký ───────────────────────────────────────────
    public String register(String username, String password) {
        try {
            // Validate dữ liệu đầu vào
            if (username == null || username.isBlank())
                throw new DataException(
                    "Tên đăng nhập không được để trống", "username", username);

            if (password == null || password.isBlank())
                throw new DataException(
                    "Mật khẩu không được để trống", "password", password);

            if (password.length() < 6)
                throw new DataException(
                    "Mật khẩu phải có ít nhất 6 ký tự", "password", password);

            // Kiểm tra tài khoản đã tồn tại chưa
            if (USER_DB.containsKey(username))
                throw new AuthenticationException(
                    "Tên đăng nhập đã tồn tại!", username);

            // Lưu tài khoản mới
            USER_DB.put(username, password);
            System.out.println("Đăng ký thành công: " + username);
            return "Đăng ký thành công! Xin chào " + username;

        } catch (AuthenticationException e) {
            return GlobalExceptionHandler.handle(e);

        } catch (DataException e) {
            return GlobalExceptionHandler.handle(e);
        }
    }

    // ── Đổi mật khẩu ─────────────────────────────────────
    public String changePassword(String username,
                                  String oldPassword,
                                  String newPassword) {
        try {
            // Validate dữ liệu
            if (username == null || username.isBlank())
                throw new DataException(
                    "Tên đăng nhập không được để trống", "username", username);

            if (oldPassword == null || oldPassword.isBlank())
                throw new DataException(
                    "Mật khẩu cũ không được để trống", "oldPassword");

            if (newPassword == null || newPassword.isBlank())
                throw new DataException(
                    "Mật khẩu mới không được để trống", "newPassword");

            if (newPassword.length() < 6)
                throw new DataException(
                    "Mật khẩu mới phải có ít nhất 6 ký tự", "newPassword");

            // Kiểm tra tài khoản tồn tại
            if (!USER_DB.containsKey(username))
                throw new AuthenticationException(
                    "Tài khoản không tồn tại!", username);

            // Kiểm tra mật khẩu cũ
            if (!USER_DB.get(username).equals(oldPassword))
                throw new AuthenticationException(
                    "Mật khẩu cũ không đúng!", username);

            // Cập nhật mật khẩu mới
            USER_DB.put(username, newPassword);
            return "Đổi mật khẩu thành công!";

        } catch (AuthenticationException e) {
            return GlobalExceptionHandler.handle(e);

        } catch (DataException e) {
            return GlobalExceptionHandler.handle(e);
        }
    }

    // ── Kiểm tra quyền Admin ──────────────────────────────
    public boolean isAdmin(String username) {
        try {
            if (username == null || username.isBlank())
                throw new DataException(
                    "Tên đăng nhập không được để trống", "username");

            return "admin".equals(username);

        } catch (DataException e) {
            System.err.println(GlobalExceptionHandler.handle(e));
            return false;
        }
    }
}
