package org.example.exception;

/**
 * Ném ra khi giá đặt không hợp lệ (thấp hơn mức tối thiểu).
 */
public class InvalidBidException extends Exception {

    private final double attemptedAmount;   // test dùng getAttemptedAmount()
    private final double minimumRequired;   // test dùng getMinimumRequired()

    public InvalidBidException(String message) {
        super(message);
        this.attemptedAmount = 0;
        this.minimumRequired = 0;
    }

    public InvalidBidException(String message, double attemptedAmount, double minimumRequired) {
        super(message);
        this.attemptedAmount = attemptedAmount;
        this.minimumRequired = minimumRequired;
    }

    public InvalidBidException(String message, Throwable cause) {
        super(message, cause);
        this.attemptedAmount = 0;
        this.minimumRequired = 0;
    }

    public double getAttemptedAmount()  { return attemptedAmount; }
    public double getMinimumRequired()  { return minimumRequired; }

    // Giữ tên cũ để không break code khác
    public double getAttemptedBid()     { return attemptedAmount; }
    public double getCurrentHighestBid(){ return minimumRequired; }

    @Override
    public String toString() {
        return "InvalidBidException{attempted=" + attemptedAmount
                + ", minimum=" + minimumRequired
                + ", message='" + getMessage() + "'}";
    }
}
