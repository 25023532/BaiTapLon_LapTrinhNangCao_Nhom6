package org.example.strategy;

import org.example.auction.AuctionSession;
import org.example.auction.Bid;
import java.util.UUID;

/** Đặt giá = currentPrice + một bước cố định */
public class NormalBidStrategy implements BidStrategy {

    private final double step;

    public NormalBidStrategy(double step) { this.step = step; }

    @Override
    public Bid execute(AuctionSession session, String bidderId) {
        double amount = session.getCurrentPrice() + step;
        return new Bid(UUID.randomUUID().toString(), bidderId, amount);
    }
}
