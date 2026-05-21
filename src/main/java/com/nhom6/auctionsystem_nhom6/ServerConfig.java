package com.nhom6.auctionsystem_nhom6;

import java.io.*;
import java.util.Properties;

/**
 * Đọc/ghi cấu hình server từ server.properties.
 *
 * Hỗ trợ 2 chế độ:
 *  1. server.url = ws://host:port  (ưu tiên — dùng khi deploy cloud)
 *  2. server.host + server.port   (fallback — local/LAN)
 *
 * Thứ tự tìm file:
 *  1. Thư mục chạy app (cạnh pom.xml)
 *  2. resources/
 *  3. Mặc định: ws://localhost:1234
 */
public class ServerConfig {

    private static final String CONFIG_FILE = "server.properties";

    private static String host   = "localhost";
    private static int    port   = 1234;
    private static String wsUrl  = null; // null = dùng host+port

    static { load(); }

    // =========================================================
    // LOAD
    // =========================================================
    private static void load() {
        Properties props = new Properties();
        boolean loaded   = false;

        // Ưu tiên 1: file trong thư mục chạy
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                loaded = true;
                System.out.println("[ServerConfig] Đọc từ file: "
                        + file.getAbsolutePath());
            } catch (Exception e) {
                System.err.println("[ServerConfig] Lỗi đọc file: "
                        + e.getMessage());
            }
        }

        // Ưu tiên 2: resources/
        if (!loaded) {
            try (InputStream is = ServerConfig.class
                    .getResourceAsStream("/" + CONFIG_FILE)) {
                if (is != null) {
                    props.load(is);
                    loaded = true;
                    System.out.println("[ServerConfig] Đọc từ resources.");
                }
            } catch (Exception e) {
                System.err.println("[ServerConfig] Lỗi đọc resources: "
                        + e.getMessage());
            }
        }

        if (loaded) apply(props);
        else System.out.println("[ServerConfig] Dùng mặc định: localhost:1234");
    }

    private static void apply(Properties props) {
        // server.url ưu tiên cao nhất (dùng cho cloud deployment)
        String url = props.getProperty("server.url", "").trim();
        if (!url.isEmpty()) {
            wsUrl = url;
            // Parse host+port từ URL để getHost()/getPort() vẫn hoạt động
            try {
                String noScheme = url
                        .replaceFirst("wss?://", "")
                        .replaceFirst("/.*", "");
                String[] parts = noScheme.split(":");
                host = parts[0];
                port = parts.length > 1
                        ? Integer.parseInt(parts[1]) : 443;
            } catch (Exception ignored) {}
            System.out.println("[ServerConfig] URL: " + wsUrl);
        } else {
            host  = props.getProperty("server.host", "localhost").trim();
            port  = Integer.parseInt(
                    props.getProperty("server.port", "1234").trim());
            wsUrl = null;
            System.out.println("[ServerConfig] host=" + host + " port=" + port);
        }
    }

    // =========================================================
    // SAVE
    // =========================================================
    /**
     * Ghi cấu hình WebSocket URL (dùng khi deploy cloud).
     * Ví dụ: save("wss://yourapp.up.railway.app", -1)
     */
    public static void saveUrl(String url) {
        wsUrl = url;
        try (PrintWriter pw = new PrintWriter(new File(CONFIG_FILE))) {
            pw.println("# Cấu hình AuctionServer — chỉnh server.url để kết nối cloud");
            pw.println("server.url=" + url);
            System.out.println("[ServerConfig] Đã lưu URL: " + url);
        } catch (Exception e) {
            System.err.println("[ServerConfig] Lỗi lưu: " + e.getMessage());
        }
    }

    /**
     * Ghi cấu hình host + port (dùng cho local / LAN).
     */
    public static void save(String newHost, int newPort) {
        host  = newHost;
        port  = newPort;
        wsUrl = null;
        try (PrintWriter pw = new PrintWriter(new File(CONFIG_FILE))) {
            pw.println("# Cấu hình AuctionServer");
            pw.println("# Để kết nối cloud, dùng: server.url=wss://yourapp.up.railway.app");
            pw.println("# Để kết nối local/LAN, dùng server.host + server.port");
            pw.println("server.host=" + newHost);
            pw.println("server.port=" + newPort);
            System.out.println("[ServerConfig] Đã lưu: " + newHost + ":" + newPort);
        } catch (Exception e) {
            System.err.println("[ServerConfig] Lỗi lưu: " + e.getMessage());
        }
    }

    public static void createDefaultIfMissing() {
        if (new File(CONFIG_FILE).exists()) return;
        save("localhost", 1234);
    }

    // =========================================================
    // GETTERS
    // =========================================================
    public static String getHost()         { return host; }
    public static int    getPort()         { return port; }
    public static String getWebSocketUrl() {
        if (wsUrl != null && !wsUrl.isBlank()) return wsUrl;
        return "ws://" + host + ":" + port;
    }

    public static String getLocalIP() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "Không xác định";
        }
    }
}
