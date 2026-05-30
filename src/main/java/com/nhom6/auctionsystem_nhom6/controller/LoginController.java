package com.nhom6.auctionsystem_nhom6.controller;

import com.nhom6.auctionsystem_nhom6.AppContext;
import com.nhom6.auctionsystem_nhom6.HelloApplication;
import com.nhom6.auctionsystem_nhom6.ServerConnection;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.example.service.AuthService;
import org.example.user.User;

import java.util.Random;

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private StackPane     rootPane;

    private final AuthService authService = AppContext.getAuthService();
    private AnimationTimer    starTimer;

    // ── Star data ──────────────────────────────────────────────
    private static final int   STAR_COUNT = 120;
    private final float[]      sx  = new float[STAR_COUNT];
    private final float[]      sy  = new float[STAR_COUNT];
    private final float[]      sz  = new float[STAR_COUNT]; // size
    private final float[]      sa  = new float[STAR_COUNT]; // alpha
    private final float[]      sda = new float[STAR_COUNT]; // alpha delta
    private final Random       rng = new Random();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleLogin();
        });

        // Thêm canvas sao sau khi scene sẵn sàng
        javafx.application.Platform.runLater(this::initStars);
    }

    // ── Khởi tạo và vẽ sao vàng li ti ─────────────────────────
    private void initStars() {
        if (rootPane == null) return;

        double w = rootPane.getWidth()  > 0 ? rootPane.getWidth()  : 900;
        double h = rootPane.getHeight() > 0 ? rootPane.getHeight() : 700;

        Canvas canvas = new Canvas(w, h);
        canvas.setMouseTransparent(true);
        rootPane.widthProperty() .addListener((o, ov, nv) -> canvas.setWidth(nv.doubleValue()));
        rootPane.heightProperty().addListener((o, ov, nv) -> canvas.setHeight(nv.doubleValue()));
        // Thêm canvas xuống dưới cùng (dưới background, trên mọi thứ)
        rootPane.getChildren().add(0, canvas);

        // Khởi tạo vị trí sao ngẫu nhiên
        for (int i = 0; i < STAR_COUNT; i++) {
            randomizeStar(i, w, h);
            sa[i]  = (float) rng.nextDouble(); // alpha ban đầu ngẫu nhiên
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        starTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                double cw = canvas.getWidth();
                double ch = canvas.getHeight();
                gc.clearRect(0, 0, cw, ch);

                for (int i = 0; i < STAR_COUNT; i++) {
                    // Twinkle: thay đổi alpha
                    sa[i] += sda[i];
                    if (sa[i] >= 1f) { sa[i] = 1f; sda[i] = -sda[i]; }
                    if (sa[i] <= 0f) { randomizeStar(i, cw, ch); }

                    // Màu vàng đồng: #c9a84c với alpha thay đổi
                    gc.setFill(Color.color(0.788, 0.659, 0.298, sa[i]));
                    double r = sz[i];
                    gc.fillOval(sx[i] - r, sy[i] - r, r * 2, r * 2);
                }
            }
        };
        starTimer.start();
    }

    private void randomizeStar(int i, double w, double h) {
        sx[i]  = (float)(rng.nextDouble() * w);
        sy[i]  = (float)(rng.nextDouble() * h);
        sz[i]  = (float)(0.5 + rng.nextDouble() * 2.0); // size 0.5–2.5px
        sa[i]  = (float)(rng.nextDouble());
        sda[i] = (float)(0.003 + rng.nextDouble() * 0.012); // tốc độ nhấp nháy
    }

    private void stopStars() {
        if (starTimer != null) { starTimer.stop(); starTimer = null; }
    }

    // ── Login / Register ───────────────────────────────────────
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        errorLabel.setVisible(false);

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
            return;
        }

        User user = authService.login(username, password);
        if (user == null) {
            showError("Sai tên đăng nhập hoặc mật khẩu.");
            passwordField.clear();
            return;
        }

        AppContext.setCurrentUser(user);

        ServerConnection conn = ServerConnection.getInstance();
        if (!conn.isConnected()) conn.connect(username);

        try {
            stopStars();
            if ("ADMIN".equals(user.getRole())) {
                HelloApplication.showAdminView();
            } else {
                HelloApplication.showMainView();
            }
        } catch (Exception e) {
            showError("Lỗi khi mở giao diện: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister() {
        try {
            stopStars();
            HelloApplication.showRegisterView();
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
