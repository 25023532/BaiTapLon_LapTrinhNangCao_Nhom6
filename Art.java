package org.example.item;

import java.time.LocalDateTime;

public class Art extends AuctionItem {

    public Art(String id, String name, String description,
               double startPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super(id, name, description, startPrice, startTime, endTime);
    }

    @Override
    public String getType() { return "Art"; }
}
