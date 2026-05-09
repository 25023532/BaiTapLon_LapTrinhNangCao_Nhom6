package org.example.exception;

/**
 * Xử lý tập trung tất cả exception trong hệ thống
 */
public class GlobalExceptionHandler {

    // ===== AuctionClosedException =====

    public static String handle(AuctionClosedException e) {

        System.err.println(
                "[AUCTION CLOSED] "
                        + e.getMessage()
                        + " | Session: " + e.getSessionId()
                        + " | Status: " + e.getCurrentStatus()
        );

        return "Phiên đấu giá đã đóng: " + e.getMessage();
    }

    // ===== InvalidBidException =====

    public static String handle(InvalidBidException e) {

        System.err.println(
                "[INVALID BID] "
                        + e.getMessage()
                        + " | Attempted: " + e.getAttemptedAmount()
                        + " | Minimum required: " + e.getMinimumRequired()
        );

        return "Giá đặt không hợp lệ: " + e.getMessage();
    }

    // ===== AuthenticationException =====

    public static String handle(AuthenticationException e) {

        System.err.println(
                "[AUTH FAILED] "
                        + e.getMessage()
                        + " | User: " + e.getUsername()
        );

        return "Xác thực thất bại: " + e.getMessage();
    }

    // ===== DataException =====

    public static String handle(DataException e) {

        System.err.println(
                "[DATA ERROR] "
                        + e.getMessage()
                        + " | Field: " + e.getField()
                        + " | Value: " + e.getInvalidValue()
        );

        return "Lỗi dữ liệu [" + e.getField() + "]";
    }

    // ===== Unknown Exception =====

    public static String handle(Exception e) {

        System.err.println("[UNKNOWN ERROR] " + e.getMessage());

        e.printStackTrace();

        return "Lỗi hệ thống: " + e.getMessage();
    }
}

