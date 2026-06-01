# 🏷️ AuctionSys – Hệ thống Đấu giá Trực tuyến (Nhóm 6)

Hệ thống đấu giá trực tuyến thời gian thực, hỗ trợ nhiều người dùng đồng thời, được xây dựng bằng **Java + JavaFX + WebSocket**.

---

## 📋 Mô tả bài toán và phạm vi hệ thống

**AuctionSys** là ứng dụng đấu giá trực tuyến theo mô hình **Client–Server**, trong đó:

- **Seller** đăng sản phẩm lên hệ thống, chờ Admin duyệt rồi mở phiên đấu giá.
- **Bidder** tham gia đặt giá realtime, hỗ trợ đặt giá tự động (**Auto-bid**) và chống đặt giá chớp nhoáng (**Anti-sniping**).
- **Admin** quản lý toàn bộ hệ thống: duyệt sản phẩm, giám sát phiên đấu giá, quản lý người dùng.

Giao tiếp giữa Client và Server sử dụng **WebSocket** (Java-WebSocket) kết hợp **TCP Socket** (LAN), đảm bảo đồng bộ trạng thái realtime cho tất cả người dùng.

---

## 🛠️ Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 17+ |
| Giao diện | JavaFX + FXML + CSS |
| Mạng | WebSocket (org.java-websocket) + TCP Socket |
| Lưu trữ | File-based (pipe-delimited text, JSON) trong thư mục `data/` |
| Build | Maven (maven-shade-plugin – fat JAR) |
| Logging | SLF4J |

---

## ⚙️ Yêu cầu môi trường

- Java 17 trở lên (có hỗ trợ JavaFX)
- Maven 3.8+ (để build từ source)
- Hệ điều hành: Windows / Linux / macOS

---

## 📁 Cấu trúc thư mục

```
AuctionSys-Nhom6/
├── src/
│   └── main/
│       ├── java/
│       │   ├── com/nhom6/auctionsystem_nhom6/   # Client app (JavaFX)
│       │   │   ├── HelloApplication.java         # Điểm khởi động Client
│       │   │   ├── Launcher.java                 # Main class (dùng cho fat JAR)
│       │   │   ├── AuctionWebSocketServer.java   # WebSocket Server
│       │   │   ├── ServerDatabase.java           # File-based persistence
│       │   │   ├── ServerConfig.java             # Đọc/ghi cấu hình server
│       │   │   ├── AppContext.java               # Trạng thái toàn cục phía client
│       │   │   ├── ServerConnection.java         # Quản lý kết nối client↔server
│       │   │   ├── NotificationManager.java      # Quản lý thông báo realtime
│       │   │   └── controller/                   # Controllers JavaFX
│       │   │       ├── LoginController.java
│       │   │       ├── MainController.java
│       │   │       ├── LiveAuctionController.java
│       │   │       ├── ProductManagementController.java
│       │   │       ├── WalletController.java
│       │   │       ├── RatingController.java
│       │   │       └── ...
│       │   ├── network/                          # TCP Server (LAN)
│       │   │   ├── AuctionServer.java            # TCP Server đa luồng
│       │   │   ├── ClientHandler.java            # Xử lý mỗi client
│       │   │   └── ClientManager.java
│       │   └── org/example/                      # Domain model & business logic
│       │       ├── auction/                      # AuctionSession, Bid, Observer...
│       │       ├── user/                         # User, Seller, Bidder, Admin
│       │       ├── item/                         # AuctionItem, Art, Electronics, Vehicle
│       │       ├── service/                      # AuctionService, AuthService, AutoBidEngine...
│       │       ├── dao/                          # AuctionDAO, UserDAO, ProductDAO...
│       │       ├── strategy/                     # BidStrategy (Normal / Aggressive)
│       │       ├── factory/                      # AuctionItemFactory
│       │       ├── manager/                      # AuctionManager, ItemManager
│       │       └── exception/                    # Custom exceptions
│       └── resources/
│           └── com/nhom6/auctionsystem_nhom6/
│               ├── view/                         # FXML views
│               ├── styles/                       # CSS
│               └── images/                       # Logo, icon
├── data/                                         # Dữ liệu server (tự tạo khi chạy)
├── pom.xml
└── server.properties                             # Cấu hình host/port server
```

---

## 📦 Vị trí file `.jar`

Sau khi build, các file JAR nằm tại:

```
target/
├── AuctionSystem_Nhom6-1.0-SNAPSHOT.jar          # JAR gốc (không đủ deps)
├── AuctionSystem_Nhom6-1.0-SNAPSHOT-server.jar   # ✅ Fat JAR – chạy Server
└── AuctionSystem_Nhom6-1.0-SNAPSHOT-client.jar   # ✅ Fat JAR – chạy Client
```
🔨 Hướng dẫn Build
# Bước 1: Clone repo
git clone <link-github-repo>
cd AuctionSystem_Nhom6

# Bước 2: Build fat JAR (bỏ qua test để build nhanh hơn)
mvn clean package -DskipTests

# Sau khi build xong, file JAR xuất hiện tại target/

---

## 📦 Vị trí file `.jar`

Sau khi build, các file JAR nằm tại:
target/
├── AuctionSystem_Nhom6-1.0-SNAPSHOT.jar
├── AuctionSystem_Nhom6-1.0-SNAPSHOT-server.jar   ✅ Fat JAR – chạy Server
└── AuctionSystem_Nhom6-1.0-SNAPSHOT-client.jar   ✅ Fat JAR – chạy Client

## 🔨 Hướng dẫn Build

**Bước 1: Clone repo**
git clone https://github.com/25023532/BaiTapLon_LapTrinhNangCao_Nhom6.git
cd BaiTapLon_LapTrinhNangCao_Nhom6

**Bước 2: Build**
mvnw.cmd clean package -DskipTests

## 🚀 Hướng dẫn chạy

> ⚠️ Chạy Server trước, sau đó mới chạy Client

**Bước 1 – Chạy Server** (terminal 1)
java -jar target/AuctionSystem_Nhom6-1.0-SNAPSHOT-server.jar

**Bước 2 – Chạy Client** (terminal mới)
java -jar target/AuctionSystem_Nhom6-1.0-SNAPSHOT-client.jar

```

- Lần đầu khởi động, ứng dụng sẽ tự động hỏi địa chỉ IP/URL của Server.
- Nhập địa chỉ server (ví dụ: `ws://192.168.1.10:1234` hoặc `ws://localhost:1234`).
- Cấu hình được lưu vào file `server.properties` cạnh file JAR, các lần sau không cần nhập lại.

### Chạy nhiều Client

Mở thêm terminal và chạy lại lệnh Client — mỗi tiến trình là một người dùng độc lập.

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
- [x] **Auto-bid** – đặt giá tự động theo ngưỡng tối đa và bước tăng
- [x] **Anti-sniping** – tự động gia hạn phiên nếu có bid trong 3 phút cuối (gia hạn 2 phút, tối đa 5 lần)
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

---

## 🏗️ Design Patterns áp dụng

- [x] **Observer Pattern** – đồng bộ bid realtime
- [x] **Factory Pattern** – tạo AuctionItem theo loại
- [x] **Strategy Pattern** – chiến lược đặt giá (Normal / Aggressive)
- [x] **DAO Pattern** – tách biệt logic truy cập dữ liệu
- [x] **MVC** – tổ chức theo Controller / Model / FXML View

---

## 📎 Link báo cáo & Demo

- 📄 **Báo cáo PDF:** [Xem tại đây](https://drive.google.com/file/d/1MuT2j-BFa23wtpbcyPpOiQvgWeaj7DkJ/view?usp=sharing)
- 🎬 **Video demo:** [Xem tại đây](https://drive.google.com/file/d/1wKUdmXX_Pb3Q6_gbgqEgr2Bnvhx9yPul/view?usp=sharing)

---

## 👥 Nhóm phát triển

| Thành viên | Vai trò |
|---|---|
| Nhóm 6 | Toàn bộ hệ thống |
