package org.example.item;

public abstract class AuctionItem {
    protected String id;
    protected String name;
    protected String description;
    protected double startPrice;
    protected double currentPrice;

    public AuctionItem(String id, String name, String desc, double startPrice) {
        this.id = id;
        this.name = name;
        this.description = desc;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
    }

    public void updatePrice(double price) {
        this.currentPrice = price;
    }

    public double getCurrentPrice() { return currentPrice; }

    public abstract String getType();
}