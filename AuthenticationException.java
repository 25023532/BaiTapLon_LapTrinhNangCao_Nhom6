package org.example.exception;

/**
 * Ném ra khi xác thực người dùng thất bại:
 * - Sai tên đăng nhập / mật khẩu
 * - Tài khoản không tồn tại
 * - Không có quyền thực hiện hành động
 */
public class AuthenticationException extends Exception {

    private String username;

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, String username) {
        super(message);
        this.username = username;
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "AuthenticationException{" +
                "username='" + username + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}