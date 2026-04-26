package org.example.manager;

import org.example.item.AuctionItem;

import java.util.HashMap;
import java.util.Map;

public class ItemManager {

    private Map<String, AuctionItem> items = new HashMap<>();

    public void addItem(AuctionItem item) {
        items.put(item.toString(), item);
    }

    public void removeItem(String id) {
        items.remove(id);
    }

    public AuctionItem getItem(String id) {
        return items.get(id);
    }

    public void updateItem(String id, AuctionItem newItem) {
        items.put(id, newItem);
    }
}