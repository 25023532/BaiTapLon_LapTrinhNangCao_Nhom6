package org.example.item;

public class Vehicle extends AuctionItem {

    public Vehicle(String id, String name, String desc, double price) {
        super(id, name, desc, price);
    }

    @Override
    public String getType() {
        return "Vehicle";
    }
}