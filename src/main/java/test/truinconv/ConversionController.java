
package test.truinconv;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Controller for the file conversion interface.
 * Handles file selection, format conversion options, and UI responsiveness.
 */
public class ConversionController {

    private static final Logger LOGGER = Logger.getLogger(ConversionController.class.getName());

    // FXML-injected UI components
    @FXML private Label titleLabel;
    @FXML private StackPane dropZone;
    @FXML private Label dropZoneLabel;
    @FXML private TextField directoryField;
    @FXML private Button browseDirBtn;
    @FXML private Button saveBtn;
    @FXML private ToggleButton themeToggleBtn;
    @FXML private VBox mainVBox;
    @FXML private ComboBox<String> sourceFormatComboBox;
    @FXML private ComboBox<String> targetFormatComboBox;
    @FXML private FontIcon arrowIcon;
    @FXML private Button swapButton;
    @FXML private TableView<FileItem> fileTableView;
    @FXML private TableColumn<FileItem, String> fileNameColumn;
    @FXML private TableColumn<FileItem, Button> removeButtonColumn;
    @FXML private Button backButton;

    // State management
    private final Set<File> selectedFiles = new LinkedHashSet<>();
    private boolean darkMode = false;
    private ConversionCategory conversionCategory = ConversionCategory.IMAGE;

    // Layout constants organized in a nested class for better maintainability
    private static final class LayoutConstants {
        static final double MIN_TITLE_SIZE = 22.0;
        static final double MAX_TITLE_SIZE = 44.0;
        static final double MIN_BUTTON_SIZE = 60.0;
        static final double MAX_BUTTON_SIZE = 100.0;
        static final double MIN_COMBO_WIDTH = 120.0;
        static final double MAX_COMBO_WIDTH = 200.0;
        static final double MIN_DROPZONE_WIDTH = 300.0;
        static final double MAX_DROPZONE_WIDTH = 500.0;
        static final double MIN_DROPZONE_HEIGHT = 80.0;
        static final double MAX_DROPZONE_HEIGHT = 120.0;
        static final double MIN_TABLE_HEIGHT = 100.0;
        static final double MAX_TABLE_HEIGHT = 200.0;
        static final double MIN_TEXTFIELD_WIDTH = 180.0;
        static final double MAX_TEXTFIELD_WIDTH = 300.0;
        static final double MIN_WINDOW_WIDTH = 400.0;
        static final double MIN_WINDOW_HEIGHT = 500.0;
        static final double RESPONSIVE_SCALE_WIDTH = 400.0;
        static final double RESPONSIVE_SCALE_HEIGHT = 300.0;
    }

    // Conversion category enumeration for type safety
    public enum ConversionCategory {
        IMAGE("Images"),
        MEDIA("Media"),  // Combined audio and video
        DOCUMENT("Documents");

        private final String displayName;

        ConversionCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static ConversionCategory fromString(String str) {
            return switch (str.toLowerCase()) {
                case "images" -> IMAGE;
                case "media" -> MEDIA;
                case "docs", "documents" -> DOCUMENT;
                default -> throw new IllegalArgumentException("Unknown conversion category: " + str);
            };
        }
    }

    // Comprehensive conversion mappings including both audio and video formats under MEDIA
    private static final Map<ConversionCategory, Map<String, Set<String>>> CONVERSION_MAPPINGS =
            Map.of(
                    ConversionCategory.IMAGE, Map.of(
                            "JPG", Set.of("PNG", "BMP", "GIF", "TIFF"),
                            "JPEG", Set.of("PNG", "BMP", "GIF", "TIFF"),
                            "PNG", Set.of("JPG", "JPEG", "BMP", "GIF", "TIFF"),
                            "BMP", Set.of("JPG", "JPEG", "PNG", "GIF", "TIFF"),
                            "GIF", Set.of("JPG", "JPEG", "PNG", "BMP", "TIFF"),
                            "TIFF", Set.of("JPG", "JPEG", "PNG", "BMP", "GIF")
                    ),
                    ConversionCategory.MEDIA, Map.of(
                            // Audio formats
                            "MP3", Set.of("WAV", "AAC", "FLAC", "OGG"),
                            "WAV", Set.of("MP3", "AAC", "FLAC", "OGG"),
                            "AAC", Set.of("MP3", "WAV", "FLAC", "OGG"),
                            "FLAC", Set.of("MP3", "WAV", "AAC", "OGG"),
                            "OGG", Set.of("MP3", "WAV", "AAC", "FLAC"),
                            // Video formats
                            "MP4", Set.of("AVI", "MKV", "MOV", "WEBM"),
                            "AVI", Set.of("MP4", "MKV", "MOV", "WEBM"),
                            "MKV", Set.of("MP4", "AVI", "MOV", "WEBM"),
                            "MOV", Set.of("MP4", "AVI", "MKV", "WEBM"),
                            "WEBM", Set.of("MP4", "AVI", "MKV", "MOV")
                    ),
                    ConversionCategory.DOCUMENT, Map.of(
                            "DOCX", Set.of("PDF"),
                            "PDF", Set.of("DOCX")
                    )
            );

    // Define audio and video format sets for validation and UI purposes
    private static final Set<String> AUDIO_FORMATS = Set.of("MP3", "WAV", "AAC", "FLAC", "OGG");
    private static final Set<String> VIDEO_FORMATS = Set.of("MP4", "AVI", "MKV", "MOV", "WEBM");

    /**
     * Represents a file item in the TableView with proper equals/hashCode implementation.
     */
    public static class FileItem {
        private final File file;
        private final SimpleStringProperty fileName;

        public FileItem(File file) {
            this.file = Objects.requireNonNull(file, "File cannot be null");
            this.fileName = new SimpleStringProperty(file.getName());
        }

        public File getFile() {
            return file;
        }

        public String getFileName() {
            return fileName.get();
        }

        public SimpleStringProperty fileNameProperty() {
            return fileName;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof FileItem other)) return false;
            return Objects.equals(file, other.file);
        }

        @Override
        public int hashCode() {
            return Objects.hash(file);
        }

        @Override
        public String toString() {
            return "FileItem{file=" + file.getName() + "}";
        }
    }

    /**
     * Custom TableCell implementation for remove buttons.
     */
    private class RemoveButtonTableCell extends TableCell<FileItem, Button> {
        private final Button removeButton;

        public RemoveButtonTableCell() {
            removeButton = createRemoveButton();
        }

        private Button createRemoveButton() {
            Button btn = new Button("×");
            btn.getStyleClass().add("remove-btn");
            btn.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-text-fill: #d32f2f; " +
                            "-fx-font-size: 16px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-cursor: hand;"
            );
            btn.setOnAction(event -> {
                if (getIndex() < getTableView().getItems().size()) {
                    FileItem fileItem = getTableView().getItems().get(getIndex());
                    removeFile(fileItem);
                }
            });
            return btn;
        }

        @Override
        protected void updateItem(Button item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty || getIndex() >= getTableView().getItems().size() ? null : removeButton);
        }
    }

    @FXML
    private void initialize() {
        setupTableView();
        setupEventHandlers();
        setupFormatComboBoxes();
        setupThemeToggle();

        // Initialize scene-dependent setup asynchronously
        Platform.runLater(this::setup);
    }

    /**
     * Configures the TableView with proper cell factories and value factories.
     */
    private void setupTableView() {
        fileNameColumn.setCellValueFactory(cellData -> cellData.getValue().fileNameProperty());
        removeButtonColumn.setCellFactory(column -> new RemoveButtonTableCell());
        removeButtonColumn.setSortable(false);
        removeButtonColumn.setResizable(false);
    }

    /**
     * Sets up all event handlers for UI components.
     */
    private void setupEventHandlers() {
        // File selection handlers
        dropZone.setOnMouseClicked(e -> openFileChooser());
        browseDirBtn.setOnAction(e -> openDirectoryChooser());

        // Navigation and action handlers
        backButton.setOnAction(e -> goBackToStartView());
        saveBtn.setOnAction(e -> onSaveClick());
        swapButton.setOnAction(e -> onSwapFormats());

        // Drag and drop handlers
        dropZone.setOnDragOver(this::onDragOver);
        dropZone.setOnDragDropped(this::onDragDropped);
        dropZone.setOnDragEntered(this::onDragEntered);
        dropZone.setOnDragExited(this::onDragExited);
    }

    /**
     * Configures format combo boxes with change listeners.
     */
    private void setupFormatComboBoxes() {
        sourceFormatComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateTargetFormatOptions();
            updateDropZonePrompt();
        });
    }

    /**
     * Sets up theme toggle functionality.
     */
    private void setupThemeToggle() {
        themeToggleBtn.selectedProperty().addListener((obs, oldVal, selected) -> {
            darkMode = selected;
            toggleTheme(selected);
            updateThemeIcon();
        });
    }

    /**
     * Sets the conversion type and initializes format options.
     *
     * @param type the conversion type string ("Images", "Media", "Docs", etc.)
     */
    public void setConversionType(String type) {
        try {
            this.conversionCategory = ConversionCategory.fromString(type);
            populateSourceFormats();
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid conversion type: " + type, e);
            showAlert(Alert.AlertType.ERROR, "Configuration Error",
                    "Invalid conversion type", "The specified conversion type is not supported.");
        }
    }

    /**
     * Populates source format combo box based on current conversion category.
     * For MEDIA category, includes both audio and video formats with logical grouping.
     */
    private void populateSourceFormats() {
        var availableFormats = CONVERSION_MAPPINGS.get(conversionCategory).keySet();

        List<String> sourceFormats;

        if (conversionCategory == ConversionCategory.MEDIA) {
            // For media, group audio formats first, then video formats
            sourceFormats = new ArrayList<>();

            // Add audio formats first (sorted)
            availableFormats.stream()
                    .filter(AUDIO_FORMATS::contains)
                    .sorted()
                    .forEach(sourceFormats::add);

            // Add video formats second (sorted)
            availableFormats.stream()
                    .filter(VIDEO_FORMATS::contains)
                    .sorted()
                    .forEach(sourceFormats::add);
        } else {
            // For other categories, simple alphabetical sort
            sourceFormats = availableFormats.stream()
                    .sorted()
                    .collect(Collectors.toList());
        }

        sourceFormatComboBox.setItems(FXCollections.observableList(sourceFormats));

        // Auto-select first format if available
        if (!sourceFormats.isEmpty()) {
            sourceFormatComboBox.getSelectionModel().selectFirst();
        }
    }

    /**
     * Updates target format options based on selected source format.
     */
    private void updateTargetFormatOptions() {
        String selectedSource = sourceFormatComboBox.getValue();
        if (selectedSource == null) {
            targetFormatComboBox.setItems(FXCollections.observableArrayList());
            return;
        }

        var targetOptions = CONVERSION_MAPPINGS.get(conversionCategory)
                .getOrDefault(selectedSource, Set.of())
                .stream()
                .sorted()
                .collect(Collectors.toList());

        targetFormatComboBox.setItems(FXCollections.observableList(targetOptions));

        // Clear invalid selection
        if (!targetOptions.contains(targetFormatComboBox.getValue())) {
            targetFormatComboBox.setValue(null);
        }

        // Auto-select first target if available
        if (!targetOptions.isEmpty() && targetFormatComboBox.getValue() == null) {
            targetFormatComboBox.getSelectionModel().selectFirst();
        }
    }

    /**
     * Handles swapping source and target format selections.
     * Enhanced to handle media format type restrictions (audio ↔ audio, video ↔ video).
     */
    @FXML
    private void onSwapFormats() {
        String currentSource = sourceFormatComboBox.getValue();
        String currentTarget = targetFormatComboBox.getValue();

        if (currentSource == null || currentTarget == null) return;

        // Check if swap is valid
        var sourceItems = sourceFormatComboBox.getItems();
        if (sourceItems.contains(currentTarget)) {
            // Additional validation for media formats
            if (conversionCategory == ConversionCategory.MEDIA) {
                // Ensure we're not trying to swap between audio and video formats
                boolean sourceIsAudio = AUDIO_FORMATS.contains(currentSource);
                boolean targetIsAudio = AUDIO_FORMATS.contains(currentTarget);

                if (sourceIsAudio != targetIsAudio) {
                    // Cannot swap between audio and video - show warning
                    showAlert(Alert.AlertType.INFORMATION, "Format Swap",
                            "Cannot swap between audio and video formats",
                            "Audio formats can only be converted to other audio formats, and video formats to other video formats.");
                    return;
                }
            }

            sourceFormatComboBox.setValue(currentTarget);
            // Update target options and set new target
            Platform.runLater(() -> {
                if (targetFormatComboBox.getItems().contains(currentSource)) {
                    targetFormatComboBox.setValue(currentSource);
                }
            });
        }
    }

    /**
     * Updates drop zone prompt text based on selected source format.
     * Enhanced to show format type (Audio/Video) for media formats.
     */
    private void updateDropZonePrompt() {
        String sourceFormat = sourceFormatComboBox.getValue();
        if (sourceFormat == null) {
            dropZoneLabel.setText("Drop files here or click to select");
            return;
        }

        String formatType = "";
        if (conversionCategory == ConversionCategory.MEDIA) {
            if (AUDIO_FORMATS.contains(sourceFormat)) {
                formatType = " audio";
            } else if (VIDEO_FORMATS.contains(sourceFormat)) {
                formatType = " video";
            }
        }

        String promptText = String.format("Drop %s%s files here or click to select", sourceFormat, formatType);
        dropZoneLabel.setText(promptText);
    }

    /**
     * Sets up responsive layout after scene becomes available.
     */
    private void setup() {
        if (titleLabel.getScene() != null) {
            setupSceneListeners();
            setupWindowConstraints();
            updateLayout();
        } else {
            Platform.runLater(this::setup);
        }
    }

    /**
     * Navigates back to the start view, preserving theme state.
     */
    private void goBackToStartView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("start-view.fxml"));
            Parent startView = loader.load();

            // Pass theme state to start view controller
            StartViewController startController = loader.getController();
            if (startController != null) {
                startController.setThemeState(darkMode);
            }

            Scene currentScene = titleLabel.getScene();
            if (currentScene != null) {
                currentScene.setRoot(startView);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load start view", e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Failed to return to start screen", "An error occurred while loading the start view.");
        }
    }

    /**
     * Sets up scene dimension listeners for responsive layout.
     */
    private void setupSceneListeners() {
        Scene scene = titleLabel.getScene();
        if (scene != null) {
            scene.widthProperty().addListener((obs, oldWidth, newWidth) -> updateLayout());
            scene.heightProperty().addListener((obs, oldHeight, newHeight) -> updateLayout());
        }
    }

    /**
     * Configures minimum window size constraints.
     */
    private void setupWindowConstraints() {
        Scene scene = titleLabel.getScene();
        if (scene != null) {
            Window window = scene.getWindow();
            if (window instanceof Stage stage) {
                stage.setMinWidth(LayoutConstants.MIN_WINDOW_WIDTH);
                stage.setMinHeight(LayoutConstants.MIN_WINDOW_HEIGHT);
            }
        }
    }

    /**
     * Updates all component sizes proportionally based on window size.
     */
    private void updateLayout() {
        Scene scene = titleLabel.getScene();
        if (scene == null) return;

        double windowWidth = scene.getWidth();
        double windowHeight = scene.getHeight();

        updateTitleStyle(windowWidth);
        updateComboBoxSizes(windowWidth);
        updateDropZoneSize(windowWidth, windowHeight);
        updateTableSize(windowHeight);
        updateButtonSizes(windowWidth);
        updateTextFieldSize(windowWidth);
    }

    /**
     * Updates title font size proportionally to window width.
     */
    private void updateTitleStyle(double windowWidth) {
        double scaleFactor = Math.min(1.0, (windowWidth - LayoutConstants.MIN_WINDOW_WIDTH) / LayoutConstants.RESPONSIVE_SCALE_WIDTH);
        double fontSize = LayoutConstants.MIN_TITLE_SIZE +
                (LayoutConstants.MAX_TITLE_SIZE - LayoutConstants.MIN_TITLE_SIZE) * scaleFactor;
        titleLabel.setStyle(String.format("-fx-font-size: %.1fpx;", fontSize));
    }

    /**
     * Updates combo box sizes proportionally to window width.
     */
    private void updateComboBoxSizes(double windowWidth) {
        double scaleFactor = Math.min(1.0, (windowWidth - LayoutConstants.MIN_WINDOW_WIDTH) / LayoutConstants.RESPONSIVE_SCALE_WIDTH);
        double comboWidth = LayoutConstants.MIN_COMBO_WIDTH +
                (LayoutConstants.MAX_COMBO_WIDTH - LayoutConstants.MIN_COMBO_WIDTH) * scaleFactor;

        sourceFormatComboBox.setPrefWidth(comboWidth);
        targetFormatComboBox.setPrefWidth(comboWidth);
    }

    /**
     * Updates drop zone size proportionally to window dimensions.
     */
    private void updateDropZoneSize(double windowWidth, double windowHeight) {
        double widthScaleFactor = Math.min(1.0, (windowWidth - LayoutConstants.MIN_WINDOW_WIDTH) / LayoutConstants.RESPONSIVE_SCALE_WIDTH);
        double heightScaleFactor = Math.min(1.0, (windowHeight - LayoutConstants.MIN_WINDOW_HEIGHT) / LayoutConstants.RESPONSIVE_SCALE_HEIGHT);

        double dropZoneWidth = LayoutConstants.MIN_DROPZONE_WIDTH +
                (LayoutConstants.MAX_DROPZONE_WIDTH - LayoutConstants.MIN_DROPZONE_WIDTH) * widthScaleFactor;
        double dropZoneHeight = LayoutConstants.MIN_DROPZONE_HEIGHT +
                (LayoutConstants.MAX_DROPZONE_HEIGHT - LayoutConstants.MIN_DROPZONE_HEIGHT) * heightScaleFactor;

        dropZone.setPrefWidth(dropZoneWidth);
        dropZone.setPrefHeight(dropZoneHeight);
    }

    /**
     * Updates table size proportionally to window height.
     */
    private void updateTableSize(double windowHeight) {
        double scaleFactor = Math.min(1.0, (windowHeight - LayoutConstants.MIN_WINDOW_HEIGHT) / LayoutConstants.RESPONSIVE_SCALE_HEIGHT);
        double tableHeight = LayoutConstants.MIN_TABLE_HEIGHT +
                (LayoutConstants.MAX_TABLE_HEIGHT - LayoutConstants.MIN_TABLE_HEIGHT) * scaleFactor;
        fileTableView.setPrefHeight(tableHeight);
    }

    /**
     * Updates button sizes proportionally to window width.
     */
    private void updateButtonSizes(double windowWidth) {
        double scaleFactor = Math.min(1.0, (windowWidth - LayoutConstants.MIN_WINDOW_WIDTH) / LayoutConstants.RESPONSIVE_SCALE_WIDTH);
        double buttonSize = LayoutConstants.MIN_BUTTON_SIZE +
                (LayoutConstants.MAX_BUTTON_SIZE - LayoutConstants.MIN_BUTTON_SIZE) * scaleFactor;

        browseDirBtn.setPrefWidth(buttonSize);
        saveBtn.setPrefWidth(Math.max(110, buttonSize));
    }

    /**
     * Updates text field size proportionally to window width.
     */
    private void updateTextFieldSize(double windowWidth) {
        double scaleFactor = Math.min(1.0, (windowWidth - LayoutConstants.MIN_WINDOW_WIDTH) / LayoutConstants.RESPONSIVE_SCALE_WIDTH);
        double textFieldWidth = LayoutConstants.MIN_TEXTFIELD_WIDTH +
                (LayoutConstants.MAX_TEXTFIELD_WIDTH - LayoutConstants.MIN_TEXTFIELD_WIDTH) * scaleFactor;
        directoryField.setPrefWidth(textFieldWidth);
    }

    /**
     * Sets the theme state for this controller.
     *
     * @param isDarkMode true if dark mode should be enabled, false for light mode
     */
    public void setThemeState(boolean isDarkMode) {
        this.darkMode = isDarkMode;
        Platform.runLater(() -> {
            toggleTheme(isDarkMode);
            themeToggleBtn.setSelected(isDarkMode);
            updateThemeIcon();
            updateBackButtonIcon();
        });
    }

    /**
     * Updates back button icon color based on current theme.
     */
    private void updateBackButtonIcon() {
        if (backButton.getGraphic() instanceof FontIcon backIcon) {
            backIcon.setIconColor(Paint.valueOf(darkMode ? "#ffd740" : "#888"));
        }
    }

    // Drag and Drop Event Handlers

    @FXML
    private void onDragOver(DragEvent event) {
        if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
        event.consume();
    }

    @FXML
    private void onDragDropped(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        boolean success = false;

        if (dragboard.hasFiles()) {
            List<File> validFiles = filterValidFiles(dragboard.getFiles());
            if (!validFiles.isEmpty()) {
                addFilesToList(validFiles);
                success = true;
            } else if (!dragboard.getFiles().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Invalid Files",
                        "No valid files found", "The dropped files don't match the selected source format.");
            }
        }

        event.setDropCompleted(success);
        event.consume();
    }

    @FXML
    private void onDragEntered(DragEvent event) {
        if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
            dropZone.getStyleClass().add("drag-over");
        }
        event.consume();
    }

    @FXML
    private void onDragExited(DragEvent event) {
        dropZone.getStyleClass().remove("drag-over");
        event.consume();
    }

    // File Management Methods

    /**
     * Filters files based on the selected source format.
     *
     * @param files the list of files to filter
     * @return list of files matching the source format
     */
    private List<File> filterValidFiles(List<File> files) {
        String sourceFormat = sourceFormatComboBox.getValue();
        if (sourceFormat == null) {
            return List.of();
        }

        return files.stream()
                .filter(File::isFile)
                .filter(file -> hasValidExtension(file, sourceFormat))
                .collect(Collectors.toList());
    }

    /**
     * Checks if a file has a valid extension for the given format.
     *
     * @param file the file to check
     * @param expectedFormat the expected format
     * @return true if the file has a valid extension
     */
    private boolean hasValidExtension(File file, String expectedFormat) {
        String fileName = file.getName().toLowerCase();
        int lastDotIndex = fileName.lastIndexOf('.');

        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return false;
        }

        String extension = fileName.substring(lastDotIndex + 1);
        return extension.equalsIgnoreCase(expectedFormat);
    }

    /**
     * Adds files to the selection list, avoiding duplicates.
     *
     * @param files the files to add
     */
    private void addFilesToList(List<File> files) {
        boolean hasNewFiles = selectedFiles.addAll(files);
        if (hasNewFiles) {
            updateFileTableView();
        }
    }

    /**
     * Removes a file from the selection.
     *
     * @param fileItem the file item to remove
     */
    private void removeFile(FileItem fileItem) {
        selectedFiles.remove(fileItem.getFile());
        updateFileTableView();
    }

    /**
     * Updates the TableView with current file selection.
     */
    private void updateFileTableView() {
        var fileItems = selectedFiles.stream()
                .map(FileItem::new)
                .collect(Collectors.toList());
        fileTableView.setItems(FXCollections.observableList(fileItems));
    }

    // File Chooser Methods

    /**
     * Opens a file chooser for selecting files to convert.
     * Enhanced to show appropriate format type for media files.
     */
    private void openFileChooser() {
        String sourceFormat = sourceFormatComboBox.getValue();
        if (sourceFormat == null) {
            showAlert(Alert.AlertType.WARNING, "No Format Selected",
                    "Please select a source format first",
                    "You need to choose what type of files you want to convert before selecting files.");
            return;
        }

        FileChooser fileChooser = new FileChooser();

        // Enhanced title for media formats
        String title;
        if (conversionCategory == ConversionCategory.MEDIA) {
            String formatType = AUDIO_FORMATS.contains(sourceFormat) ? "Audio" : "Video";
            title = String.format("Select %s %s Files", sourceFormat, formatType);
        } else {
            title = "Select " + sourceFormat + " Files";
        }
        fileChooser.setTitle(title);

        // Configure file extension filter
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
                sourceFormat + " files", "*." + sourceFormat.toLowerCase()
        );
        fileChooser.getExtensionFilters().add(filter);

        Window window = dropZone.getScene().getWindow();
        List<File> files = fileChooser.showOpenMultipleDialog(window);
        if (files != null && !files.isEmpty()) {
            addFilesToList(files);
        }
    }

    /**
     * Opens a directory chooser for selecting output directory.
     */
    private void openDirectoryChooser() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Output Directory");

        Window window = directoryField.getScene().getWindow();
        File directory = directoryChooser.showDialog(window);
        if (directory != null) {
            directoryField.setText(directory.getAbsolutePath());
        }
    }

    // Action Handlers

    /**
     * Handles the save/convert button click.
     */
    @FXML
    private void onSaveClick() {
        if (!validateInputs()) {
            return;
        }

        // Process conversions asynchronously
        playSaveBtnAnimation();
        processConversions();
    }

    /**
     * Validates all required inputs before processing.
     *
     * @return true if all inputs are valid
     */
    private boolean validateInputs() {
        if (selectedFiles.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Files Selected",
                    "Please add files to convert",
                    "You need to select at least one file to convert.");
            return false;
        }

        String sourceFormat = sourceFormatComboBox.getValue();
        String targetFormat = targetFormatComboBox.getValue();

        if (sourceFormat == null || targetFormat == null) {
            showAlert(Alert.AlertType.WARNING, "Missing Format Selection",
                    "Please select both source and target formats",
                    "You need to choose what format to convert from and to.");
            return false;
        }

        String outputDirectory = directoryField.getText();
        if (outputDirectory.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Output Directory",
                    "Please select an output directory",
                    "You need to choose where to save the converted files.");
            return false;
        }

        return true;
    }

    /**
     * Processes file conversions asynchronously.
     */
    private void processConversions() {
        CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate conversion process
                Thread.sleep(2000); // Replace with actual conversion logic
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }).thenAcceptAsync(success -> {
            Platform.runLater(() -> {
                resetSaveButton();
                if (success) {
                    playCompletionAnimation();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Conversion Complete",
                            "Files have been successfully converted and saved.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Conversion Failed",
                            "An error occurred during file conversion.");
                }
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                resetSaveButton();
                showAlert(Alert.AlertType.ERROR, "Error", "Conversion Failed",
                        "An unexpected error occurred: " + throwable.getMessage());
            });
            return null;
        });
    }


    /**
     * Plays a subtle completion animation to indicate successful file conversion.
     */
    private void playCompletionAnimation() {
        // Create a gentle pulse animation
        Timeline pulseAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(saveBtn.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(saveBtn.scaleYProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(saveBtn.opacityProperty(), 1.0, Interpolator.EASE_BOTH)
                ),
                new KeyFrame(Duration.millis(200),
                        new KeyValue(saveBtn.scaleXProperty(), 1.05, Interpolator.EASE_BOTH),
                        new KeyValue(saveBtn.scaleYProperty(), 1.05, Interpolator.EASE_BOTH),
                        new KeyValue(saveBtn.opacityProperty(), 0.9, Interpolator.EASE_BOTH)
                ),
                new KeyFrame(Duration.millis(400),
                        new KeyValue(saveBtn.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(saveBtn.scaleYProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(saveBtn.opacityProperty(), 1.0, Interpolator.EASE_BOTH)
                )
        );

        // Temporarily change button text and add a checkmark icon
        String originalText = saveBtn.getText();
        FontIcon checkIcon = new FontIcon("mdi2c-check-circle");
        checkIcon.setIconSize(16);
        checkIcon.setIconColor(Paint.valueOf(darkMode ? "#4caf50" : "#2e7d32"));

        saveBtn.setText("Complete!");
        saveBtn.setGraphic(checkIcon);

        // Add temporary success styling
        saveBtn.getStyleClass().add("completion-success");

        pulseAnimation.setOnFinished(e -> {
            // Reset after animation with a delay
            Timeline resetDelay = new Timeline(new KeyFrame(Duration.millis(1500), ev -> {
                saveBtn.setText(originalText);
                saveBtn.setGraphic(null);
                saveBtn.getStyleClass().remove("completion-success");
            }));
            resetDelay.play();
        });

        pulseAnimation.play();
    }



    /**
     * Displays an alert dialog with the specified parameters.
     */
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Animates the save button during conversion process.
     */
    private void playSaveBtnAnimation() {
        saveBtn.setDisable(true);
        saveBtn.setText("Converting...");
    }

    /**
     * Resets the save button to its original state.
     */
    private void resetSaveButton() {
        saveBtn.setText("Save File");
        saveBtn.setDisable(false);
    }

    // Theme Management

    /**
     * Toggles between dark and light themes.
     *
     * @param dark true for dark mode, false for light mode
     */
    public void toggleTheme(boolean dark) {
        darkMode = dark;

        // Update the root style class
        if (darkMode) {
            dropZone.getScene().getRoot().getStyleClass().add("dark-mode");
        } else {
            dropZone.getScene().getRoot().getStyleClass().remove("dark-mode");
        }

        // Update all icon colors
        updateThemeIcon();
        updateBackButtonIcon();
        updateSwapButtonIcon();
    }


    /**
     * Updates the theme toggle button icon based on current theme.
     */
    public void updateThemeIcon() {
        if (themeToggleBtn.getGraphic() instanceof FontIcon icon) {
            icon.setIconLiteral(darkMode ? "mdi2w-white-balance-sunny" : "mdi2m-moon-waning-crescent");
            icon.setIconColor(Paint.valueOf(darkMode ? "#ffd740" : "#888"));
        }
    }


    /**
     * Updates swap button (arrow) icon color based on current theme.
     */
    private void updateSwapButtonIcon() {
        if (arrowIcon != null) {
            arrowIcon.setIconColor(Paint.valueOf(darkMode ? "#ffd740" : "#3d3246"));
        }
    }


    /**
     * Sets the title text based on the conversion type.
     *
     * @param btnText the button text that triggered the conversion type
     */
    public void setButtonTextAsTitle(String btnText) {
        titleLabel.setText(btnText + " Conversion");
    }
}