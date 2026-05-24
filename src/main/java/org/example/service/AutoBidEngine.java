package org.example.service;

import org.example.auction.AuctionSession;
import org.example.exception.InvalidBidException;
import org.example.auction.Bid;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AutoBidEngine — tự động đặt giá thay cho bidder đến khi đạt maxAmount.
 *
 * <p>Engine theo dõi từng bidder đã đăng ký và tự tăng giá theo increment
 * tối thiểu để luôn dẫn đầu, miễn là chưa vượt {@code maxAmount}.</p>
 */
public class AutoBidEngine {

    private final AuctionSession session;

    /** bidderId → maxAmount */
    private final Map<String, Double> registrations = new ConcurrentHashMap<>();

    /** Bộ đếm tạo bidId duy nhất */
    private int bidCounter = 0;

    /**
     * Khởi tạo engine gắn với một phiên đấu giá.
     *
     * @param session phiên cần theo dõi
     * @throws IllegalArgumentException nếu session null
     */
    public AutoBidEngine(AuctionSession session) {
        if (session == null) {
            throw new IllegalArgumentException("session không được null");
        }
        this.session = session;
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
            throw new InvalidBidException(maxAmount, session.getCurrentPrice() + session.getMinimumIncrement());
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
     * <p>Không đặt nếu bidder đang dẫn đầu. Dừng nếu bid tiếp theo
     * vượt maxAmount đã đăng ký.</p>
     *
     * @param bidderId ID bidder cần thực thi
     * @throws IllegalStateException    nếu bidderId chưa đăng ký
     * @throws Exception                nếu phiên đã đóng hoặc bid không hợp lệ
     */
    public void executeAutoBid(String bidderId) throws Exception {
        if (!registrations.containsKey(bidderId)) {
            throw new IllegalStateException("bidderId chưa đăng ký: " + bidderId);
        }

        // Đang dẫn đầu rồi — không cần bid thêm
        if (bidderId.equals(session.getLeadingBidderId())) {
            return;
        }

        double maxAmount = registrations.get(bidderId);
        double nextBid   = session.getCurrentPrice() + session.getMinimumIncrement();

        if (nextBid > maxAmount) {
            return; // vượt giới hạn — dừng lại
        }

        String bidId = "AUTO-" + bidderId + "-" + (++bidCounter);
        session.placeBid(new Bid(bidId, bidderId, nextBid));
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
}
