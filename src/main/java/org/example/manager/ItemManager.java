package org.example.manager;

import org.example.item.AuctionItem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ItemManager {

    private final Map<String, AuctionItem> items = new HashMap<>();

    public void addItem(AuctionItem item) {
        if (item == null)
            throw new IllegalArgumentException("Item không được null");
        if (items.containsKey(item.getId()))
            throw new IllegalStateException("Item với id '" + item.getId() + "' đã tồn tại");

        items.put(item.getId(), item);   
        System.out.println("Đã thêm item: " + item.getName());
    }

    public void removeItem(String id) {
        if (!items.containsKey(id))
            throw new IllegalArgumentException("Không tìm thấy item với id: " + id);
        AuctionItem removed = items.remove(id);
        System.out.println("Đã xóa item: " + removed.getName());
    }

    public AuctionItem getItem(String id) {
        AuctionItem item = items.get(id);
        if (item == null)
            throw new IllegalArgumentException("Không tìm thấy item với id: " + id);
        return item;
    }

    public void updateItem(String id, AuctionItem newItem) {
        if (!items.containsKey(id))
            throw new IllegalArgumentException("Không tìm thấy item với id: " + id);
        if (newItem == null)
            throw new IllegalArgumentException("Item mới không được null");

        items.put(id, newItem);
        System.out.println("Đã cập nhật item: " + id);
    }

    public Map<String, AuctionItem> getAllItems() {
        return Collections.unmodifiableMap(items);
    }
}
