package test.truinconv;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the application's start view.
 * Manages the main menu interface with proportional responsive sizing.
 */
public class StartViewController {

    private static final Logger LOGGER = Logger.getLogger(StartViewController.class.getName());

    // FXML-injected UI components
    @FXML private Label title;
    @FXML private HBox buttonPane;
    @FXML private Button imageButton;
    @FXML private Button audioButton;
    @FXML private Button videoButton;
    @FXML private ToggleButton themeToggleBtn;

    // State management
    private ThemeManager themeManager;
    private boolean isInitialized = false;

    // Layout configuration for proportional scaling
    private static final class LayoutConstants {
        // Base dimensions - the "ideal" starting size
        static final double BASE_WINDOW_WIDTH = 600.0;
        static final double BASE_WINDOW_HEIGHT = 550.0; // Increased by 30px as requested
        static final double BASE_BUTTON_SIZE = 100.0;
        static final double BASE_TITLE_SIZE = 44.0; // 2.8em * 16px base
        static final double BASE_SPACING = 36.0;
        static final double BASE_THEME_ICON_SIZE = 24.0; // Base theme icon size
        static final double BASE_THEME_BUTTON_SIZE = 40.0; // Base theme button size
        
        // Minimum constraints
        static final double MIN_BUTTON_SIZE = 80.0;
        static final double MIN_TITLE_SIZE = 32.0;
        static final double MIN_SPACING = 24.0;
        static final double MIN_THEME_ICON_SIZE = 18.0;
        static final double MIN_THEME_BUTTON_SIZE = 30.0;
        static final double MIN_SIDE_MARGIN = 15.0; // 15px minimum from left/right sides
        
        // Layout structure
        static final double BUTTON_PANE_PADDING = 40.0; // HBox padding (20 * 2)
        static final double BORDER_PANE_PADDING = 50.0; // BorderPane padding (25 * 2)
        static final double VBOX_SPACING = 24.0;
        static final double TOP_SECTION_HEIGHT = 50.0; // Theme toggle area
        static final int BUTTON_COUNT = 3;
    }

    /**
     * Enumeration for conversion types.
     */
    public enum ConversionCategory {
        IMAGES("Images", "Images"),
        AUDIO("Audio", "Audio"),
        VIDEO("Video", "Video");

        private final String buttonText;
        private final String conversionType;

        ConversionCategory(String buttonText, String conversionType) {
            this.buttonText = buttonText;
            this.conversionType = conversionType;
        }

        public String getButtonText() {
            return buttonText;
        }
        
        public String getConversionType() {
            return conversionType;
        }

        public static ConversionCategory fromButtonText(String text) {
            for (ConversionCategory category : values()) {
                if (category.getButtonText().equalsIgnoreCase(text)) {
                    return category;
                }
            }
            throw new IllegalArgumentException("Unknown button text: " + text);
        }
    }

    @FXML
    private void initialize() {
        try {
            validateFXMLInjection();
            themeManager = ThemeManager.getInstance();
            Platform.runLater(() -> initializeAsync());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during controller initialization", e);
        }
    }

    /**
     * Validates that all FXML components have been properly injected.
     */
    private void validateFXMLInjection() {
        Objects.requireNonNull(title, "Title label not injected");
        Objects.requireNonNull(buttonPane, "Button pane not injected");
        Objects.requireNonNull(imageButton, "Image button not injected");
        Objects.requireNonNull(audioButton, "Audio button not injected");
        Objects.requireNonNull(videoButton, "Video button not injected");
        Objects.requireNonNull(themeToggleBtn, "Theme toggle button not injected");
    }

    /**
     * Asynchronous initialization to avoid blocking the UI thread.
     */
    private void initializeAsync() {
        try {
            setupButtons();
            setupButtonActions();
            setupTheme();
            setupResponsiveLayout();
            enforceMinimumWindowSize();
            isInitialized = true;
            
            if (isDebugMode()) {
                LOGGER.info("StartViewController initialized successfully");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during async initialization", e);
        }
    }

    /**
     * Sets up the buttons with proper CSS classes and no inline styling.
     */
    private void setupButtons() {
        try {
            // Start with base size class
            ensureButtonClass(imageButton, "circle-btn");
            ensureButtonClass(audioButton, "circle-btn");
            ensureButtonClass(videoButton, "circle-btn");

            // Set button texts
            imageButton.setText("Images");
            audioButton.setText("Audio");
            videoButton.setText("Video");

            // Remove any inline styles that might interfere
            imageButton.setStyle("");
            audioButton.setStyle("");
            videoButton.setStyle("");

            LOGGER.info("Buttons configured with CSS classes");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting up buttons", e);
        }
    }

    /**
     * Ensures a button has the specified CSS classes.
     */
    private void ensureButtonClass(Button button, String... classes) {
        for (String className : classes) {
            if (!button.getStyleClass().contains(className)) {
                button.getStyleClass().add(className);
            }
        }
    }

    /**
     * Sets up responsive layout with proportional scaling.
     */
    private void setupResponsiveLayout() {
        try {
            // Wait for scene to be available
            if (buttonPane.getScene() != null) {
                setupWindowListeners();
            } else {
                buttonPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) {
                        setupWindowListeners();
                        enforceMinimumWindowSize();
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting up responsive layout", e);
        }
    }

    /**
     * Sets up window size listeners for proportional responsive behavior.
     */
    private void setupWindowListeners() {
        try {
            Window window = buttonPane.getScene().getWindow();
            if (window != null) {
                window.widthProperty().addListener((obs, oldWidth, newWidth) -> updateProportionalSizing());
                window.heightProperty().addListener((obs, oldHeight, newHeight) -> updateProportionalSizing());
                
                // Initial size update
                Platform.runLater(this::updateProportionalSizing);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error setting up window listeners", e);
        }
    }

    /**
     * Updates all element sizes proportionally based on window size.
     */
    private void updateProportionalSizing() {
        if (!isInitialized) return;
        
        try {
            Platform.runLater(() -> {
                Window window = buttonPane.getScene().getWindow();
                if (window == null) return;

                double currentWidth = window.getWidth();
                double currentHeight = window.getHeight();
                
                // Calculate scaling factors
                double widthScale = currentWidth / LayoutConstants.BASE_WINDOW_WIDTH;
                double heightScale = currentHeight / LayoutConstants.BASE_WINDOW_HEIGHT;
                
                // Use the smaller scale to maintain proportions and fit within window
                double scale = Math.min(widthScale, heightScale);
                
                // Calculate proportional sizes
                double scaledButtonSize = LayoutConstants.BASE_BUTTON_SIZE * scale;
                double scaledTitleSize = LayoutConstants.BASE_TITLE_SIZE * scale;
                double scaledSpacing = LayoutConstants.BASE_SPACING * scale;
                double scaledThemeIconSize = LayoutConstants.BASE_THEME_ICON_SIZE * scale;
                double scaledThemeButtonSize = LayoutConstants.BASE_THEME_BUTTON_SIZE * scale;
                
                // Apply minimum constraints
                scaledButtonSize = Math.max(scaledButtonSize, LayoutConstants.MIN_BUTTON_SIZE);
                scaledTitleSize = Math.max(scaledTitleSize, LayoutConstants.MIN_TITLE_SIZE);
                scaledSpacing = Math.max(scaledSpacing, LayoutConstants.MIN_SPACING);
                scaledThemeIconSize = Math.max(scaledThemeIconSize, LayoutConstants.MIN_THEME_ICON_SIZE);
                scaledThemeButtonSize = Math.max(scaledThemeButtonSize, LayoutConstants.MIN_THEME_BUTTON_SIZE);
                
                // Ensure minimum side margins (15px from each side)
                double requiredWidth = (LayoutConstants.BUTTON_COUNT * scaledButtonSize) + 
                                     ((LayoutConstants.BUTTON_COUNT - 1) * scaledSpacing) + 
                                     LayoutConstants.BUTTON_PANE_PADDING + 
                                     LayoutConstants.BORDER_PANE_PADDING + 
                                     (2 * LayoutConstants.MIN_SIDE_MARGIN);
                
                if (requiredWidth > currentWidth) {
                    // Recalculate button size to fit with minimum margins
                    double availableForButtons = currentWidth - LayoutConstants.BUTTON_PANE_PADDING - 
                                               LayoutConstants.BORDER_PANE_PADDING - 
                                               (2 * LayoutConstants.MIN_SIDE_MARGIN) - 
                                               ((LayoutConstants.BUTTON_COUNT - 1) * scaledSpacing);
                    scaledButtonSize = Math.max(availableForButtons / LayoutConstants.BUTTON_COUNT, 
                                              LayoutConstants.MIN_BUTTON_SIZE);
                }
                
                // Apply the calculated sizes
                applyProportionalSizes(scaledButtonSize, scaledTitleSize, scaledSpacing, 
                                     scaledThemeIconSize, scaledThemeButtonSize);
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating proportional sizing", e);
        }
    }

    /**
     * Applies calculated proportional sizes to UI elements.
     */
    private void applyProportionalSizes(double buttonSize, double titleSize, double spacing, 
                                      double themeIconSize, double themeButtonSize) {
        try {
            // Update button sizes with inline styles for precise control
            String buttonStyle = String.format(
                "-fx-min-width: %.1fpx; -fx-min-height: %.1fpx; " +
                "-fx-max-width: %.1fpx; -fx-max-height: %.1fpx; " +
                "-fx-pref-width: %.1fpx; -fx-pref-height: %.1fpx; " +
                "-fx-font-size: %.1fpx;",
                buttonSize, buttonSize, buttonSize, buttonSize, buttonSize, buttonSize,
                buttonSize * 0.16 // Font size as 16% of button size
            );
            
            imageButton.setStyle(buttonStyle);
            audioButton.setStyle(buttonStyle);
            videoButton.setStyle(buttonStyle);
            
            // Update title size
            title.setStyle(String.format("-fx-font-size: %.1fpx;", titleSize));
            
            // Update button pane spacing
            buttonPane.setSpacing(spacing);
            
            // Update theme toggle button and icon sizes
            updateThemeToggleSize(themeButtonSize, themeIconSize);
            
            LOGGER.fine(String.format("Applied proportional sizes - Button: %.1f, Title: %.1f, Spacing: %.1f, ThemeIcon: %.1f", 
                       buttonSize, titleSize, spacing, themeIconSize));
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error applying proportional sizes", e);
        }
    }

    /**
     * Updates the theme toggle button and icon sizes proportionally.
     */
    private void updateThemeToggleSize(double buttonSize, double iconSize) {
        try {
            // Update theme toggle button size
            String themeButtonStyle = String.format(
                "-fx-min-width: %.1fpx; -fx-min-height: %.1fpx; " +
                "-fx-max-width: %.1fpx; -fx-max-height: %.1fpx; " +
                "-fx-pref-width: %.1fpx; -fx-pref-height: %.1fpx;",
                buttonSize, buttonSize, buttonSize, buttonSize, buttonSize, buttonSize
            );
            themeToggleBtn.setStyle(themeButtonStyle);
            
            // Update theme icon size
            if (themeToggleBtn.getGraphic() instanceof FontIcon fontIcon) {
                fontIcon.setIconSize((int) Math.round(iconSize));
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating theme toggle size", e);
        }
    }

    /**
     * Sets up theme management using ThemeManager.
     */
    private void setupTheme() {
        try {
            themeManager = ThemeManager.getInstance();

            // Apply theme to current scene and setup toggle button
            Platform.runLater(() -> {
                if (title.getScene() != null) {
                    themeManager.initializeTheme(title.getScene(), themeToggleBtn);
                }
            });
            
            // If scene is not ready yet, wait for it
            title.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    themeManager.initializeTheme(newScene, themeToggleBtn);
                }
            });
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting up theme management", e);
        }
    }

    /**
     * Enforces minimum window size and sets initial size.
     */
    private void enforceMinimumWindowSize() {
        Platform.runLater(() -> {
            try {
                if (buttonPane.getScene() != null && buttonPane.getScene().getWindow() instanceof Stage stage) {
                    double minWidth = calculateAbsoluteMinWidth();
                    double minHeight = calculateAbsoluteMinHeight();
                    
                    stage.setMinWidth(minWidth);
                    stage.setMinHeight(minHeight);
                    
                    // Set initial size to base dimensions if window is smaller
                    if (stage.getWidth() < LayoutConstants.BASE_WINDOW_WIDTH) {
                        stage.setWidth(LayoutConstants.BASE_WINDOW_WIDTH);
                    }
                    if (stage.getHeight() < LayoutConstants.BASE_WINDOW_HEIGHT) {
                        stage.setHeight(LayoutConstants.BASE_WINDOW_HEIGHT);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error enforcing minimum window size", e);
            }
        });
    }

    /**
     * Calculates absolute minimum width needed.
     */
    private double calculateAbsoluteMinWidth() {
        return (LayoutConstants.BUTTON_COUNT * LayoutConstants.MIN_BUTTON_SIZE) +
               ((LayoutConstants.BUTTON_COUNT - 1) * LayoutConstants.MIN_SPACING) +
               LayoutConstants.BUTTON_PANE_PADDING +
               LayoutConstants.BORDER_PANE_PADDING +
               (2 * LayoutConstants.MIN_SIDE_MARGIN);
    }

    /**
     * Calculates absolute minimum height needed.
     */
    private double calculateAbsoluteMinHeight() {
        return LayoutConstants.MIN_BUTTON_SIZE + 
               LayoutConstants.BUTTON_PANE_PADDING + 
               LayoutConstants.VBOX_SPACING + 
               LayoutConstants.MIN_TITLE_SIZE + 
               LayoutConstants.TOP_SECTION_HEIGHT + 
               30; // BorderPane padding
    }

    /**
     * Checks if debug mode is enabled.
     */
    private boolean isDebugMode() {
        return Boolean.parseBoolean(System.getProperty("app.debug", "false"));
    }

    /**
     * Configures button click actions with proper error handling.
     */
    private void setupButtonActions() {
        try {
            imageButton.setOnAction(e -> handleConversionButtonClick(imageButton));
            audioButton.setOnAction(e -> handleConversionButtonClick(audioButton));
            videoButton.setOnAction(e -> handleConversionButtonClick(videoButton));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting up button actions", e);
        }
    }

    /**
     * Handles conversion button clicks with comprehensive error handling.
     */
    private void handleConversionButtonClick(Button sourceButton) {
        try {
            ConversionCategory category = ConversionCategory.fromButtonText(sourceButton.getText());
            switchToConversionView(category);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling button click for: " + sourceButton.getText(), e);
            showErrorDialog("Navigation Error", "Failed to switch to conversion view: " + e.getMessage());
        }
    }

    /**
     * Switches to the conversion view with simplified navigation.
     */
    private void switchToConversionView(ConversionCategory category) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/test/truinconv/conversion-controller.fxml"));
            Parent conversionRoot = loader.load();
            
            ConversionController controller = loader.getController();
            configureConversionController(controller, category);
            
            Scene currentScene = title.getScene();
            Scene conversionScene = new Scene(conversionRoot, currentScene.getWidth(), currentScene.getHeight());
            
            // Copy stylesheets from current scene
            conversionScene.getStylesheets().addAll(currentScene.getStylesheets());
            
            Stage stage = (Stage) currentScene.getWindow();
            stage.setScene(conversionScene);
            
            // Apply current theme to new scene
            if (themeManager != null) {
                themeManager.applyTheme(conversionScene);
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load conversion view", e);
            showErrorDialog("Loading Error", "Could not load the conversion interface.");
        }
    }

    /**
     * Configures the conversion controller with proper state transfer.
     */
    private void configureConversionController(ConversionController controller, ConversionCategory category) {
        try {
            controller.setConversionCategory(category.getConversionType());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error configuring conversion controller", e);
        }
    }

    /**
     * Shows an error dialog to the user.
     */
    private void showErrorDialog(String title, String message) {
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error showing error dialog", e);
        }
    }
}