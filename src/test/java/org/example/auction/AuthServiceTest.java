package org.example.auction;

import org.example.service.AuthService;
import org.example.user.Admin;
import org.example.user.Bidder;
import org.example.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthService Tests")
public class AuthServiceTest {

    private AuthService authService;
    private String uid;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        // Keep tests away from real data/users.json.
        System.setProperty("auction.users.file",
                tempDir.resolve("users.json").toString());
        authService = new AuthService();
        uid = "t" + System.nanoTime(); // username ngẫu nhiên, không bao giờ trùng
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("auction.users.file");
    }

    // ===== Login =====

    @Test
    @DisplayName("Đăng nhập thành công — trả về User không null")
    void testLogin_Success() {
        authService.register(new Admin(uid, uid, "admin123"));
        User result = authService.login(uid, "admin123");
        assertNotNull(result);
        assertEquals(uid, result.getUsername());
        System.out.println("✔ Test đăng nhập thành công: " + result.getUsername());
    }

    @Test
    @DisplayName("Sai mật khẩu — trả về null")
    void testLogin_WrongPassword() {
        authService.register(new Admin(uid, uid, "admin123"));
        User result = authService.login(uid, "wrongpass");
        assertNull(result);
        System.out.println("✔ Test sai mật khẩu: trả về null");
    }

    @Test
    @DisplayName("Tài khoản không tồn tại — trả về null")
    void testLogin_UserNotFound() {
        User result = authService.login("nobody_" + uid, "pass123");
        assertNull(result);
        System.out.println("✔ Test tài khoản không tồn tại: trả về null");
    }

    @Test
    @DisplayName("Username rỗng — throw IllegalArgumentException")
    void testLogin_EmptyUsername() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("", "pass123"));
        System.out.println("✔ Test username rỗng: ném IllegalArgumentException");
    }

    @Test
    @DisplayName("Password rỗng — throw IllegalArgumentException")
    void testLogin_EmptyPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(uid, ""));
        System.out.println("✔ Test password rỗng: ném IllegalArgumentException");
    }

    @Test
    @DisplayName("Username null — throw IllegalArgumentException")
    void testLogin_NullUsername() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(null, "pass123"));
        System.out.println("✔ Test username null: ném IllegalArgumentException");
    }

    @Test
    @DisplayName("Password null — throw IllegalArgumentException")
    void testLogin_NullPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(uid, null));
        System.out.println("✔ Test password null: ném IllegalArgumentException");
    }

    @Test
    @DisplayName("Đăng nhập với tài khoản Bidder — trả về Bidder instance")
    void testLogin_BidderAccount_ReturnsBidder() {
        authService.register(new Bidder(uid, uid, "bidpass"));
        User result = authService.login(uid, "bidpass");
        assertNotNull(result);
        assertInstanceOf(Bidder.class, result);
        System.out.println("✔ Test login Bidder: " + result.getClass().getSimpleName());
    }

    @Test
    @DisplayName("Đăng nhập với tài khoản Admin — trả về Admin instance")
    void testLogin_AdminAccount_ReturnsAdmin() {
        authService.register(new Admin(uid, uid, "adminpass"));
        User result = authService.login(uid, "adminpass");
        assertNotNull(result);
        assertInstanceOf(Admin.class, result);
        System.out.println("✔ Test login Admin: " + result.getClass().getSimpleName());
    }

    // ===== Register =====

    @Test
    @DisplayName("Đăng ký thành công")
    void testRegister_Success() {
        authService.register(new Bidder(uid, uid, "pass123"));
        assertTrue(authService.isRegistered(uid));
        System.out.println("✔ Test đăng ký thành công");
    }

    @Test
    @DisplayName("Đăng ký tài khoản đã tồn tại — throw IllegalStateException")
    void testRegister_UserAlreadyExists() {
        authService.register(new Bidder(uid, uid, "pass123"));
        assertThrows(IllegalStateException.class,
                () -> authService.register(new Bidder(uid + "2", uid, "pass456")));
        System.out.println("✔ Test tài khoản đã tồn tại: ném IllegalStateException");
    }

    @Test
    @DisplayName("Đăng ký null — throw IllegalArgumentException")
    void testRegister_NullUser() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(null));
        System.out.println("✔ Test register null: ném IllegalArgumentException");
    }

    @Test
    @DisplayName("Đăng ký nhiều loại user khác nhau — đều thành công")
    void testRegister_MultipleUserTypes() {
        String adminUid = uid + "_admin";
        String bidderUid = uid + "_bidder";
        authService.register(new Admin(adminUid, adminUid, "adminpass"));
        authService.register(new Bidder(bidderUid, bidderUid, "bidpass"));
        assertTrue(authService.isRegistered(adminUid));
        assertTrue(authService.isRegistered(bidderUid));
        System.out.println("✔ Test đăng ký nhiều loại user");
    }

    // ===== IsRegistered =====

    @Test
    @DisplayName("isRegistered = true khi đã đăng ký")
    void testIsRegistered_True() {
        authService.register(new Admin(uid, uid, "admin123"));
        assertTrue(authService.isRegistered(uid));
        System.out.println("✔ Test isRegistered = true");
    }

    @Test
    @DisplayName("isRegistered = false khi chưa đăng ký")
    void testIsRegistered_False() {
        assertFalse(authService.isRegistered("nobody_" + uid));
        System.out.println("✔ Test isRegistered = false");
    }

    // ===== Unregister =====

    @Test
    @DisplayName("Xóa tài khoản thành công")
    void testUnregister_Success() {
        authService.register(new Bidder(uid, uid, "pass123"));
        authService.unregister(uid);
        assertFalse(authService.isRegistered(uid));
        System.out.println("✔ Test xóa tài khoản thành công");
    }

    @Test
    @DisplayName("Xóa tài khoản không tồn tại — throw IllegalArgumentException")
    void testUnregister_NotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.unregister("nobody_" + uid));
        System.out.println("✔ Test xóa tài khoản không tồn tại: ném IllegalArgumentException");
    }

    @Test
    @DisplayName("Sau khi xóa, đăng nhập lại phải thất bại")
    void testUnregister_ThenLogin_ShouldFail() {
        authService.register(new Bidder(uid, uid, "pass123"));
        authService.unregister(uid);
        User result = authService.login(uid, "pass123");
        assertNull(result);
        System.out.println("✔ Test login sau khi unregister: trả về null");
    }

    // ===== GetAllUsers =====

    @Test
    @DisplayName("getAllUsers trả về danh sách >= số user đã đăng ký")
    void testGetAllUsers() {
        authService.register(new Bidder(uid + "a", uid + "a", "pass123"));
        authService.register(new Bidder(uid + "b", uid + "b", "pass456"));
        List<User> users = new ArrayList<>(authService.getAllUsers().values());
        assertTrue(users.size() >= 2);
        System.out.println("✔ Test getAllUsers: " + users.size() + " users");
    }

    @Test
    @DisplayName("getAllUsers không trả về null")
    void testGetAllUsers_NotNull() {
        assertNotNull(authService.getAllUsers());
        System.out.println("✔ Test getAllUsers: không null khi chưa có user");
    }

    @Test
    @DisplayName("getAllUsers — sau unregister, danh sách giảm đi 1")
    void testGetAllUsers_AfterUnregister_SizeDecreases() {
        authService.register(new Bidder(uid, uid, "pass123"));
        int sizeBefore = authService.getAllUsers().size();
        authService.unregister(uid);
        int sizeAfter = authService.getAllUsers().size();
        assertEquals(sizeBefore - 1, sizeAfter);
        System.out.println("✔ Test getAllUsers sau unregister: giảm đúng 1 phần tử");
    }
}
