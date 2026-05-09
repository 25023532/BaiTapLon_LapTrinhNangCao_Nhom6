package org.example.exception;

/**
 * Ném ra khi người dùng cố đặt giá
 * vào một phiên đấu giá đã đóng
 */
public class AuctionClosedException extends Exception {

    private String sessionId;
    private String currentStatus;

    // ===== Constructor cơ bản =====

    public AuctionClosedException(String message) {
        super(message);
    }

    // ===== Constructor cũ =====

    public AuctionClosedException(String message, String sessionId) {
        super(message);
        this.sessionId = sessionId;
    }

    // ===== Constructor đầy đủ =====

    public AuctionClosedException(
            String message,
            String sessionId,
            String currentStatus
    ) {
        super(message);

        this.sessionId = sessionId;
        this.currentStatus = currentStatus;
    }

    // ===== Constructor có cause =====

    public AuctionClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    // ===== Getter =====

    public String getSessionId() {
        return sessionId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    // ===== toString =====

    @Override
    public String toString() {
        return "AuctionClosedException{" +
                "sessionId='" + sessionId + '\'' +
                ", currentStatus='" + currentStatus + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
