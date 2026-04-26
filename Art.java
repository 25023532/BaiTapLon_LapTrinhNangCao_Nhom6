package org.example.item;

public class Art extends AuctionItem {

    public Art(String id, String name, String desc, double price) {
        super(id, name, desc, price);
    }

    @Override
    public String getType() {
        return "Art";
    }
}