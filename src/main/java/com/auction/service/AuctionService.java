package com.auction.service;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.DataException;
import com.auction.exception.GlobalExceptionHandler;
import com.auction.exception.InvalidBidException;
import com.auction.model.AuctionSession;
import com.auction.repository.AuctionRepository;

import java.util.List;

public class AuctionService {

    private AuctionRepository repository;

    public AuctionService() {
        this.repository = new AuctionRepository();
    }

    // ── Tạo phiên đấu giá mới ────────────────────────────
    public AuctionSession createSession(String sessionId,
                                        String itemName,
                                        double startingBid) {
        try {
            AuctionSession session = new AuctionSession(
                sessionId, itemName, startingBid);
            repository.save(session);
            System.out.println("Tạo phiên thành công: " + sessionId);
            return session;

        } catch (DataException e) {
            System.err.println(GlobalExceptionHandler.handle(e));
            return null;
        }
    }

    // ── Đặt giá ──────────────────────────────────────────
    public String placeBid(String sessionId,
                            String bidderId,
                            double bidAmount) {
        try {
            // Lấy phiên từ repository
            AuctionSession session = repository.findById(sessionId);

            // Kiểm tra phiên tồn tại
            if (session == null)
                throw new DataException(
                    "Không tìm thấy phiên đấu giá!", "sessionId", sessionId);

            // Thực hiện đặt giá
            session.placeBid(bidderId, bidAmount);
            repository.save(session);

            return "Đặt giá thành công! Giá mới: " + bidAmount;

        } catch (AuctionClosedException e) {
            return GlobalExceptionHandler.handle(e);

        } catch (InvalidBidException e) {
            return GlobalExceptionHandler.handle(e);

        } catch (DataException e) {
            return GlobalExceptionHandler.handle(e);
        }
    }

    // ── Đóng phiên đấu giá ───────────────────────────────
    public String closeSession(String sessionId) {
        try {
            AuctionSession session = repository.findById(sessionId);

            if (session == null)
                throw new DataException(
                    "Không tìm thấy phiên đấu giá!", "sessionId", sessionId);

            session.closeSession();
            repository.save(session);

            return "Phiên " + sessionId + " đã đóng thành công!";

        } catch (AuctionClosedException e) {
            return GlobalExceptionHandler.handle(e);

        } catch (DataException e) {
            return GlobalExceptionHandler.handle(e);
        }
    }

    // ── Lấy thông tin phiên ──────────────────────────────
    public AuctionSession getSession(String sessionId) {
        try {
            if (sessionId == null || sessionId.isBlank())
                throw new DataException(
                    "Session ID không được để trống", "sessionId", sessionId);

            AuctionSession session = repository.findById(sessionId);

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
    public List<AuctionSession> getAllSessions() {
        try {
            List<AuctionSession> sessions = repository.findAll();

            if (sessions == null || sessions.isEmpty())
                throw new DataException("Không có phiên đấu giá nào!", "sessions");

            return sessions;

        } catch (DataException e) {
            System.err.println(GlobalExceptionHandler.handle(e));
            return List.of();
        }
    }
}
