package com.coffrefort.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ConfirmLogoutController {

    //propriétés
    @FXML private Button cancelButton;
    @FXML private Button confirmButton;
    @FXML private Label infoLabel;
    @FXML private Label infoLabel1;

    private Stage dialogStage;
    private Runnable onLogoutConfirmed;

   //Méthodes
    @FXML
    private void initialize() {
        if (infoLabel != null && (infoLabel.getText() == null || infoLabel.getText().isBlank())) {
            infoLabel.setText("Voulez-vous vraiment vous déconnecter ?");
        }

        if (infoLabel1 != null && (infoLabel1.getText() == null || infoLabel1.getText().isBlank())) {
            infoLabel1.setText("Toutes les opérations en cours seront interrompues.");
        }
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setOnLogoutConfirmed(Runnable onLogoutConfirmed) {
        this.onLogoutConfirmed = onLogoutConfirmed;
    }

    @FXML
    private void handleCancel() {
        System.out.println("Déconnexion annulée par l'utilisateur.");
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Gestion de la confirmation de déconnection
     */
    @FXML
    private void handleConfirm() {
        try {
            System.out.println("Confirmation de la déconnexion...");

            // Exécuter la logique de déconnexion
            if (onLogoutConfirmed != null) {
                onLogoutConfirmed.run();
            } else {
                System.err.println("ATTENTION: onLogoutConfirmed est null !");
                // Fermer quand même la fenêtre
                if (dialogStage != null) {
                    dialogStage.close();
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la confirmation de déconnexion");
            e.printStackTrace();

            // Fermer la fenêtre même en cas d'erreur
            if (dialogStage != null) {
                dialogStage.close();
            }
        }
    }


}
