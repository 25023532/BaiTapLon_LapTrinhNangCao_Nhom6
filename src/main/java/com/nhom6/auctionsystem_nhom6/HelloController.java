package com.nhom6.auctionsystem_nhom6;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Đang kết nối tới Server...");

        new Thread(() -> {
            try {
                // 1. Kết nối tới Server
                Socket socket = new Socket("localhost", 1234);

                // 2. Chuẩn bị luồng gửi dữ liệu
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("HELLO_SERVER"); // Gửi tín hiệu chào hỏi

                // 3. Cập nhật lại giao diện (Phải dùng Platform.runLater trong JavaFX)
                javafx.application.Platform.runLater(() -> {
                    welcomeText.setText("Kết nối thành công tới Server!");
                    System.out.println("Client: Đã gửi tín hiệu kết nối.");
                });

            } catch (IOException e) {
                javafx.application.Platform.runLater(() -> {
                    welcomeText.setText("Lỗi: Không tìm thấy Server!");
                    System.err.println("Client: Lỗi kết nối - " + e.getMessage());
                });
            }
        }).start();
    }
}