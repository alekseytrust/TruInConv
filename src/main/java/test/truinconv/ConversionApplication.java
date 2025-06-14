/*
 * Copyright (c) 2025 Aleksey Trust
 * This file is licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package test.truinconv;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for the TruInConv JavaFX application.
 * <p>
 * Handles loading the UI, configuring the primary stage, applying a theme,
 * and handling application-level errors.
 */
public class ConversionApplication extends Application {

    private static final Logger LOGGER = Logger.getLogger(ConversionApplication.class.getName());
    
    /**
     * CSS stylesheet used for styling the application's UI theme.
     */
    private static final String THEME_STYLESHEET = "/test/truinconv/ThemeStyles.css";
    
    private static final String FXML_VIEW = "/test/truinconv/start-view.fxml";
    private static final String APP_TITLE = "TruInConv";
    private static final double INITIAL_WIDTH = 600;
    private static final double INITIAL_HEIGHT = 400;
    private static final double MIN_WIDTH = 350;
    private static final double MIN_HEIGHT = 400;

    /**
     * Starts the JavaFX application by loading the main view and configuring the stage.
     *
     * @param primaryStage the main application window provided by the JavaFX runtime
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            Scene scene = loadMainScene();
            configureStage(primaryStage, scene);
            primaryStage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load application", e);
            showLoadErrorDialog(e.getMessage());
            Platform.exit();
        }
    }
    
    /**
     * Loads the main scene with FXML and applies styling.
     *
     * @return the configured Scene
     * @throws IOException if FXML loading fails
     */
    private Scene loadMainScene() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(FXML_VIEW));
        Scene scene = new Scene(fxmlLoader.load(), INITIAL_WIDTH, INITIAL_HEIGHT);
        addThemeStylesheet(scene);
        return scene;
    }
    
    /**
     * Configures the primary stage with title, scene, and constraints.
     *
     * @param primaryStage the stage to configure
     * @param scene the scene to set
     */
    private void configureStage(Stage primaryStage, Scene scene) {
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.setResizable(true);
    }

    /**
     * Applies the application's theme stylesheet to the given scene.
     *
     * @param scene the scene to which the theme will be applied
     */
    private void addThemeStylesheet(Scene scene) {
        try {
            var stylesheetUrl = getClass().getResource(THEME_STYLESHEET);
            if (stylesheetUrl != null) {
                scene.getStylesheets().add(stylesheetUrl.toExternalForm());
            } else {
                LOGGER.warning("Theme stylesheet not found: " + THEME_STYLESHEET);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load theme stylesheet", e);
        }
    }

    /**
     * Displays an error dialog to the user if the application fails to load.
     *
     * @param message error message to display in the dialog
     */
    private void showLoadErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Application Error");
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