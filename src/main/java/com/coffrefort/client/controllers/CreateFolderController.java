package com.coffrefort.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class CreateFolderController {

    //propriétés
    @FXML private TextField folderNameField;
    @FXML private Label errorLabel;
    @FXML private Button cancelButton;
    @FXML private Button createButton;

    // Stage de la fenêtre modale (optionnel mais pratique)
    private Stage dialogStage;

    // Callback appelé quand l'utilisateur valide avec un nom correct
    private Consumer<String> onCreateFolder;

    // Callback quand l'utilisateur annule
    private Runnable onCancel;


    //méthodes
    @FXML
    private void initialize() {
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /** Sera appelé avec le nom du dossier quand l'utilisateur clique sur "Créer". */
    public void setOnCreateFolder(Consumer<String> onCreateFolder) {
        this.onCreateFolder = onCreateFolder;
    }

    /** Sera appelé quand l'utilisateur clique sur "Annuler". */
    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    /**
     * Gestion l'annulation de création d'un folder
     */
    @FXML
    private void handleCancel() {
        clearError();

        if (onCancel != null) {
            onCancel.run();
        }

        if (dialogStage != null) {
            dialogStage.close();
        }

    }


    /**
     * Gestion de la création d'un folder
     */
    @FXML
    private void handleCreate() {
        clearError();

        String name = folderNameField != null ? folderNameField.getText() : "";
        if (name == null) {
            name = "";
        }
        name = name.trim();

        if (name.isEmpty()) {
            showError("Le nom du dossier ne peut pas être vide.");
            return;
        }

        if (name.length() > 50) {
            showError("Le nom est trop long (maximum 50 caractères).");
            return;
        }

        // Validation du nom
        if (!isValidFolderName(name)) {
            showError("Le nom du dossier contient des caractères invalides (\\/:*?\"<>|).");
            return;
        }

        if (onCreateFolder != null) {
            onCreateFolder.accept(name);
        }

        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Valide le nom du dossier
     */
    private boolean isValidFolderName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        // Caractères interdits dans les noms de fichiers/dossiers
        String invalidChars = "[\\\\/:*?\"<>|]";

        if (name.matches(".*" + invalidChars + ".*")) {
            return false;
        }

        return true;
    }


    // --- Gestion des erreurs ---

    private void showError(String message) {
        if (errorLabel == null) return;

        errorLabel.setText(message == null ? "" : message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        if (errorLabel == null) return;

        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
