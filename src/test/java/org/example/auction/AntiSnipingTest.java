package org.example.auction;

import org.example.service.AntiSnipingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Anti-sniping: khi bid được đặt trong khoảng thời gian ngắn trước khi
 * phiên kết thúc, thời gian kết thúc sẽ được tự động gia hạn thêm.
 */
@DisplayName("AntiSnipingService Tests")
class AntiSnipingTest {

    /** Ngưỡng anti-snipe: bid trong 3 phút cuối sẽ gia hạn thêm */
    private static final int SNIPE_THRESHOLD_SECONDS = 180;

    /** Số giây gia hạn khi phát hiện snipe */
    private static final int EXTENSION_SECONDS = 120;

    private AntiSnipingService antiSnipingService;

    @BeforeEach
    void setUp() {
        antiSnipingService = new AntiSnipingService(
                SNIPE_THRESHOLD_SECONDS,
                EXTENSION_SECONDS
        );
    }

    // ===== isSnipe detection =====

    @Test
    @DisplayName("Bid đặt trong ngưỡng cuối — phát hiện là snipe")
    void isSnipe_BidWithinThreshold_ShouldReturnTrue() {
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(60); // còn 60s
        assertTrue(antiSnipingService.isSnipe(endTime));
    }

    @Test
    @DisplayName("Bid đặt đúng tại ngưỡng — phát hiện là snipe")
    void isSnipe_BidAtExactThreshold_ShouldReturnTrue() {
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(SNIPE_THRESHOLD_SECONDS);
        assertTrue(antiSnipingService.isSnipe(endTime));
    }

    @Test
    @DisplayName("Bid đặt ngoài ngưỡng — không phải snipe")
    void isSnipe_BidOutsideThreshold_ShouldReturnFalse() {
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(SNIPE_THRESHOLD_SECONDS + 60);
        assertFalse(antiSnipingService.isSnipe(endTime));
    }

    @Test
    @DisplayName("Bid khi phiên đã hết giờ — không phải snipe (phiên đóng)")
    void isSnipe_SessionAlreadyEnded_ShouldReturnFalse() {
        LocalDateTime endTime = LocalDateTime.now().minusMinutes(5);
        assertFalse(antiSnipingService.isSnipe(endTime));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 60, 90, 179, 180})
    @DisplayName("Các mốc thời gian trong ngưỡng — đều là snipe")
    void isSnipe_VariousWithinThreshold_ShouldBeSnipe(int secondsRemaining) {
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(secondsRemaining);
        assertTrue(antiSnipingService.isSnipe(endTime));
    }

    @ParameterizedTest
    @ValueSource(ints = {181, 300, 600, 3600})
    @DisplayName("Các mốc thời gian ngoài ngưỡng — không phải snipe")
    void isSnipe_VariousOutsideThreshold_ShouldNotBeSnipe(int secondsRemaining) {
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(secondsRemaining);
        assertFalse(antiSnipingService.isSnipe(endTime));
    }

    // ===== extendEndTime =====

    @Test
    @DisplayName("Gia hạn khi phát hiện snipe — endTime tăng thêm đúng số giây")
    void extendEndTime_WhenSnipe_ShouldExtend() {
        LocalDateTime originalEnd = LocalDateTime.now().plusSeconds(60);
        LocalDateTime extended    = antiSnipingService.extendEndTime(originalEnd);
        assertEquals(
                originalEnd.plusSeconds(EXTENSION_SECONDS),
                extended
        );
    }

    @Test
    @DisplayName("Không gia hạn khi không phải snipe — endTime không đổi")
    void extendEndTime_WhenNotSnipe_ShouldNotExtend() {
        // Không phải snipe: còn nhiều giờ
        LocalDateTime originalEnd = LocalDateTime.now().plusHours(2);
        LocalDateTime result      = antiSnipingService.extendEndTime(originalEnd);
        assertEquals(originalEnd, result);
    }

    @Test
    @DisplayName("extendEndTime — endTime null → throw IllegalArgumentException")
    void extendEndTime_NullEndTime_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> antiSnipingService.extendEndTime(null));
    }

    // ===== AuctionSession integration =====

    @Test
    @DisplayName("Đặt bid sát giờ — phiên tự động gia hạn")
    void auctionSession_BidNearEnd_ShouldExtendTime() throws Exception {
        // Tạo phiên chỉ còn 60 giây
        LocalDateTime nearEnd = LocalDateTime.now().plusSeconds(60);
        AuctionSession session = new AuctionSession(
                "S-SNIPE", "iPhone", 500.0, 10.0, nearEnd
        );
        session.start();

        LocalDateTime before = session.getEndTime();
        session.placeBid(new Bid("B001", "sniper", 520.0));
        LocalDateTime after  = session.getEndTime();

        assertTrue(after.isAfter(before),
                "endTime phải được gia hạn sau snipe bid");
    }

    @Test
    @DisplayName("Đặt bid sớm — phiên KHÔNG gia hạn")
    void auctionSession_BidEarly_ShouldNotExtendTime() throws Exception {
        // Tạo phiên còn nhiều giờ
        LocalDateTime farEnd = LocalDateTime.now().plusHours(2);
        AuctionSession session = new AuctionSession(
                "S-EARLY", "iPad", 500.0, 10.0, farEnd
        );
        session.start();

        LocalDateTime before = session.getEndTime();
        session.placeBid(new Bid("B001", "bidder1", 520.0));
        LocalDateTime after  = session.getEndTime();

        assertEquals(before, after,
                "endTime không được thay đổi khi bid sớm");
    }

    // ===== Config =====

    @Test
    @DisplayName("getThresholdSeconds — trả về đúng giá trị cấu hình")
    void getThresholdSeconds_ShouldReturnConfiguredValue() {
        assertEquals(SNIPE_THRESHOLD_SECONDS, antiSnipingService.getThresholdSeconds());
    }

    @Test
    @DisplayName("getExtensionSeconds — trả về đúng giá trị cấu hình")
    void getExtensionSeconds_ShouldReturnConfiguredValue() {
        assertEquals(EXTENSION_SECONDS, antiSnipingService.getExtensionSeconds());
    }

    @Test
    @DisplayName("Khởi tạo với threshold âm — throw IllegalArgumentException")
    void constructor_NegativeThreshold_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> new AntiSnipingService(-1, EXTENSION_SECONDS));
    }

    @Test
    @DisplayName("Khởi tạo với extension âm — throw IllegalArgumentException")
    void constructor_NegativeExtension_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> new AntiSnipingService(SNIPE_THRESHOLD_SECONDS, -1));
    }
}
