package com.coffrefort.client.controllers;

import com.coffrefort.client.ApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button connexionButton;
    @FXML private Label statusLabel;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

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


    @FXML
    public void handleLogin() {

        // Nettoyer le message d'erreur
        if (errorLabel != null) {
            errorLabel.setText("");
        }
        if (statusLabel != null){
            statusLabel.setText("");
        }

        String email = emailField != null && emailField.getText() != null ? emailField.getText().trim() : "";
        String password = passwordField != null && passwordField.getText() != null ? passwordField.getText().trim() : "";

        // Validation simple
        if (email.isEmpty() || password.isEmpty()) {
            if (errorLabel != null) {
                errorLabel.setText("Veuillez saisir l'email et le mot de passe.");
            }
            return;
        }

        // Désactive le bouton pendant la connexion
        if(connexionButton != null) {
            connexionButton.setDisable(true);
        }

        if(statusLabel != null) {
            statusLabel.setText("Connexion...");
        }

        // utilisation d'une Task pour exécuter l'appel HTTP hors du thread JavaFX
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                String url = "http://localhost:8080/auth/login";

                String jsonBody = String.format(
                        "{\"email\":\"%s\",\"password\":\"%s\"}",
                        email, password
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Accept", "application/json")
                        .header ("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

                int status = response.statusCode();
                String responseBody = response.body();
                System.out.println("Status: " + status);
                System.out.println("Response: " + responseBody);

                if ( status != 200) {
                    return false;
                }
                return true;
            }

        };

        //après la requête terminée => succès ou erreur HTTP
        task.setOnSucceeded(event -> {
            boolean ok = task.getValue();

            if(connexionButton != null) {
                connexionButton.setDisable(false);
            }

            if(ok){
                if(statusLabel != null) {
                    statusLabel.setText("Connexion réussie.");
                }

                if(onSuccess != null) {
                    onSuccess.run(); //=> changer de scène/ fenêtre
                }
            }else{
                if(statusLabel != null) {
                    statusLabel.setText("");
                }
                if(errorLabel != null) {
                    errorLabel.setText("Identifiants invalides.");
                }
            }
        });

        task.setOnFailed(event -> {
            if(connexionButton != null) {
                connexionButton.setDisable(false);
            }
            if(statusLabel != null) {
                statusLabel.setText("");
            }
            if(errorLabel != null) {
                errorLabel.setText("Erreur de connexion au serveur.");
            }

            Throwable ex = task.getException();
            if (ex != null) {
                ex.printStackTrace();
            }
        });

        //lancer le Task dans un thread séparé
        new Thread(task).start();

    }

    // Handler du lien "S'inscrire"
    @FXML
    public void handleGoToRegister() {
        if (onGoToRegister != null) {
            onGoToRegister.run();   // App.java ouvrira register.fxml
        }
    }
}
