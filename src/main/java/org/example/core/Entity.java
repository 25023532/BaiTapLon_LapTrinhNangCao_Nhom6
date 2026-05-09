package org.example.core;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp gốc cho toàn bộ hệ thống.
 * Serializable → hỗ trợ Socket transmission (Tuần 9)
 */
public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    protected final String id;
    protected final LocalDateTime createdAt;

    protected Entity(String id) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("ID không được rỗng");
        this.id        = id;
        this.createdAt = LocalDateTime.now();
    }

    // Auto-generate ID nếu không truyền
    protected Entity() {
        this.id        = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public String        getId()        { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public abstract void printInfo();
}
