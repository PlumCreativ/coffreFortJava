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

    @FXML private TextField expiresField;
    @FXML private TextField maxUsesField;

    private Stage stage;

    // Callback appelé si l'utilisateur valide (destinataire)
    private Consumer<String> onShare;
    private Runnable onCancel;

    @FXML
    /**
     * Initialise l’UI (masque l’erreur) et déclenche le partage si l’utilisateur appuie sur Entrée dans le champ destinataire
     */
    private void initialize() {
        hideError();

        //appuyer sur Entrée dans le champ lance le partage ??
        recipientField.setOnAction(e -> handleShare());
    }


    /**
     * Injecte le Stage de la fenêtre modale pour pouvoir la fermer depuis le contrôleur
     * @param stage
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }


    /**
     * Affiche le nom de l’élément (fichier/dossier) à partager dans l’interface
     * @param name
     */
    public void setItemName(String name) {
        itemNameLabel.setText(name != null ? name : "");
    }


    /**
     * Définit le callback appelé lors de la validation du partage avec les paramètres saisis
     * @param onShare
     */
    public void setOnShare(Consumer<String> onShare) {
        this.onShare = onShare;
    }


    @FXML
    /**
     * Valide les champs (destinataire, maxUses, expiration), puis appelle le callback et ferme la fenêtre
     */
    private void handleShare() {
        String recipient = (recipientField.getText() == null) ? "" : recipientField.getText().trim();

        if (recipient.isEmpty()) {
            showError("Veuillez renseigner un destinataire.");
            return;
        }

        hideError();

        int maxUses = 2; // => valeur par défaut
        try {
            String maxUsesText = maxUsesField.getText();
            if(maxUsesText != null && !maxUsesText.isBlank()) {
                maxUses = Integer.parseInt(maxUsesText.trim());
            }
        } catch (NumberFormatException e) {
            showError("Max uses invalide");
            return;
        }

        int expiresDays = 7; // => valeur par défaut

        try {
            String expiresText = expiresField.getText();
            if(expiresText != null && !expiresText.isBlank()){
                expiresDays = Integer.parseInt(expiresText.trim());
            }
        }catch(NumberFormatException e){
            showError("Expiration invalide");
            return;
        }

        // Appel du callback avec destinataire, masUses, expiresDays
        if (onShare != null) {
            onShare.accept(recipient + "|" + maxUses + "|" + expiresDays);
        }

        // Ferme le dialogue après validation
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * Affiche un message d’erreur dans l’UI en rendant le label visible
     * @param msg
     */
    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    /**
     * Efface et masque le message d’erreur dans l’UI
     */
    private void hideError() {
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    @FXML
    /**
     * Gère le clic sur “Annuler” en exécutant le callback éventuel puis en fermant la fenêtre
     */
    private void handleCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
        close();
    }

    /**
     * Ferme la fenêtre de partage (Stage injecté sinon récupération via le bouton en fallback)
     */
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
