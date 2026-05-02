module com.nhom6.auctionsystem_nhom6 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;

    opens com.nhom6.auctionsystem_nhom6 to javafx.fxml;
    opens com.nhom6.auctionsystem_nhom6.network to javafx.fxml;

    exports com.nhom6.auctionsystem_nhom6;
    exports com.nhom6.auctionsystem_nhom6.network;
}