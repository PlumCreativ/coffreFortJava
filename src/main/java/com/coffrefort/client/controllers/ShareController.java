package com.coffrefort.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class ShareController {

    @FXML private Label itemNameLabel;
    @FXML private TextField recipientField;
    @FXML private Label errorLabel;
    @FXML private Button cancelButton;

    private Stage stage;

    // Callback appelé si l'utilisateur valide (destinataire)
    private Consumer<String> onShare;
    private Runnable onCancel;

    @FXML
    private void initialize() {
        hideError();

        // Optionnel : appuyer sur Entrée dans le champ lance le partage ??
        recipientField.setOnAction(e -> handleShare());
    }

    /** Permet au code qui ouvre la fenêtre de fournir le Stage pour pouvoir la fermer */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /** Affiche le nom de l'élément à partager */
    public void setItemName(String name) {
        itemNameLabel.setText(name != null ? name : "");
    }

    /** Définit l'action à exécuter quand l'utilisateur clique sur "Partager" */
    public void setOnShare(Consumer<String> onShare) {
        this.onShare = onShare;
    }

    @FXML
    private void handleShare() {
        String recipient = (recipientField.getText() == null) ? "" : recipientField.getText().trim();

        if (recipient.isEmpty()) {
            showError("Veuillez renseigner un destinataire.");
            return;
        }

        hideError();

        // Appel du callback si défini
        if (onShare != null) {
            onShare.accept(recipient);
        }

        // Ferme le dialogue après validation
        if (stage != null) {
            stage.close();
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    @FXML
    private void handleCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
        close();
    }

    private void close() {
        if (stage != null) {
            stage.close();
        } else {
            // fallback si jamais le stage n’est pas injecté
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        }
    }

}
