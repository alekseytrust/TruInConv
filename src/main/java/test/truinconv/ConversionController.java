/*
 * Copyright (c) 2025 Aleksey Trust
 * This file is licensed under the MIT License.
 * See the LICENSE file in the project root for full license information.
 */

package test.truinconv;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.geometry.Pos;
import org.kordamp.ikonli.javafx.FontIcon;
import test.truinconv.converters.ConverterRouter;
import test.truinconv.constants.LayoutConstants;
import test.truinconv.constants.ConversionMappings;
import test.truinconv.model.ConversionCategory;
import test.truinconv.model.FileItem;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the file conversion interface with proportional responsive sizing.
 * Handles file selection, format conversion options, and dynamic UI scaling.
 */
public class ConversionController {

    private static final Logger LOGGER = Logger.getLogger(ConversionController.class.getName());

    // FXML-injected UI components
    @FXML private VBox mainVBox;
    @FXML private Label titleLabel;
    @FXML private HBox formatSelectionContainer;
    @FXML private ComboBox<String> sourceFormatComboBox;
    @FXML private ComboBox<String> targetFormatComboBox;
    @FXML private Button swapButton;
    @FXML private FontIcon arrowIcon;
    @FXML private StackPane dropZone;
    @FXML private Label dropZoneLabel;
    @FXML private TableView<FileItem> fileTableView;
    @FXML private TableColumn<FileItem, String> fileNameColumn;
    @FXML private TableColumn<FileItem, Button> removeButtonColumn;
    @FXML private HBox directoryPickerContainer;
    @FXML private TextField directoryField;
    @FXML private Button browseDirBtn;
    @FXML private Button saveBtn;
    @FXML private ToggleButton themeToggleBtn;
    @FXML private Button backButton;

    // State management
    private final Set<File> selectedFiles = new LinkedHashSet<>();
    private ConversionCategory conversionCategory = ConversionCategory.IMAGE;
    private ThemeManager themeManager;
    private boolean isInitialized = false;

    /**
     * Called by the FXML loader to initialize this controller.
     */
    @FXML
    private void initialize() {
        try {
            validateFXMLInjection();
            themeManager = ThemeManager.getInstance();
            Platform.runLater(this::initializeAsync);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during initialization", e);
        }
    }

    /**
     * Ensures all @FXML fields were injected correctly.
     */
    private void validateFXMLInjection() {
        Objects.requireNonNull(titleLabel, "Title label not injected");
        Objects.requireNonNull(mainVBox, "Main VBox not injected");
        Objects.requireNonNull(sourceFormatComboBox, "Source format combo box not injected");
        Objects.requireNonNull(targetFormatComboBox, "Target format combo box not injected");
        Objects.requireNonNull(swapButton, "Swap button not injected");
        Objects.requireNonNull(dropZone, "Drop zone not injected");
        Objects.requireNonNull(fileTableView, "File table view not injected");
        Objects.requireNonNull(directoryField, "Directory field not injected");
        Objects.requireNonNull(browseDirBtn, "Browse button not injected");
        Objects.requireNonNull(saveBtn, "Save button not injected");
        Objects.requireNonNull(themeToggleBtn, "Theme toggle button not injected");
        Objects.requireNonNull(backButton, "Back button not injected");
    }
    /**
     * Runs all setup steps off the JavaFX application thread.
     */
    private void initializeAsync() {
        try {
            setupTheme();
            setupTableView();
            setupDropZone();
            setupDirectoryBrowser();
            setupFormatComboBoxes();
            setupButtonActions();
            setupResponsiveLayout();
            enforceMinimumWindowSize();
            isInitialized = true;
            // Center all children of mainVBox horizontally
            mainVBox.setAlignment(Pos.TOP_CENTER);

            if (isDebugMode()) {
                LOGGER.info("ConversionController initialized successfully");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during async initialization", e);
        }
    }

    /**
     * Attaches listeners to resize and restyle the UI when the window changes.
     */
    private void setupResponsiveLayout() {
        try {
            // Wait for scene to be available
            if (titleLabel.getScene() != null) {
                setupWindowListeners();
            } else {
                titleLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
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
     * Adds width/height listeners on the primary window to update element sizes.
     */
    private void setupWindowListeners() {
        try {
            Window window = titleLabel.getScene().getWindow();
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
     * Recalculates and reapplies proportional sizes for every UI element.
     */
    private void updateProportionalSizing() {
        if (!isInitialized) return;

        try {
            Platform.runLater(() -> {
                Window window = titleLabel.getScene().getWindow();
                if (window == null) return;

                double currentWidth = window.getWidth();
                double currentHeight = window.getHeight();

                // Calculate scaling factors
                double widthScale = currentWidth / LayoutConstants.BASE_WINDOW_WIDTH;
                double heightScale = currentHeight / LayoutConstants.BASE_WINDOW_HEIGHT;

                // Use the smaller scale to maintain proportions and fit within window
                double scale = Math.min(widthScale, heightScale);

                // Calculate proportional sizes for all elements
                ScaledSizes sizes = calculateScaledSizes(scale);

                // Apply the calculated sizes
                applyProportionalSizes(sizes);
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating proportional sizing", e);
        }
    }


    /**
     * Container class for all scaled element sizes.
     */
    private static class ScaledSizes {
        final double titleSize;
        final double labelSize;
        final double comboWidth;
        final double comboHeight;
        final double swapButtonSize;
        final double arrowIconSize;
        final double dropZoneWidth;
        final double dropZoneHeight;
        final double dropZoneFont;
        final double tableHeight;
        final double tableFont;
        final double directoryFieldWidth;
        final double directoryFieldHeight;
        final double browseButtonWidth;
        final double browseButtonHeight;
        final double saveButtonWidth;
        final double saveButtonHeight;
        final double saveButtonFont;
        final double themeIconSize;
        final double themeButtonSize;
        final double backIconSize;
        final double backButtonSize;
        final double mainSpacing;
        final double formatSpacing;
        final double directorySpacing;
        // New fields for remove button/icon sizes
        final double removeButtonSize;
        final double removeIconSize;

        ScaledSizes(double scale) {
            this.titleSize = Math.max(LayoutConstants.BASE_TITLE_SIZE * scale, LayoutConstants.MIN_TITLE_SIZE);
            this.labelSize = Math.max(LayoutConstants.BASE_LABEL_SIZE * scale, LayoutConstants.MIN_LABEL_SIZE);
            this.comboWidth = Math.max(LayoutConstants.BASE_COMBO_WIDTH * scale, LayoutConstants.MIN_COMBO_WIDTH);
            this.comboHeight = Math.max(LayoutConstants.BASE_COMBO_HEIGHT * scale, LayoutConstants.MIN_COMBO_HEIGHT);
            this.swapButtonSize = Math.max(LayoutConstants.BASE_SWAP_BUTTON_SIZE * scale, LayoutConstants.MIN_SWAP_BUTTON_SIZE);
            this.arrowIconSize = Math.max(LayoutConstants.BASE_ARROW_ICON_SIZE * scale, LayoutConstants.MIN_ARROW_ICON_SIZE);
            this.dropZoneWidth = Math.max(LayoutConstants.BASE_DROP_ZONE_WIDTH * scale, LayoutConstants.MIN_DROP_ZONE_WIDTH);
            this.dropZoneHeight = Math.max(LayoutConstants.BASE_DROP_ZONE_HEIGHT * scale, LayoutConstants.MIN_DROP_ZONE_HEIGHT);
            this.dropZoneFont = Math.max(LayoutConstants.BASE_DROP_ZONE_FONT * scale, LayoutConstants.MIN_DROP_ZONE_FONT);
            this.tableHeight = Math.max(LayoutConstants.BASE_TABLE_HEIGHT * scale, LayoutConstants.MIN_TABLE_HEIGHT);
            this.tableFont = Math.max(LayoutConstants.BASE_TABLE_FONT * scale, LayoutConstants.MIN_TABLE_FONT);
            this.directoryFieldWidth = Math.max(LayoutConstants.BASE_DIRECTORY_FIELD_WIDTH * scale, LayoutConstants.MIN_DIRECTORY_FIELD_WIDTH);
            this.directoryFieldHeight = Math.max(LayoutConstants.BASE_DIRECTORY_FIELD_HEIGHT * scale, LayoutConstants.MIN_DIRECTORY_FIELD_HEIGHT);
            this.browseButtonWidth = Math.max(LayoutConstants.BASE_BROWSE_BUTTON_WIDTH * scale, LayoutConstants.MIN_BROWSE_BUTTON_WIDTH);
            this.browseButtonHeight = Math.max(LayoutConstants.BASE_BROWSE_BUTTON_HEIGHT * scale, LayoutConstants.MIN_BROWSE_BUTTON_HEIGHT);
            this.saveButtonWidth = Math.max(LayoutConstants.BASE_SAVE_BUTTON_WIDTH * scale, LayoutConstants.MIN_SAVE_BUTTON_WIDTH);
            this.saveButtonHeight = Math.max(LayoutConstants.BASE_SAVE_BUTTON_HEIGHT * scale, LayoutConstants.MIN_SAVE_BUTTON_HEIGHT);
            this.saveButtonFont = Math.max(LayoutConstants.BASE_SAVE_BUTTON_FONT * scale, LayoutConstants.MIN_SAVE_BUTTON_FONT);
            this.themeIconSize = Math.max(LayoutConstants.BASE_THEME_ICON_SIZE * scale, LayoutConstants.MIN_THEME_ICON_SIZE);
            this.themeButtonSize = Math.max(LayoutConstants.BASE_THEME_BUTTON_SIZE * scale, LayoutConstants.MIN_THEME_BUTTON_SIZE);
            this.backIconSize = Math.max(LayoutConstants.BASE_BACK_ICON_SIZE * scale, LayoutConstants.MIN_BACK_ICON_SIZE);
            this.backButtonSize = Math.max(LayoutConstants.BASE_BACK_BUTTON_SIZE * scale, LayoutConstants.MIN_BACK_BUTTON_SIZE);
            this.mainSpacing = Math.max(LayoutConstants.BASE_MAIN_SPACING * scale, LayoutConstants.MIN_MAIN_SPACING);
            this.formatSpacing = Math.max(LayoutConstants.BASE_FORMAT_SPACING * scale, LayoutConstants.MIN_FORMAT_SPACING);
            this.directorySpacing = Math.max(LayoutConstants.BASE_DIRECTORY_SPACING * scale, LayoutConstants.MIN_DIRECTORY_SPACING);
            // New fields: remove button/icon sizes
            this.removeButtonSize = Math.max(LayoutConstants.BASE_SWAP_BUTTON_SIZE * scale, LayoutConstants.MIN_SWAP_BUTTON_SIZE);
            this.removeIconSize = Math.max(LayoutConstants.BASE_ARROW_ICON_SIZE * scale, LayoutConstants.MIN_ARROW_ICON_SIZE);
        }
    }

    /**
     * Computes a new ScaledSizes based on the given scale factor.
     *
     * @param scale  the scale factor calculated from window dimensions
     * @return       a fresh ScaledSizes instance
     */
    private ScaledSizes calculateScaledSizes(double scale) {
        return new ScaledSizes(scale);
    }

    /**
     * Applies each value in the provided ScaledSizes to the matching UI control.
     *
     * @param sizes  container of all computed pixel values
     */
    private void applyProportionalSizes(ScaledSizes sizes) {
        try {
            // Update title
            titleLabel.setStyle(String.format("-fx-font-size: %.1fpx;", sizes.titleSize));

            // Update main VBox spacing
            mainVBox.setSpacing(sizes.mainSpacing);

            // Update format selection area
            updateFormatSelectionSizes(sizes);

            // Update drop zone
            updateDropZoneSizes(sizes);

            // Update table view
            updateTableViewSizes(sizes);

            // Update directory picker
            updateDirectoryPickerSizes(sizes);

            // Update save button
            updateSaveButtonSizes(sizes);

            // Update navigation buttons
            updateNavigationButtonSizes(sizes);
            // Update remove-button sizes in table cells
            updateRemoveButtonSizes(sizes);

            LOGGER.fine(String.format("Applied proportional sizes - Scale factor applied, Title: %.1f, DropZone: %.1fx%.1f",
                    sizes.titleSize, sizes.dropZoneWidth, sizes.dropZoneHeight));

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error applying proportional sizes", e);
        }
    }

    /**
     * Resizes and restyles the format‐selection HBox, its labels, combo‐boxes, and swap button.
     *
     * @param sizes  container of all computed pixel values
     */
    private void updateFormatSelectionSizes(ScaledSizes sizes) {
        try {
            // Update container spacing
            if (formatSelectionContainer != null) {
                formatSelectionContainer.setSpacing(sizes.formatSpacing);
            }

            // Update combo boxes
            String comboStyle = String.format(
                    "-fx-min-width: %.1fpx; -fx-pref-width: %.1fpx; -fx-max-width: %.1fpx; " +
                            "-fx-min-height: %.1fpx; -fx-pref-height: %.1fpx; -fx-max-height: %.1fpx; " +
                            "-fx-font-size: %.1fpx;",
                    sizes.comboWidth, sizes.comboWidth, sizes.comboWidth,
                    sizes.comboHeight, sizes.comboHeight, sizes.comboHeight,
                    sizes.labelSize
            );

            sourceFormatComboBox.setStyle(comboStyle);
            targetFormatComboBox.setStyle(comboStyle);

            // Ensure both combos stay exactly the same size
            if (sourceFormatComboBox != null) {
                sourceFormatComboBox.setMinWidth(sizes.comboWidth);
                sourceFormatComboBox.setPrefWidth(sizes.comboWidth);
                sourceFormatComboBox.setMaxWidth(sizes.comboWidth);
                sourceFormatComboBox.setMinHeight(sizes.comboHeight);
                sourceFormatComboBox.setPrefHeight(sizes.comboHeight);
                sourceFormatComboBox.setMaxHeight(sizes.comboHeight);
            }
            if (targetFormatComboBox != null) {
                targetFormatComboBox.setMinWidth(sizes.comboWidth);
                targetFormatComboBox.setPrefWidth(sizes.comboWidth);
                targetFormatComboBox.setMaxWidth(sizes.comboWidth);
                targetFormatComboBox.setMinHeight(sizes.comboHeight);
                targetFormatComboBox.setPrefHeight(sizes.comboHeight);
                targetFormatComboBox.setMaxHeight(sizes.comboHeight);
            }

            // Update swap button
            String swapButtonStyle = String.format(
                    "-fx-min-width: %.1fpx; -fx-min-height: %.1fpx; " +
                            "-fx-max-width: %.1fpx; -fx-max-height: %.1fpx; " +
                            "-fx-pref-width: %.1fpx; -fx-pref-height: %.1fpx;",
                    sizes.swapButtonSize, sizes.swapButtonSize,
                    sizes.swapButtonSize, sizes.swapButtonSize,
                    sizes.swapButtonSize, sizes.swapButtonSize
            );
            swapButton.setStyle(swapButtonStyle);

            // Update arrow icon
            if (arrowIcon != null) {
                arrowIcon.setIconSize((int) Math.round(sizes.arrowIconSize));
            }

            // Update format labels in VBoxes (find labels in the format selection containers)
            if (formatSelectionContainer != null) {
                formatSelectionContainer.getChildren().forEach(node -> {
                    if (node instanceof VBox vbox) {
                        vbox.getChildren().forEach(child -> {
                            if (child instanceof Label label && label.getText().contains("Convert")) {
                                label.setStyle(String.format("-fx-font-size: %.1fpx;", sizes.labelSize));
                            }
                        });
                    }
                });
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating format selection sizes", e);
        }
    }

    /**
     * Updates the drop‐zone pane’s min/pref/max dimensions and font size.
     *
     * @param sizes  container of all computed pixel values
     */
    private void updateDropZoneSizes(ScaledSizes sizes) {
        try {
            String dropZoneStyle = String.format(
                    "-fx-min-width: %.1fpx; -fx-pref-width: %.1fpx; -fx-max-width: %.1fpx; " +
                            "-fx-min-height: %.1fpx; -fx-pref-height: %.1fpx; -fx-max-height: %.1fpx;",
                    sizes.dropZoneWidth, sizes.dropZoneWidth, sizes.dropZoneWidth,
                    sizes.dropZoneHeight, sizes.dropZoneHeight, sizes.dropZoneHeight
            );
            dropZone.setStyle(dropZoneStyle);

            if (dropZoneLabel != null) {
                dropZoneLabel.setStyle(String.format("-fx-font-size: %.1fpx;", sizes.dropZoneFont));
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating drop zone sizes", e);
        }
    }

    /**
     * Constrains the file TableView’s width/height and font size.
     *
     * @param sizes  container of all computed pixel values
     */
    private void updateTableViewSizes(ScaledSizes sizes) {
        try {
            // Constrain table width to 65% of current window width and center it
            Window window = titleLabel.getScene().getWindow();
            double tableWidth = window.getWidth() * 0.65;
            fileTableView.setMinWidth(tableWidth);
            fileTableView.setPrefWidth(tableWidth);
            fileTableView.setMaxWidth(tableWidth);
            String tableStyle = String.format(
                    "-fx-min-height: %.1fpx; -fx-pref-height: %.1fpx; -fx-max-height: %.1fpx; " +
                            "-fx-font-size: %.1fpx;",
                    sizes.tableHeight, sizes.tableHeight, sizes.tableHeight,
                    sizes.tableFont
            );
            fileTableView.setStyle(tableStyle);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating table view sizes", e);
        }
    }

    /**
     * Adjusts the directory‐picker TextField and Browse button to scaled dimensions.
     *
     * @param sizes  container of all computed pixel values
     */
    private void updateDirectoryPickerSizes(ScaledSizes sizes) {
        try {
            // Update container spacing
            if (directoryPickerContainer != null) {
                directoryPickerContainer.setSpacing(sizes.directorySpacing);
            }

            // Update directory field
            String fieldStyle = String.format(
                    "-fx-min-width: %.1fpx; -fx-pref-width: %.1fpx; " +
                            "-fx-min-height: %.1fpx; -fx-pref-height: %.1fpx; -fx-max-height: %.1fpx; " +
                            "-fx-font-size: %.1fpx;",
                    sizes.directoryFieldWidth, sizes.directoryFieldWidth,
                    sizes.directoryFieldHeight, sizes.directoryFieldHeight, sizes.directoryFieldHeight,
                    sizes.labelSize
            );
            directoryField.setStyle(fieldStyle);

            // Update browse button
            String browseStyle = String.format(
                    "-fx-min-width: %.1fpx; -fx-pref-width: %.1fpx; -fx-max-width: %.1fpx; " +
                            "-fx-min-height: %.1fpx; -fx-pref-height: %.1fpx; -fx-max-height: %.1fpx; " +
                            "-fx-font-size: %.1fpx;",
                    sizes.browseButtonWidth, sizes.browseButtonWidth, sizes.browseButtonWidth,
                    sizes.browseButtonHeight, sizes.browseButtonHeight, sizes.browseButtonHeight,
                    sizes.labelSize
            );
            browseDirBtn.setStyle(browseStyle);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating directory picker sizes", e);
        }
    }


    /**
     * Adjusts the Save button to scaled dimensions and font size.
     *
     * @param sizes  container of all computed pixel values
     */
    private void updateSaveButtonSizes(ScaledSizes sizes) {
        try {
            String saveStyle = String.format(
                    "-fx-min-width: %.1fpx; -fx-pref-width: %.1fpx; -fx-max-width: %.1fpx; " +
                            "-fx-min-height: %.1fpx; -fx-pref-height: %.1fpx; -fx-max-height: %.1fpx; " +
                            "-fx-font-size: %.1fpx;",
                    sizes.saveButtonWidth, sizes.saveButtonWidth, sizes.saveButtonWidth,
                    sizes.saveButtonHeight, sizes.saveButtonHeight, sizes.saveButtonHeight,
                    sizes.saveButtonFont
            );
            saveBtn.setStyle(saveStyle);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating save button sizes", e);
        }
    }

    /**
     * Resizes the theme‐toggle and back buttons (and their icons) proportionally.
     *
     * @param sizes  container of all computed pixel values
     */
    private void updateNavigationButtonSizes(ScaledSizes sizes) {
        try {
            // Update theme toggle button
            String themeButtonStyle = String.format(
                    "-fx-min-width: %.1fpx; -fx-min-height: %.1fpx; " +
                            "-fx-max-width: %.1fpx; -fx-max-height: %.1fpx; " +
                            "-fx-pref-width: %.1fpx; -fx-pref-height: %.1fpx;",
                    sizes.themeButtonSize, sizes.themeButtonSize,
                    sizes.themeButtonSize, sizes.themeButtonSize,
                    sizes.themeButtonSize, sizes.themeButtonSize
            );
            themeToggleBtn.setStyle(themeButtonStyle);

            // Update theme icon
            if (themeToggleBtn.getGraphic() instanceof FontIcon themeIcon) {
                themeIcon.setIconSize((int) Math.round(sizes.themeIconSize));
            }

            // Update back button
            String backButtonStyle = String.format(
                    "-fx-min-width: %.1fpx; -fx-min-height: %.1fpx; " +
                            "-fx-max-width: %.1fpx; -fx-max-height: %.1fpx; " +
                            "-fx-pref-width: %.1fpx; -fx-pref-height: %.1fpx;",
                    sizes.backButtonSize, sizes.backButtonSize,
                    sizes.backButtonSize, sizes.backButtonSize,
                    sizes.backButtonSize, sizes.backButtonSize
            );
            backButton.setStyle(backButtonStyle);

            // Update back icon
            if (backButton.getGraphic() instanceof FontIcon backIcon) {
                backIcon.setIconSize((int) Math.round(sizes.backIconSize));
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating navigation button sizes", e);
        }
    }

    /**
     * Resizes all “remove” buttons/icons inside the file table.
     *
     * @param sizes  container of all computed pixel values
     */
    private void updateRemoveButtonSizes(ScaledSizes sizes) {
        try {
            // Style all remove buttons in the table
            String btnStyle = String.format(
                "-fx-min-width: %.1fpx; -fx-pref-width: %.1fpx; -fx-max-width: %.1fpx;"
              + "-fx-min-height: %.1fpx; -fx-pref-height: %.1fpx; -fx-max-height: %.1fpx;",
                sizes.removeButtonSize, sizes.removeButtonSize, sizes.removeButtonSize,
                sizes.removeButtonSize, sizes.removeButtonSize, sizes.removeButtonSize
            );
            String iconSizeCss = String.format("-fx-icon-size: %dpx;", (int)Math.round(sizes.removeIconSize));

            // Apply to each button via style class
            fileTableView.lookupAll(".remove-btn").forEach(node -> {
                if (node instanceof Button btn) {
                    btn.setStyle(btnStyle);
                    if (btn.getGraphic() instanceof FontIcon icon) {
                        icon.setIconSize((int)Math.round(sizes.removeIconSize));
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating remove button sizes", e);
        }
    }

    /**
     * Enforces minimum window size and sets initial size.
     */
    private void enforceMinimumWindowSize() {
        Platform.runLater(() -> {
            try {
                if (titleLabel.getScene() != null && titleLabel.getScene().getWindow() instanceof Stage stage) {
                    double minWidth = calculateAbsoluteMinWidth();
                    double minHeight = calculateAbsoluteMinHeight();

                    stage.setMinWidth(LayoutConstants.BASE_WINDOW_WIDTH);
                    stage.setMinHeight(LayoutConstants.BASE_WINDOW_HEIGHT);

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
        return Math.max(
                LayoutConstants.MIN_DROP_ZONE_WIDTH + LayoutConstants.BORDER_PANE_PADDING,
                (2 * LayoutConstants.MIN_COMBO_WIDTH) + LayoutConstants.MIN_SWAP_BUTTON_SIZE +
                        (2 * LayoutConstants.MIN_FORMAT_SPACING) + LayoutConstants.BORDER_PANE_PADDING
        );
    }

    /**
     * Calculates absolute minimum height needed.
     */
    private double calculateAbsoluteMinHeight() {
        return LayoutConstants.MIN_TITLE_SIZE +
                LayoutConstants.MIN_COMBO_HEIGHT +
                LayoutConstants.MIN_DROP_ZONE_HEIGHT +
                LayoutConstants.MIN_TABLE_HEIGHT +
                LayoutConstants.MIN_DIRECTORY_FIELD_HEIGHT +
                LayoutConstants.MIN_SAVE_BUTTON_HEIGHT +
                (5 * LayoutConstants.MIN_MAIN_SPACING) +
                LayoutConstants.TOP_SECTION_HEIGHT +
                LayoutConstants.BORDER_PANE_PADDING;
    }

    /**
     * Checks if debug mode is enabled.
     */
    private boolean isDebugMode() {
        return Boolean.parseBoolean(System.getProperty("app.debug", "false"));
    }

    /**
     * Sets up theme management using ThemeManager.
     */
    private void setupTheme() {
        try {
            themeManager = ThemeManager.getInstance();

            // Apply theme to current scene and setup toggle button
            Platform.runLater(() -> {
                if (titleLabel.getScene() != null) {
                    themeManager.initializeTheme(titleLabel.getScene(), themeToggleBtn);
                }
            });

            // If scene is not ready yet, wait for it
            titleLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    themeManager.initializeTheme(newScene, themeToggleBtn);
                }
            });

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting up theme management", e);
        }
    }


    /**
     * Changes the active conversion category and refreshes formats/UI.
     *
     * @param categoryText  display name of the new category ("Images", "Audio", "Video")
     */
    public void setConversionCategory(String categoryText) {
        try {
            this.conversionCategory = ConversionCategory.fromString(categoryText);
            updateTitle();
            updateFormatComboBoxes();
            resetPageExceptDirectory();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting conversion category: " + categoryText, e);
        }
    }

    /**
     * Updates the title label based on the current conversion category.
     */
    private void updateTitle() {
        if (titleLabel != null) {
            titleLabel.setText(conversionCategory.getDisplayName() + " Conversion");
        }
    }

    /**
     * Updates format combo boxes based on current conversion category.
     */
    private void updateFormatComboBoxes() {
        Map<String, Set<String>> formats = ConversionMappings.MAPPINGS.get(conversionCategory);
        if (formats != null) {
            sourceFormatComboBox.getItems().clear();
            sourceFormatComboBox.getItems().addAll(formats.keySet());

            targetFormatComboBox.getItems().clear();
            // Will be populated when source format is selected
        }
    }


    /**
     * Sets up the file table view with proper columns and cell factories.
     */
    private void setupTableView() {
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));

        removeButtonColumn.setCellFactory(tableColumn -> new TableCell<>() {
            private final Button removeButton = createRemoveButton();

            private Button createRemoveButton() {
                Button removeBtn = new Button();
                FontIcon closeIcon = new FontIcon("mdi2c-close");
                closeIcon.setIconSize(16);
                removeBtn.setGraphic(closeIcon);
                removeBtn.getStyleClass().add("remove-btn");
                removeBtn.setOnAction(actionEvent -> {
                    FileItem selectedItem = getTableView().getItems().get(getIndex());
                    removeFile(selectedItem.getFile());
                });
                return removeBtn;
            }

            @Override
            protected void updateItem(Button buttonItem, boolean isEmpty) {
                super.updateItem(buttonItem, isEmpty);
                if (isEmpty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(removeButton);
                }
            }
        });
    }


    /**
     * Sets up the drag and drop zone functionality.
     */
    private void setupDropZone() {
        dropZone.setOnMouseClicked(event -> openFileChooser());
    }

    /**
     * Updates drag and drop validation message based on selected format.
     */
    private void updateDropZoneMessage() {
        String selectedFormat = sourceFormatComboBox.getSelectionModel().getSelectedItem();

        if (selectedFormat != null) {
            dropZoneLabel.setText("Drop " + selectedFormat + " files here or click to select");
        } else {
            dropZoneLabel.setText("Drop files here or click to select");
        }
    }


    /**
     * Filters a list of files down to only those valid for the current source format.
     *
     * @param filesToFilter  list of user‐dropped or chosen File objects
     * @return               only those files matching the selected format
     */
    private List<File> filterValidFiles(List<File> filesToFilter) {
        String selectedFormat = sourceFormatComboBox.getSelectionModel().getSelectedItem();
        List<File> validFiles = new ArrayList<>();

        for (File currentFile : filesToFilter) {
            boolean isValidFile;
            if (selectedFormat != null) {
                isValidFile = isValidForSelectedFormat(currentFile);
            } else {
                isValidFile = isValidFileType(currentFile);
            }

            if (isValidFile) {
                validFiles.add(currentFile);
            }
        }

        return validFiles;
    }

    /**
     * Drag‐over event handler for the drop zone.
     *
     * @param dragEvent  the DragEvent fired by JavaFX
     */
    @FXML
    private void onDragOver(DragEvent dragEvent) {
        if (dragEvent.getGestureSource() != dropZone && dragEvent.getDragboard().hasFiles()) {
            dragEvent.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
        dragEvent.consume();
    }

    /**
     * Drag‐dropped event handler; adds valid files or shows an error.
     *
     * @param dragEvent  the DragEvent fired by JavaFX
     */
    @FXML
    private void onDragDropped(DragEvent dragEvent) {
        Dragboard dragboard = dragEvent.getDragboard();
        boolean operationSuccess = false;

        if (dragboard.hasFiles()) {
            List<File> droppedFiles = dragboard.getFiles();
            List<File> validFiles = filterValidFiles(droppedFiles);

            if (!validFiles.isEmpty()) {
                addFiles(validFiles);
                operationSuccess = true;
            } else {
                String selectedFormat = sourceFormatComboBox.getSelectionModel().getSelectedItem();
                String expectedFormat = selectedFormat != null ? selectedFormat :
                        conversionCategory.getDisplayName().toLowerCase();
                showAlert("Invalid File Types",
                        "Please select valid " + expectedFormat + " files for conversion.");
            }
        }

        dragEvent.setDropCompleted(operationSuccess);
        dragEvent.consume();
    }

    /**
     * Drag‐entered event handler; highlights the drop zone.
     *
     * @param dragEvent  the DragEvent fired by JavaFX
     */
    @FXML
    private void onDragEntered(DragEvent dragEvent) {
        if (dragEvent.getGestureSource() != dropZone && dragEvent.getDragboard().hasFiles()) {
            dropZone.getStyleClass().add("drag-over");
        }
        dragEvent.consume();
    }

    /**
     * Drag‐exited event handler; removes the drop‐zone highlight.
     *
     * @param dragEvent  the DragEvent fired by JavaFX
     */
    @FXML
    private void onDragExited(DragEvent dragEvent) {
        dropZone.getStyleClass().remove("drag-over");
        dragEvent.consume();
    }

    /**
     * Sets up the directory browser functionality.
     */
    private void setupDirectoryBrowser() {
        browseDirBtn.setOnAction(actionEvent -> chooseOutputDirectory());
        directoryField.textProperty().addListener((observableValue, previousText, currentText) -> updateSaveButtonState());
    }


    /**
     * Sets up the format combo boxes with appropriate listeners.
     */
    private void setupFormatComboBoxes() {
        sourceFormatComboBox.getSelectionModel().selectedItemProperty().addListener(
                (observableValue, previousValue, selectedValue) -> {
                    updateTargetFormats(selectedValue);
                    updateDropZoneMessage();
                    // Reset everything except directory when source format changes
                    if (previousValue != null && selectedValue != null && !previousValue.equals(selectedValue)) {
                        resetPageExceptDirectory();
                    }
                }
        );

        targetFormatComboBox.getSelectionModel().selectedItemProperty().addListener(
                (observableValue, previousValue, selectedValue) -> updateSaveButtonState()
        );
    }


    /**
     * Updates target format combo box based on selected source format.
     */
    private void updateTargetFormats(String sourceFormat) {
        Map<String, Set<String>> formats = ConversionMappings.MAPPINGS.get(conversionCategory);
        if (formats != null && formats.containsKey(sourceFormat)) {
            targetFormatComboBox.getItems().clear();
            targetFormatComboBox.getItems().addAll(formats.get(sourceFormat));
        }
    }

    /**
     * Swaps the source and target selections in their ComboBoxes.
     */
    @FXML
    private void onSwapFormats() {
        String source = sourceFormatComboBox.getValue();
        String target = targetFormatComboBox.getValue();

        if (source != null && target != null) {
            sourceFormatComboBox.setValue(target);
            updateTargetFormats(target);
            targetFormatComboBox.setValue(source);
        }
    }

    /**
     * Sets up button actions for navigation and theme.
     */
    private void setupButtonActions() {
        try {
            // Back button
            backButton.setOnAction(e -> goBackToStartView());

            // Theme is now handled by ThemeManager in setupTheme()

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting up button actions", e);
        }
    }

    /**
     * Handler for the Save File button; kicks off conversion.
     */
    @FXML
    private void onSaveClick() {
        performConversion();
    }

    /**
     * Resets the page state except for the directory field.
     */
    private void resetPageExceptDirectory() {
        selectedFiles.clear();
        fileTableView.getItems().clear();
        targetFormatComboBox.getSelectionModel().clearSelection();
        updateSaveButtonState();
    }

    /**
     * Adds a batch of files to the table and shows errors for invalid ones.
     *
     * @param filesToAdd  list of File objects to attempt to convert
     */
    private void addFiles(List<File> filesToAdd) {
        String selectedFormat = sourceFormatComboBox.getSelectionModel().getSelectedItem();
        List<File> validFiles = new ArrayList<>();
        List<File> invalidFiles = new ArrayList<>();

        for (File currentFile : filesToAdd) {
            boolean isValidFile;
            if (selectedFormat != null) {
                isValidFile = isValidForSelectedFormat(currentFile);
            } else {
                isValidFile = isValidFileType(currentFile);
            }

            if (isValidFile) {
                validFiles.add(currentFile);
            } else {
                invalidFiles.add(currentFile);
            }
        }

        // Add valid files
        for (File validFile : validFiles) {
            if (selectedFiles.add(validFile)) {
                fileTableView.getItems().add(new FileItem(validFile));
            }
        }

        // Show error for invalid files
        if (!invalidFiles.isEmpty()) {
            String expectedFormat = selectedFormat != null ? selectedFormat :
                    "valid " + conversionCategory.getDisplayName().toLowerCase();
            String invalidFilesList = buildInvalidFilesMessage(invalidFiles);
            String errorMessage = String.format(
                    "The following files were not added because they don't match the expected format (%s):\n%s",
                    expectedFormat, invalidFilesList
            );
            showAlert("Invalid File Format", errorMessage);
        }

        updateSaveButtonState();
    }


    /**
     * Updates available source formats without triggering listeners.
     */
    private void updateSourceFormatsQuietly() {
        String currentSourceSelection = sourceFormatComboBox.getSelectionModel().getSelectedItem();
        String currentTargetSelection = targetFormatComboBox.getSelectionModel().getSelectedItem();

        Map<String, Set<String>> categoryMappings = ConversionMappings.MAPPINGS.get(conversionCategory);
        if (categoryMappings != null) {
            sourceFormatComboBox.getItems().clear();
            sourceFormatComboBox.getItems().addAll(categoryMappings.keySet());

            // Restore both selections
            if (currentSourceSelection != null && categoryMappings.containsKey(currentSourceSelection)) {
                sourceFormatComboBox.getSelectionModel().select(currentSourceSelection);

                // Restore target selection as well
                if (currentTargetSelection != null) {
                    Set<String> targetFormats = categoryMappings.get(currentSourceSelection);
                    if (targetFormats != null && targetFormats.contains(currentTargetSelection)) {
                        targetFormatComboBox.getSelectionModel().select(currentTargetSelection);
                    }
                }
            }
        }
    }


    /**
     * Updates available source formats.
     */
    private void updateSourceFormats() {
        updateSourceFormatsQuietly();
    }



    /**
     * Removes a file from the conversion list.
     */
    private void removeFile(File fileToRemove) {
        selectedFiles.remove(fileToRemove);

        // Remove from table view using traditional loop for better readability
        List<FileItem> itemsToRemove = new ArrayList<>();
        for (FileItem fileItem : fileTableView.getItems()) {
            if (fileItem.getFile().equals(fileToRemove)) {
                itemsToRemove.add(fileItem);
            }
        }
        fileTableView.getItems().removeAll(itemsToRemove);

        updateSaveButtonState();
        // Only update source formats if no files remain
        if (selectedFiles.isEmpty()) {
            updateSourceFormats();
        }
    }


    /**
     * Opens file chooser for manual file selection.
     */
    private void openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Files to Convert");

        String selectedFormat = sourceFormatComboBox.getSelectionModel().getSelectedItem();

        if (selectedFormat != null) {
            FileChooser.ExtensionFilter fileFilter = new FileChooser.ExtensionFilter(
                    selectedFormat + " Files",
                    "*." + selectedFormat.toLowerCase()
            );
            fileChooser.getExtensionFilters().add(fileFilter);
        } else {
            List<String> validExtensions = getValidExtensions();
            if (!validExtensions.isEmpty()) {
                String[] extensionArray = new String[validExtensions.size()];
                for (int i = 0; i < validExtensions.size(); i++) {
                    extensionArray[i] = "*." + validExtensions.get(i).toLowerCase();
                }

                FileChooser.ExtensionFilter categoryFilter = new FileChooser.ExtensionFilter(
                        conversionCategory.getDisplayName() + " Files", extensionArray);
                fileChooser.getExtensionFilters().add(categoryFilter);
            }
        }

        List<File> selectedFilesList = fileChooser.showOpenMultipleDialog(dropZone.getScene().getWindow());
        if (selectedFilesList != null && !selectedFilesList.isEmpty()) {
            addFiles(selectedFilesList);
        }
    }


    /**
     * Gets file extension from filename.
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * Validates if file type is valid for current conversion category.
     */
    private boolean isValidFileType(File file) {
        String extension = getFileExtension(file.getName()).toUpperCase();
        Map<String, Set<String>> categoryMappings = ConversionMappings.MAPPINGS.get(conversionCategory);
        return categoryMappings != null && categoryMappings.containsKey(extension);
    }


    /**
     * Validates if a file matches the selected source format.
     */
    private boolean isValidForSelectedFormat(File file) {
        String selectedFormat = sourceFormatComboBox.getSelectionModel().getSelectedItem();
        if (selectedFormat == null) {
            return isValidFileType(file);
        }

        String fileExtension = getFileExtension(file.getName()).toUpperCase();
        return selectedFormat.equals(fileExtension);
    }

    /**
     * Gets valid extensions for current category.
     */
    private List<String> getValidExtensions() {
        Map<String, Set<String>> categoryMappings = ConversionMappings.MAPPINGS.get(conversionCategory);
        if (categoryMappings == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(categoryMappings.keySet());
    }

    /**
     * Builds invalid files message.
     */
    private String buildInvalidFilesMessage(List<File> invalidFiles) {
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < invalidFiles.size(); i++) {
            if (i > 0) {
                messageBuilder.append("\n");
            }
            messageBuilder.append(invalidFiles.get(i).getName());
        }
        return messageBuilder.toString();
    }


    /**
     * Opens directory chooser for output directory selection.
     */
    private void chooseOutputDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose Output Directory");

        File selectedDirectory = directoryChooser.showDialog(browseDirBtn.getScene().getWindow());
        if (selectedDirectory != null) {
            directoryField.setText(selectedDirectory.getAbsolutePath());
        }
    }


    /**
     * Updates the save button state based on current selections.
     */
    private void updateSaveButtonState() {
        boolean hasFiles = !selectedFiles.isEmpty();
        boolean hasSourceFormat = sourceFormatComboBox.getSelectionModel().getSelectedItem() != null;
        boolean hasTargetFormat = targetFormatComboBox.getSelectionModel().getSelectedItem() != null;
        boolean hasOutputDirectory = !directoryField.getText().trim().isEmpty();

        saveBtn.setDisable(!(hasFiles && hasSourceFormat && hasTargetFormat && hasOutputDirectory));
    }


    /**
     * Performs the actual file conversion.
     */
    private void performConversion() {
        if (selectedFiles.isEmpty()) {
            showAlert("No Files", "Please select files to convert.");
            return;
        }

        String sourceFormat = sourceFormatComboBox.getValue();
        String targetFormat = targetFormatComboBox.getValue();
        String outputDir = directoryField.getText();

        if (sourceFormat == null || targetFormat == null) {
            showAlert("Missing Format", "Please select both source and target formats.");
            return;
        }

        if (outputDir == null || outputDir.trim().isEmpty()) {
            showAlert("Missing Directory", "Please select an output directory.");
            return;
        }

        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists() || !outputDirectory.isDirectory()) {
            showAlert("Invalid Directory", "The selected output directory does not exist.");
            return;
        }

        // Perform conversion in background thread (with progress dialog)
        Task<Void> conversionTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int totalFiles = selectedFiles.size();
                int processedFiles = 0;

                for (File inputFile : selectedFiles) {
                    String baseName = inputFile.getName();
                    int dotIndex = baseName.lastIndexOf('.');
                    if (dotIndex > 0) {
                        baseName = baseName.substring(0, dotIndex);
                    }

                    File outputFile = new File(outputDirectory, baseName + "." + targetFormat.toLowerCase());

                    // Make sure we don't overwrite the input file
                    if (outputFile.equals(inputFile)) {
                        outputFile = new File(outputDirectory, baseName + "_converted." + targetFormat.toLowerCase());
                    }

                    ConverterRouter.convert(inputFile, outputFile, targetFormat, conversionCategory);

                    processedFiles++;
                    // update the message so the dialog shows "Converting file X of Y"
                    updateMessage("Converting file " + processedFiles + " of " + totalFiles);
                    final int completed = processedFiles;
                    Platform.runLater(() -> updateProgress(completed, totalFiles));
                }

                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    showAlert("Conversion Complete", "All files have been converted successfully.");
                    resetPageExceptDirectory();
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    String message = exception != null ? exception.getMessage() : "Unknown error occurred";
                    showAlert("Conversion Failed", "Error during conversion: " + message);
                });
            }
        };

        // create the dialog
        ProgressDialog progressDialog = new ProgressDialog(conversionTask);

        // kick off conversion *before* showing the dialog
        Thread conversionThread = new Thread(conversionTask);
        conversionThread.setDaemon(true);
        conversionThread.start();

        progressDialog.show();

        // re-enable Save when done
        saveBtn.setDisable(false);

    }

    /**
     * Goes back to the start view.
     */
    private void goBackToStartView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/test/truinconv/start-view.fxml"));
            Parent startRoot = loader.load();

            Scene currentScene = titleLabel.getScene();
            Scene startScene = new Scene(startRoot, currentScene.getWidth(), currentScene.getHeight());

            // Copy stylesheets from current scene
            startScene.getStylesheets().addAll(currentScene.getStylesheets());

            Stage stage = (Stage) currentScene.getWindow();
            stage.setScene(startScene);

            // Apply current theme to new scene
            if (themeManager != null) {
                themeManager.applyTheme(startScene);
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load start view", e);
            showAlert("Navigation Error", "Could not return to the start view.");
        }
    }

    /**
     * Shows a simple information Alert with the given title and message.
     *
     * @param alertTitle   the window title of the Alert
     * @param alertMessage the body text of the Alert
     */
    private void showAlert(String alertTitle, String alertMessage) {
        Alert informationAlert = new Alert(Alert.AlertType.INFORMATION);
        informationAlert.setTitle(alertTitle);
        informationAlert.setHeaderText(null);
        informationAlert.setContentText(alertMessage);
        informationAlert.showAndWait();
    }

}