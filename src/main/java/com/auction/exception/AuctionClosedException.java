package org.example.exception;

/**
 * Ném ra khi người dùng cố đặt giá vào một phiên đấu giá đã kết thúc
 */
public class AuctionClosedException extends Exception {

    private String sessionId;

    public AuctionClosedException(String message) {
        super(message);
    }

    public AuctionClosedException(String message, String sessionId) {
        super(message);
        this.sessionId = sessionId;
    }

    public AuctionClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return "AuctionClosedException{" +
                "sessionId='" + sessionId + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
