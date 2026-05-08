package com.auction.model;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.DataException;
import com.auction.exception.InvalidBidException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionSession {
    private String        sessionId;
    private String        itemName;
    private double        startingBid;
    private double        currentHighestBid;
    private String        currentHighestBidder;
    private boolean       isClosed;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Bid>     bidHistory;

    public AuctionSession(String sessionId, String itemName, double startingBid)
            throws DataException {

        // Validate sessionId
        if (sessionId == null || sessionId.isBlank())
            throw new DataException(
                "Session ID không được để trống", "sessionId", sessionId);

        // Validate itemName
        if (itemName == null || itemName.isBlank())
            throw new DataException(
                "Tên sản phẩm không được để trống", "itemName", itemName);

        // Validate startingBid
        if (startingBid < 0)
            throw new DataException(
                "Giá khởi điểm không được âm", "startingBid", startingBid);

        this.sessionId            = sessionId;
        this.itemName             = itemName;
        this.startingBid          = startingBid;
        this.currentHighestBid    = startingBid;
        this.currentHighestBidder = null;
        this.isClosed             = false;
        this.startTime            = LocalDateTime.now();
        this.bidHistory           = new ArrayList<>();
    }

    // ── Đặt giá ──────────────────────────────────────────
    public void placeBid(String bidderId, double bidAmount)
            throws AuctionClosedException, InvalidBidException, DataException {

        // 1. Kiểm tra phiên còn mở không
        if (isClosed)
            throw new AuctionClosedException(
                "Phiên đấu giá đã kết thúc!", sessionId);

        // 2. Kiểm tra bidderId hợp lệ
        if (bidderId == null || bidderId.isBlank())
            throw new DataException(
                "Bidder ID không được để trống", "bidderId", bidderId);

        // 3. Kiểm tra giá bid hợp lệ
        if (bidAmount <= 0)
            throw new InvalidBidException(
                "Giá đặt phải lớn hơn 0!",
                bidAmount, currentHighestBid);

        if (bidAmount <= currentHighestBid)
            throw new InvalidBidException(
                "Giá phải cao hơn giá hiện tại: " + currentHighestBid,
                bidAmount, currentHighestBid);

        // 4. Tạo Bid mới và lưu vào lịch sử
        Bid newBid = new Bid(
            "BID-" + (bidHistory.size() + 1),
            bidderId,
            bidAmount
        );
        bidHistory.add(newBid);

        // 5. Cập nhật giá cao nhất
        this.currentHighestBid    = bidAmount;
        this.currentHighestBidder = bidderId;

        System.out.println("Bid hợp lệ! Người bid: " + bidderId
            + " | Giá mới: " + bidAmount);
    }

    // ── Đóng phiên ───────────────────────────────────────
    public void closeSession() throws AuctionClosedException {
        if (isClosed)
            throw new AuctionClosedException(
                "Phiên đã đóng trước đó!", sessionId);

        this.isClosed = true;
        this.endTime  = LocalDateTime.now();

        System.out.println("Phiên " + sessionId + " đã kết thúc!");
        System.out.println("Người thắng: " + currentHighestBidder
            + " | Giá: " + currentHighestBid);
    }

    // ── Getters ───────────────────────────────────────────
    public String        getSessionId()            { return sessionId;            }
    public String        getItemName()             { return itemName;             }
    public double        getStartingBid()          { return startingBid;          }
    public double        getCurrentHighestBid()    { return currentHighestBid;    }
    public String        getCurrentHighestBidder() { return currentHighestBidder; }
    public boolean       isClosed()                { return isClosed;             }
    public LocalDateTime getStartTime()            { return startTime;            }
    public LocalDateTime getEndTime()              { return endTime;              }
    public List<Bid>     getBidHistory()           { return bidHistory;           }

    @Override
    public String toString() {
        return "AuctionSession{" +
                "sessionId='"             + sessionId            + '\'' +
                ", itemName='"            + itemName             + '\'' +
                ", currentHighestBid="    + currentHighestBid    +
                ", currentHighestBidder=" + currentHighestBidder +
                ", isClosed="             + isClosed             +
                '}';
    }
}
