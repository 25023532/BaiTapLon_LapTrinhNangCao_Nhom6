package com.nhom6.auctionsystem_nhom6;

import java.io.*;
import java.util.Properties;

/**
 * Đọc cấu hình server từ file server.properties.
 * Tìm theo thứ tự:
 *   1. Thư mục chạy app (cạnh pom.xml)
 *   2. resources/
 *   3. Mặc định: localhost:1234
 */
public class ServerConfig {

    private static final String CONFIG_FILE = "server.properties";

    private static String host = "localhost";
    private static int    port = 1234;

    static { load(); }

    // =========================================================
    // LOAD
    // =========================================================
    private static void load() {
        // Ưu tiên 1: file trong thư mục chạy
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(fis);
                host = props.getProperty("server.host", "localhost").trim();
                port = Integer.parseInt(
                        props.getProperty("server.port", "1234").trim());
                System.out.println("[ServerConfig] Đọc từ file: "
                        + host + ":" + port);
                return;
            } catch (Exception e) {
                System.err.println("[ServerConfig] Lỗi đọc file: "
                        + e.getMessage());
            }
        }

        // Ưu tiên 2: resources/
        try (InputStream is = ServerConfig.class
                .getResourceAsStream("/" + CONFIG_FILE)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                host = props.getProperty("server.host", "localhost").trim();
                port = Integer.parseInt(
                        props.getProperty("server.port", "1234").trim());
                System.out.println("[ServerConfig] Đọc từ resources: "
                        + host + ":" + port);
                return;
            }
        } catch (Exception e) {
            System.err.println("[ServerConfig] Lỗi đọc resources: "
                    + e.getMessage());
        }

        System.out.println("[ServerConfig] Dùng mặc định: localhost:1234");
    }

    // =========================================================
    // SAVE — ghi lại khi người dùng đổi IP
    // =========================================================
    public static void save(String newHost, int newPort) {
        host = newHost;
        port = newPort;
        try (PrintWriter pw = new PrintWriter(new File(CONFIG_FILE))) {
            pw.println("# Cấu hình AuctionServer");
            pw.println("# Sửa server.host thành IP máy chạy AuctionServer");
            pw.println("server.host=" + newHost);
            pw.println("server.port=" + newPort);
            System.out.println("[ServerConfig] Đã lưu: "
                    + newHost + ":" + newPort);
        } catch (Exception e) {
            System.err.println("[ServerConfig] Lỗi lưu: " + e.getMessage());
        }
    }

    /** Tạo file mặc định nếu chưa có */
    public static void createDefaultIfMissing() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) return;
        save("localhost", 1234);
    }

    // =========================================================
    // GETTERS
    // =========================================================
    public static String getHost() { return host; }
    public static int    getPort() { return port; }

    /** IP máy hiện tại (để hiển thị gợi ý cho user) */
    public static String getLocalIP() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "Không xác định";
        }
    }
}
