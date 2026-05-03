package org.example.exception;

/**
 * Ném khi xác thực thất bại:
 * - Sai username / password
 * - Tài khoản không tồn tại
 * - Không có quyền thực hiện hành động
 */
public class AuthenticationException extends RuntimeException {

    public enum Reason {
        INVALID_CREDENTIALS,   // Sai username/password
        USER_NOT_FOUND,        // Tài khoản không tồn tại
        UNAUTHORIZED_ROLE      // Không đủ quyền
    }

    private final Reason reason;

    public AuthenticationException(Reason reason) {
        super(switch (reason) {
            case INVALID_CREDENTIALS -> "Sai username hoặc password.";
            case USER_NOT_FOUND      -> "Tài khoản không tồn tại.";
            case UNAUTHORIZED_ROLE   -> "Bạn không có quyền thực hiện hành động này.";
        });
        this.reason = reason;
    }

    public AuthenticationException(Reason reason, String detail) {
        super(detail);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}