package com.coffrefort.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ConfirmDeleteController {

    //propriétés
    @FXML private Label fileNameLabel;

    @FXML private Button cancelButton;
    @FXML private Button confirmButton;

    private Stage dialogStage;

    // callbacks
    private Runnable onCancel;
    private Runnable onConfirm;

    //méthodes
    @FXML
    private void initialize() {
        // rien d'obligatoire ici, mais tu peux ajouter du log si besoin
    }

    /** Injecté par la vue (ou MainController) pour fermer la fenêtre */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /** Permet d'afficher le nom du fichier sélectionné */
    public void setFileName(String fileName) {
        if (fileNameLabel != null) {
            fileNameLabel.setText(fileName != null ? fileName : "");
        }
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public void setOnConfirm(Runnable onConfirm) {
        this.onConfirm = onConfirm;
    }

    @FXML
    private void handleCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
        close();
    }

    @FXML
    private void handleConfirm() {
        if (onConfirm != null) {
            onConfirm.run();
        }
        close();
    }

    private void close() {
        if (dialogStage != null) {
            dialogStage.close();
        } else {
            // fallback si jamais le stage n’est pas injecté
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        }
    }
}
