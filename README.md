# 🏷️ AuctionSys – Hệ thống Đấu giá Trực tuyến (Nhóm 6)

Hệ thống đấu giá trực tuyến thời gian thực, hỗ trợ nhiều người dùng đồng thời, được xây dựng bằng **Java + JavaFX + WebSocket**.

---

## 📋 Mô tả bài toán và phạm vi hệ thống

AuctionSys là ứng dụng đấu giá trực tuyến theo mô hình **Client–Server**, trong đó:

- **Seller** đăng sản phẩm lên hệ thống, chờ Admin duyệt rồi mở phiên đấu giá.
- **Bidder** tham gia đặt giá realtime, hỗ trợ đặt giá tự động (Auto-bid) và chống đặt giá chớp nhoáng (Anti-sniping).
- **Admin** quản lý toàn bộ hệ thống: duyệt sản phẩm, giám sát phiên đấu giá, quản lý người dùng.

Giao tiếp giữa Client và Server sử dụng **WebSocket (Java-WebSocket)** kết hợp **TCP Socket** (LAN), đảm bảo đồng bộ trạng thái realtime cho tất cả người dùng.

---

## 🛠️ Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 17+ |
| Giao diện | JavaFX + FXML + CSS |
| Mạng | WebSocket (`org.java-websocket`) + TCP Socket |
| Lưu trữ | File-based (JSON) trong thư mục `data/` |
| Build | Maven (`maven-shade-plugin` – fat JAR) |
| Logging | SLF4J |

---

## ⚙️ Yêu cầu môi trường

- **Java 17** trở lên
- Hệ điều hành: Windows / Linux / macOS

---

## 📁 Cấu trúc thư mục

| Đường dẫn | Mô tả |
|---|---|
| `src/main/java/com/nhom6/auctionsystem_nhom6/` | **Client app** – giao diện JavaFX |
| &nbsp;&nbsp;`HelloApplication.java` | Điểm khởi động Client |
| &nbsp;&nbsp;`Launcher.java` | Main class dùng cho fat JAR |
| &nbsp;&nbsp;`AuctionWebSocketServer.java` | WebSocket Server |
| &nbsp;&nbsp;`ServerDatabase.java` | Lưu trữ dữ liệu dạng file |
| &nbsp;&nbsp;`ServerConfig.java` | Đọc/ghi cấu hình server |
| &nbsp;&nbsp;`AppContext.java` | Trạng thái toàn cục phía client |
| &nbsp;&nbsp;`ServerConnection.java` | Quản lý kết nối client ↔ server |
| &nbsp;&nbsp;`NotificationManager.java` | Quản lý thông báo realtime |
| &nbsp;&nbsp;`controller/` | Controllers JavaFX (Login, Main, LiveAuction, Wallet, Rating...) |
| `src/main/java/network/` | **TCP Server** dùng cho LAN |
| &nbsp;&nbsp;`AuctionServer.java` | TCP Server đa luồng |
| &nbsp;&nbsp;`ClientHandler.java` | Xử lý từng client |
| &nbsp;&nbsp;`ClientManager.java` | Quản lý danh sách client |
| `src/main/java/org/example/` | **Domain model & Business logic** |
| &nbsp;&nbsp;`auction/` | AuctionSession, Bid, Observer, Anti-sniping |
| &nbsp;&nbsp;`user/` | User, Seller, Bidder, Admin |
| &nbsp;&nbsp;`item/` | AuctionItem, Art, Electronics, Vehicle |
| &nbsp;&nbsp;`service/` | AuctionService, AuthService, AutoBidEngine |
| &nbsp;&nbsp;`dao/` | AuctionDAO, UserDAO, ProductDAO |
| &nbsp;&nbsp;`strategy/` | BidStrategy (Normal / Aggressive) |
| &nbsp;&nbsp;`factory/` | AuctionItemFactory |
| &nbsp;&nbsp;`manager/` | AuctionManager, ItemManager |
| &nbsp;&nbsp;`exception/` | Custom exceptions |
| `data/` | Dữ liệu server (tự tạo khi chạy lần đầu) |
| `pom.xml` | Maven build config |
| `server.properties` | Cấu hình host/port server |

---

## 📦 Vị trí file `.jar`

Sau khi build, các file JAR nằm tại:

```
target/
├── AuctionSystem_Nhom6-1.0-SNAPSHOT.jar
├── AuctionSystem_Nhom6-1.0-SNAPSHOT-server.jar   (Fat JAR - chạy Server)
└── AuctionSystem_Nhom6-1.0-SNAPSHOT-client.jar   (Fat JAR - chạy Client)
```

---

## 🔨 Hướng dẫn Build

**Bước 1 – Clone repo**

```
git clone https://github.com/25023532/BaiTapLon_LapTrinhNangCao_Nhom6.git
cd BaiTapLon_LapTrinhNangCao_Nhom6
```

**Bước 2 – Build (Windows)**

```
mvnw.cmd clean package -DskipTests
```

**Bước 2 – Build (Linux / macOS)**

```
./mvnw clean package -DskipTests
```

Sau khi thấy `BUILD SUCCESS`, file JAR xuất hiện tại `target/`.

---

## 🚀 Hướng dẫn chạy Server và Client

> ⚠️ Chạy Server trước, sau đó mới chạy Client.

**Bước 1 – Chạy Server** (terminal 1)

```
java -jar target/AuctionSystem_Nhom6-1.0-SNAPSHOT-server.jar
```

Server khởi động thành công khi console hiển thị thông báo đang lắng nghe kết nối trên port 1234.

**Bước 2 – Chạy Client** (mở terminal mới)

```
cd BaiTapLon_LapTrinhNangCao_Nhom6
java -jar target/AuctionSystem_Nhom6-1.0-SNAPSHOT-client.jar
```

Giao diện đăng nhập sẽ hiện ra. Dùng tài khoản demo để đăng nhập:

| Vai trò | Tên đăng nhập | Mật khẩu |
|---|---|---|
| Admin | `admin` | `admin123` |
| Bidder | `bidder07` | `bidder123` |

**Chạy nhiều Client:** Mở thêm terminal và chạy lại lệnh Client — mỗi tiến trình là một người dùng độc lập.

---

## ✅ Danh sách chức năng đã hoàn thành

### 👤 Xác thực & Tài khoản
- [x] Đăng ký tài khoản (Seller / Bidder)
- [x] Đăng nhập / Đăng xuất
- [x] Mã hóa mật khẩu bằng SHA-256
- [x] Xem và chỉnh sửa hồ sơ cá nhân

### 🛒 Quản lý sản phẩm (Seller)
- [x] Đăng sản phẩm mới (Nghệ thuật / Điện tử / Xe cộ)
- [x] Xem danh sách sản phẩm của mình
- [x] Mở phiên đấu giá sau khi được Admin duyệt

### 🔨 Đấu giá realtime (Bidder)
- [x] Xem danh sách phiên đang mở
- [x] Tham gia đấu giá trực tiếp (Live Auction)
- [x] Đặt giá thủ công
- [x] Auto-bid – đặt giá tự động theo ngưỡng tối đa và bước tăng
- [x] Anti-sniping – tự động gia hạn phiên nếu có bid trong 3 phút cuối (gia hạn 2 phút, tối đa 5 lần)
- [x] Xem lịch sử bid realtime
- [x] Thông báo kết quả thắng/thua

### 🏦 Ví & Giao dịch
- [x] Xem số dư ví
- [x] Nạp tiền (mô phỏng)
- [x] Lịch sử giao dịch

### ⭐ Đánh giá
- [x] Đánh giá Seller sau phiên đấu giá (thang điểm 1–10)
- [x] Xem thống kê đánh giá (điểm trung bình, phân bố)

### 🔧 Quản trị (Admin)
- [x] Duyệt / Từ chối sản phẩm của Seller
- [x] Giám sát tất cả phiên đấu giá
- [x] Quản lý người dùng

### 🔔 Thông báo
- [x] Thông báo realtime qua WebSocket (popup)
- [x] Đếm số người online

### 🏗️ Design Patterns áp dụng
- [x] Observer Pattern – đồng bộ bid realtime
- [x] Factory Pattern – tạo AuctionItem theo loại
- [x] Strategy Pattern – chiến lược đặt giá (Normal / Aggressive)
- [x] DAO Pattern – tách biệt logic truy cập dữ liệu
- [x] MVC – tổ chức theo Controller / Model / FXML View

---

## 📎 Link báo cáo & Demo

- 📄 **Báo cáo PDF:** [Chèn link tại đây]
- 🎬 **Video demo:** [Chèn link tại đây]

---
## 👥 Nhóm phát triển

| Thành viên | Vai trò |
|---|---|
| Nhóm 6 | Toàn bộ hệ thống |
