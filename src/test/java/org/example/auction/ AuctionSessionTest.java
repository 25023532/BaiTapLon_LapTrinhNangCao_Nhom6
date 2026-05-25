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
    void placeBid_VariousValidAmounts(double amount, boolean expected) throws Exception {
        Bid bid = new Bid("B001", "bidder1", amount);
        assertEquals(expected, session.placeBid(bid));
    }

    @Test
    @DisplayName("Đặt nhiều giá liên tiếp — currentPrice phải tăng dần")
    void placeBid_MultipleBids_PriceShouldIncrease() throws Exception {
        session.placeBid(new Bid("B010", "bidder1", 1100.0));
        session.placeBid(new Bid("B011", "bidder2", 1300.0));
        session.placeBid(new Bid("B012", "bidder3", 1600.0));
        assertEquals(1600.0, session.getCurrentPrice());
    }

    @Test
    @DisplayName("Bid thắng cuối cùng phải là người đặt cao nhất")
    void placeBid_LastWinner_ShouldBeHighestBidder() throws Exception {
        session.placeBid(new Bid("B013", "bidder1", 1100.0));
        session.placeBid(new Bid("B014", "bidder2", 1500.0));
        session.finish();
        assertEquals("bidder2", session.getWinnerBidderId());
    }

    @Test
    @DisplayName("Giá đặt bằng đúng minimumIncrement so với currentPrice — hợp lệ")
    void placeBid_ExactMinIncrement_ShouldAccept() throws Exception {
        // startingPrice=1000, increment=50 => min valid = 1050
        Bid bid = new Bid("B015", "bidder1", 1050.0);
        assertTrue(session.placeBid(bid));
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
        assertThrows(InvalidBidException.class, () -> session.placeBid(bid));
    }

    @Test
    @DisplayName("Bid âm — nên throw IllegalArgumentException")
    void placeBid_NegativeAmount_ShouldThrow() {
        // Bid validates amount before the session receives it.
        assertThrows(IllegalArgumentException.class,
                () -> new Bid("B016", "bidder1", -500.0));
    }

    @Test
    @DisplayName("Bid bằng 0 — nên throw IllegalArgumentException")
    void placeBid_ZeroAmount_ShouldThrow() {
        // Bid validates amount before the session receives it.
        assertThrows(IllegalArgumentException.class,
                () -> new Bid("B017", "bidder1", 0.0));
    }

    @Test
    @DisplayName("Bid null — nên throw IllegalArgumentException")
    void placeBid_NullBid_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> session.placeBid(null));
    }

    // ===== Closed Session =====

    @Test
    @DisplayName("Đặt giá khi phiên FINISHED")
    void placeBid_AfterFinish_ShouldThrow() throws Exception {
        session.placeBid(new Bid("B004", "bidder4", 1200.0));
        session.finish();
        AuctionClosedException ex = assertThrows(
                AuctionClosedException.class,
                () -> session.placeBid(new Bid("B005", "bidder5", 1500.0))
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
                () -> session.placeBid(new Bid("B006", "bidder6", 1200.0))
        );
    }

    @Test
    @DisplayName("Đặt giá khi phiên PAID — nên throw AuctionClosedException")
    void placeBid_AfterPaid_ShouldThrow() throws Exception {
        session.placeBid(new Bid("B018", "bidder1", 1200.0));
        session.finish();
        session.markPaid();
        assertThrows(
                AuctionClosedException.class,
                () -> session.placeBid(new Bid("B019", "bidder2", 1500.0))
        );
    }

    // ===== Finish Session =====

    @Test
    @DisplayName("Kết thúc có bid → FINISHED")
    void finish_WithBids_ShouldBeFinished() throws Exception {
        session.placeBid(new Bid("B007", "bidder7", 1200.0));
        session.finish();
        assertEquals(AuctionStatus.FINISHED, session.getStatus());
    }

    @Test
    @DisplayName("Kết thúc không có bid → CANCELED")
    void finish_NoBids_ShouldBeCanceled() {
        session.finish();
        assertEquals(AuctionStatus.CANCELED, session.getStatus());
    }

    // ===== Cancel Session =====

    @Test
    @DisplayName("Cancel phiên đang RUNNING → CANCELED")
    void cancel_RunningSession_ShouldBeCanceled() {
        session.cancel();
        assertEquals(AuctionStatus.CANCELED, session.getStatus());
    }

    @Test
    @DisplayName("Cancel phiên đã FINISHED — nên throw IllegalStateException")
    void cancel_FinishedSession_ShouldThrow() throws Exception {
        session.placeBid(new Bid("B020", "bidder1", 1100.0));
        session.finish();
        assertThrows(IllegalStateException.class, () -> session.cancel());
    }

    // ===== MarkPaid =====

    @Test
    @DisplayName("markPaid từ FINISHED → PAID")
    void markPaid_FromFinished_ShouldBePaid() throws Exception {
        session.placeBid(new Bid("B021", "bidder1", 1200.0));
        session.finish();
        session.markPaid();
        assertEquals(AuctionStatus.PAID, session.getStatus());
    }

    @Test
    @DisplayName("markPaid khi chưa FINISHED — nên throw IllegalStateException")
    void markPaid_NotFinished_ShouldThrow() {
        assertThrows(IllegalStateException.class, () -> session.markPaid());
    }

    // ===== Session Info =====

    @Test
    @DisplayName("getSessionId trả về đúng ID")
    void getSessionId_ShouldReturnCorrectId() {
        assertEquals("S001", session.getSessionId());
    }

    @Test
    @DisplayName("getProductName trả về đúng tên sản phẩm")
    void getProductName_ShouldReturnCorrectName() {
        assertEquals("MacBook Pro", session.getProductName());
    }

    @Test
    @DisplayName("getStartingPrice trả về đúng giá khởi điểm")
    void getStartingPrice_ShouldReturnCorrectPrice() {
        assertEquals(1000.0, session.getStartingPrice());
    }

    // ===== Full Flow =====

    @Test
    @DisplayName("OPEN → RUNNING → FINISHED → PAID")
    void stateTransition_FullFlow_ShouldSucceed() throws Exception {
        session.placeBid(new Bid("B008", "bidder8", 1500.0));
        session.finish();
        assertEquals(AuctionStatus.FINISHED, session.getStatus());
        session.markPaid();
        assertEquals(AuctionStatus.PAID, session.getStatus());
    }

    @Test
    @DisplayName("Nhiều bidder cạnh tranh — người thắng là bidder trả cao nhất")
    void fullFlow_MultipleCompetingBidders_CorrectWinner() throws Exception {
        session.placeBid(new Bid("B022", "alice", 1100.0));
        session.placeBid(new Bid("B023", "bob", 1200.0));
        session.placeBid(new Bid("B024", "alice", 1400.0));
        assertThrows(InvalidBidException.class,
                () -> session.placeBid(new Bid("B025", "charlie", 1350.0)));
        session.finish();
        assertEquals("alice", session.getWinnerBidderId());
        assertEquals(1400.0, session.getCurrentPrice());
    }
}
