module com.example.music {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires java.desktop;
    requires TarsosDSP.core;
    requires TarsosDSP.jvm;

    opens com.example.music to javafx.fxml;
    exports com.example.music;
    exports com.example.music.data;
}