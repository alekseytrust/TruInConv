package test.truinconv;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * @TODO
 * Make it so all elements in the Conversion Controller page grow proportionally
 *  - Also all the elements must be able to fit in the start window size and any window size and not overflow
 *
 * @TODO
 * Fix the theme issue: the theme does not stay between both pages. If you change to dark it doesn't stay that way
 *  -
 *
 */




/**
 * Entry point for the TruInConv JavaFX application.
 * <p>
 * Handles loading the UI, configuring the primary stage, applying a theme,
 * and handling application-level errors.
 */
public class ConversionApplication extends Application {

    /**
     * CSS stylesheet used for styling the application's UI theme.
     */
    private static final String THEME_STYLESHEET = "ThemeStyles.css";

    /**
     * Starts the JavaFX application by loading the main view and configuring the stage.
     *
     * @param primaryStage the main application window provided by the JavaFX runtime
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(
                    ConversionApplication.class.getResource("start-view.fxml")
            );
            Scene scene = new Scene(fxmlLoader.load(), 600, 400);
            primaryStage.setTitle("TruInConv");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(350);
            primaryStage.setMinHeight(400);
            primaryStage.setResizable(true);

            addThemeStylesheet(scene);

            primaryStage.show();
        } catch (IOException e) {
            showLoadErrorDialog(e.getMessage());
            Platform.exit();
        }
    }

    /**
     * Applies the application's theme stylesheet to the given scene.
     *
     * @param scene the scene to which the theme will be applied
     */
    private void addThemeStylesheet(Scene scene) {
        String stylesheetUrl = Objects.requireNonNull(
            ConversionApplication.class.getResource(THEME_STYLESHEET)
        ).toExternalForm();
        scene.getStylesheets().add(stylesheetUrl);
    }

    /**
     * Displays an error dialog to the user if the application fails to load.
     *
     * @param message error message to display in the dialog
     */
    private void showLoadErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Failed to load application");
        alert.setContentText("Could not load the FXML file: " + message);
        alert.showAndWait();
    }

    /**
     * Launches the JavaFX application.
    */
    public static void main(String[] args) {
        launch(args);
    }
}