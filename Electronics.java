package org.example.item;

public class Electronics extends AuctionItem {

    public Electronics(String id, String name, String desc, double price) {
        super(id, name, desc, price);
    }

    @Override
    public String getType() {
        return "Electronics";
    }
}