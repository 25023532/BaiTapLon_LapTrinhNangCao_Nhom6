package org.example.user;

public class Seller extends User {

    public Seller(String id, String username, String password) {
        super(id, username, password);
    }

    public Seller(String id, String username, String password,
                  String email, String fullName) {
        super(id, username, password, email, fullName);
    }

    // Constructor load từ file
    public Seller(String id, String username, String hashedPassword,
                  String email, String fullName, boolean alreadyHashed) {
        super(id, username, hashedPassword, email, fullName, alreadyHashed);
    }

    @Override public String getRole() { return "SELLER"; }
}
