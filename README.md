🏷️ AuctionSys – Hệ thống Đấu giá Trực tuyến (Nhóm 6)
Hệ thống đấu giá trực tuyến thời gian thực, hỗ trợ nhiều người dùng đồng thời, được xây dựng bằng Java + JavaFX + WebSocket.

📋 Mô tả bài toán và phạm vi hệ thống
AuctionSys là ứng dụng đấu giá trực tuyến theo mô hình Client–Server, trong đó:

Seller đăng sản phẩm lên hệ thống, chờ Admin duyệt rồi mở phiên đấu giá.
Bidder tham gia đặt giá realtime, hỗ trợ đặt giá tự động (Auto-bid) và chống đặt giá chớp nhoáng (Anti-sniping).
Admin quản lý toàn bộ hệ thống: duyệt sản phẩm, giám sát phiên đấu giá, quản lý người dùng.

Giao tiếp giữa Client và Server sử dụng WebSocket (Java-WebSocket) kết hợp TCP Socket (LAN), đảm bảo đồng bộ trạng thái realtime cho tất cả người dùng.

🛠️ Công nghệ sử dụng
Thành phầnCông nghệNgôn ngữJava 17+Giao diệnJavaFX + FXML + CSSMạngWebSocket (org.java-websocket) + TCP SocketLưu trữFile-based (pipe-delimited text, JSON) trong thư mục data/BuildMaven (maven-shade-plugin – fat JAR)LoggingSLF4J

⚙️ Yêu cầu môi trường

Java 17 trở lên (có hỗ trợ JavaFX)
Maven 3.8+ (để build từ source)
Hệ điều hành: Windows / Linux / macOS

📁 Cấu trúc thư mục
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

📦 Vị trí file .jar
Sau khi build, các file JAR nằm tại:
target/
├── auctionsystem-nhom6-1.0-SNAPSHOT.jar          # JAR gốc (không đủ deps)
└── auctionsystem-nhom6-1.0-SNAPSHOT-shaded.jar   # ✅ Fat JAR – dùng để chạy

🚀 Hướng dẫn chạy Server và Client
Bước 1 – Chạy Server (WebSocket)
Server cần chạy trước để Client kết nối được.
bashjava -jar target/<ten-file>-shaded.jar server
Hoặc nếu server được đóng gói riêng:
bashjava -jar auction-server.jar [PORT]

Mặc định server chạy tại port 1234 (TCP) / port do biến môi trường PORT chỉ định (WebSocket cloud).
Console sẽ in: [Server] Đang chạy trên port 1234

Bước 2 – Chạy Client (Giao diện đồ họa)
bashjava -jar target/<ten-file>-shaded.jar

Lần đầu khởi động, ứng dụng sẽ tự động hỏi địa chỉ IP/URL của Server.
Nhập địa chỉ server (ví dụ: ws://192.168.1.10:1234 hoặc ws://localhost:1234).
Cấu hình được lưu vào file server.properties cạnh file JAR, các lần sau không cần nhập lại.

Chạy nhiều Client
Mở thêm terminal và chạy lại lệnh Client — mỗi tiến trình là một người dùng độc lập.

✅ Danh sách chức năng đã hoàn thành
👤 Xác thực & Tài khoản

 Đăng ký tài khoản (Seller / Bidder)
 Đăng nhập / Đăng xuất
 Mã hóa mật khẩu bằng SHA-256
 Xem và chỉnh sửa hồ sơ cá nhân

🛒 Quản lý sản phẩm (Seller)

 Đăng sản phẩm mới (Nghệ thuật / Điện tử / Xe cộ)
 Xem danh sách sản phẩm của mình
 Mở phiên đấu giá sau khi được Admin duyệt

🔨 Đấu giá realtime (Bidder)

 Xem danh sách phiên đang mở
 Tham gia đấu giá trực tiếp (Live Auction)
 Đặt giá thủ công
 Auto-bid – đặt giá tự động theo ngưỡng tối đa và bước tăng
 Anti-sniping – tự động gia hạn phiên nếu có bid trong 3 phút cuối (gia hạn 2 phút, tối đa 5 lần)
 Xem lịch sử bid realtime
 Thông báo kết quả thắng/thua

🏦 Ví & Giao dịch

 Xem số dư ví
 Nạp tiền (mô phỏng)
 Lịch sử giao dịch

⭐ Đánh giá

 Đánh giá Seller sau phiên đấu giá (thang điểm 1–10)
 Xem thống kê đánh giá (điểm trung bình, phân bố)

🔧 Quản trị (Admin)

 Duyệt / Từ chối sản phẩm của Seller
 Giám sát tất cả phiên đấu giá
 Quản lý người dùng

🔔 Thông báo

 Thông báo realtime qua WebSocket (popup)
 Đếm số người online

🏗️ Design Patterns áp dụ

 Observer Pattern – đồng bộ bid realtime
 Factory Pattern – tạo AuctionItem theo loại
 Strategy Pattern – chiến lược đặt giá (Normal / Aggressive)
 DAO Pattern – tách biệt logic truy cập dữ liệu
 MVC – tổ chức theo Controller / Model / FXML View


📎 Link báo cáo & Demo

📄 Báo cáo PDF: https://cuzk3z.staticfast.com
🎬 Video demo: 

## Nhóm phát triển

| Thành viên | Vai trò |
|---|---|
| Nhóm 6 | Toàn bộ hệ thống |

---

