package org.example.auction;

import org.example.exception.InvalidBidException;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Engine xử lý Auto-Bidding.
 *
 * Logic:
 *  1. Khi có bid mới → duyệt tất cả AutoBidConfig còn lại
 *  2. Tìm config có maxBid cao nhất (tie → ưu tiên đăng ký trước)
 *  3. Đặt giá = currentPrice + increment (nếu không vượt maxBid)
 */
public class AutoBidEngine {

    private final List<AutoBidConfig> configs = new CopyOnWriteArrayList<>();

    public void register(AutoBidConfig config) {
        // Loại bỏ config cũ của cùng bidder nếu có
        configs.removeIf(c -> c.getBidderId().equals(config.getBidderId()));
        configs.add(config);
        System.out.println("[AutoBid] Đã đăng ký auto-bid cho: " + config.getBidderId());
    }

    public void unregister(String bidderId) {
        configs.removeIf(c -> c.getBidderId().equals(bidderId));
    }

    /**
     * Gọi sau mỗi bid hợp lệ. Trả về AutoBid thắng hoặc null nếu không ai bid được.
     */
    public Bid trigger(String sessionId, double currentPrice, String lastBidderId) {
        return configs.stream()
                // Không tự bid lại chính mình
                .filter(c -> !c.getBidderId().equals(lastBidderId))
                // Chỉ những ai còn đủ tiền để bid thêm
                .filter(c -> c.getMaxBid() >= currentPrice + c.getIncrement())
                // Ưu tiên maxBid cao nhất; nếu bằng → đăng ký trước
                .max(Comparator.comparingDouble(AutoBidConfig::getMaxBid)
                        .thenComparingLong(c -> -c.getRegisteredAt()))
                .map(winner -> {
                    double nextAmount = Math.min(
                            currentPrice + winner.getIncrement(),
                            winner.getMaxBid()
                    );
                    System.out.printf("[AutoBid] %s tự động đặt %.2f%n",
                            winner.getBidderId(), nextAmount);
                    return new Bid(
                            java.util.UUID.randomUUID().toString(),
                            winner.getBidderId(),
                            nextAmount
                    );
                })
                .orElse(null);
    }
}
