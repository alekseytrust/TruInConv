module test.truinconv {
    // JavaFX core modules for controls and FXML support
    requires javafx.controls;          // UI controls (Buttons, Labels, TableView, etc.)
    requires javafx.fxml;              // FXML loading and controller injection

    // Validation and extended UI components
    requires org.controlsfx.controls;  // ControlsFX library (extra controls like CheckListView)
    requires net.synedra.validatorfx;   // ValidatorFX for form/input validation

    // Icon fonts and Bootstrap styling
    requires org.kordamp.ikonli.javafx;     // Ikonli JavaFX support for FontIcon
    requires org.kordamp.bootstrapfx.core;  // BootstrapFX for Bootstrap-themed CSS

    // Core Java SE APIs
    requires java.desktop;             // AWT/Swing interop (used by ImageIO, etc.)
    requires java.logging;             // Java util logging for debug/info/error messages
    requires java.prefs;               // Preferences API to persist theme settings

    // JSON serialization libraries
    requires com.fasterxml.jackson.databind;  // Jackson for more advanced JSON mapping
    requires com.google.gson;                 // Gson as a lightweight alternative

    // Native media encoding
    requires jave.core;                // JAVE (Java Audio Video Encoder) core bindings

    // Allow FXML to reflectively access these packages
    opens test.truinconv to javafx.fxml;           // Controllers & application classes
    opens test.truinconv.model to javafx.base;      // Model classes for JavaFX properties
    opens test.truinconv.converters to javafx.fxml; // Converter classes invoked via FXML

    // Public API packages
    exports test.truinconv;           // Main application entry and controllers
    exports test.truinconv.converters; // Converter utilities for other modules
}