package org.example.strategy;

import org.example.auction.Bid;
import org.example.auction.AuctionSession;

/**
 * Strategy — cho phép thay đổi cách đặt giá linh hoạt.
 */
public interface BidStrategy {
    /**
     * Tính toán và thực hiện bid.
     * @return Bid được đặt, hoặc null nếu bỏ qua.
     */
    Bid execute(AuctionSession session, String bidderId);
}
