package org.example.auction;

import org.example.service.AuthService;
import org.example.user.Admin;
import org.example.user.Bidder;
import org.example.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private AuthService authService;
    private String uid;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
        uid = "t" + System.nanoTime(); // username ngẫu nhiên, không bao giờ trùng
    }

    @Test
    void testLogin_Success() {
        authService.register(new Admin(uid, uid, "admin123"));
        User result = authService.login(uid, "admin123");
        assertNotNull(result);
        System.out.println("✔ Test đăng nhập thành công: " + result.getUsername());
    }

    @Test
    void testLogin_WrongPassword() {
        authService.register(new Admin(uid, uid, "admin123"));
        User result = authService.login(uid, "wrongpass");
        assertNull(result);
        System.out.println("✔ Test sai mật khẩu: trả về null");
    }

    @Test
    void testLogin_UserNotFound() {
        User result = authService.login("nobody_" + uid, "pass123");
        assertNull(result);
        System.out.println("✔ Test tài khoản không tồn tại: trả về null");
    }

    @Test
    void testLogin_EmptyUsername() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("", "pass123"));
        System.out.println("✔ Test username rỗng: ném IllegalArgumentException");
    }

    @Test
    void testLogin_EmptyPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(uid, ""));
        System.out.println("✔ Test password rỗng: ném IllegalArgumentException");
    }

    @Test
    void testRegister_Success() {
        authService.register(new Bidder(uid, uid, "pass123"));
        assertTrue(authService.isRegistered(uid));
        System.out.println("✔ Test đăng ký thành công");
    }

    @Test
    void testRegister_UserAlreadyExists() {
        authService.register(new Bidder(uid, uid, "pass123"));
        assertThrows(IllegalStateException.class,
                () -> authService.register(new Bidder(uid + "2", uid, "pass456")));
        System.out.println("✔ Test tài khoản đã tồn tại: ném IllegalStateException");
    }

    @Test
    void testRegister_NullUser() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(null));
        System.out.println("✔ Test register null: ném IllegalArgumentException");
    }

    @Test
    void testIsRegistered_True() {
        authService.register(new Admin(uid, uid, "admin123"));
        assertTrue(authService.isRegistered(uid));
        System.out.println("✔ Test isRegistered = true");
    }

    @Test
    void testIsRegistered_False() {
        assertFalse(authService.isRegistered("nobody_" + uid));
        System.out.println("✔ Test isRegistered = false");
    }

    @Test
    void testUnregister_Success() {
        authService.register(new Bidder(uid, uid, "pass123"));
        authService.unregister(uid);
        assertFalse(authService.isRegistered(uid));
        System.out.println("✔ Test xóa tài khoản thành công");
    }

    @Test
    void testUnregister_NotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.unregister("nobody_" + uid));
        System.out.println("✔ Test xóa tài khoản không tồn tại: ném IllegalArgumentException");
    }

    @Test
    void testGetAllUsers() {
        authService.register(new Bidder(uid + "a", uid + "a", "pass123"));
        authService.register(new Bidder(uid + "b", uid + "b", "pass456"));
        assertTrue(authService.getAllUsers().size() >= 2);
        System.out.println("✔ Test getAllUsers: " + authService.getAllUsers().size() + " users");
    }
}

    // ── Test đăng nhập thành công ────────────────────────
    @Test
    void testLogin_Success() {
        authService.register(new Admin(uid, uid, "admin123"));
        User result = authService.login(uid, "admin123");
        assertNotNull(result);
        System.out.println("✔ Test đăng nhập thành công: " + result.getUsername());
    }

    @Test
    void testLogin_WrongPassword() {
        authService.register(new Admin(uid, uid, "admin123"));
        User result = authService.login(uid, "wrongpass");
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
        authService.register(new Bidder(uid, uid, "pass123"));
        assertTrue(authService.isRegistered(uid));
        System.out.println("✔ Test đăng ký thành công");
    }

    @Test
    void testRegister_UserAlreadyExists() {
        authService.register(new Bidder(uid, uid, "pass123"));
        assertThrows(IllegalStateException.class,
                () -> authService.register(new Bidder(uid + "2", uid, "pass456")));
        System.out.println("✔ Test tài khoản đã tồn tại: ném IllegalStateException");
    }

    @Test
    void testRegister_NullUser() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(null));
        System.out.println("✔ Test register null: ném IllegalArgumentException");
    }

    @Test
    void testIsRegistered_True() {
        authService.register(new Admin(uid, uid, "admin123"));
        assertTrue(authService.isRegistered(uid));
        System.out.println("✔ Test isRegistered = true");
    }

    @Test
    void testIsRegistered_False() {
        assertFalse(authService.isRegistered("nobody_" + uid));
        System.out.println("✔ Test isRegistered = false");
    }

    @Test
    void testUnregister_Success() {
        authService.register(new Bidder(uid, uid, "pass123"));
        authService.unregister(uid);
        assertFalse(authService.isRegistered(uid));
        System.out.println("✔ Test xóa tài khoản thành công");
    }

    @Test
    void testUnregister_NotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.unregister("nobody_" + uid));
        System.out.println("✔ Test xóa tài khoản không tồn tại: ném IllegalArgumentException");
    }

    @Test
    void testGetAllUsers() {
        authService.register(new Bidder(uid + "a", uid + "a", "pass123"));
        authService.register(new Bidder(uid + "b", uid + "b", "pass456"));
        assertTrue(authService.getAllUsers().size() >= 2);
        System.out.println("✔ Test getAllUsers: " + authService.getAllUsers().size() + " users");
    }
}
