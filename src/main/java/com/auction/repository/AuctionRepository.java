package com.auction.repository;

import com.auction.exception.DataException;
import com.auction.exception.GlobalExceptionHandler;
import com.auction.model.AuctionSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionRepository {

    // Giả lập database
    // TODO: thay bằng kết nối database thực tế
    private static final Map<String, AuctionSession> DATABASE = new HashMap<>();

    // ── Lưu phiên đấu giá ────────────────────────────────
    public void save(AuctionSession session) {
        try {
            // Validate session
            if (session == null)
                throw new DataException(
                    "Session không được null", "session", null);

            if (session.getSessionId() == null || session.getSessionId().isBlank())
                throw new DataException(
                    "Session ID không được để trống", "sessionId", session.getSessionId());

            DATABASE.put(session.getSessionId(), session);
            System.out.println("[REPO] Lưu phiên thành công: " + session.getSessionId());

        } catch (DataException e) {
            System.err.println(GlobalExceptionHandler.handle(e));
        }
    }

    // ── Tìm phiên theo ID ─────────────────────────────────
    public AuctionSession findById(String sessionId) {
        try {
            // Validate sessionId
            if (sessionId == null || sessionId.isBlank())
                throw new DataException(
                    "Session ID không được để trống", "sessionId", sessionId);

            AuctionSession session = DATABASE.get(sessionId);

            if (session == null)
                throw new DataException(
                    "Không tìm thấy phiên đấu giá!", "sessionId", sessionId);

            return session;

        } catch (DataException e) {
            System.err.println(GlobalExceptionHandler.handle(e));
            return null;
        }
    }

    // ── Lấy tất cả phiên ─────────────────────────────────
    public List<AuctionSession> findAll() {
        try {
            if (DATABASE.isEmpty())
                throw new DataException(
                    "Không có phiên đấu giá nào trong hệ thống!", "database");

            return new ArrayList<>(DATABASE.values());

        } catch (DataException e) {
            System.err.println(GlobalExceptionHandler.handle(e));
            return new ArrayList<>();
        }
    }

    // ── Lấy các phiên đang mở ─────────────────────────────
    public List<AuctionSession> findAllOpen() {
        try {
            List<AuctionSession> openSessions = DATABASE.values()
                .stream()
                .filter(session -> !session.isClosed())
                .toList();

            if (openSessions.isEmpty())
                throw new DataException(
                    "Không có phiên đấu giá nào đang mở!", "openSessions");

            return openSessions;

        } catch (DataException e) {
            System.err.println(GlobalExceptionHandler.handle(e));
            return new ArrayList<>();
        }
    }

    // ── Lấy các phiên đã đóng ────────────────────────────
    public List<AuctionSession> findAllClosed() {
        try {
            List<AuctionSession> closedSessions = DATABASE.values()
                .stream()
                .filter(AuctionSession::isClosed)
                .toList();

            if (closedSessions.isEmpty())
                throw new DataException(
                    "Không có phiên đấu giá nào đã đóng!", "closedSessions");

            return closedSessions;

        } catch (DataException e) {
            System.err.println(GlobalExceptionHandler.handle(e));
            return new ArrayList<>();
        }
    }

    // ── Xóa phiên theo ID ─────────────────────────────────
    public void deleteById(String sessionId) {
        try {
            if (sessionId == null || sessionId.isBlank())
                throw new DataException(
                    "Session ID không được để trống", "sessionId", sessionId);

            if (!DATABASE.containsKey(sessionId))
                throw new DataException(
                    "Không tìm thấy phiên để xóa!", "sessionId", sessionId);

            DATABASE.remove(sessionId);
            System.out.println("[REPO] Xóa phiên thành công: " + sessionId);

        } catch (DataException e) {
            System.err.println(GlobalExceptionHandler.handle(e));
        }
    }

    // ── Kiểm tra phiên tồn tại ───────────────────────────
    public boolean existsById(String sessionId) {
        try {
            if (sessionId == null || sessionId.isBlank())
                throw new DataException(
                    "Session ID không được để trống", "sessionId", sessionId);

            return DATABASE.containsKey(sessionId);

        } catch (DataException e) {
            System.err.println(GlobalExceptionHandler.handle(e));
            return false;
        }
    }

    // ── Đếm tổng số phiên ────────────────────────────────
    public int count() {
        return DATABASE.size();
    }
}
