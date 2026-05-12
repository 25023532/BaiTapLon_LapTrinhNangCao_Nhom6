package org.example.auction;
import org.example.exception.AuctionClosedException;
import org.example.exception.InvalidBidException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionSession {

    private final String sessionId;
    private final String itemName;
    private final double startingPrice;
    private final double minBidStep;
    private LocalDateTime endTime;          // BỎ final để gia hạn được

    private AuctionStatus status;
    private double currentPrice;
    private Bid highestBid;

    private final List<Bid> bidHistory = new ArrayList<>();
    private final List<Observer> observers = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    // ── Anti-sniping config ───────────────────────────────────
    private static final long TRIGGER_SECONDS   = 30; // X: bid trong 30s cuối → gia hạn
    private static final long EXTENSION_SECONDS = 70; // Y: kéo dài thêm 70s (ví dụ: 19:59:50 → 20:01:00)
    private static final int  MAX_EXTENSIONS    = 5;  // tối đa 5 lần
    private int     extensionCount              = 0;
    private boolean lastBidTriggeredExtension   = false;
    // ─────────────────────────────────────────────────────────

    public AuctionSession(String sessionId, String itemName,
                          double startingPrice, double minBidStep,
                          LocalDateTime endTime) {
        if (sessionId == null || sessionId.isBlank()) throw new IllegalArgumentException("SessionId không được rỗng");
        if (itemName == null || itemName.isBlank())   throw new IllegalArgumentException("ItemName không được rỗng");
        if (startingPrice <= 0) throw new IllegalArgumentException("Giá khởi điểm phải > 0");
        if (minBidStep <= 0)    throw new IllegalArgumentException("Bước giá phải > 0");
        if (endTime == null || endTime.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Thời gian kết thúc không hợp lệ");

        this.sessionId     = sessionId;
        this.itemName      = itemName;
        this.startingPrice = startingPrice;
        this.currentPrice  = startingPrice;
        this.minBidStep    = minBidStep;
        this.endTime       = endTime;
        this.status        = AuctionStatus.OPEN;
    }

    // ── Observer ──────────────────────────────────────────────
    public void addObserver(Observer observer)    { observers.add(observer); }
    public void removeObserver(Observer observer) { observers.remove(observer); }

    private void notifyObservers(Bid bid) {
        for (Observer o : observers) o.update(bid);
    }

    // ── State transitions ─────────────────────────────────────
    public void start() {
        lock.lock();
        try {
            if (status != AuctionStatus.OPEN)
                throw new IllegalStateException("Chỉ có thể bắt đầu phiên ở trạng thái OPEN");
            status = AuctionStatus.RUNNING;
            System.out.println("Phiên đấu giá [" + sessionId + "] đã bắt đầu.");
        } finally { lock.unlock(); }
    }

    public void finish() {
        lock.lock();
        try {
            if (status != AuctionStatus.RUNNING)
                throw new IllegalStateException("Chỉ có thể kết thúc phiên đang RUNNING");
            if (highestBid != null) {
                status = AuctionStatus.FINISHED;
                System.out.printf("Phiên [%s] kết thúc. Người thắng: %s với giá %.2f%n",
                        sessionId, highestBid.getBidderId(), highestBid.getAmount());
            } else {
                status = AuctionStatus.CANCELED;
                System.out.println("Phiên [" + sessionId + "] bị hủy do không có bid nào.");
            }
        } finally { lock.unlock(); }
    }

    public void markPaid() {
        lock.lock();
        try {
            if (status != AuctionStatus.FINISHED)
                throw new IllegalStateException("Chỉ có thể thanh toán khi phiên FINISHED");
            status = AuctionStatus.PAID;
            System.out.println("Phiên [" + sessionId + "] đã được thanh toán.");
        } finally { lock.unlock(); }
    }

    public void cancel() {
        lock.lock();
        try {
            if (status == AuctionStatus.PAID)
                throw new IllegalStateException("Không thể hủy phiên đã thanh toán");
            status = AuctionStatus.CANCELED;
            System.out.println("Phiên [" + sessionId + "] đã bị hủy thủ công.");
        } finally { lock.unlock(); }
    }

    // ── placeBid + Anti-sniping ───────────────────────────────
    public boolean placeBid(Bid bid) throws AuctionClosedException, InvalidBidException {
        lock.lock();
        try {
            lastBidTriggeredExtension = false; // reset mỗi lần bid

            // Kiểm tra trạng thái
            if (status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException(
                        "Phiên [" + sessionId + "] không ở trạng thái RUNNING",
                        sessionId, status.name());
            }

            // Kiểm tra hết giờ
            if (LocalDateTime.now().isAfter(endTime)) {
                finish();
                throw new AuctionClosedException(
                        "Phiên [" + sessionId + "] đã hết thời gian",
                        sessionId, status.name());
            }

            // Kiểm tra số tiền
            double minRequired = currentPrice + minBidStep;
            if (bid.getAmount() < minRequired) {
                throw new InvalidBidException(
                        String.format("Bid %.2f không hợp lệ, phải >= %.2f",
                                bid.getAmount(), minRequired),
                        bid.getAmount(), minRequired);
            }

            currentPrice = bid.getAmount();
            highestBid   = bid;
            bidHistory.add(bid);

            System.out.printf("Bid được chấp nhận: %s đặt %.2f%n",
                    bid.getBidderId(), bid.getAmount());

            // ── Anti-sniping: tự động gia hạn nếu bid vào phút chót ──
            applyAntiSniping();
            // ─────────────────────────────────────────────────────────

            notifyObservers(bid);
            return true;

        } finally { lock.unlock(); }
    }

    /**
     * Nếu bid xảy ra trong TRIGGER_SECONDS giây cuối
     * → tự động kéo dài thêm EXTENSION_SECONDS giây.
     *
     * Ví dụ theo tài liệu:
     *   endTime = 20:00:00, bid lúc 19:59:50 (còn 10s < 30s)
     *   → endTime mới = 20:00:00 + 70s = 20:01:10
     */
    private void applyAntiSniping() {
        long secondsLeft = java.time.temporal.ChronoUnit.SECONDS
                .between(LocalDateTime.now(), endTime);

        // Chỉ kích hoạt nếu bid trong ngưỡng X giây cuối
        if (secondsLeft < 0 || secondsLeft > TRIGGER_SECONDS) return;

        // Kiểm tra giới hạn số lần
        if (extensionCount >= MAX_EXTENSIONS) {
            System.out.printf("[Anti-sniping] Phiên [%s] đã đạt giới hạn %d lần, không gia hạn thêm.%n",
                    sessionId, MAX_EXTENSIONS);
            return;
        }

        LocalDateTime oldEndTime = endTime;
        endTime = endTime.plusSeconds(EXTENSION_SECONDS);
        extensionCount++;
        lastBidTriggeredExtension = true;

        System.out.printf(
                "[Anti-sniping] Phiên [%s]: bid lúc %s (còn %ds < %ds) " +
                "→ kéo dài từ %s đến %s (lần %d/%d)%n",
                sessionId,
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")),
                secondsLeft, TRIGGER_SECONDS,
                oldEndTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")),
                endTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")),
                extensionCount, MAX_EXTENSIONS);
    }

    // ── Getters ───────────────────────────────────────────────
    public String        getSessionId()                  { return sessionId; }
    public String        getItemName()                   { return itemName; }
    public double        getStartingPrice()              { return startingPrice; }
    public double        getCurrentPrice()               { return currentPrice; }
    public double        getMinBidStep()                 { return minBidStep; }
    public AuctionStatus getStatus()                     { return status; }
    public Bid           getHighestBid()                 { return highestBid; }
    public List<Bid>     getBidHistory()                 { return new ArrayList<>(bidHistory); }
    public LocalDateTime getEndTime()                    { return endTime; }
    public int           getExtensionCount()             { return extensionCount; }
    public boolean       isLastBidTriggeredExtension()   { return lastBidTriggeredExtension; }
}
