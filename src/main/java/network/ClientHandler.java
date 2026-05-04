package network;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable, Observer {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Object message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.out.println("Không thể gửi tin tới client.");
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Object data = in.readObject();
                AuctionServer.notifyAllObservers("Giá mới là: " + data);
            }
        } catch (Exception e) {
            AuctionServer.removeObserver(this);
        }
    }
}