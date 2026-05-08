package org.example.exception;

/**
 * Ném ra khi giá đặt không hợp lệ:
 * - Thấp hơn hoặc bằng giá hiện tại
 * - Giá trị âm hoặc bằng 0
 */
public class InvalidBidException extends Exception {

    private double attemptedBid;
    private double currentHighestBid;

    public InvalidBidException(String message) {
        super(message);
    }

    public InvalidBidException(String message, double attemptedBid, double currentHighestBid) {
        super(message);
        this.attemptedBid = attemptedBid;
        this.currentHighestBid = currentHighestBid;
    }

    public InvalidBidException(String message, Throwable cause) {
        super(message, cause);
    }

    public double getAttemptedBid() {
        return attemptedBid;
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    @Override
    public String toString() {
        return "InvalidBidException{" +
                "attemptedBid=" + attemptedBid +
                ", currentHighestBid=" + currentHighestBid +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
