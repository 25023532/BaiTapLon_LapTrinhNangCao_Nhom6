package org.example.auction;

import org.example.exception.AuctionClosedException;
import org.example.exception.InvalidBidException;
import org.example.service.AutoBidEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AutoBidEngine: đặt giá tự động cho bidder đến khi đạt maxAmount.
 * Engine tự tăng giá theo increment tối thiểu để luôn dẫn đầu.
 */
@DisplayName("AutoBidEngine Tests")
class AutoBidEngineTest {

    private AuctionSession session;
    private AutoBidEngine engine;

    @BeforeEach
    void setUp() {
        session = new AuctionSession(
                "S-AUTO", "Gaming Laptop", 1000.0, 50.0,
                LocalDateTime.now().plusHours(1)
        );
        session.start();
        engine = new AutoBidEngine(session, 50.0);
    }

    // ===== Register AutoBid =====

    @Test
    @DisplayName("Đăng ký auto-bid hợp lệ — không throw")
    void register_ValidParams_ShouldNotThrow() {
        assertDoesNotThrow(() -> engine.register("bidder1", 2000.0));
    }

    @Test
    @DisplayName("Đăng ký auto-bid với maxAmount < currentPrice — throw InvalidBidException")
    void register_MaxAmountBelowCurrent_ShouldThrow() {
        assertThrows(InvalidBidException.class,
                () -> engine.register("bidder1", 800.0));
    }

    @Test
    @DisplayName("Đăng ký auto-bid với maxAmount = currentPrice — throw InvalidBidException")
    void register_MaxAmountEqualCurrent_ShouldThrow() {
        assertThrows(InvalidBidException.class,
                () -> engine.register("bidder1", 1000.0));
    }

    @Test
    @DisplayName("bidderId null — throw IllegalArgumentException")
    void register_NullBidderId_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.register(null, 2000.0));
    }

    @Test
    @DisplayName("maxAmount âm — throw IllegalArgumentException")
    void register_NegativeMaxAmount_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.register("bidder1", -100.0));
    }

    // ===== ExecuteAutoBid =====

    @Test
    @DisplayName("Auto-bid đặt giá tối thiểu vượt currentPrice")
    void executeAutoBid_ShouldPlaceMinimumViableBid() throws Exception {
        engine.register("auto1", 1500.0);
        engine.executeAutoBid("auto1");
        // currentPrice = 1000, increment = 50 → auto bid = 1050
        assertEquals(1050.0, session.getCurrentPrice());
    }

    @Test
    @DisplayName("Auto-bid khi đã dẫn đầu — không đặt thêm")
    void executeAutoBid_WhenAlreadyLeading_ShouldNotBid() throws Exception {
        engine.register("auto1", 2000.0);
        engine.executeAutoBid("auto1"); // 1050
        double priceAfterFirst = session.getCurrentPrice();

        engine.executeAutoBid("auto1"); // đang dẫn đầu, không cần bid thêm
        assertEquals(priceAfterFirst, session.getCurrentPrice());
    }

    @Test
    @DisplayName("Auto-bid khi người khác vượt — tự động tăng giá")
    void executeAutoBid_WhenOutbid_ShouldIncrease() throws Exception {
        engine.register("auto1", 2000.0);
        engine.executeAutoBid("auto1"); // auto1: 1050

        // bidder2 đặt 1200, vượt auto1
        session.placeBid(new Bid("B-MAN", "bidder2", 1200.0));

        engine.executeAutoBid("auto1"); // auto1 tự tăng lên 1250
        assertEquals(1250.0, session.getCurrentPrice());
        assertEquals("auto1", session.getLeadingBidderId());
    }

    @Test
    @DisplayName("Auto-bid khi maxAmount bị vượt — không đặt nữa")
    void executeAutoBid_ExceedsMaxAmount_ShouldStop() throws Exception {
        engine.register("auto1", 1100.0); // maxAmount = 1100

        // bidder2 đặt 1150, vượt maxAmount của auto1
        session.placeBid(new Bid("B-MAN", "bidder2", 1150.0));

        engine.executeAutoBid("auto1"); // không thể đặt vì vượt max
        // giá vẫn là 1150, do bidder2 dẫn đầu
        assertEquals(1150.0, session.getCurrentPrice());
        assertEquals("bidder2", session.getLeadingBidderId());
    }

    // ===== Multiple AutoBidders =====

    @Test
    @DisplayName("Hai auto-bidder — người có maxAmount cao hơn thắng")
    void multipleAutoBidders_HigherMaxWins() throws Exception {
        engine.register("auto1", 1300.0);
        engine.register("auto2", 1600.0);

        engine.executeAutoBid("auto1"); // 1050
        engine.executeAutoBid("auto2"); // 1100

        // Hai người thi nhau cho đến khi auto1 hết max
        engine.executeAutoBid("auto1"); // 1150
        engine.executeAutoBid("auto2"); // 1200
        engine.executeAutoBid("auto1"); // 1250
        engine.executeAutoBid("auto2"); // 1300
        engine.executeAutoBid("auto1"); // 1350 > max 1300 → dừng

        assertEquals("auto2", session.getLeadingBidderId());
    }

    // ===== Session State =====

    @Test
    @DisplayName("executeAutoBid khi phiên FINISHED — throw AuctionClosedException")
    void executeAutoBid_FinishedSession_ShouldThrow() throws Exception {
        engine.register("auto1", 2000.0);
        session.placeBid(new Bid("B001", "bidder1", 1100.0));
        session.finish();

        assertThrows(AuctionClosedException.class,
                () -> engine.executeAutoBid("auto1"));
    }

    @Test
    @DisplayName("executeAutoBid cho bidderId chưa đăng ký — throw IllegalStateException")
    void executeAutoBid_UnregisteredBidder_ShouldThrow() {
        assertThrows(IllegalStateException.class,
                () -> engine.executeAutoBid("ghost_bidder"));
    }

    // ===== Unregister =====

    @Test
    @DisplayName("Hủy đăng ký auto-bid thành công")
    void unregister_Registered_ShouldSucceed() throws Exception {
        engine.register("auto1", 2000.0);
        assertDoesNotThrow(() -> engine.unregister("auto1"));
    }

    @Test
    @DisplayName("Hủy đăng ký auto-bid chưa đăng ký — throw IllegalArgumentException")
    void unregister_NotRegistered_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.unregister("nobody"));
    }

    @Test
    @DisplayName("Sau khi unregister, executeAutoBid phải throw")
    void unregister_ThenExecute_ShouldThrow() throws Exception {
        engine.register("auto1", 2000.0);
        engine.unregister("auto1");

        assertThrows(IllegalStateException.class,
                () -> engine.executeAutoBid("auto1"));
    }

    // ===== GetMaxAmount =====

    @ParameterizedTest
    @CsvSource({
            "bidder1, 1500.0",
            "bidder2, 2000.0",
            "bidder3, 3000.0"
    })
    @DisplayName("getMaxAmount — trả về đúng giá trị đã đăng ký")
    void getMaxAmount_ShouldReturnRegisteredValue(
            String bidderId, double maxAmount) throws Exception {
        engine.register(bidderId, maxAmount);
        assertEquals(maxAmount, engine.getMaxAmount(bidderId));
    }
}
