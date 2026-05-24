package org.example.auction;
import org.example.exception.DataException;  // ✅ Fix import 1
// Bid đã cùng package org.example.auction nên không cần import
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class BidTest {
    // ── Test tạo Bid hợp lệ ──────────────────────────────
    @Test
    void testCreateBid_Success() {
        Bid bid = new Bid("BID-001", "bidder1", 6000000);
        assertNotNull(bid);
        assertEquals("BID-001", bid.getBidId());
        assertEquals("bidder1", bid.getBidderId());
        assertEquals(6000000, bid.getAmount());
        assertNotNull(bid.getTimestamp());          // ✅ Fix: getBidTime() → getTimestamp()
        System.out.println("✔ Test tạo Bid thành công: " + bid);
    }
    // ── Test tạo Bid với bidId rỗng ──────────────────────
    @Test
    void testCreateBid_EmptyBidId() {
        assertThrows(IllegalArgumentException.class, () ->
            new Bid("", "bidder1", 6000000));      // ✅ Fix: dùng IllegalArgumentException
        System.out.println("✔ Test bidId rỗng OK");
    }
    // ── Test tạo Bid với bidderId rỗng ───────────────────
    @Test
    void testCreateBid_EmptyBidderId() {
        assertThrows(IllegalArgumentException.class, () ->
            new Bid("BID-001", "", 6000000));
        System.out.println("✔ Test bidderId rỗng OK");
    }
    // ── Test tạo Bid với amount = 0 ──────────────────────
    @Test
    void testCreateBid_ZeroAmount() {
        assertThrows(IllegalArgumentException.class, () ->
            new Bid("BID-001", "bidder1", 0));
        System.out.println("✔ Test amount = 0 OK");
    }
    // ── Test tạo Bid với amount âm ───────────────────────
    @Test
    void testCreateBid_NegativeAmount() {
        assertThrows(IllegalArgumentException.class, () ->
            new Bid("BID-001", "bidder1", -5000));
        System.out.println("✔ Test amount âm OK");
    }
    // ── Test bidId null ───────────────────────────────────
    @Test
    void testCreateBid_NullBidId() {
        assertThrows(IllegalArgumentException.class, () ->
            new Bid(null, "bidder1", 6000000));
        System.out.println("✔ Test bidId null OK");
    }
    // ── Test bidderId null ────────────────────────────────
    @Test
    void testCreateBid_NullBidderId() {
        assertThrows(IllegalArgumentException.class, () ->
            new Bid("BID-001", null, 6000000));
        System.out.println("✔ Test bidderId null OK");
    }
}
