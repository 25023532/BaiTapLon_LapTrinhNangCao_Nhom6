package org.example.exception;

/**
 * Ném khi cố đặt giá vào phiên không ở trạng thái RUNNING:
 * - Phiên đã FINISHED / PAID / CANCELED
 * - Phiên chưa bắt đầu (OPEN)
 * - Phiên đã hết giờ
 */
public class AuctionClosedException extends RuntimeException {

    private final String sessionId;
    private final String currentStatus;

    public AuctionClosedException(String sessionId, String currentStatus) {
        super("Phiên đấu giá '%s' không thể nhận bid. Trạng thái hiện tại: %s"
                .formatted(sessionId, currentStatus));
        this.sessionId     = sessionId;
        this.currentStatus = currentStatus;
    }

    public String getSessionId()     { return sessionId; }
    public String getCurrentStatus() { return currentStatus; }
}
