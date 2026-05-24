package org.example.service;

import org.example.auction.AuctionSession;
import org.example.auction.Bid;
import org.example.exception.InvalidBidException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AutoBidEngine — tự động đặt giá thay cho bidder đến khi đạt maxAmount.
 */
public class AutoBidEngine {

    private final AuctionSession session;
    private final double minimumIncrement;

    /** bidderId → maxAmount */
    private final Map<String, Double> registrations = new ConcurrentHashMap<>();

    /** Theo dõi ai đang dẫn đầu nội bộ */
    private String leadingBidderId = null;

    private int bidCounter = 0;

    /**
     * @param session          phiên đấu giá cần theo dõi
     * @param minimumIncrement mức tăng tối thiểu mỗi lần bid
     */
    public AutoBidEngine(AuctionSession session, double minimumIncrement) {
        if (session == null) {
            throw new IllegalArgumentException("session không được null");
        }
        if (minimumIncrement <= 0) {
            throw new IllegalArgumentException("minimumIncrement phải > 0");
        }
        this.session          = session;
        this.minimumIncrement = minimumIncrement;
    }

    /**
     * Đăng ký auto-bid cho một bidder.
     *
     * @param bidderId  ID người đặt giá
     * @param maxAmount giá tối đa sẵn sàng trả
     * @throws IllegalArgumentException nếu bidderId null/rỗng hoặc maxAmount âm
     * @throws InvalidBidException      nếu maxAmount <= currentPrice
     */
    public void register(String bidderId, double maxAmount) throws InvalidBidException {
        if (bidderId == null || bidderId.isBlank()) {
            throw new IllegalArgumentException("bidderId không được null/rỗng");
        }
        if (maxAmount < 0) {
            throw new IllegalArgumentException("maxAmount không được âm");
        }
        if (maxAmount <= session.getCurrentPrice()) {
            // Dùng String message vì InvalidBidException của dự án nhận String
            throw new InvalidBidException(
                    "maxAmount " + maxAmount
                    + " phải lớn hơn giá hiện tại " + session.getCurrentPrice());
        }
        registrations.put(bidderId, maxAmount);
    }

    /**
     * Hủy đăng ký auto-bid.
     *
     * @param bidderId ID cần hủy
     * @throws IllegalArgumentException nếu bidderId chưa đăng ký
     */
    public void unregister(String bidderId) {
        if (!registrations.containsKey(bidderId)) {
            throw new IllegalArgumentException("bidderId chưa đăng ký: " + bidderId);
        }
        registrations.remove(bidderId);
    }

    /**
     * Thực hiện auto-bid cho bidder nếu cần thiết.
     *
     * @param bidderId ID bidder cần thực thi
     * @throws IllegalStateException nếu bidderId chưa đăng ký
     * @throws Exception             nếu phiên đã đóng hoặc bid không hợp lệ
     */
    public void executeAutoBid(String bidderId) throws Exception {
        if (!registrations.containsKey(bidderId)) {
            throw new IllegalStateException("bidderId chưa đăng ký: " + bidderId);
        }

        // Đang dẫn đầu — không cần bid thêm
        if (bidderId.equals(leadingBidderId)) {
            return;
        }

        double maxAmount = registrations.get(bidderId);
        double nextBid   = session.getCurrentPrice() + minimumIncrement;

        if (nextBid > maxAmount) {
            return; // vượt giới hạn — dừng
        }

        String bidId = "AUTO-" + bidderId + "-" + (++bidCounter);
        boolean success = session.placeBid(new Bid(bidId, bidderId, nextBid));
        if (success) {
            leadingBidderId = bidderId;
        }
    }

    /**
     * Lấy maxAmount đã đăng ký của một bidder.
     *
     * @param bidderId ID cần tra
     * @return maxAmount
     * @throws IllegalArgumentException nếu chưa đăng ký
     */
    public double getMaxAmount(String bidderId) {
        if (!registrations.containsKey(bidderId)) {
            throw new IllegalArgumentException("bidderId chưa đăng ký: " + bidderId);
        }
        return registrations.get(bidderId);
    }

    /** @return ID bidder đang dẫn đầu theo engine theo dõi */
    public String getLeadingBidderId() {
        return leadingBidderId;
    }
}
