package org.example.auction;

/**
 * Cấu hình đấu giá tự động của một Bidder.
 */
public class AutoBidConfig {

    private final String bidderId;
    private final double maxBid;
    private final double increment;
    private final long   registeredAt;   // ưu tiên theo thời gian đăng ký

    public AutoBidConfig(String bidderId, double maxBid, double increment) {
        if (maxBid <= 0)     throw new IllegalArgumentException("maxBid phải > 0");
        if (increment <= 0)  throw new IllegalArgumentException("increment phải > 0");

        this.bidderId     = bidderId;
        this.maxBid       = maxBid;
        this.increment    = increment;
        this.registeredAt = System.nanoTime();
    }

    public String getBidderId()  { return bidderId; }
    public double getMaxBid()    { return maxBid; }
    public double getIncrement() { return increment; }
    public long   getRegisteredAt() { return registeredAt; }
}