package org.example.user;

import org.example.auction.Bid;
import org.example.auction.Observer;

public class Bidder extends User implements Observer {

    // Constructor đăng ký mới
    public Bidder(String id, String username, String password) {
        super(id, username, password);
    }

    public Bidder(String id, String username, String password,
                  String email, String fullName) {
        super(id, username, password, email, fullName);
    }

    // Constructor load từ file
    public Bidder(String id, String username, String hashedPassword,
                  String email, String fullName, boolean alreadyHashed) {
        super(id, username, hashedPassword, email, fullName, alreadyHashed);
    }

    @Override public String getRole() { return "BIDDER"; }

    @Override
    public void update(Bid bid) {
        System.out.println("[NOTIFY] " + username + " sees new bid: "
                + bid.getAmount() + " from " + bid.getBidderId());
    }
}
