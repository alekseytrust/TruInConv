package test.truinconv;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class ConversionController {
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

    // New TableView elements replacing the ListView
    @FXML private TableView<FileItem> fileTableView;
    @FXML private TableColumn<FileItem, String> fileNameColumn;
    @FXML private TableColumn<FileItem, Button> removeButtonColumn;

    private final List<File> selectedFiles = new ArrayList<>();
    private boolean darkMode = false;

    // Define possible conversions (add more as desired)
    private final Map<String, List<String>> CONVERSION_MAP = Map.of(
            "JPG", List.of("PNG"),
            "PNG", List.of("JPG"),
            "PDF", List.of("DOCX"),
            "DOCX", List.of("PDF"),
            "MP3", List.of("WAV"),
            "WAV", List.of("MP3")
    );

    // Class to represent file items in the TableView
    public static class FileItem {
        private final File file;
        private final SimpleStringProperty fileName;

        public FileItem(File file) {
            this.file = file;
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
    }

    @FXML
    private void initialize() {
        // Populate source formats
        sourceFormatComboBox.getItems().setAll(CONVERSION_MAP.keySet());
        sourceFormatComboBox.setOnAction(e -> updateTargetFormatOptions());
        targetFormatComboBox.setPromptText("Select target format");

        // Set up TableView columns
        fileNameColumn.setCellValueFactory(cellData -> cellData.getValue().fileNameProperty());
        fileNameColumn.prefWidthProperty().bind(fileTableView.widthProperty().multiply(0.85));

        // Set up the remove button column
        removeButtonColumn.setCellFactory(col -> {
            TableCell<FileItem, Button> cell = new TableCell<>() {
                private final Button removeButton = new Button();

                {
                    // Use an icon instead of text for a cleaner look
                    FontIcon icon = new FontIcon("mdi2c-close-circle");
                    icon.getStyleClass().add("remove-btn");
                    icon.setIconSize(16);
                    removeButton.setGraphic(icon);

                    removeButton.setOnAction(event -> {
                        FileItem fileItem = getTableView().getItems().get(getIndex());
                        removeFile(fileItem);
                    });
                    removeButton.getStyleClass().add("remove-btn");

                    // Center the button in the cell
                    setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                }

                @Override
                protected void updateItem(Button item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(removeButton);
                    }
                }
            };
            return cell;
        });

        // Prompt and theme handlings
        updateDropZonePrompt();

        // Initialize theme toggle button
        Platform.runLater(() -> {
            // Check if we're in dark mode already (from parent scene)
            Parent root = themeToggleBtn.getScene().getRoot();
            boolean isDarkMode = root.getStyleClass().contains("dark-mode");
            darkMode = isDarkMode;
            themeToggleBtn.setSelected(isDarkMode);
            updateThemeIcon();
        });

        themeToggleBtn.setOnAction(e -> toggleTheme(themeToggleBtn.isSelected()));
        browseDirBtn.setOnAction(e -> openDirectoryChooser());
        saveBtn.setOnAction(e -> onSaveClick());
        dropZone.setOnMouseClicked(event -> openFileChooser());
    }

    private void updateTargetFormatOptions() {
        String source = sourceFormatComboBox.getValue();
        targetFormatComboBox.getItems().clear();
        if (source != null && CONVERSION_MAP.containsKey(source)) {
            targetFormatComboBox.getItems().setAll(CONVERSION_MAP.get(source));
            // Only check validity if the current value is not null
            String currentTarget = targetFormatComboBox.getValue();
            if (currentTarget != null && !CONVERSION_MAP.get(source).contains(currentTarget)) {
                targetFormatComboBox.setValue(null);
            }
        }
        updateDropZonePrompt();
    }

    private void updateDropZonePrompt() {
        String source = sourceFormatComboBox.getValue();
        String target = targetFormatComboBox.getValue();
        String prompt = "Drop file here or click to select";
        if (source != null && target != null && !source.equals(target)) {
            prompt = "Drop file (" + source + " → " + target + ")";
        }
        dropZoneLabel.setText(prompt);
    }

    @FXML
    private void onDragOver(DragEvent event) {
        if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
        event.consume();
    }

    @FXML
    private void onDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            List<File> files = filterValidFiles(db.getFiles());
            addFilesToList(files);
            success = !files.isEmpty();
        }
        event.setDropCompleted(success);
        event.consume();
    }

    private List<File> filterValidFiles(List<File> files) {
        String ext = getSourceExtension();
        if (ext == null) return Collections.emptyList();
        // Accept files with the matching extension
        return files.stream()
                .filter(f -> f.isFile() && f.getName().toLowerCase().endsWith("." + ext.toLowerCase()))
                .collect(Collectors.toList());
    }

    private void addFilesToList(List<File> files) {
        for (File file : files) {
            if (!selectedFiles.contains(file)) {
                selectedFiles.add(file);
            }
        }
        updateFileTableView();
    }

    // New method to remove a file from the list
    private void removeFile(FileItem fileItem) {
        selectedFiles.remove(fileItem.getFile());
        updateFileTableView();
    }

    // Updated to work with TableView instead of ListView
    private void updateFileTableView() {
        Platform.runLater(() -> {
            fileTableView.getItems().clear();
            for (File file : selectedFiles) {
                fileTableView.getItems().add(new FileItem(file));
            }
        });
    }

    private void openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        String ext = getSourceExtension();
        if (ext != null) {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(ext + " files", "*." + ext.toLowerCase())
            );
        }
        List<File> files = fileChooser.showOpenMultipleDialog(dropZone.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            addFilesToList(filterValidFiles(files));
        }
    }

    private void openDirectoryChooser() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choose Output Directory");
        File selectedDir = directoryChooser.showDialog(dropZone.getScene().getWindow());
        if (selectedDir != null) {
            directoryField.setText(selectedDir.getAbsolutePath());
        }
    }

    @FXML
    private void onSaveClick() {
        if (selectedFiles.isEmpty()
                || sourceFormatComboBox.getValue() == null
                || targetFormatComboBox.getValue() == null
                || directoryField.getText().isBlank()) {
            new Alert(Alert.AlertType.ERROR, "Please select files, formats, and output directory.").showAndWait();
            return;
        }
        String source = sourceFormatComboBox.getValue();
        String target = targetFormatComboBox.getValue();
        if (!CONVERSION_MAP.containsKey(source) || !CONVERSION_MAP.get(source).contains(target)) {
            new Alert(Alert.AlertType.ERROR, "Unsupported conversion!").showAndWait();
            return;
        }

        for (File file : selectedFiles) {
            String outputPath = directoryField.getText() + File.separator +
                    getBaseName(file.getName()) + "." + target.toLowerCase();
            File output = new File(outputPath);
            try {
                convertFile(file, output, source, target);
            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Failed to convert " + file.getName() + ": " + ex.getMessage()).showAndWait();
            }
        }
        playSaveBtnAnimation();
    }

    private void convertFile(File input, File output, String sourceFormat, String targetFormat) throws Exception {
        // Image conversions (JPG <-> PNG)
        if ((sourceFormat.equals("JPG") && targetFormat.equals("PNG")) ||
                (sourceFormat.equals("PNG") && targetFormat.equals("JPG"))) {
            BufferedImage img = ImageIO.read(input);
            if (img == null) throw new IOException("Failed to read image");
            ImageIO.write(img, targetFormat.toLowerCase(), output);
        }
        // Audio conversions (MP3 <-> WAV)
        else if ((sourceFormat.equals("MP3") && targetFormat.equals("WAV")) ||
                (sourceFormat.equals("WAV") && targetFormat.equals("MP3"))) {
            convertAudio(input, output, targetFormat.toLowerCase());
        }
        // PDF <-> DOCX: [Stub implementations]
        else if (sourceFormat.equals("PDF") && targetFormat.equals("DOCX")) {
            convertPdfToDocx(input, output);
        }
        else if (sourceFormat.equals("DOCX") && targetFormat.equals("PDF")) {
            convertDocxToPdf(input, output);
        }
        else {
            throw new UnsupportedOperationException("This conversion is not implemented.");
        }
    }

    // --- File format helper methods ---

    private void convertAudio(File input, File output, String outFormat) throws EncoderException {
        // WAV <-> MP3 using JAVE2
        MultimediaObject source = new MultimediaObject(input);
        AudioAttributes audio = new AudioAttributes();
        if ("mp3".equals(outFormat)) {
            audio.setCodec("libmp3lame");
        } else if ("wav".equals(outFormat)) {
            audio.setCodec("pcm_s16le");
        } else {
            throw new IllegalArgumentException("Unsupported audio codec: " + outFormat);
        }
        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setAudioAttributes(audio);
        attrs.setOutputFormat(outFormat);
        Encoder encoder = new Encoder();
        encoder.encode(source, output, attrs);
    }

    // PDF -> DOCX (stub: just copies file, real conversion needs an advanced lib such as aspose/pdf2docx)
    private void convertPdfToDocx(File input, File output) throws IOException {
        // Place holder: just copy file and change extension. Replace with real logic as needed.
        try (InputStream in = new FileInputStream(input); OutputStream out = new FileOutputStream(output)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }
    }

    // DOCX -> PDF (stub: just copies file, real conversion needs an advanced lib such as docx4j or aspose)
    private void convertDocxToPdf(File input, File output) throws IOException {
        // Place holder: just copy file and change extension. Replace with real logic as needed.
        try (InputStream in = new FileInputStream(input); OutputStream out = new FileOutputStream(output)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }
    }

    private String getSourceExtension() {
        String ext = sourceFormatComboBox.getValue();
        return ext == null ? null : ext.toLowerCase();
    }

    private String getBaseName(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(0, idx) : fileName;
    }

    private void playSaveBtnAnimation() {
        saveBtn.setDisable(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> saveBtn.setDisable(false));
        pause.play();
    }

    public void toggleTheme(boolean dark) {
        this.darkMode = dark;

        // Apply the theme class to the root
        if (themeToggleBtn.getScene() != null) {
            Scene scene = themeToggleBtn.getScene();
            Parent root = scene.getRoot();
            if (dark) {
                if (!root.getStyleClass().contains("dark-mode")) {
                    root.getStyleClass().add("dark-mode");
                }
            } else {
                root.getStyleClass().removeAll("dark-mode");
            }
        }

        updateThemeIcon();
    }

    public void updateThemeIcon() {
        if (themeToggleBtn == null) return;

        FontIcon icon;
        if (darkMode) {
            icon = new FontIcon("mdi2w-white-balance-sunny");
        } else {
            icon = new FontIcon("mdi2m-moon-waning-crescent");
        }
        icon.setIconSize(26);
        icon.setIconColor(Paint.valueOf(darkMode ? "#ffd740" : "#21243d"));
        themeToggleBtn.setGraphic(icon);
    }

    public void setButtonTextAsTitle(String btnText) {
        titleLabel.setText(btnText);
    }

    @FXML
    private void onDragEntered(DragEvent event) {
        dropZone.setStyle("-fx-background-color: -drop-zone-active-bg; " +
                "-fx-border-color: -drop-zone-active-border; " +
                "-fx-border-style: solid; " +
                "-fx-background-radius: 12; " +
                "-fx-border-radius: 12;");
    }

    @FXML
    private void onDragExited(DragEvent event) {
        dropZone.setStyle("-fx-background-color: -drop-zone-bg; " +
                "-fx-border-color: -drop-zone-border; " +
                "-fx-border-style: dashed; " +
                "-fx-background-radius: 12; " +
                "-fx-border-radius: 12;");
    }
}