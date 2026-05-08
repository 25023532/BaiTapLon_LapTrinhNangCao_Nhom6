package org.example.exception;

/**
 * Xử lý tập trung tất cả exception trong hệ thống
 * Tránh lặp lại try-catch ở nhiều nơi
 */
public class GlobalExceptionHandler {

    // ── Xử lý từng loại exception ────────────────────────

    public static String handle(AuctionClosedException e) {
        System.err.println("[AUCTION CLOSED] " + e.getMessage()
            + " | Session: " + e.getSessionId());
        return "Phiên đấu giá đã kết thúc: " + e.getMessage();
    }

    public static String handle(InvalidBidException e) {
        System.err.println("[INVALID BID] " + e.getMessage()
            + " | Bid cố đặt: " + e.getAttemptedBid()
            + " | Giá hiện tại: " + e.getCurrentHighestBid());
        return "Giá không hợp lệ: " + e.getMessage();
    }

    public static String handle(AuthenticationException e) {
        System.err.println("[AUTH FAILED] " + e.getMessage()
            + " | User: " + e.getUsername());
        return "Xác thực thất bại: " + e.getMessage();
    }

    public static String handle(DataException e) {
        System.err.println("[DATA ERROR] " + e.getMessage()
            + " | Field: " + e.getField()
            + " | Value: " + e.getInvalidValue());
        return "Lỗi dữ liệu [" + e.getField() + "]: " + e.getMessage();
    }

    // ── Xử lý exception không xác định ───────────────────

    public static String handle(Exception e) {
        System.err.println("[UNKNOWN ERROR] " + e.getMessage());
        e.printStackTrace();
        return "Lỗi hệ thống: " + e.getMessage();
    }
}
