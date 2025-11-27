package com.coffrefort.client.controllers;

import com.coffrefort.client.ApiClient;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UploadDialogController {

    @FXML private Button selectFileButton;
    @FXML private Button cancelButton;
    @FXML private Button uploadButton;
    @FXML private VBox fileListContainer;
    @FXML private VBox selectedFilesList;
    @FXML private Label noFileLabel;
    @FXML private TextField customNameField;
    @FXML private VBox progressContainer;
    @FXML private Label progressLabel;
    @FXML private Label progressPercentLabel;
    @FXML private ProgressBar uploadProgressBar;
    @FXML private Label messageLabel;


    private final List<File> selectedFiles = new ArrayList<>();

    private ApiClient apiClient;
    private Stage dialogStage;

    // Callback optionnel après succès (pour rafraîchir la liste des fichiers, etc.)
    private Runnable onUploadSuccess;


    //méthodes
    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setOnUploadSuccess(Runnable onUploadSuccess) {
        this.onUploadSuccess = onUploadSuccess;
    }


    @FXML
    private void initialize() {
        // Au début, pas de progression
        progressContainer.setVisible(false);
        progressContainer.setManaged(false);

        messageLabel.setVisible(false);
        messageLabel.setManaged(false);

        uploadButton.setDisable(true); // pas de fichier sélectionné

        refreshSelectedFilesUI();
    }


    @FXML
    private void handleSelectFile() {
        Window owner = getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier à uploader");

        // une autre possibilité pour choisir des fichiers
        // fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));

        List<File> files = fileChooser.showOpenMultipleDialog(owner);
        if (files != null && !files.isEmpty()) {
            selectedFiles.clear();
            selectedFiles.addAll(files);
            refreshSelectedFilesUI();
            uploadButton.setDisable(false);
            hideMessage();
        }
    }

    @FXML
    private void handleUpload() {
        if (selectedFiles.isEmpty()) {
            showErrorMessage("Veuillez sélectionner au moins un fichier.");
            return;
        }

        if (apiClient == null) {
            showErrorMessage("Erreur interne : ApiClient non initialisé.");
            return;
        }

        // Préparation UI
        selectFileButton.setDisable(true);
        uploadButton.setDisable(true);
        cancelButton.setDisable(true);

        progressContainer.setVisible(true);
        progressContainer.setManaged(true);
        uploadProgressBar.setProgress(0);
        progressLabel.setText("Upload en cours...");
        progressPercentLabel.setText("0%");
        hideMessage();

        // Task pour ne pas bloquer le thread JavaFX
        Task<Void> uploadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {

                //apiClient.uploadFile(fileToUpload);
                int total = selectedFiles.size();
                int done = 0;

                updateProgress(0, total);
                updateMessage("Upload 0/" + total);

                // boucle upload fichiers
                for (File file : selectedFiles) {
                    if (isCancelled()) break;

                    updateMessage("Upload de " + file.getName() + " (" + (done + 1) + "/" + total + ")");

                    // upload réel
                    apiClient.uploadFile(file);

                    done++;
                    updateProgress(done, total);
                }
                return null;
            }
        };

        // Binding de la progression
        uploadProgressBar.progressProperty().unbind();
        uploadProgressBar.progressProperty().bind(uploadTask.progressProperty());

        //Bind progress message => text
        uploadTask.messageProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                progressLabel.setText(newVal);
            }
        });

        // % label stable
        uploadTask.progressProperty().addListener((obs, oldVal, newVal) -> {
            double p = (newVal == null ? 0 : newVal.doubleValue());

            if (p < 0) { // indeterminate
                progressPercentLabel.setText("...");
                return;
            }

            int percent = (int) Math.round(p * 100);
            progressPercentLabel.setText(percent + "%");
        });

        uploadTask.setOnSucceeded(event -> {
            uploadProgressBar.progressProperty().unbind();
            uploadProgressBar.setProgress(1.0);

            selectFileButton.setDisable(false);
            uploadButton.setDisable(false);
            cancelButton.setDisable(false);

            progressLabel.setText("Upload terminé ✔");
            progressPercentLabel.setText("100%");
            showSuccessMessage("Fichier(s) uploadé(s) avec succès.");

            if (onUploadSuccess != null) {
                onUploadSuccess.run();
            }

            // fermer la fenêtre après succès
            closeDialog();
        });

        uploadTask.setOnFailed(event -> {
            uploadProgressBar.progressProperty().unbind();
            uploadProgressBar.setProgress(0);

            selectFileButton.setDisable(false);
            uploadButton.setDisable(false);
            cancelButton.setDisable(false);

            progressLabel.setText("Erreur lors de l'upload");
            showErrorMessage("Une erreur est survenue pendant l’upload.");

            Throwable ex = uploadTask.getException();
            if (ex != null) {
                ex.printStackTrace();
            }
        });

        new Thread(uploadTask, "upload-task").start();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }


    // ====== Méthodes utilitaires ======

    private void refreshSelectedFilesUI() {
        selectedFilesList.getChildren().clear();

        if (selectedFiles.isEmpty()) {
            noFileLabel.setVisible(true);
            noFileLabel.setManaged(true);
            selectedFilesList.getChildren().add(noFileLabel);
            uploadButton.setDisable(true);
            return;
        }

        noFileLabel.setVisible(false);
        noFileLabel.setManaged(false);

        for (File file : selectedFiles) {
            HBox row = new HBox(8);
            Label nameLabel = new Label(file.getName());
            nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #333333;");

            Label sizeLabel = new Label(formatSize(file.length()));
            sizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #777777;");

            Button removeBtn = new Button("✖");
            removeBtn.setStyle("-fx-font-size: 10px; -fx-background-color: transparent; -fx-text-fill: #980b0b;");
            removeBtn.setOnAction(e -> {
                selectedFiles.remove(file);
                refreshSelectedFilesUI();
            });

            row.getChildren().addAll(nameLabel, sizeLabel, removeBtn);
            selectedFilesList.getChildren().add(row);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " o";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f Ko", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f Mo", mb);
        double gb = mb / 1024.0;
        return String.format("%.2f Go", gb);
    }

    private void showErrorMessage(String text) {
        messageLabel.setText(text);
        messageLabel.setStyle("-fx-text-fill: #b00020; -fx-background-color: #fdecea;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void showSuccessMessage(String text) {
        messageLabel.setText(text);
        messageLabel.setStyle("-fx-text-fill: #1b5e20; -fx-background-color: #e8f5e9;");
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
    }

    private void hideMessage() {
        messageLabel.setVisible(false);
        messageLabel.setManaged(false);
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        } else {
            // fallback si pas de stage injecté
            Window w = getWindow();
            if (w instanceof Stage) {
                ((Stage) w).close();
            }
        }
    }

    private Window getWindow() {
        if (dialogStage != null) {
            return dialogStage;
        }
        if (selectFileButton != null && selectFileButton.getScene() != null) {
            return selectFileButton.getScene().getWindow();
        }
        return null;
    }
}
