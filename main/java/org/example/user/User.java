package org.example.user;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public abstract class User {
    protected String id;
    protected String username;
    private String hashedPassword;

    public User(String id, String username, String password) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID không được để trống");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username không được để trống");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password không được để trống");
        }

        this.id = id;
        this.username = username;
        this.hashedPassword = hashPassword(password);
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public boolean checkPassword(String password) {
        if (password == null) return false;
        return this.hashedPassword.equals(hashPassword(password));
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Không thể hash password", e);
        }
    }

    public abstract String getRole();

    @Override
    public String toString() {
        return String.format("User{id='%s', username='%s', role='%s'}", id, username, getRole());
    }
}
