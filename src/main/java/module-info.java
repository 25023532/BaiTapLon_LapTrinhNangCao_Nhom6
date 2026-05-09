module com.nhom6.auctionsystem_nhom6 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;

    // Mở cho FXML reflection
    opens com.nhom6.auctionsystem_nhom6 to javafx.fxml;
    opens network to javafx.fxml;

    // Mở org.example packages để JavaFX có thể access
    opens org.example.auction   to javafx.fxml;
    opens org.example.user      to javafx.fxml;
    opens org.example.item      to javafx.fxml;
    opens org.example.service   to javafx.fxml;
    opens org.example.manager   to javafx.fxml;
    opens org.example.exception to javafx.fxml;

    exports com.nhom6.auctionsystem_nhom6;
    exports network;
    exports org.example.auction;
    exports org.example.user;
    exports org.example.item;
    exports org.example.service;
    exports org.example.manager;
    exports org.example.exception;
}
