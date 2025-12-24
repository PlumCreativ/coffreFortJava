package com.coffrefort.client.controllers;

import com.coffrefort.client.config.AppProperties;
import com.coffrefort.client.util.JwtUtils;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import com.coffrefort.client.ApiClient;
import javafx.fxml.FXML;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.coffrefort.client.util.JsonUtils;


public class RegisterController {

    //propriétés
    @FXML private GridPane rootPane;
    @FXML private TextField emailField1;
    @FXML private PasswordField passwordField1;
    @FXML private TextField passwordVisibleField1;
    @FXML private PasswordField confirmPasswordField1;
    @FXML private TextField confirmPasswordVisibleField1;

    @FXML private CheckBox loginSelectShowPassword;

    @FXML private Label emailError1;
    @FXML private Label passwordError1;
    @FXML private Label confirmPasswordError1;
    @FXML private Label errorLabel1;
    @FXML private Label successLabel1;

    @FXML private Button registerButton1;
    @FXML private Label statusLabel1;

    private ApiClient apiClient;

    private Runnable onRegisterSuccess;
    private Runnable onGoToLogin;


    //méthodes
    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setOnRegisterSuccess(Runnable onRegisterSuccess) {
        this.onRegisterSuccess = onRegisterSuccess;
    }

    public void setOnGoToLogin(Runnable onGoToLogin) {
        this.onGoToLogin = onGoToLogin;
    }


    @FXML //=> il faut qu'il soit lier avec le .fxml!!
    private void initialize(){

        //on clique sur la scèe => retirer le focus du champs
        rootPane.setOnMouseClicked(event -> {
            if(event.getTarget() != emailField1 && event.getTarget() != passwordField1 && event.getTarget() != confirmPasswordField1 ){
                rootPane.requestFocus();
            }
        });



        // Binder le texte des 2 champs mot de passe => avoir le même texte
        passwordVisibleField1.textProperty().bindBidirectional(passwordField1.textProperty());
        confirmPasswordVisibleField1.textProperty().bindBidirectional(confirmPasswordField1.textProperty());
    }

    /**
     * Afficher ou masquer le mot de passe
     */
    @FXML
    private void handleToggleShowPassword(){
        if(loginSelectShowPassword.isSelected()){

            //afficher le mot de passe et le confirmation de mot de passe
            passwordVisibleField1.setVisible(true);
            passwordVisibleField1.setManaged(true);
            confirmPasswordVisibleField1.setVisible(true);
            confirmPasswordVisibleField1.setManaged(true);

            //cacher le password et le mot de passe
            passwordField1.setVisible(false);
            passwordField1.setManaged(false);
            confirmPasswordField1.setVisible(false);
            confirmPasswordField1.setManaged(false);

            //garder le curseur à la fin
            passwordVisibleField1.requestFocus();
            passwordVisibleField1.positionCaret(passwordVisibleField1.getText().length());


        }else{
            //cacher le mot de passe et confirmation de mot de passe
            passwordField1.setVisible(true);
            passwordField1.setManaged(true);
            confirmPasswordField1.setVisible(true);
            confirmPasswordField1.setManaged(true);

            //cacher les champs de texte
            passwordVisibleField1.setVisible(false);
            passwordVisibleField1.setManaged(false);
            confirmPasswordVisibleField1.setVisible(false);
            confirmPasswordVisibleField1.setManaged(false);

            //garder le curseur à la fin
            passwordField1.requestFocus();
            passwordField1.positionCaret(passwordField1.getText().length());
        }
    }

    /**
     * Gestion de l'Inscription et connexion: l'un après l'autre
     */
    @FXML
    public void handleRegister(){

        System.out.println("handleRegister() appelé !");
        clearMessages();

        // Nettoyer le message d'erreur
        if (errorLabel1 != null) {
            errorLabel1.setText("");
        }
        if (statusLabel1 != null){
            statusLabel1.setText("");
        }

        String email = emailField1.getText() != null ? emailField1.getText().trim() : "";
        String password = passwordField1.getText() != null ? passwordField1.getText().trim() : "";
        String confirmPassword = confirmPasswordField1.getText() != null ? confirmPasswordField1.getText().trim() : "";

        boolean hasError = false;

        if(loginSelectShowPassword.isSelected()){
            showLabel(errorLabel1, "Veuillez masquer le mot de passe avant de vous inscrire.");
            return;
        }

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
            showLabel(passwordError1, "Le password est trop court (min. 8 caractères).");
            hasError = true;
        }

        //Validation la confirmation de password
        if(confirmPassword.isEmpty()){
            showLabel(confirmPasswordError1, "Veuillez confirmer le mot de passe.");
            hasError = true;
        }else if(!confirmPassword.equals(password)){
            showLabel(confirmPasswordError1, "Les mots de passe ne correspondent pas.");
            hasError = true;
        }

        if(hasError){
            return;
        }

        // Désactiver le bouton pendant l'inscription
        if(registerButton1 != null) {
            registerButton1.setDisable(true);
        }

        if(statusLabel1 != null) { //=> ???????????
            statusLabel1.setText("Inscription en cours...");
        }

        //int quotaTotal = 31457280; // 30 Mo par défaut pour tests
        int quotaTotal = 1073741824; //=> 1 giga
        //Boolean isAdmin = false;     //=> pas admin => backend qui décide!!!


        //Task pour appel HTTP en arrière-plan
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {

                // Appel à ApiClient.register()
                return apiClient.register(email, password, quotaTotal);

            }
        };

        //après la requête terminée => succès ou erreur HTTP
        task.setOnSucceeded(event -> {
            if(registerButton1 != null) {
                registerButton1.setDisable(false);
            }

            String token = task.getValue(); //échec => null
            String apiMessage = task.getMessage();  // message ou null
            if(token != null){

                //Inscription et login => ok
                if(statusLabel1 != null) {
                    statusLabel1.setText("Inscription réussie, connexion automatique réussie.");
                }

                showSuccess("Bienvenue !  Vous êtes connecté(e).");

                if(onRegisterSuccess != null) {
                    onRegisterSuccess.run(); //=> changer de scène/ fenêtre
                }
            }else{

                // échec côté API (inscription ou login)
                if(statusLabel1 != null) {
                    statusLabel1.setText("");
                }

                String message = (apiMessage != null && !apiMessage.isEmpty())
                        ? apiMessage
                        : "Erreur lors de l'inscription/connexion.";

                if(message.toLowerCase().contains("email")){
                    showLabel(emailError1, message);
                }else if(message.toLowerCase().contains("password")){
                    showLabel(passwordError1, message);
                }else{
                    showLabel(errorLabel1, message);
                }
            }
        });

        task.setOnFailed(event -> {
            if(registerButton1 != null) {
                registerButton1.setDisable(false);
            }
            if(statusLabel1 != null) {
                statusLabel1.setText("");
            }
            if(errorLabel1 != null) {
                showLabel(errorLabel1, "Erreur pendant l'inscription.");

            }

            Throwable ex = task.getException();
            if (ex != null) {
                ex.printStackTrace();
            }
        });


        //lancer le Task en arrière-plan
        new Thread(task).start();

    }


    /**
     * Gestion du lien de "se connecter"
     */
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
        showLabel(successLabel1, message);
    }


//    public static void main(String[] args) {
//        launch(args);
//    }
//
//    @Override
//    public void start(Stage primaryStage) {
//
//    }
}
