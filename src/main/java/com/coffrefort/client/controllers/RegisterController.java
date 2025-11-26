package com.coffrefort.client.controllers;

import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import com.coffrefort.client.ApiClient;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;


public class RegisterController extends Application {
    @FXML private TextField emailField1;
    @FXML private PasswordField passwordField1;
    @FXML private PasswordField confirmPasswordField1;

    @FXML private Label emailError1;
    @FXML private Label passwordError1;
    @FXML private Label confirmPasswordError1;
    @FXML private Label errorLabel1;
    @FXML private Label successLabel1;

    @FXML private Button registerButton1;

    private ApiClient apiClient;

    private Runnable onRegisterSuccess;
    private Runnable onGoToLogin;

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setOnRegisterSuccess(Runnable onRegisterSuccess) {
        this.onRegisterSuccess = onRegisterSuccess;
    }

    public void setOnGoToLogin(Runnable onGoToLogin) {
        this.onGoToLogin = onGoToLogin;
    }

    //Handler bouton pour s'inscrire
    @FXML
    public void handleRegister(){

        //Nettoyer les Messages
        clearMessages();

        String email = emailField1.getText() != null ? emailField1.getText().trim() : "";
        String password = passwordField1.getText() != null ? passwordField1.getText().trim() : "";
        String confirmPassword = confirmPasswordField1.getText() != null ? confirmPasswordField1.getText().trim() : "";

        boolean hasError = false;

        //Validation email
        if(email.isEmpty()){
            showLabel(emailError1, "L'email est obligatoire.");
            hasError = true;
        }else if(!email.contains("@") || !email.contains(".")){
            showLabel(emailError1, "L'email invalide.");
            hasError = true;
        }

        //Validation password
        if(password.isEmpty()){
            showLabel(passwordError1, "Le password est obligatoire.");
            hasError = true;
        }else if(password.length() < 8){
            showLabel(passwordError1, "Le password est trop court.");
            hasError = true;
        }

        //Validation la confirmation de password
        if(confirmPassword.isEmpty()){
            showLabel(confirmPasswordError1, "Veuillez confirmer le mot de passe.");
            hasError = true;
        }else if(confirmPassword.equals(password)){
            showLabel(confirmPasswordError1, "Les mots de passe ne correspondent pas.");
            hasError = true;
        }

        if(hasError){
            return;
        }

        // TODO: brancher ici l'appel réel à l'API quand tu auras l'endpoint d'inscription
        try {
            // Exemple de pseudo-code si tu ajoutes plus tard une méthode dans ApiClient :
            // boolean ok = apiClient.register(email, password);
            // if (!ok) { showLabel(errorLabel1, "Inscription impossible."); return; }

            // Pour l’instant : on simule le succès
            showSuccess("Inscription réussie, vous pouvez vous connecter.");

            if (onRegisterSuccess != null) {
                onRegisterSuccess.run(); // App.java ouvrira le login
            }
        } catch (Exception e) {
            e.printStackTrace();
            showLabel(errorLabel1, "Erreur lors de l'inscription. Veuillez réessayer.");
        }

    }

    //Handler de lien de se connecter
    @FXML
    public void handleGoToLogin(){
        openLogin();
    }

    //méthode intern => pour revenir à l'écran Login
    public void openLogin(){
        if(onGoToLogin != null){
            onGoToLogin.run();
        }
    }



    //Méthodes utilitaires

    public void clearMessages(){
        hideLabel(emailError1);
        hideLabel(passwordError1);
        hideLabel(confirmPasswordError1);
        hideLabel(errorLabel1);
        hideLabel(successLabel1);
    }

    public void hideLabel(Label label){
        if(label == null) return;
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    public void showLabel(Label label, String message){
        if(label == null) return;
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    public void showSuccess(String message){
        successLabel1.setText(message);
    }



    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

    }
}
