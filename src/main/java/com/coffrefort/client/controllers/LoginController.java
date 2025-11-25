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
    private Runnable onSuccess;

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    public void handleLogin() {
        if (errorLabel != null) errorLabel.setText("");
        String email = emailField != null ? emailField.getText() : "";
        String pwd = passwordField != null ? passwordField.getText() : "";
        boolean ok = apiClient != null && apiClient.login(email, pwd);
        if (ok) {
            if (onSuccess != null) onSuccess.run();
        } else {
            if (errorLabel != null) errorLabel.setText("Identifiants invalides (simulation)");
        }
    }
}
