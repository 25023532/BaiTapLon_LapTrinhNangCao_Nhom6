package org.example.exception;

/**
 * Ném khi bid không hợp lệ:
 * - Giá thấp hơn hoặc bằng giá hiện tại
 * - Giá âm hoặc bằng 0
 */
public class InvalidBidException extends RuntimeException {

    private final double attemptedAmount;
    private final double minimumRequired;

    public InvalidBidException(double attemptedAmount, double minimumRequired) {
        super("Giá đặt %.2f không hợp lệ. Giá tối thiểu cần là: %.2f"
                .formatted(attemptedAmount, minimumRequired));
        this.attemptedAmount = attemptedAmount;
        this.minimumRequired = minimumRequired;
    }

    public InvalidBidException(String message) {
        super(message);
        this.attemptedAmount = 0;
        this.minimumRequired = 0;
    }

    public double getAttemptedAmount() { return attemptedAmount; }
    public double getMinimumRequired() { return minimumRequired; }
}