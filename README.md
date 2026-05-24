# AuctionSystem — Nhóm 6

Hệ thống đấu giá thời gian thực xây dựng trên **JavaFX + WebSocket**, theo kiến trúc **Client-Server** và mô hình **MVC**.

---

## Kiến trúc tổng thể

```
┌──────────────────────────────────────────────────────────────┐
│                      CLIENT (JavaFX)                         │
│                                                              │
│   View (FXML) ◄──► Controller ◄──► AuctionClient (WS)       │
└──────────────────────────┬───────────────────────────────────┘
                           │  WebSocket  (port 8080)
┌──────────────────────────▼───────────────────────────────────┐
│                         SERVER                               │
│                                                              │
│   AuctionWebSocketServer                                     │
│     ├── MessageRouter      (phân loại JSON message)         │
│     ├── SessionManager     (quản lý phiên đấu giá)          │
│     ├── AutoBidEngine      (đặt giá tự động)                │
│     ├── AntiSnipingService (gia hạn chống snipe)            │
│     └── BroadcastService   (real-time update → clients)     │
│                                                              │
│   DAO layer (in-memory, dễ thay bằng DB)                    │
│     ├── AuctionSessionDAO                                    │
│     ├── ProductDAO                                           │
│     └── UserDAO                                             │
└──────────────────────────────────────────────────────────────┘
```

---

## Tính năng chính

| Tính năng | Mô tả |
|---|---|
| Đấu giá thời gian thực | Nhiều người dùng cùng đặt giá qua WebSocket |
| Anti-sniping | Tự động gia hạn phiên khi có bid sát giờ kết thúc |
| Auto-bid | Đặt giá tự động theo giới hạn tối đa của bidder |
| Quản lý trạng thái | OPEN → RUNNING → FINISHED / CANCELED → PAID |
| Phân quyền | Admin (tạo/hủy phiên) và Bidder (đặt giá) |

---

## Cấu trúc thư mục

```
AuctionSystem_Nhom6/
├── pom.xml
├── checkstyle.xml
├── .editorconfig
├── README.md
├── .github/
│   └── workflows/
│       ├── ci.yml          # Build + test tự động
│       └── cd.yml          # Deploy lên server
└── src/
    ├── main/java/
    │   ├── network/
    │   │   └── package-info.java
    │   └── org/example/
    │       ├── auction/
    │       │   ├── AuctionSession.java
    │       │   ├── AuctionStatus.java
    │       │   └── Bid.java
    │       ├── dao/
    │       │   ├── AuctionSessionDAO.java
    │       │   ├── ProductDAO.java
    │       │   └── UserDAO.java
    │       ├── exception/
    │       │   ├── AuctionClosedException.java
    │       │   └── InvalidBidException.java
    │       ├── service/
    │       │   ├── AntiSnipingService.java
    │       │   ├── AuthService.java
    │       │   └── AutoBidEngine.java
    │       └── user/
    │           ├── Admin.java
    │           ├── Bidder.java
    │           └── User.java
    └── test/java/org/example/auction/
        ├── AuctionSessionTest.java
        ├── AuthServiceTest.java
        ├── BidTest.java
        ├── AntiSnipingTest.java
        └── AutoBidEngineTest.java
```

---

## Yêu cầu môi trường

- **Java 21** (LTS)
- **Maven 3.9+**
- **JavaFX 21**

---

## Build & Chạy

### Chạy unit test

```bash
mvn test
```

### Kiểm tra coding convention

```bash
mvn checkstyle:check
```

### Build fat JAR cho server

```bash
mvn clean package -DskipTests
```

### Chạy server

```bash
java -jar target/AuctionSystem_Nhom6-1.0-SNAPSHOT-server.jar
```

### Chạy client (JavaFX)

```bash
mvn javafx:run
```

---

## CI/CD

| Workflow | Trigger | Tác vụ |
|---|---|---|
| `ci.yml` | Push / PR lên `main`, `develop` | Compile → Checkstyle → JUnit |
| `cd.yml` | Push lên `main` hoặc tag `v*.*.*` | Build JAR → SSH Deploy → GitHub Release |

---

## Giao thức WebSocket (JSON)

```json
// Client gửi bid
{ "type": "PLACE_BID", "sessionId": "S001", "amount": 1500.0, "bidderId": "alice" }

// Server broadcast khi có bid mới
{ "type": "BID_UPDATE", "sessionId": "S001", "currentPrice": 1500.0, "leadingBidder": "alice" }

// Server broadcast khi phiên kết thúc
{ "type": "SESSION_END", "sessionId": "S001", "winner": "alice", "finalPrice": 1500.0 }
```

---

## Nhóm phát triển

| Thành viên | Vai trò |
|---|---|
| Nhóm 6 | Toàn bộ hệ thống |

---

## License

Dự án học thuật — không dùng cho mục đích thương mại.
