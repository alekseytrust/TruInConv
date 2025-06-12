package test.truinconv;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the application's start view.
 * Manages the main menu interface with dynamic layout adaptation and theme support.
 */
public class StartViewController {
    
    private static final Logger LOGGER = Logger.getLogger(StartViewController.class.getName());

    // FXML-injected UI components
    @FXML private Label title;
    @FXML private Pane buttonPane;
    @FXML private Button imageButton;
    @FXML private Button audioButton;
    @FXML private Button videoButton;
    @FXML private ToggleButton themeToggleBtn;

    // State management
    private boolean darkMode = false;
    private boolean isHorizontalLayout = true;
    private boolean isLayoutTransitioning = false;

    // Layout configuration organized in nested classes
    private static final class LayoutConstants {
        // Button sizing
        static final double MIN_BUTTON_SIZE = 80.0;
        static final double MAX_BUTTON_SIZE = 192.0;
        
        // Title sizing
        static final double MIN_TITLE_SIZE = 22.0;
        static final double MAX_TITLE_SIZE = 44.0;
        
        // Spacing and padding
        static final double BUTTON_SPACING = 36.0;
        static final double PANE_PADDING = 20.0;
        static final double BORDER_PANE_PADDING = 50.0; // From FXML: 25 left + 25 right
        static final double VBOX_SPACING = 24.0;
        static final double TOP_SECTION_HEIGHT = 50.0;
        
        // Layout calculations
        static final int BUTTON_COUNT = 3;
        static final double TITLE_SCALE_FACTOR = 0.06;
        static final double BUTTON_FONT_SCALE = 0.15;
        static final double MIN_BUTTON_FONT = 10.0;
        static final double LAYOUT_BUFFER = 10.0;
        static final double TITLE_EXTRA_SPACE = 10.0;
    }

    /**
     * Enumeration for conversion types with display names and validation.
     */
    public enum ConversionCategory {
        IMAGES("Images", "Images"),
        MEDIA("Media", "Media"),
        DOCUMENTS("Docs", "Docs");
        
        private final String buttonText;
        private final String conversionType;
        
        ConversionCategory(String buttonText, String conversionType) {
            this.buttonText = buttonText;
            this.conversionType = conversionType;
        }
        
        public String getButtonText() { return buttonText; }
        public String getConversionType() { return conversionType; }
        
        public static ConversionCategory fromButtonText(String text) {
            return Arrays.stream(values())
                    .filter(category -> category.buttonText.equals(text))
                    .findFirst()
                    .orElse(IMAGES); // Default fallback
        }
    }

    /**
     * Utility class for layout calculations.
     */
    private static final class LayoutCalculator {
        
        static double calculateMinHorizontalWidth() {
            // Pattern: [spacing][button][spacing][button][spacing][button][spacing]
            double buttonPaneWidth = (LayoutConstants.BUTTON_SPACING * 4) + 
                                   (LayoutConstants.MIN_BUTTON_SIZE * LayoutConstants.BUTTON_COUNT);
            return buttonPaneWidth + LayoutConstants.BORDER_PANE_PADDING;
        }
        
        static double calculateMinVerticalWidth() {
            double buttonPaneWidth = LayoutConstants.MIN_BUTTON_SIZE + (LayoutConstants.PANE_PADDING * 2);
            return buttonPaneWidth + LayoutConstants.BORDER_PANE_PADDING;
        }
        
        static double calculateMinHorizontalHeight() {
            double titleHeight = LayoutConstants.MIN_TITLE_SIZE + LayoutConstants.TITLE_EXTRA_SPACE;
            double buttonPaneHeight = LayoutConstants.MIN_BUTTON_SIZE + (LayoutConstants.PANE_PADDING * 2);
            return titleHeight + LayoutConstants.VBOX_SPACING + buttonPaneHeight + 
                   LayoutConstants.BORDER_PANE_PADDING + LayoutConstants.TOP_SECTION_HEIGHT;
        }
        
        static double calculateMinVerticalHeight() {
            double titleHeight = LayoutConstants.MIN_TITLE_SIZE + LayoutConstants.TITLE_EXTRA_SPACE;
            double buttonPaneHeight = (LayoutConstants.BUTTON_SPACING * 4) + 
                                    (LayoutConstants.MIN_BUTTON_SIZE * LayoutConstants.BUTTON_COUNT);
            return titleHeight + LayoutConstants.VBOX_SPACING + buttonPaneHeight + 
                   LayoutConstants.BORDER_PANE_PADDING + LayoutConstants.TOP_SECTION_HEIGHT;
        }
        
        static double calculateRequiredHorizontalWidth() {
            return (LayoutConstants.BUTTON_SPACING * 4) + 
                   (LayoutConstants.MIN_BUTTON_SIZE * LayoutConstants.BUTTON_COUNT);
        }
    }

    @FXML
    private void initialize() {
        validateFXMLInjection();
        setupButtonActions();
        
        // Initialize asynchronously to avoid blocking the JavaFX Application Thread
        Platform.runLater(this::initializeAsync);
    }

    /**
     * Validates that all FXML components have been properly injected.
     */
    private void validateFXMLInjection() {
        Objects.requireNonNull(title, "Title label was not injected");
        Objects.requireNonNull(buttonPane, "Button pane was not injected");
        Objects.requireNonNull(imageButton, "Image button was not injected");
        Objects.requireNonNull(audioButton, "Audio button was not injected");
        Objects.requireNonNull(videoButton, "Video button was not injected");
        Objects.requireNonNull(themeToggleBtn, "Theme toggle button was not injected");
    }

    /**
     * Asynchronous initialization to avoid blocking the UI thread.
     */
    private void initializeAsync() {
        try {
            setupEventListeners();
            setupThemeToggle();
            updateThemeIcon();
            updateLayout();
            
            // Setup window constraints after initial layout
            Platform.runLater(this::setupWindowConstraints);
            
            // Optional: Setup debugging console listener
            if (isDebugMode()) {
                setupConsoleListener();
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize StartViewController", e);
            showErrorAlert("Initialization Error", "Failed to initialize the application interface.");
        }
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
        imageButton.setOnAction(e -> handleConversionButtonClick(imageButton));
        audioButton.setOnAction(e -> handleConversionButtonClick(audioButton));
        videoButton.setOnAction(e -> handleConversionButtonClick(videoButton));
    }

    /**
     * Handles conversion button clicks with comprehensive error handling.
     */
    private void handleConversionButtonClick(Button sourceButton) {
        if (isLayoutTransitioning) {
            LOGGER.info("Ignoring button click during layout transition");
            return;
        }
        
        try {
            ConversionCategory category = ConversionCategory.fromButtonText(sourceButton.getText());
            switchToConversionView(category);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to handle button click", e);
            showErrorAlert("Navigation Error", "Failed to navigate to the conversion screen.");
        }
    }

    /**
     * Switches to the conversion view with proper error handling and state management.
     */
    private void switchToConversionView(ConversionCategory category) {
        CompletableFuture.supplyAsync(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("conversion-controller.fxml"));
                return loader;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create FXML loader", e);
            }
        }).thenCompose(loader -> CompletableFuture.supplyAsync(() -> {
            try {
                Parent conversionView = loader.load();
                return new ViewLoadResult(loader, conversionView);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load conversion view", e);
            }
        })).thenAcceptAsync(result -> {
            Platform.runLater(() -> {
                try {
                    configureConversionController(result.loader, category);
                    Scene currentScene = title.getScene();
                    if (currentScene != null) {
                        currentScene.setRoot(result.view);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to configure conversion view", e);
                    showErrorAlert("Configuration Error", "Failed to configure the conversion interface.");
                }
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                LOGGER.log(Level.SEVERE, "Failed to switch to conversion view", throwable);
                showErrorAlert("Navigation Error", "Failed to load the conversion screen.");
            });
            return null;
        });
    }

    /**
     * Helper record for view loading results.
     */
    private record ViewLoadResult(FXMLLoader loader, Parent view) {}

    /**
     * Configures the conversion controller with proper state transfer.
     */
    private void configureConversionController(FXMLLoader loader, ConversionCategory category) {
        ConversionController controller = loader.getController();
        if (controller != null) {
            controller.setConversionType(category.getConversionType());
            controller.setButtonTextAsTitle(category.getButtonText());
            controller.setThemeState(darkMode);
        } else {
            LOGGER.warning("Conversion controller was null after loading FXML");
        }
    }

    /**
     * Sets up all event listeners for responsive layout.
     */
    private void setupEventListeners() {
        Scene scene = title.getScene();
        if (scene != null) {
            // Scene dimension listeners
            scene.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (!isLayoutTransitioning) {
                    updateLayout();
                }
            });
            scene.heightProperty().addListener((obs, oldVal, newVal) -> {
                if (!isLayoutTransitioning) {
                    updateLayout();
                }
            });
            
            // Button pane dimension listeners
            setupButtonPaneListeners();
        }
    }

    /**
     * Sets up button pane-specific listeners for layout changes.
     */
    private void setupButtonPaneListeners() {
        buttonPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (!isLayoutTransitioning) {
                updateButtonPaneLayout();
            }
        });
        buttonPane.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!isLayoutTransitioning) {
                updateButtonPaneLayout();
            }
        });
    }

    /**
     * Configures theme toggle functionality.
     */
    private void setupThemeToggle() {
        themeToggleBtn.selectedProperty().addListener((obs, oldVal, selected) -> {
            toggleTheme(selected);
        });
        themeToggleBtn.setOnAction(e -> updateThemeIcon());
    }

    /**
     * Determines optimal layout orientation based on available space.
     */
    private boolean shouldUseHorizontalLayout() {
        double availableWidth = buttonPane.getWidth();
        double minRequiredWidth = LayoutCalculator.calculateRequiredHorizontalWidth();
        return availableWidth >= minRequiredWidth;
    }

    /**
     * Updates button pane layout with smooth transitions.
     */
    private void updateButtonPaneLayout() {
        if (isLayoutTransitioning) return;
        
        boolean shouldBeHorizontal = shouldUseHorizontalLayout();
        
        if (shouldBeHorizontal != isHorizontalLayout) {
            isLayoutTransitioning = true;
            
            Platform.runLater(() -> {
                try {
                    isHorizontalLayout = shouldBeHorizontal;
                    recreateButtonPane();
                    
                    // Update window constraints after layout change
                    Platform.runLater(this::setupWindowConstraints);
                } finally {
                    isLayoutTransitioning = false;
                }
            });
        }
    }

    /**
     * Recreates the button pane with the appropriate layout orientation.
     */
    private void recreateButtonPane() {
        Parent parent = buttonPane.getParent();
        if (!(parent instanceof VBox parentVBox)) {
            LOGGER.warning("Button pane parent is not a VBox, cannot recreate layout");
            return;
        }

        // Store current position and remove from parent
        int buttonPaneIndex = parentVBox.getChildren().indexOf(buttonPane);
        parentVBox.getChildren().remove(buttonPane);

        // Create new layout container
        Pane newButtonPane = createOptimizedButtonPane();
        
        // Replace reference and add to parent at correct position
        buttonPane = newButtonPane;
        parentVBox.getChildren().add(buttonPaneIndex, buttonPane);

        // Re-setup listeners for the new pane
        setupButtonPaneListeners();
        
        LOGGER.info("Recreated button pane with " + 
                   (isHorizontalLayout ? "horizontal" : "vertical") + " layout");
    }

    /**
     * Creates an optimized button pane with proper configuration.
     */
    private Pane createOptimizedButtonPane() {
        Pane newPane;
        
        if (isHorizontalLayout) {
            newPane = createHorizontalButtonPane();
        } else {
            newPane = createVerticalButtonPane();
        }

        newPane.setId("buttonPane");
        
        // Add buttons in consistent order
        List<Button> buttons = List.of(imageButton, audioButton, videoButton);
        newPane.getChildren().setAll(buttons);
        
        return newPane;
    }

    /**
     * Creates a horizontal button pane with proper spacing.
     */
    private HBox createHorizontalButtonPane() {
        HBox hbox = new HBox(LayoutConstants.BUTTON_SPACING);
        hbox.setAlignment(Pos.CENTER);
        hbox.setPadding(new Insets(
            LayoutConstants.PANE_PADDING, 
            LayoutConstants.BUTTON_SPACING, 
            LayoutConstants.PANE_PADDING, 
            LayoutConstants.BUTTON_SPACING
        ));
        return hbox;
    }

    /**
     * Creates a vertical button pane with proper spacing.
     */
    private VBox createVerticalButtonPane() {
        VBox vbox = new VBox(LayoutConstants.BUTTON_SPACING);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(
            LayoutConstants.BUTTON_SPACING, 
            LayoutConstants.PANE_PADDING, 
            LayoutConstants.BUTTON_SPACING, 
            LayoutConstants.PANE_PADDING
        ));
        return vbox;
    }

    /**
     * Calculates optimal button size based on current layout and available space.
     */
    private double calculateOptimalButtonSize() {
        Scene scene = title.getScene();
        if (scene == null) return LayoutConstants.MIN_BUTTON_SIZE;
        
        double paneWidth = Math.max(buttonPane.getWidth(), scene.getWidth() - LayoutConstants.BORDER_PANE_PADDING);
        double paneHeight = Math.max(buttonPane.getHeight(), scene.getHeight() * 0.6);

        double calculatedSize;
        
        if (isHorizontalLayout) {
            // Account for spacing on sides and between buttons
            double availableWidth = paneWidth - (LayoutConstants.BUTTON_SPACING * 4);
            calculatedSize = availableWidth / LayoutConstants.BUTTON_COUNT;
        } else {
            // Account for spacing on top/bottom and between buttons
            double availableHeight = paneHeight - (LayoutConstants.BUTTON_SPACING * 4);
            double heightBasedSize = availableHeight / LayoutConstants.BUTTON_COUNT;
            double widthBasedSize = paneWidth - (LayoutConstants.PANE_PADDING * 2);
            calculatedSize = Math.min(heightBasedSize, widthBasedSize);
        }

        return Math.min(LayoutConstants.MAX_BUTTON_SIZE, 
                       Math.max(LayoutConstants.MIN_BUTTON_SIZE, calculatedSize));
    }

    /**
     * Updates button styles with optimal sizing and font scaling.
     */
    private void updateButtonStyles(double buttonSize) {
        double buttonFont = Math.round(Math.max(
            LayoutConstants.MIN_BUTTON_FONT, 
            buttonSize * LayoutConstants.BUTTON_FONT_SCALE
        ));
        
        String buttonStyle = String.format("-fx-font-size: %.0fpx;", buttonFont);
        
        List<Button> buttons = List.of(imageButton, audioButton, videoButton);
        buttons.forEach(button -> {
            button.setStyle(buttonStyle);
            button.setMinSize(buttonSize, buttonSize);
            button.setPrefSize(buttonSize, buttonSize);
            button.setMaxSize(buttonSize, buttonSize);
        });
    }

    /**
     * Updates title styling with responsive font sizing.
     */
    private void updateTitleStyle() {
        Scene scene = title.getScene();
        if (scene == null) return;

        double windowWidth = Math.max(350, scene.getWidth());
        double titleSize = Math.min(LayoutConstants.MAX_TITLE_SIZE, 
                                  Math.max(LayoutConstants.MIN_TITLE_SIZE, 
                                          windowWidth * LayoutConstants.TITLE_SCALE_FACTOR));
        
        String titleStyle = String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold;", titleSize);
        title.setStyle(titleStyle);
    }

    /**
     * Updates the complete layout with optimized calculations.
     */
    private void updateLayout() {
        if (isLayoutTransitioning) return;
        
        updateTitleStyle();
        
        double buttonSize = calculateOptimalButtonSize();
        updateButtonStyles(buttonSize);
    }

    /**
     * Sets up window size constraints based on current layout configuration.
     */
    private void setupWindowConstraints() {
        Window window = title.getScene().getWindow();
        if (!(window instanceof Stage stage)) return;

        double minWidth, minHeight;

        if (isHorizontalLayout) {
            minWidth = LayoutCalculator.calculateMinHorizontalWidth();
            minHeight = LayoutCalculator.calculateMinHorizontalHeight();
        } else {
            minWidth = LayoutCalculator.calculateMinVerticalWidth();
            minHeight = LayoutCalculator.calculateMinVerticalHeight();
        }

        // Add buffer to prevent UI cutoff
        minWidth += LayoutConstants.LAYOUT_BUFFER;
        minHeight += LayoutConstants.LAYOUT_BUFFER;

        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);

        LOGGER.info(String.format("Window constraints updated: minWidth=%.1f, minHeight=%.1f (layout: %s)", 
                                 minWidth, minHeight, isHorizontalLayout ? "horizontal" : "vertical"));
    }

    // Theme Management

    /**
     * Sets the theme state for this controller with proper state synchronization.
     */
    public void setThemeState(boolean isDarkMode) {
        this.darkMode = isDarkMode;
        
        Platform.runLater(() -> {
            updateRootStyleClass();
            themeToggleBtn.setSelected(isDarkMode);
            updateThemeIcon();
        });
    }

    /**
     * Toggles between dark and light themes with proper state management.
     */
    public void toggleTheme(boolean dark) {
        this.darkMode = dark;
        updateRootStyleClass();
        updateThemeIcon();
    }

    /**
     * Updates the root style class for theme changes.
     */
    private void updateRootStyleClass() {
        Scene scene = themeToggleBtn.getScene();
        if (scene == null) return;
        
        Parent root = scene.getRoot();
        if (darkMode) {
            if (!root.getStyleClass().contains("dark-mode")) {
                root.getStyleClass().add("dark-mode");
            }
        } else {
            root.getStyleClass().removeAll("dark-mode");
        }
    }

    /**
     * Updates the theme toggle button icon with proper styling.
     */
    public void updateThemeIcon() {
        if (themeToggleBtn == null) return;
        
        FontIcon icon = createOptimizedThemeIcon();
        themeToggleBtn.setGraphic(icon);
    }

    /**
     * Creates an optimized theme icon based on current mode.
     */
    private FontIcon createOptimizedThemeIcon() {
        FontIcon icon;
        if (darkMode) {
            icon = new FontIcon("mdi2w-white-balance-sunny");
            icon.setIconColor(Paint.valueOf("#ffd740"));
        } else {
            icon = new FontIcon("mdi2m-moon-waning-crescent");
            icon.setIconColor(Paint.valueOf("#888"));
        }
        icon.setIconSize(26);
        return icon;
    }

    // Utility Methods

    /**
     * Shows an error alert with consistent styling.
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Sets up console listener for debugging purposes (development only).
     */
    private void setupConsoleListener() {
        Thread consoleThread = new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    handleConsoleCommand(line);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Console listener error", e);
            }
        }, "Console-Listener");
        
        consoleThread.setDaemon(true);
        consoleThread.start();
        
        LOGGER.info("Debug console listener started");
    }

    /**
     * Handles console commands for debugging.
     */
    private void handleConsoleCommand(String command) {
        switch (command.toLowerCase()) {
            case "get size", "size" -> Platform.runLater(this::printDebugInfo);
            case "layout" -> Platform.runLater(() -> {
                System.out.printf("Current layout: %s%n", 
                                isHorizontalLayout ? "horizontal" : "vertical");
            });
            case "theme" -> Platform.runLater(() -> {
                System.out.printf("Current theme: %s%n", darkMode ? "dark" : "light");
            });
            default -> System.out.println("Unknown command: " + command);
        }
    }

    /**
     * Prints debug information about current layout state.
     */
    private void printDebugInfo() {
        System.out.printf(
            "=== Debug Info ===%n" +
            "ButtonPane dimensions: width=%.2f, height=%.2f%n" +
            "Layout: %s%n" +
            "Theme: %s%n" +
            "Transitioning: %s%n",
            buttonPane.getWidth(),
            buttonPane.getHeight(),
            isHorizontalLayout ? "horizontal" : "vertical",
            darkMode ? "dark" : "light",
            isLayoutTransitioning
        );
    }
}