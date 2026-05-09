package org.example.user;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public abstract class User {

    protected String id;
    protected String username;
    protected String email;
    protected String fullName;
    private   String hashedPassword;

    // ── Constructor thông thường (đăng ký mới) ───────────────
    public User(String id, String username, String password) {
        this(id, username, password, "", "");
    }

    public User(String id, String username, String password,
                String email, String fullName) {
        validate(id, username, password);
        this.id             = id;
        this.username       = username;
        this.email          = email     != null ? email     : "";
        this.fullName       = fullName  != null ? fullName  : "";
        this.hashedPassword = hashPassword(password);
    }

    // ── Constructor load từ file (nhận hash sẵn) ─────────────
    public User(String id, String username, String hashedPassword,
                String email, String fullName, boolean alreadyHashed) {
        this.id             = id;
        this.username       = username;
        this.hashedPassword = hashedPassword;
        this.email          = email     != null ? email     : "";
        this.fullName       = fullName  != null ? fullName  : "";
    }

    // ── Validation ────────────────────────────────────────────
    private void validate(String id, String username, String password) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("ID không được để trống");
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username không được để trống");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password không được để trống");
        if (password.length() < 6)
            throw new IllegalArgumentException("Password phải có ít nhất 6 ký tự");
    }

    // ── Kiểm tra mật khẩu ────────────────────────────────────
    public boolean checkPassword(String password) {
        if (password == null) return false;
        return this.hashedPassword.equals(hashPassword(password));
    }

    // ── Hash SHA-256 ──────────────────────────────────────────
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Không thể hash password", e);
        }
    }

    // ── Getters ───────────────────────────────────────────────
    public String getId()             { return id; }
    public String getUsername()       { return username; }
    public String getEmail()          { return email; }
    public String getFullName()       { return fullName; }
    public String getHashedPassword() { return hashedPassword; }

    public abstract String getRole();

    // ── Setters ✅ ────────────────────────────────────────────
    public void setFullName(String fullName) {
        this.fullName = fullName != null ? fullName.trim() : "";
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim() : "";
    }

    public void setPassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank())
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        if (newPassword.length() < 6)
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        this.hashedPassword = hashPassword(newPassword);
    }

    @Override
    public String toString() {
        return String.format("User{id='%s', username='%s', role='%s'}",
                id, username, getRole());
    }
}
