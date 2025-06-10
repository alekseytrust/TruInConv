module test.truinconv {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;
    requires org.apache.pdfbox;
    requires org.apache.poi.ooxml;
    requires com.fasterxml.jackson.databind;
    requires com.google.gson;
    requires jave.core;
    requires java.logging;

    opens test.truinconv to javafx.fxml;
    exports test.truinconv;
}