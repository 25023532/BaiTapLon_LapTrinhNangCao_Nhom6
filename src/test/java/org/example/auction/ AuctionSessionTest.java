package org.example.auction;

import org.example.exception.AuctionClosedException;
import org.example.exception.InvalidBidException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuctionSession Tests")
class AuctionSessionTest {

    private AuctionSession session;

    @BeforeEach
    void setUp() {

        session = new AuctionSession(
                "S001",
                "MacBook Pro",
                1000.0,
                50.0,
                LocalDateTime.now().plusHours(2)
        );

        session.start();
    }

    // ===== Valid Bid =====

    @Test
    @DisplayName("Đặt giá hợp lệ — nên được chấp nhận")
    void placeBid_ValidAmount_ShouldAccept() throws Exception {

        Bid bid = new Bid("B001", "bidder1", 1100.0);

        assertTrue(session.placeBid(bid));

        assertEquals(1100.0, session.getCurrentPrice());
    }

    @ParameterizedTest
    @CsvSource({
            "1050, true",
            "1500, true",
            "2000, true"
    })
    @DisplayName("Các mức giá hợp lệ khác nhau")
    void placeBid_VariousValidAmounts(
            double amount,
            boolean expected
    ) throws Exception {

        Bid bid = new Bid("B001", "bidder1", amount);

        assertEquals(expected, session.placeBid(bid));
    }

    // ===== Invalid Bid =====

    @Test
    @DisplayName("Đặt giá thấp hơn tối thiểu — nên throw InvalidBidException")
    void placeBid_BelowMinimum_ShouldThrow() {

        Bid bid = new Bid("B002", "bidder2", 900.0);

        InvalidBidException ex = assertThrows(
                InvalidBidException.class,
                () -> session.placeBid(bid)
        );

        assertEquals(900.0, ex.getAttemptedAmount());

        assertEquals(1050.0, ex.getMinimumRequired());
    }

    @Test
    @DisplayName("Đặt giá bằng currentPrice — nên throw InvalidBidException")
    void placeBid_ExactCurrentPrice_ShouldThrow() {

        Bid bid = new Bid("B003", "bidder3", 1000.0);

        assertThrows(
                InvalidBidException.class,
                () -> session.placeBid(bid)
        );
    }

    // ===== Closed Session =====

    @Test
    @DisplayName("Đặt giá khi phiên FINISHED")
    void placeBid_AfterFinish_ShouldThrow() throws Exception {

        session.placeBid(
                new Bid("B004", "bidder4", 1200.0)
        );

        session.finish();

        AuctionClosedException ex = assertThrows(
                AuctionClosedException.class,
                () -> session.placeBid(
                        new Bid("B005", "bidder5", 1500.0)
                )
        );

        assertEquals("S001", ex.getSessionId());

        assertEquals("FINISHED", ex.getCurrentStatus());
    }

    @Test
    @DisplayName("Đặt giá khi phiên CANCELED")
    void placeBid_AfterCancel_ShouldThrow() {

        session.cancel();

        assertThrows(
                AuctionClosedException.class,
                () -> session.placeBid(
                        new Bid("B006", "bidder6", 1200.0)
                )
        );
    }

    // ===== Finish Session =====

    @Test
    @DisplayName("Kết thúc có bid → FINISHED")
    void finish_WithBids_ShouldBeFinished() throws Exception {

        session.placeBid(
                new Bid("B007", "bidder7", 1200.0)
        );

        session.finish();

        assertEquals(
                AuctionStatus.FINISHED,
                session.getStatus()
        );
    }

    @Test
    @DisplayName("Kết thúc không có bid → CANCELED")
    void finish_NoBids_ShouldBeCanceled() {

        session.finish();

        assertEquals(
                AuctionStatus.CANCELED,
                session.getStatus()
        );
    }

    // ===== Full Flow =====

    @Test
    @DisplayName("OPEN → RUNNING → FINISHED → PAID")
    void stateTransition_FullFlow_ShouldSucceed()
            throws Exception {

        session.placeBid(
                new Bid("B008", "bidder8", 1500.0)
        );

        session.finish();

        assertEquals(
                AuctionStatus.FINISHED,
                session.getStatus()
        );

        session.markPaid();

        assertEquals(
                AuctionStatus.PAID,
                session.getStatus()
        );
    }
}
