package com.coffrefort.client.controllers;

import com.coffrefort.client.ApiClient;
import com.coffrefort.client.App;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.coffrefort.client.util.JsonUtils;
import com.coffrefort.client.util.JwtUtils;
import com.coffrefort.client.config.AppProperties;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import javax.swing.*;

public class LoginController {

    //Propriétés
    @FXML private GridPane rootPane;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Label errorLabel;

    @FXML private CheckBox loginSelectShowPassword;

    @FXML private Button connexionButton;
    @FXML private Label statusLabel;


    private final HttpClient http = HttpClient.newHttpClient();

    private ApiClient apiClient;

    //Callback appelé quand le login est OK
    private Runnable onSuccess;

    //Callback appelé quand user clique sur "S'inscrire"
    private Runnable onGoToRegister;


    //méthodes


    @FXML //=> il faut qu'il soit lier avec le .fxml!!
    private void initialize(){

        //on clique sur la scène => retirer le focus du champs
        rootPane.setOnMouseClicked(event -> {
            if(event.getTarget() != emailField && event.getTarget() != passwordField && event.getTarget() != passwordVisibleField){
                rootPane.requestFocus();
            }
        });

        // Binder le texte des 2 champs mot de passe => avoir le même texte
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
    }


    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    //Setter pour recevoir le callback de App!!!
    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    public void setOnGoToRegister(Runnable onGoToRegister) {
        this.onGoToRegister = onGoToRegister;
    }

    /**
     * Afficher ou masquer le mot de passe
     */
    @FXML
    private void handleToggleShowPassword(){
        if(loginSelectShowPassword.isSelected()){

            //afficher le mdp en clair
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);

            //cacher le mdp masqué
            passwordField.setVisible(false);
            passwordField.setManaged(false);

            //garder le curseur à la fin
            passwordVisibleField.requestFocus();
            passwordVisibleField.positionCaret(passwordVisibleField.getText().length());
        }else{
            //afficher le mdp caché
            passwordField.setVisible(true);
            passwordField.setManaged(true);

            //cacher le mdp en clair
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);

            //garder le curseur à la fin
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    /**
     * Gestion de Connexion
     */
    @FXML
    public void handleLogin() {
        if (apiClient == null) {
            System.err.println("ApiClient n'a pas été injecté !");
            return;
        }

        errorLabel.setText("");
        statusLabel.setText("");

        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez saisir l'email et le mot de passe.");
            return;
        }

        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            errorLabel.setText("Format d'email invalide.");
            return;
        }

        if(loginSelectShowPassword.isSelected()){
            errorLabel.setText("Veuillez masquer le mot de passe avant de vous connecter.");
            return;
        }

        connexionButton.setDisable(true);
        statusLabel.setText("Connexion...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                try {
                    return apiClient.login(email, password);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        task.setOnSucceeded(event -> {
            connexionButton.setDisable(false);
            String token  = task.getValue();
            if (token  != null) {
                statusLabel.setText("Connexion réussie.");

                //il faut appeler callback onSuccess au lieu de charger manuellement main.fxml
                if (onSuccess != null){
                    onSuccess.run();   //Execute: App.openMainAndClose(stage)
                }

                //si je mets ca=> après la déconnexion la fenêtre s'affiche pas!!!!
//                try {
//                    FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/com/coffrefort/client/main.fxml"));
//                    Parent mainRoot = mainLoader.load();
//
//                    MainController mainController = mainLoader.getController();
//                    mainController.setApiClient(apiClient);
//                    mainController.setApp(app);
//
//                    Stage stage = (Stage) emailField.getScene().getWindow();
//                    stage.setScene(new Scene(mainRoot));
//                    stage.setTitle("CryptoVault - Accueil");
//                    stage.show();
//                } catch (IOException e) {
//                    errorLabel.setText("Erreur de chargement de l'application.");
//                    e.printStackTrace();
//                }
            } else {
                errorLabel.setText("Email ou mot de passe incorrect.");
                statusLabel.setText("");
            }
        });


        task.setOnFailed(event -> {
            connexionButton.setDisable(false);
            statusLabel.setText("");

            Throwable exception = task.getException();

            //erreur métier venant de l'API
            if (exception.getCause() instanceof ApiClient.AuthenticationException authEx) {
                errorLabel.setText(authEx.getMessage());
            }else if (exception.getCause() instanceof ApiClient.RegistrationException regEx) {
                errorLabel.setText(regEx.getMessage());
            }else if (exception.getCause() instanceof java.net.ConnectException) { //=> Problème réseau / serveur
                errorLabel.setText("Impossible de joindre le serveur.");
            }else {
                errorLabel.setText("Erreur technique inattendue.");
                exception.printStackTrace();
            }
        });

        new Thread(task).start();
    }


    /**
     * Gestion du lien "S'inscrire"
     */
    @FXML
    public void handleGoToRegister() {
        if (onGoToRegister != null) {
            onGoToRegister.run();   // App.java ouvrira register.fxml
        }
    }


}
