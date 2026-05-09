package org.example.exception;

/**
 * Ném ra khi giá đặt không hợp lệ:
 * - Thấp hơn mức tối thiểu
 * - Giá trị âm hoặc bằng 0
 */
public class InvalidBidException extends Exception {

    private double attemptedAmount;
    private double minimumRequired;

    // ===== Constructor cơ bản =====

    public InvalidBidException(String message) {
        super(message);
    }

    // ===== Constructor đầy đủ =====

    public InvalidBidException(
            String message,
            double attemptedAmount,
            double minimumRequired
    ) {
        super(message);

        this.attemptedAmount = attemptedAmount;
        this.minimumRequired = minimumRequired;
    }

    // ===== Constructor có cause =====

    public InvalidBidException(String message, Throwable cause) {
        super(message, cause);
    }

    // ===== Getter =====

    public double getAttemptedAmount() {
        return attemptedAmount;
    }

    public double getMinimumRequired() {
        return minimumRequired;
    }

    // ===== toString =====

    @Override
    public String toString() {
        return "InvalidBidException{" +
                "attemptedAmount=" + attemptedAmount +
                ", minimumRequired=" + minimumRequired +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
