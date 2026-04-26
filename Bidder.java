package org.example.user;

import org.example.model.Bid;
import org.example.observer.Observer;

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
        System.out.println(username + " sees new bid: " + bid.getAmount());
    }
}
