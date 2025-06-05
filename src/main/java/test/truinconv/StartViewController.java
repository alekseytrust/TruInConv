package test.truinconv;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Controller for the application's start view.
 * <p>
 * Handles layout responsiveness, dynamic resizing, and theme switching between dark and light modes.
 * Adjusts button and title sizes based on the window size, updates layout on resize, and manages theme icons and styles.
 */
import java.io.IOException;
import java.util.Objects;

public class StartViewController {

    // --- FXML-Injected UI Components ---
    /** Label for application title. */
    @FXML public Label title;

    /** Pane holding primary action buttons. */
    @FXML private TilePane buttonPane;

    /** Image conversion button. */
    @FXML private Button imageButton;

    /** Audio conversion button. */
    @FXML private Button audioButton;

    /** Video conversion button. */
    @FXML private Button videoButton;

    /** Toggle button for switching application theme. */
    @FXML private ToggleButton themeToggleBtn;

    // --- State and Configuration ---
    /** Tracks whether dark mode is enabled. */
    private boolean darkMode = false;

    /** Minimum and maximum button size (px). */
    private static final double MIN_BUTTON_SIZE = 80.0, MAX_BUTTON_SIZE = 192.0;
    /** Minimum and maximum title font size (px). */
    private static final double MIN_TITLE_SIZE = 22.0, MAX_TITLE_SIZE = 44.0;

    /**
     * Initializes the controller after FXML load.
     * Sets up layout listeners, theme toggle logic, and initial layout.
     */
    @FXML
    private void initialize() {
        Platform.runLater(this::setup);

        // Add these lines to wire the button actions:
        imageButton.setOnAction(e -> switchToConversionView(imageButton));
        audioButton.setOnAction(e -> switchToConversionView(audioButton));
        videoButton.setOnAction(e -> switchToConversionView(videoButton));
    }

    /**
     * Switches the root of the current scene to the conversion view.
     */
    private void switchToConversionView(Button sourceButton) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(ConversionController.class.getResource("conversion-controller.fxml"))
            );
            Parent conversionView = loader.load();
        
        // Get the controller and set the title
        ConversionController controller = loader.getController();
        controller.setButtonTextAsTitle(sourceButton.getText());
        
            // Get the Scene from any current control
            Scene scene = title.getScene();
            scene.setRoot(conversionView);
        } catch (IOException e) {
            // Handle error (could show alert)
            e.printStackTrace();
        }
    }

    /**
     * Configures UI listeners, window minimums, theme icon, and triggers initial layout logic.
     */
    private void setup() {
        Scene scene = title.getScene();
        scene.widthProperty().addListener((obs, o, n) -> updateLayout());
        scene.heightProperty().addListener((obs, o, n) -> updateLayout());
        buttonPane.widthProperty().addListener((o, ov, nv) -> updateButtonPaneLayout(nv.doubleValue()));
        themeToggleBtn.selectedProperty().addListener((obs, oldVal, selected) -> toggleTheme(selected));
        themeToggleBtn.setOnAction(e -> updateThemeIcon());
        updateThemeIcon();
        updateLayout();
        Window window = scene.getWindow();
        if (window instanceof Stage stage) {
            stage.setMinWidth(415);
            stage.setMinHeight(400);
        }
    }

    /**
     * Adjusts button pane columns and spacing dynamically based on available width.
     *
     * @param paneWidth current width of the button pane
     */
    private void updateButtonPaneLayout(double paneWidth) {
        final int btnCount = 3;
        final double hgap = 36;
        final double tilePadding = 40; // pane horizontal padding

        // Calculate min width needed for 3 columns layout
        double minWidthNeeded = (btnCount * MIN_BUTTON_SIZE) + ((btnCount - 1) * hgap) + tilePadding;

        if (paneWidth >= minWidthNeeded) {
            buttonPane.setPrefColumns(3);
            buttonPane.setHgap(hgap);
        } else {
            buttonPane.setPrefColumns(1);
            buttonPane.setHgap(0);
        }
        buttonPane.setVgap(36); // constant vertical gap
    }

    /**
     * Resizes title and buttons proportionally to the window size and updates their styles.
     */
    private void updateLayout() {
        Scene scene = title.getScene();
        if (scene == null) return;

        double w = Math.max(350, scene.getWidth());
        double h = Math.max(320, scene.getHeight());

        // Adjust title font size
        double titleSz = Math.min(MAX_TITLE_SIZE, Math.max(MIN_TITLE_SIZE, w * 0.06));
        title.setStyle(String.format("-fx-font-size: %.0fpx; -fx-font-weight: bold;", titleSz));

        // Adjust button sizes and fonts
        int columns = buttonPane.getPrefColumns();
        double paneW = buttonPane.getWidth() > 0 ? buttonPane.getWidth() : w - 50;
        double paneH = buttonPane.getHeight() > 0 ? buttonPane.getHeight() : h * 0.6;
        int btnCount = 3;
        int rows = (int) Math.ceil((double) btnCount / columns);

        double hgap = buttonPane.getHgap(), vgap = buttonPane.getVgap();
        double availW = paneW - hgap * (columns - 1);
        double availH = paneH - vgap * (rows - 1);
        double btnSizeW = Math.max(MIN_BUTTON_SIZE, Math.min(MAX_BUTTON_SIZE, availW / columns));
        double btnSizeH = Math.max(MIN_BUTTON_SIZE, Math.min(MAX_BUTTON_SIZE, availH / rows));
        double btnSize = Math.min(btnSizeW, btnSizeH);

        double btnFont = Math.round(Math.max(15, btnSize * 0.21));

        for (Button btn : new Button[]{imageButton, audioButton, videoButton}) {
            btn.setStyle(String.format("-fx-font-size: %.0fpx;", btnFont));
            btn.setMinSize(btnSize, btnSize);
            btn.setPrefSize(btnSize, btnSize);
            btn.setMaxSize(btnSize, btnSize);
        }
    }

    /**
     * Switches between dark and light mode, updates application root style, and theme icon.
     *
     * @param dark {@code true} to enable dark mode, {@code false} for light mode
     */
    public void toggleTheme(boolean dark) {
        darkMode = dark;
        Scene scene = themeToggleBtn.getScene();
        if (scene == null) return;
        Parent root = scene.getRoot();
        if (dark) {
            if (!root.getStyleClass().contains("dark-mode"))
                root.getStyleClass().add("dark-mode");
        } else {
            root.getStyleClass().removeAll("dark-mode");
        }
        updateThemeIcon();
    }

    /**
     * Updates the theme toggle button's icon and color according to the selected theme.
     */
    public void updateThemeIcon() {
        if (themeToggleBtn == null) return;
        FontIcon icon;
        if (darkMode)
            icon = new FontIcon("mdi2w-white-balance-sunny");
        else
            icon = new FontIcon("mdi2m-moon-waning-crescent");
        icon.setIconSize(26);
        icon.setIconColor(Paint.valueOf(darkMode ? "#ffd740" : "#888"));
        themeToggleBtn.setGraphic(icon);
    }
}