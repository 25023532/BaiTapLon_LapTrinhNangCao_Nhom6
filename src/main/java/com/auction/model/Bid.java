package com.auction.model;

import com.auction.exception.DataException;
import java.time.LocalDateTime;

public class Bid {
    private String bidId;
    private String bidderId;
    private double amount;
    private LocalDateTime bidTime;

    public Bid(String bidId, String bidderId, double amount) throws DataException {
        // Validate bidId
        if (bidId == null || bidId.isBlank())
            throw new DataException("Bid ID không được để trống", "bidId", bidId);

        // Validate bidderId
        if (bidderId == null || bidderId.isBlank())
            throw new DataException("Bidder ID không được để trống", "bidderId", bidderId);

        // Validate amount
        if (amount <= 0)
            throw new DataException("Giá bid phải lớn hơn 0", "amount", amount);

        this.bidId    = bidId;
        this.bidderId = bidderId;
        this.amount   = amount;
        this.bidTime  = LocalDateTime.now();
    }

    // ── Getters ───────────────────────────────────────────
    public String        getBidId()    { return bidId;    }
    public String        getBidderId() { return bidderId; }
    public double        getAmount()   { return amount;   }
    public LocalDateTime getBidTime()  { return bidTime;  }

    @Override
    public String toString() {
        return "Bid{" +
                "bidId='"    + bidId    + '\'' +
                ", bidderId='" + bidderId + '\'' +
                ", amount="    + amount   +
                ", bidTime="   + bidTime  +
                '}';
    }
}
