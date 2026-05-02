package com.nhom6.auctionsystem_nhom6.network;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            System.out.println("Đang phục vụ Client tại: " + socket.getRemoteSocketAddress());

        } catch (IOException e) {
            System.out.println("Lỗi kết nối Client: " + e.getMessage());
        }
    }
}