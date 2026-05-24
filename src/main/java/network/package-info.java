/**
 * Package {@code network} — tầng kết nối Client-Server qua WebSocket.
 *
 * <h2>Kiến trúc tổng thể</h2>
 * <pre>
 * ┌──────────────────────────────────────────────────────┐
 * │                   CLIENT (JavaFX)                    │
 * │  AuctionClient ──► sends JSON messages via WS        │
 * └──────────────────────┬───────────────────────────────┘
 *                        │  WebSocket (port 8080)
 * ┌──────────────────────▼───────────────────────────────┐
 * │                   SERVER                             │
 * │  AuctionWebSocketServer                              │
 * │    ├── MessageRouter  (phân loại message)            │
 * │    ├── SessionManager (quản lý phiên đấu giá)        │
 * │    └── BroadcastService (gửi update tới mọi client)  │
 * └──────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Giao thức tin nhắn (JSON)</h2>
 * Mọi tin nhắn đều có trường {@code type} để router phân loại:
 * <ul>
 *   <li>{@code LOGIN}       — đăng nhập, kèm username/password</li>
 *   <li>{@code PLACE_BID}   — đặt giá, kèm sessionId và amount</li>
 *   <li>{@code SESSION_LIST}— yêu cầu danh sách phiên hiện tại</li>
 *   <li>{@code BID_UPDATE}  — server broadcast khi có bid mới</li>
 *   <li>{@code SESSION_END} — server broadcast khi phiên kết thúc</li>
 * </ul>
 *
 * <h2>Các lớp chính</h2>
 * <ul>
 *   <li>{@code AuctionWebSocketServer} — entry-point của server,
 *       lắng nghe kết nối và điều phối xử lý</li>
 *   <li>{@code AuctionClient}         — phía client, gửi/nhận message</li>
 *   <li>{@code MessageRouter}         — phân loại và chuyển message
 *       đến handler tương ứng</li>
 *   <li>{@code BroadcastService}      — broadcast cập nhật thời gian
 *       thực đến tất cả client đang kết nối</li>
 * </ul>
 *
 * @see org.java_websocket.server.WebSocketServer
 */
package network;
