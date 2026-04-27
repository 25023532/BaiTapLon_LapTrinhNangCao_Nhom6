package org.example.item;

import java.time.LocalDateTime;

public abstract class AuctionItem {
    protected String id;
    protected String name;
    protected String description;
    protected double startPrice;
    protected double currentPrice;
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;

    public AuctionItem(String id, String name, String description,
                       double startPrice, LocalDateTime startTime, LocalDateTime endTime) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("ID không được rỗng");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Tên không được rỗng");
        if (startPrice <= 0)
            throw new IllegalArgumentException("Giá khởi điểm phải > 0");
        if (startTime == null || endTime == null)
            throw new IllegalArgumentException("Thời gian không được null");
        if (!endTime.isAfter(startTime))
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");

        this.id           = id;
        this.name         = name;
        this.description  = description;
        this.startPrice   = startPrice;
        this.currentPrice = startPrice;
        this.startTime    = startTime;
        this.endTime      = endTime;
    }

    public void updatePrice(double newPrice) {
        if (newPrice <= this.currentPrice)
            throw new IllegalArgumentException(
                "Giá mới (%.2f) phải cao hơn giá hiện tại (%.2f)"
                    .formatted(newPrice, this.currentPrice));
        this.currentPrice = newPrice;
    }

    public String getId()            { return id; }
    public String getName()          { return name; }
    public String getDescription()   { return description; }
    public double getStartPrice()    { return startPrice; }
    public double getCurrentPrice()  { return currentPrice; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime()   { return endTime; }

    public void setName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Tên không được rỗng");
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEndTime(LocalDateTime endTime) {
        if (endTime == null || !endTime.isAfter(this.startTime))
            throw new IllegalArgumentException("Thời gian kết thúc không hợp lệ");
        this.endTime = endTime;
    }

    public abstract String getType();

    @Override
    public String toString() {
        return String.format("AuctionItem{id='%s', name='%s', type='%s', " +
                             "startPrice=%.2f, currentPrice=%.2f, " +
                             "startTime=%s, endTime=%s}",
                id, name, getType(), startPrice, currentPrice, startTime, endTime);
    }
}
