package com.coffrefort.client.controllers;

import com.coffrefort.client.ApiClient;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private ApiClient apiClient;

    //Callback appelé quand le login est OK
    private Runnable onSuccess;

    //Callback appelé quand user clique sur "S'inscrire"
    private Runnable onGoToRegister;


    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    public void setOnGoToRegister(Runnable onGoToRegister) {
        this.onGoToRegister = onGoToRegister;
    }


    //@FXML
//    public void handleLogin() {
//        if (errorLabel != null) errorLabel.setText("");
//        String email = emailField != null ? emailField.getText() : "";
//        String pwd = passwordField != null ? passwordField.getText() : "";
//        boolean ok = apiClient != null && apiClient.login(email, pwd);
//        if (ok) {
//            if (onSuccess != null) onSuccess.run();
//        } else {
//            if (errorLabel != null) errorLabel.setText("Identifiants invalides");
//        }
    //}

    @FXML
    public void handleLogin() {

        // Nettoyer le message d'erreur
        if (errorLabel != null) {
            errorLabel.setText("");
        }

        String email = emailField != null && emailField.getText() != null ? emailField.getText().trim() : "";
        String password = passwordField != null && passwordField.getText() != null ? passwordField.getText() : "";

        // Validation simple
        if (email.isEmpty() || password.isEmpty()) {
            if (errorLabel != null) {
                errorLabel.setText("Veuillez saisir l'email et le mot de passe.");
            }
            return;
        }

        // Appel à l'API
        boolean ok = false;
        if (apiClient != null) {
            try {
                ok = apiClient.login(email, password);
            } catch (Exception e) {
                e.printStackTrace();
                if (errorLabel != null) {
                    errorLabel.setText("Erreur de connexion au serveur.");
                }
                return;
            }
        }

        if (ok) {
            if (onSuccess != null) {
                onSuccess.run();
            }
        } else {
            if (errorLabel != null) {
                errorLabel.setText("Identifiants invalides");
            }
        }
    }

    // Handler du lien "S'inscrire"
    @FXML
    public void handleGoToRegister() {
        if (onGoToRegister != null) {
            onGoToRegister.run();   // App.java ouvrira register.fxml
        }
    }
}
