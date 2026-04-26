package org.example.user;

public abstract class User {
    protected String id;
    protected String username;
    private String password; 

    public User(String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public abstract String getRole();
}
