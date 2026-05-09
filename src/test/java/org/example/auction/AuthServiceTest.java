package org.example.auction;

import org.example.service.AuthService;
import org.example.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
    }

    // ── Test đăng nhập thành công ────────────────────────
    @Test
    void testLogin_Success() {
        authService.register(new User("admin", "admin123", "ADMIN"));
        User result = authService.login("admin", "admin123");
        assertNotNull(result);
        System.out.println("✔ Test đăng nhập thành công: " + result.getUsername());
    }

    // ── Test đăng nhập sai mật khẩu ─────────────────────
    @Test
    void testLogin_WrongPassword() {
        authService.register(new User("admin", "admin123", "ADMIN"));
        User result = authService.login("admin", "wrongpass");
        assertNull(result);
        System.out.println("✔ Test sai mật khẩu: trả về null");
    }

    // ── Test đăng nhập tài khoản không tồn tại ──────────
    @Test
    void testLogin_UserNotFound() {
        User result = authService.login("unknown", "pass123");
        assertNull(result);
        System.out.println("✔ Test tài khoản không tồn tại: trả về null");
    }

    // ── Test đăng nhập username rỗng ─────────────────────
    @Test
    void testLogin_EmptyUsername() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("", "pass123"));
        System.out.println("✔ Test username rỗng: ném IllegalArgumentException");
    }

    // ── Test đăng nhập password rỗng ─────────────────────
    @Test
    void testLogin_EmptyPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("admin", ""));
        System.out.println("✔ Test password rỗng: ném IllegalArgumentException");
    }

    // ── Test đăng ký thành công ──────────────────────────
    @Test
    void testRegister_Success() {
        authService.register(new User("newuser", "pass123", "USER"));
        assertTrue(authService.isRegistered("newuser"));
        System.out.println("✔ Test đăng ký thành công");
    }

    // ── Test đăng ký tài khoản đã tồn tại ───────────────
    @Test
    void testRegister_UserAlreadyExists() {
        authService.register(new User("newuser", "pass123", "USER"));
        assertThrows(IllegalStateException.class,
                () -> authService.register(new User("newuser", "pass456", "USER")));
        System.out.println("✔ Test tài khoản đã tồn tại: ném IllegalStateException");
    }

    // ── Test đăng ký user null ────────────────────────────
    @Test
    void testRegister_NullUser() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(null));
        System.out.println("✔ Test register null: ném IllegalArgumentException");
    }

    // ── Test kiểm tra username đã đăng ký ────────────────
    @Test
    void testIsRegistered_True() {
        authService.register(new User("admin", "admin123", "ADMIN"));
        assertTrue(authService.isRegistered("admin"));
        System.out.println("✔ Test isRegistered = true");
    }

    @Test
    void testIsRegistered_False() {
        assertFalse(authService.isRegistered("nobody"));
        System.out.println("✔ Test isRegistered = false");
    }

    // ── Test xóa tài khoản thành công ────────────────────
    @Test
    void testUnregister_Success() {
        authService.register(new User("tempuser", "pass123", "USER"));
        authService.unregister("tempuser");
        assertFalse(authService.isRegistered("tempuser"));
        System.out.println("✔ Test xóa tài khoản thành công");
    }

    // ── Test xóa tài khoản không tồn tại ─────────────────
    @Test
    void testUnregister_NotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.unregister("nobody"));
        System.out.println("✔ Test xóa tài khoản không tồn tại: ném IllegalArgumentException");
    }

    // ── Test lấy danh sách tất cả users ──────────────────
    @Test
    void testGetAllUsers() {
        authService.register(new User("user1", "pass1", "USER"));
        authService.register(new User("user2", "pass2", "USER"));
        assertTrue(authService.getAllUsers().size() >= 2);
        System.out.println("✔ Test getAllUsers: " + authService.getAllUsers().size() + " users");
    }
}
