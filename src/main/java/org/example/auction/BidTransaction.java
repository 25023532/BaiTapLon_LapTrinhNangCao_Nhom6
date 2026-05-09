package org.example.auction;

import org.example.core.Entity;
import java.time.LocalDateTime;

/**
 * Ghi lại mỗi lần đặt giá hợp lệ — immutable sau khi tạo.
 */
public class BidTransaction extends Entity {
    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final String bidderId;
    private final double amount;
    private final LocalDateTime placedAt;

    public BidTransaction(String sessionId, String bidderId, double amount) {
        super();  // auto UUID
        this.sessionId = sessionId;
        this.bidderId  = bidderId;
        this.amount    = amount;
        this.placedAt  = LocalDateTime.now();
    }

    public String        getSessionId() { return sessionId; }
    public String        getBidderId()  { return bidderId; }
    public double        getAmount()    { return amount; }
    public LocalDateTime getPlacedAt()  { return placedAt; }

    @Override
    public void printInfo() {
        System.out.printf("[BidTransaction] id=%s | session=%s | bidder=%s | amount=%.2f | at=%s%n",
                id, sessionId, bidderId, amount, placedAt);
    }
}
