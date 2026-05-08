package com.auction;

import com.auction.exception.DataException;
import com.auction.model.Bid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BidTest {

    // ── Test tạo Bid hợp lệ ──────────────────────────────
    @Test
    void testCreateBid_Success() throws DataException {
        Bid bid = new Bid("BID-001", "bidder1", 6000000);
        assertNotNull(bid);
        assertEquals("BID-001", bid.getBidId());
        assertEquals("bidder1", bid.getBidderId());
        assertEquals(6000000, bid.getAmount());
        assertNotNull(bid.getBidTime());
        System.out.println("✔ Test tạo Bid thành công: " + bid);
    }

    // ── Test tạo Bid với bidId rỗng ──────────────────────
    @Test
    void testCreateBid_EmptyBidId() {
        DataException ex = assertThrows(DataException.class, () ->
            new Bid("", "bidder1", 6000000));
        assertEquals("bidId", ex.getField());
        System.out.println("✔ Test bidId rỗng: " + ex.getMessage());
    }

    // ── Test tạo Bid với bidderId rỗng ───────────────────
    @Test
    void testCreateBid_EmptyBidderId() {
        DataException ex = assertThrows(DataException.class, () ->
            new Bid("BID-001", "", 6000000));
        assertEquals("bidderId", ex.getField());
        System.out.println("✔ Test bidderId rỗng: " + ex.getMessage());
    }

    // ── Test tạo Bid với amount = 0 ──────────────────────
    @Test
    void testCreateBid_ZeroAmount() {
        DataException ex = assertThrows(DataException.class, () ->
            new Bid("BID-001", "bidder1", 0));
        assertEquals("amount", ex.getField());
        System.out.println("✔ Test amount = 0: " + ex.getMessage());
    }

    // ── Test tạo Bid với amount âm ───────────────────────
    @Test
    void testCreateBid_NegativeAmount() {
        DataException ex = assertThrows(DataException.class, () ->
            new Bid("BID-001", "bidder1", -5000));
        assertEquals("amount", ex.getField());
        System.out.println("✔ Test amount âm: " + ex.getMessage());
    }

    // ── Test bidId null ───────────────────────────────────
    @Test
    void testCreateBid_NullBidId() {
        DataException ex = assertThrows(DataException.class, () ->
            new Bid(null, "bidder1", 6000000));
        assertEquals("bidId", ex.getField());
        System.out.println("✔ Test bidId null: " + ex.getMessage());
    }

    // ── Test bidderId null ────────────────────────────────
    @Test
    void testCreateBid_NullBidderId() {
        DataException ex = assertThrows(DataException.class, () ->
            new Bid("BID-001", null, 6000000));
        assertEquals("bidderId", ex.getField());
        System.out.println("✔ Test bidderId null: " + ex.getMessage());
    }
}
