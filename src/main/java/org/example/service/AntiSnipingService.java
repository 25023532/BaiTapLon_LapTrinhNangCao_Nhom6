package org.example.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * AntiSnipingService — gia hạn phiên khi bid được đặt sát giờ kết thúc.
 *
 * <p>Nếu bid đến trong khoảng {@code thresholdSeconds} giây cuối,
 * thời gian kết thúc sẽ được đẩy thêm {@code extensionSeconds} giây.</p>
 */
public class AntiSnipingService {

    private final int thresholdSeconds;
    private final int extensionSeconds;

    /**
     * Khởi tạo service với ngưỡng và số giây gia hạn.
     *
     * @param thresholdSeconds số giây cuối tính là "snipe zone"
     * @param extensionSeconds số giây gia hạn khi phát hiện snipe
     * @throws IllegalArgumentException nếu tham số âm
     */
    public AntiSnipingService(int thresholdSeconds, int extensionSeconds) {
        if (thresholdSeconds < 0) {
            throw new IllegalArgumentException("thresholdSeconds không được âm");
        }
        if (extensionSeconds < 0) {
            throw new IllegalArgumentException("extensionSeconds không được âm");
        }
        this.thresholdSeconds = thresholdSeconds;
        this.extensionSeconds = extensionSeconds;
    }

    /**
     * Kiểm tra xem thời điểm hiện tại có nằm trong "snipe zone" không.
     *
     * @param endTime thời điểm kết thúc phiên
     * @return {@code true} nếu còn {@code <= thresholdSeconds} giây
     * @throws IllegalArgumentException nếu endTime null
     */
    public boolean isSnipe(LocalDateTime endTime) {
        if (endTime == null) {
            throw new IllegalArgumentException("endTime không được null");
        }
        long secondsRemaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);
        return secondsRemaining >= 0 && secondsRemaining <= thresholdSeconds;
    }

    /**
     * Gia hạn endTime nếu đang trong snipe zone, ngược lại trả về nguyên.
     *
     * @param endTime thời điểm kết thúc hiện tại
     * @return endTime mới (đã gia hạn hoặc không đổi)
     * @throws IllegalArgumentException nếu endTime null
     */
    public LocalDateTime extendEndTime(LocalDateTime endTime) {
        if (endTime == null) {
            throw new IllegalArgumentException("endTime không được null");
        }
        if (isSnipe(endTime)) {
            return endTime.plusSeconds(extensionSeconds);
        }
        return endTime;
    }

    /** @return ngưỡng giây cấu hình */
    public int getThresholdSeconds() {
        return thresholdSeconds;
    }

    /** @return số giây gia hạn cấu hình */
    public int getExtensionSeconds() {
        return extensionSeconds;
    }
}
