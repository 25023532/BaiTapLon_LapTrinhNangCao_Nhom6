package org.example.user;

public class Admin extends User {

    public Admin(String id, String username, String password) {
        super(id, username, password);
    }

    public Admin(String id, String username, String password,
                 String email, String fullName) {
        super(id, username, password, email, fullName);
    }

    // Constructor load từ file (hash sẵn)
    public Admin(String id, String username, String hashedPassword,
                 String email, String fullName, boolean alreadyHashed) {
        super(id, username, hashedPassword, email, fullName, alreadyHashed);
    }

    @Override
    public String getRole() { return "ADMIN"; }
}
