package test.truinconv;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Modal dialog displaying the progress of a file conversion task.
 * Binds a progress bar and status label to the task, and closes
 * automatically when the task completes, fails, or is cancelled.
 */
public final class ProgressDialog {
    // Width of the bound ProgressBar
    private static final double PROGRESS_BAR_WIDTH = 300.0;
    // Spacing between content elements in the dialog
    private static final double CONTENT_SPACING    = 10.0;
    // Padding around the dialog content
    private static final double CONTENT_PADDING    = 20.0;
    // Title shown on the dialog window
    private static final String DIALOG_TITLE      = "Converting Files...";
    // Initial message displayed until the task updates it
    private static final String INITIAL_MESSAGE   = "Preparing conversion...";

    private final Stage dialogStage;

    /**
     * Constructs a modal ProgressDialog bound to the given conversion task.
     * @param conversionTask the Task whose progress and message drive the dialog
     */
    public ProgressDialog(Task<?> conversionTask) {
        this.dialogStage = createDialogStage();
        Scene dialogScene = createDialogScene(conversionTask);
        configureStage(dialogScene);
        bindTaskEvents(conversionTask);
    }

    /**
     * Displays the dialog as an application-modal window.
     */
    public void show() {
        dialogStage.show();
    }

    /**
     * Initializes the Stage with a utility style and application modality.
     */
    private Stage createDialogStage() {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UTILITY);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        return stage;
    }

    /**
     * Builds the Scene containing the progress bar and status label.
     */
    private Scene createDialogScene(Task<?> conversionTask) {
        ProgressBar progressBar = createProgressBar(conversionTask);
        Label statusLabel       = createStatusLabel(conversionTask);
        VBox contentContainer   = createContentContainer(statusLabel, progressBar);
        return new Scene(contentContainer);
    }

    /**
     * Creates a ProgressBar bound to the task's progressProperty.
     */
    private ProgressBar createProgressBar(Task<?> conversionTask) {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(PROGRESS_BAR_WIDTH);
        progressBar.progressProperty().bind(conversionTask.progressProperty());
        return progressBar;
    }

    /**
     * Creates a Label bound to the task's messageProperty.
     */
    private Label createStatusLabel(Task<?> conversionTask) {
        Label statusLabel = new Label(INITIAL_MESSAGE);
        statusLabel.textProperty().bind(conversionTask.messageProperty());
        return statusLabel;
    }

    /**
     * Arranges the status label and progress bar in a vertically spaced VBox.
     */
    private VBox createContentContainer(Label statusLabel, ProgressBar progressBar) {
        VBox container = new VBox(CONTENT_SPACING);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(CONTENT_PADDING));
        container.getChildren().setAll(statusLabel, progressBar);
        return container;
    }

    /**
     * Sets the dialog's title and scene.
     */
    private void configureStage(Scene dialogScene) {
        dialogStage.setTitle(DIALOG_TITLE);
        dialogStage.setScene(dialogScene);
    }

    /**
     * Attaches listeners to close the dialog when the task ends.
     */
    private void bindTaskEvents(Task<?> conversionTask) {
        conversionTask.setOnSucceeded(event -> closeDialog());
        conversionTask.setOnFailed(event -> closeDialog());
        conversionTask.setOnCancelled(event -> closeDialog());
    }

    /**
     * Closes the progress dialog stage.
     */
    private void closeDialog() {
        dialogStage.close();
    }
}