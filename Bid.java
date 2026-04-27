package org.example.auction;

import java.time.LocalDateTime;

public class Bid {
    private String bidId;
    private String bidderId;
    private double amount;
    private LocalDateTime timestamp;

    public Bid(String bidId, String bidderId, double amount) {
        if (bidId == null || bidId.isBlank()) throw new IllegalArgumentException("BidId không được rỗng");
        if (bidderId == null || bidderId.isBlank()) throw new IllegalArgumentException("BidderId không được rỗng");
        if (amount <= 0) throw new IllegalArgumentException("Số tiền phải lớn hơn 0");

        this.bidId = bidId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public String getBidId()     { return bidId; }
    public String getBidderId()  { return bidderId; }
    public double getAmount()    { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("Bid{id='%s', bidderId='%s', amount=%.2f, time=%s}",
                bidId, bidderId, amount, timestamp);
    }
}