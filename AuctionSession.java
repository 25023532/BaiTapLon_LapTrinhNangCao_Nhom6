package org.example.auction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionSession {

    private final String sessionId;
    private final String itemName;
    private final double startingPrice;
    private final double minBidStep;       
    private final LocalDateTime endTime;

    private AuctionStatus status;
    private double currentPrice;
    private Bid highestBid;

    private final List<Bid> bidHistory = new ArrayList<>();
    private final List<Observer> observers = new ArrayList<>();

    private final ReentrantLock lock = new ReentrantLock();

    public AuctionSession(String sessionId, String itemName,
                          double startingPrice, double minBidStep,
                          LocalDateTime endTime) {
        if (sessionId == null || sessionId.isBlank()) throw new IllegalArgumentException("SessionId không được rỗng");
        if (itemName == null || itemName.isBlank())   throw new IllegalArgumentException("ItemName không được rỗng");
        if (startingPrice <= 0) throw new IllegalArgumentException("Giá khởi điểm phải > 0");
        if (minBidStep <= 0)    throw new IllegalArgumentException("Bước giá phải > 0");
        if (endTime == null || endTime.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Thời gian kết thúc không hợp lệ");

        this.sessionId    = sessionId;
        this.itemName     = itemName;
        this.startingPrice = startingPrice;
        this.currentPrice  = startingPrice;
        this.minBidStep    = minBidStep;
        this.endTime       = endTime;
        this.status        = AuctionStatus.OPEN;
    }


    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    private void notifyObservers(Bid bid) {
        for (Observer o : observers) {
            o.update(bid);
        }
    }


    public void start() {
        lock.lock();
        try {
            if (status != AuctionStatus.OPEN) {
                throw new IllegalStateException("Chỉ có thể bắt đầu phiên ở trạng thái OPEN");
            }
            status = AuctionStatus.RUNNING;
            System.out.println("Phiên đấu giá [" + sessionId + "] đã bắt đầu.");
        } finally {
            lock.unlock();
        }
    }


    public void finish() {
        lock.lock();
        try {
            if (status != AuctionStatus.RUNNING) {
                throw new IllegalStateException("Chỉ có thể kết thúc phiên đang RUNNING");
            }
            status = AuctionStatus.FINISHED;

            if (highestBid != null) {
                System.out.printf("Phiên [%s] kết thúc. Người thắng: %s với giá %.2f%n",
                        sessionId, highestBid.getBidderId(), highestBid.getAmount());
            } else {

                status = AuctionStatus.CANCELED;
                System.out.println("Phiên [" + sessionId + "] bị hủy do không có bid nào.");
            }
        } finally {
            lock.unlock();
        }
    }

    public void markPaid() {
        lock.lock();
        try {
            if (status != AuctionStatus.FINISHED) {
                throw new IllegalStateException("Chỉ có thể thanh toán khi phiên FINISHED");
            }
            status = AuctionStatus.PAID;
            System.out.println("Phiên [" + sessionId + "] đã được thanh toán.");
        } finally {
            lock.unlock();
        }
    }

    public void cancel() {
        lock.lock();
        try {
            if (status == AuctionStatus.PAID) {
                throw new IllegalStateException("Không thể hủy phiên đã thanh toán");
            }
            status = AuctionStatus.CANCELED;
            System.out.println("Phiên [" + sessionId + "] đã bị hủy thủ công.");
        } finally {
            lock.unlock();
        }
    }


    public boolean placeBid(Bid bid) {
        lock.lock();
        try {

            if (status != AuctionStatus.RUNNING) {
                System.out.println("Phiên không ở trạng thái RUNNING.");
                return false;
            }

            if (LocalDateTime.now().isAfter(endTime)) {
                finish();
                return false;
            }

            if (bid.getAmount() < currentPrice + minBidStep) {
                System.out.printf("Bid không hợp lệ: %.2f phải >= %.2f%n",
                        bid.getAmount(), currentPrice + minBidStep);
                return false;
            }

            currentPrice = bid.getAmount();
            highestBid   = bid;
            bidHistory.add(bid);

            System.out.printf("Bid được chấp nhận: %s đặt %.2f%n",
                    bid.getBidderId(), bid.getAmount());

            notifyObservers(bid);
            return true;

        } finally {
            lock.unlock();
        }
    }

    public String getSessionId()     { return sessionId; }
    public String getItemName()      { return itemName; }
    public double getCurrentPrice()  { return currentPrice; }
    public AuctionStatus getStatus() { return status; }
    public Bid getHighestBid()       { return highestBid; }
    public List<Bid> getBidHistory() { return new ArrayList<>(bidHistory); }
    public LocalDateTime getEndTime(){ return endTime; }
}