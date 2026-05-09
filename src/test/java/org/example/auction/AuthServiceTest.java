package org.example.auction;

import org.example.service.AuthService; // ✅ Sửa import đúng package
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(); // Tạo instance mới trước mỗi test
    }

    // ── Test đăng nhập thành công ────────────────────────
    @Test
    void testLogin_Success() {
        String result = authService.login("admin", "admin123");
        assertTrue(result.contains("thành công"));
        System.out.println("✔ Test đăng nhập thành công: " + result);
    }

    // ── Test đăng nhập sai mật khẩu ─────────────────────
    @Test
    void testLogin_WrongPassword() {
        String result = authService.login("admin", "wrongpass");
        assertTrue(result.contains("thất bại") || result.contains("Sai"));
        System.out.println("✔ Test sai mật khẩu: " + result);
    }

    // ── Test đăng nhập tài khoản không tồn tại ──────────
    @Test
    void testLogin_UserNotFound() {
        String result = authService.login("unknown", "pass123");
        assertTrue(result.contains("không tồn tại") || result.contains("thất bại"));
        System.out.println("✔ Test tài khoản không tồn tại: " + result);
    }

    // ── Test đăng nhập username rỗng ─────────────────────
    @Test
    void testLogin_EmptyUsername() {
        String result = authService.login("", "pass123");
        assertTrue(result.contains("Lỗi") || result.contains("trống"));
        System.out.println("✔ Test username rỗng: " + result);
    }

    // ── Test đăng nhập password rỗng ─────────────────────
    @Test
    void testLogin_EmptyPassword() {
        String result = authService.login("admin", "");
        assertTrue(result.contains("Lỗi") || result.contains("trống"));
        System.out.println("✔ Test password rỗng: " + result);
    }

    // ── Test đăng ký thành công ──────────────────────────
    @Test
    void testRegister_Success() {
        String result = authService.register("newuser", "pass123");
        assertTrue(result.contains("thành công"));
        System.out.println("✔ Test đăng ký thành công: " + result);
    }

    // ── Test đăng ký tài khoản đã tồn tại ───────────────
    @Test
    void testRegister_UserAlreadyExists() {
        authService.register("newuser", "pass123");
        String result = authService.register("newuser", "pass456");
        assertTrue(result.contains("đã tồn tại") || result.contains("thất bại"));
        System.out.println("✔ Test tài khoản đã tồn tại: " + result);
    }

    // ── Test đăng ký mật khẩu quá ngắn ──────────────────
    @Test
    void testRegister_ShortPassword() {
        String result = authService.register("newuser2", "123");
        assertTrue(result.contains("Lỗi") || result.contains("ký tự"));
        System.out.println("✔ Test mật khẩu quá ngắn: " + result);
    }

    // ── Test đổi mật khẩu thành công ────────────────────
    @Test
    void testChangePassword_Success() {
        // Dùng instance riêng, không ảnh hưởng test khác
        String result = authService.changePassword("admin", "admin123", "newpass123");
        assertTrue(result.contains("thành công"));
        System.out.println("✔ Test đổi mật khẩu thành công: " + result);
    }

    // ── Test đổi mật khẩu sai mật khẩu cũ ───────────────
    @Test
    void testChangePassword_WrongOldPassword() {
        String result = authService.changePassword("admin", "wrongpass", "newpass123");
        assertTrue(result.contains("thất bại") || result.contains("không đúng"));
        System.out.println("✔ Test sai mật khẩu cũ: " + result);
    }

    // ── Test kiểm tra quyền admin ────────────────────────
    @Test
    void testIsAdmin_True() {
        assertTrue(authService.isAdmin("admin"));
        System.out.println("✔ Test isAdmin = true");
    }

    @Test
    void testIsAdmin_False() {
        assertFalse(authService.isAdmin("bidder1"));
        System.out.println("✔ Test isAdmin = false");
    }
}
