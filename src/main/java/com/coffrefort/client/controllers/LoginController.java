package com.coffrefort.client.controllers;

import com.coffrefort.client.ApiClient;
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

        //on clique sur la scèe => retirer le focus du champs
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

            //afficher le mot de passe
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);

            //cacher le password
            passwordField.setVisible(false);
            passwordField.setManaged(false);

            //garder le curseur à la fin
            passwordVisibleField.requestFocus();
            passwordVisibleField.positionCaret(passwordVisibleField.getText().length());
        }else{
            //cacher le mot de passe
            passwordField.setVisible(true);
            passwordField.setManaged(true);

            //cacher le champ de texte
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

        if(loginSelectShowPassword.isSelected()){
            errorLabel.setText("Veuillez masquer le mot de passe avant de vous inscrire.");
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
                try {
                    FXMLLoader mainLoader = new FXMLLoader(getClass().getResource("/com/coffrefort/client/main.fxml"));
                    Parent mainRoot = mainLoader.load();

                    MainController mainController = mainLoader.getController();
                    mainController.setApiClient(apiClient);

                    Stage stage = (Stage) emailField.getScene().getWindow();
                    stage.setScene(new Scene(mainRoot));
                    stage.setTitle("CryptoVault - Accueil");
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                errorLabel.setText("Email ou mot de passe incorrect.");
                statusLabel.setText("");
            }
        });

        //!!!!
        task.setOnFailed(event -> {
            connexionButton.setDisable(false);
            statusLabel.setText("");
            errorLabel.setText("Erreur de connexion au serveur.");
            task.getException().printStackTrace();
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
