// org/example/auction/AntiSnipingConfig.java
package org.example.auction;

/**
 * Cấu hình thuật toán chống snipe (đặt giá vào phút chót).
 * Đặt trong package auction cùng với AuctionSession.
 */
public class AntiSnipingConfig {

    /** Nếu bid xảy ra trong khoảng này (giây) trước khi hết giờ → gia hạn */
    private final long triggerThresholdSeconds;

    /** Số giây gia hạn thêm mỗi lần */
    private final long extensionSeconds;

    /** Số lần gia hạn tối đa */
    private final int maxExtensions;

    public AntiSnipingConfig(long triggerThresholdSeconds,
                             long extensionSeconds,
                             int maxExtensions) {
        this.triggerThresholdSeconds = triggerThresholdSeconds;
        this.extensionSeconds        = extensionSeconds;
        this.maxExtensions           = maxExtensions;
    }

    /** Mặc định: bid trong 30s cuối → gia hạn 30s, tối đa 5 lần */
    public static AntiSnipingConfig defaultConfig() {
        return new AntiSnipingConfig(30, 30, 5);
    }

    public long getTriggerThresholdSeconds() { return triggerThresholdSeconds; }
    public long getExtensionSeconds()        { return extensionSeconds; }
    public int  getMaxExtensions()           { return maxExtensions; }
}
