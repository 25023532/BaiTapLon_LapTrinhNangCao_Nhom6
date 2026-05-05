module com.nhom6.auctionsystem_nhom6 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;

    opens network to javafx.fxml;
    opens com.nhom6.auctionsystem_nhom6 to javafx.fxml;

    exports network;
    exports com.nhom6.auctionsystem_nhom6;
}