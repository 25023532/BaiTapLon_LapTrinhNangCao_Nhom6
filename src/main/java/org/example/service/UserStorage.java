package org.example.service;

import org.example.user.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * UserStorage – Lưu và đọc tài khoản từ file users.json
 * File được lưu tại: thư mục chạy ứng dụng / data / users.json
 */
public class UserStorage {

    private static final String DATA_DIR  = "data";
    private static final String FILE_PATH = DATA_DIR + "/users.json";

    // ── Lưu toàn bộ danh sách user xuống file ────────────────
    public static void saveAll(Map<String, User> users) {
        try {
            // Tạo thư mục data nếu chưa có
            Files.createDirectories(Paths.get(DATA_DIR));

            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            List<User> list = new ArrayList<>(users.values());
            for (int i = 0; i < list.size(); i++) {
                User u = list.get(i);
                sb.append("  {\n");
                sb.append("    \"id\": \"").append(u.getId()).append("\",\n");
                sb.append("    \"username\": \"").append(u.getUsername()).append("\",\n");
                sb.append("    \"hashedPassword\": \"").append(u.getHashedPassword()).append("\",\n");
                sb.append("    \"role\": \"").append(u.getRole()).append("\",\n");
                sb.append("    \"email\": \"").append(u.getEmail()).append("\",\n");
                sb.append("    \"fullName\": \"").append(u.getFullName()).append("\"\n");
                sb.append("  }");
                if (i < list.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");

            Files.writeString(Paths.get(FILE_PATH), sb.toString());
            System.out.println("[Storage] Đã lưu " + users.size() + " tài khoản.");
        } catch (IOException e) {
            System.err.println("[Storage] Lỗi lưu file: " + e.getMessage());
        }
    }

    // ── Đọc tài khoản từ file lên Map ────────────────────────
    public static Map<String, User> loadAll() {
        Map<String, User> users = new LinkedHashMap<>();
        File file = new File(FILE_PATH);
        System.out.println("[Storage] Đọc từ: " + file.getAbsolutePath());

        if (!file.exists()) {
            System.out.println("[Storage] Chưa có file users.json, bắt đầu mới.");
            return users;
        }

        try {
            String content = Files.readString(Paths.get(FILE_PATH)).trim();
            if (content.equals("[]") || content.isEmpty()) return users;

            // Parse JSON thủ công (không cần thư viện)
            // Mỗi object nằm giữa { }
            String[] objects = content
                    .replaceAll("^\\[", "").replaceAll("\\]$", "") // bỏ [ ]
                    .split("\\},\\s*\\{");                          // tách từng object

            for (String obj : objects) {
                obj = obj.replaceAll("[\\[\\]\\{\\}]", "").trim();
                Map<String, String> fields = parseFields(obj);

                String id       = fields.getOrDefault("id", "");
                String username = fields.getOrDefault("username", "");
                String hashed   = fields.getOrDefault("hashedPassword", "");
                String role     = fields.getOrDefault("role", "BIDDER");
                String email    = fields.getOrDefault("email", "");
                String fullName = fields.getOrDefault("fullName", "");

                if (username.isEmpty()) continue;

                User user = createUser(role, id, username, hashed, email, fullName);
                if (user != null) users.put(username, user);
            }

            System.out.println("[Storage] Đã load " + users.size() + " tài khoản.");
        } catch (IOException e) {
            System.err.println("[Storage] Lỗi đọc file: " + e.getMessage());
        }

        return users;
    }

    // ── Parse từng dòng "key": "value" ───────────────────────
    private static Map<String, String> parseFields(String obj) {
        Map<String, String> map = new HashMap<>();
        String[] lines = obj.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.contains(":")) continue;
            String[] parts = line.split(":", 2);
            String key = parts[0].replaceAll("\"", "").trim();
            String val = parts[1].replaceAll("\"", "").replaceAll(",\\s*$", "").trim();
            map.put(key, val);
        }
        return map;
    }

    // ── Tạo đúng subclass theo role ───────────────────────────
    private static User createUser(String role, String id, String username,
                                   String hashed, String email, String fullName) {
        return switch (role.toUpperCase()) {
            case "ADMIN"  -> new Admin(id, username, hashed, email, fullName, true);
            case "SELLER" -> new Seller(id, username, hashed, email, fullName, true);
            default       -> new Bidder(id, username, hashed, email, fullName, true);
        };
    }
}
