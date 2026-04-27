package org.example.user;

public class Admin extends User {
    public Admin(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }
}
