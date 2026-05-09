package org.example.strategy;

import org.example.auction.AuctionSession;
import org.example.auction.Bid;
import java.util.UUID;

/** Đặt giá = currentPrice × hệ số (ví dụ 1.1 = +10%) */
public class AggressiveBidStrategy implements BidStrategy {

    private final double multiplier;

    public AggressiveBidStrategy(double multiplier) { this.multiplier = multiplier; }

    @Override
    public Bid execute(AuctionSession session, String bidderId) {
        double amount = session.getCurrentPrice() * multiplier;
        return new Bid(UUID.randomUUID().toString(), bidderId, amount);
    }
}
