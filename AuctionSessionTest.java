package com.auction;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.DataException;
import com.auction.exception.InvalidBidException;
import com.auction.model.AuctionSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionSessionTest {

    private AuctionSession session;

    @BeforeEach
    void setUp() throws DataException {
        session = new AuctionSession("S001", "Laptop Dell", 5000000);
    }

    // ── Test tạo phiên hợp lệ ────────────────────────────
    @Test
    void testCreateSession_Success() throws DataException {
        AuctionSession s = new AuctionSession("S002", "iPhone 15", 10000000);
        assertNotNull(s);
        assertEquals("S002", s.getSessionId());
        assertEquals("iPhone 15", s.getItemName());
        assertEquals(10000000, s.getStartingBid());
        assertFalse(s.isClosed());
    }

    // ── Test tạo phiên với sessionId rỗng ────────────────
    @Test
    void testCreateSession_EmptySessionId() {
        DataException ex = assertThrows(DataException.class, () ->
            new AuctionSession("", "Laptop", 5000000));
        assertEquals("sessionId", ex.getField());
        System.out.println("✔ Test sessionId rỗng: " + ex.getMessage());
    }

    // ── Test tạo phiên với itemName rỗng ─────────────────
    @Test
    void testCreateSession_EmptyItemName() {
        DataException ex = assertThrows(DataException.class, () ->
            new AuctionSession("S003", "", 5000000));
        assertEquals("itemName", ex.getField());
        System.out.println("✔ Test itemName rỗng: " + ex.getMessage());
    }

    // ── Test tạo phiên với giá âm ────────────────────────
    @Test
    void testCreateSession_NegativeStartingBid() {
        DataException ex = assertThrows(DataException.class, () ->
            new AuctionSession("S004", "Laptop", -1000));
        assertEquals("startingBid", ex.getField());
        System.out.println("✔ Test giá khởi điểm âm: " + ex.getMessage());
    }

    // ── Test đặt giá hợp lệ ──────────────────────────────
    @Test
    void testPlaceBid_Success()
            throws AuctionClosedException, InvalidBidException, DataException {
        session.placeBid("bidder1", 6000000);
        assertEquals(6000000, session.getCurrentHighestBid());
        assertEquals("bidder1", session.getCurrentHighestBidder());
        assertEquals(1, session.getBidHistory().size());
        System.out.println("✔ Test đặt giá hợp lệ thành công");
    }

    // ── Test đặt giá nhiều lần ───────────────────────────
    @Test
    void testPlaceBid_MultipleBids()
            throws AuctionClosedException, InvalidBidException, DataException {
        session.placeBid("bidder1", 6000000);
        session.placeBid("bidder2", 7000000);
        session.placeBid("bidder3", 8000000);

        assertEquals(8000000, session.getCurrentHighestBid());
        assertEquals("bidder3", session.getCurrentHighestBidder());
        assertEquals(3, session.getBidHistory().size());
        System.out.println("✔ Test đặt giá nhiều lần thành công");
    }

    // ── Test đặt giá thấp hơn giá hiện tại ──────────────
    @Test
    void testPlaceBid_LowerThanCurrentBid() {
        InvalidBidException ex = assertThrows(InvalidBidException.class, () ->
            session.placeBid("bidder1", 3000000));
        assertEquals(3000000, ex.getAttemptedBid());
        assertEquals(5000000, ex.getCurrentHighestBid());
        System.out.println("✔ Test giá thấp hơn hiện tại: " + ex.getMessage());
    }

    // ── Test đặt giá bằng giá hiện tại ──────────────────
    @Test
    void testPlaceBid_EqualToCurrentBid() {
        InvalidBidException ex = assertThrows(InvalidBidException.class, () ->
            session.placeBid("bidder1", 5000000));
        System.out.println("✔ Test giá bằng hiện tại: " + ex.getMessage());
    }

    // ── Test đặt giá = 0 ─────────────────────────────────
    @Test
    void testPlaceBid_ZeroAmount() {
        InvalidBidException ex = assertThrows(InvalidBidException.class, () ->
            session.placeBid("bidder1", 0));
        System.out.println("✔ Test giá = 0: " + ex.getMessage());
    }

    // ── Test đặt giá âm ──────────────────────────────────
    @Test
    void testPlaceBid_NegativeAmount() {
        InvalidBidException ex = assertThrows(InvalidBidException.class, () ->
            session.placeBid("bidder1", -5000));
        System.out.println("✔ Test giá âm: " + ex.getMessage());
    }

    // ── Test đặt giá với bidderId rỗng ───────────────────
    @Test
    void testPlaceBid_EmptyBidderId() {
        DataException ex = assertThrows(DataException.class, () ->
            session.placeBid("", 6000000));
        assertEquals("bidderId", ex.getField());
        System.out.println("✔ Test bidderId rỗng: " + ex.getMessage());
    }

    // ── Test đóng phiên thành công ───────────────────────
    @Test
    void testCloseSession_Success() throws AuctionClosedException {
        session.closeSession();
        assertTrue(session.isClosed());
        assertNotNull(session.getEndTime());
        System.out.println("✔ Test đóng phiên thành công");
    }

    // ── Test đóng phiên đã đóng ──────────────────────────
    @Test
    void testCloseSession_AlreadyClosed() throws AuctionClosedException {
        session.closeSession();
        AuctionClosedException ex = assertThrows(AuctionClosedException.class,
            () -> session.closeSession());
        assertEquals("S001", ex.getSessionId());
        System.out.println("✔ Test đóng phiên đã đóng: " + ex.getMessage());
    }

    // ── Test đặt giá vào phiên đã đóng ──────────────────
    @Test
    void testPlaceBid_OnClosedSession() throws AuctionClosedException {
        session.closeSession();
        AuctionClosedException ex = assertThrows(AuctionClosedException.class,
            () -> session.placeBid("bidder1", 6000000));
        assertEquals("S001", ex.getSessionId());
        System.out.println("✔ Test bid vào phiên đã đóng: " + ex.getMessage());
    }
}