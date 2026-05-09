package org.example.manager;

import org.example.auction.AuctionSession;
import org.example.exception.DataException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton — quản lý trung tâm toàn bộ phiên đấu giá.
 * Thread-safe với double-checked locking.
 */
public class AuctionManager {

    private static volatile AuctionManager instance;
    private final Map<String, AuctionSession> sessions = new HashMap<>();

    private AuctionManager() {}

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    public void addSession(AuctionSession session) {
        if (session == null)
            throw new DataException("session", "Không được null");

        // ✅ Fix: getId() → getSessionId()
        if (sessions.containsKey(session.getSessionId()))
            throw new DataException("sessionId",
                    "Phiên đã tồn tại: " + session.getSessionId());

        sessions.put(session.getSessionId(), session);
        System.out.println("[AuctionManager] Thêm phiên: " + session.getSessionId());
    }

    public AuctionSession getSession(String id) {
        AuctionSession s = sessions.get(id);
        if (s == null)
            throw new DataException("sessionId", "Không tìm thấy: " + id);
        return s;
    }

    public void removeSession(String id) {
        sessions.remove(id);
    }

    public Map<String, AuctionSession> getAllSessions() {
        return Collections.unmodifiableMap(sessions);
    }
}
