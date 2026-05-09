package org.example.exception;

/**
 * Ném ra khi người dùng cố đặt giá vào một phiên đấu giá đã kết thúc.
 */
public class AuctionClosedException extends Exception {

    private final String sessionId;
    private final String currentStatus;  // thêm field này để test dùng

    public AuctionClosedException(String message) {
        super(message);
        this.sessionId     = null;
        this.currentStatus = null;
    }

    public AuctionClosedException(String message, String sessionId) {
        super(message);
        this.sessionId     = sessionId;
        this.currentStatus = null;
    }

    // Constructor mới: test dùng ex.getCurrentStatus()
    public AuctionClosedException(String message, String sessionId, String currentStatus) {
        super(message);
        this.sessionId     = sessionId;
        this.currentStatus = currentStatus;
    }

    public AuctionClosedException(String message, Throwable cause) {
        super(message, cause);
        this.sessionId     = null;
        this.currentStatus = null;
    }

    public String getSessionId()     { return sessionId; }
    public String getCurrentStatus() { return currentStatus; }

    @Override
    public String toString() {
        return "AuctionClosedException{sessionId='" + sessionId
                + "', status='" + currentStatus
                + "', message='" + getMessage() + "'}";
    }
}
