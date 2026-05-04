package org.example.user;

import org.example.auction.Bid;
import org.example.auction.Observer;

public class Bidder extends User implements Observer {

    public Bidder(String id, String username, String password) {
        super(id, username, password);
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }

    @Override
    public void update(Bid bid) {
        System.out.println("[NOTIFY] " + username + " sees new bid: " + bid.getAmount()
                + " from " + bid.getBidderId());
    }
}
