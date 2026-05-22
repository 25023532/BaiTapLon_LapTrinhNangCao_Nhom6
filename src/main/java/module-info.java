module com.nhom6.auctionsystem_nhom6 {

    // ── JavaFX ────────────────────────────────────────────────
    requires javafx.controls;
    requires javafx.fxml;

    // ── Java standard ─────────────────────────────────────────
    requires java.base;

    // ── WebSocket (kết nối đa máy qua internet) ───────────────
    requires org.java_websocket;

    // ── Logging (Java-WebSocket dùng SLF4J) ───────────────────
    requires org.slf4j;

    // ── SQL  ───────────────────
    requires java.sql;

    // ── Mở cho FXML reflection ────────────────────────────────
    opens com.nhom6.auctionsystem_nhom6 to javafx.fxml;
    opens com.nhom6.auctionsystem_nhom6.controller to javafx.fxml;
    opens network                        to javafx.fxml;
    opens org.example.auction            to javafx.fxml;
    opens org.example.user               to javafx.fxml;
    opens org.example.item               to javafx.fxml;
    opens org.example.service            to javafx.fxml;
    opens org.example.manager            to javafx.fxml;
    opens org.example.exception          to javafx.fxml;

    // ── Export ────────────────────────────────────────────────
    exports com.nhom6.auctionsystem_nhom6;
    exports com.nhom6.auctionsystem_nhom6.controller;
    exports network;
    exports org.example.auction;
    exports org.example.user;
    exports org.example.item;
    exports org.example.service;
    exports org.example.manager;
    exports org.example.exception;

}
