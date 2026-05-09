package org.example.factory;

import org.example.item.*;
import java.time.LocalDateTime;

/**
 * Factory Method — tạo AuctionItem theo type.
 * Mở rộng thêm loại mới chỉ cần thêm case, không sửa code cũ (OCP).
 */
public class AuctionItemFactory {

    public enum ItemType { ELECTRONICS, ART, VEHICLE }

    public static AuctionItem create(ItemType type,
                                     String id, String name, String description,
                                     double startPrice,
                                     LocalDateTime startTime, LocalDateTime endTime) {
        return switch (type) {
            case ELECTRONICS -> new Electronics(id, name, description, startPrice, startTime, endTime);
            case ART         -> new Art        (id, name, description, startPrice, startTime, endTime);
            case VEHICLE     -> new Vehicle    (id, name, description, startPrice, startTime, endTime);
        };
    }
}
