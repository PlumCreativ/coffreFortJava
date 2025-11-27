package com.coffrefort.client.controllers;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import com.coffrefort.client.ApiClient;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class RegisterController extends Application {

    //propriétés
    @FXML private TextField emailField1;
    @FXML private PasswordField passwordField1;
    @FXML private PasswordField confirmPasswordField1;

    @FXML private Label emailError1;
    @FXML private Label passwordError1;
    @FXML private Label confirmPasswordError1;
    @FXML private Label errorLabel1;
    @FXML private Label successLabel1;

    @FXML private Button registerButton1;
    @FXML private Label statusLabel1;

    private final HttpClient http = HttpClient.newHttpClient();

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


    // Méthode pour extraire un champ texte d'un petit JSON du genre {"error":"..."}
    private String extractJsonField(String json, String fieldName) {
        if (json == null) return null;

        String pattern = "\"" + fieldName + "\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return null;

        int colon = json.indexOf(":", idx + pattern.length()); //=> chercher le ":"
        if (colon == -1) return null;

        int firstQuote = json.indexOf("\"", colon); //=> chercher la guillemet ouvrant
        if (firstQuote == -1) return null;

        int secondQuote = json.indexOf("\"", firstQuote + 1); //=> chercher la guillemet fermant
        if (secondQuote == -1) return null;

        return json.substring(firstQuote + 1, secondQuote);
    }

    //Handler bouton pour s'inscrire
    @FXML
    public void handleRegister(){

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

        int quotaTotal = 1073741824; //=> 1 giga
        Boolean isAdmin = false;     //=> pas admin


        //Task pour appel HTTP en arrière-plan
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {

                // auth/register
                String registerUrl = "http://localhost:8080/auth/register";

                String registerJson = String.format(
                        "{\"email\":\"%s\",\"password\":\"%s\",\"quota_total\":\"%d\",\"is_admin\":\"%b\"}",
                        email, password, quotaTotal, isAdmin
                );
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(registerUrl))
                        .header("Accept", "application/json")
                        .header ("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(registerJson))
                        .build();

                HttpResponse<String> registerResponse = http.send(request, HttpResponse.BodyHandlers.ofString());

                int regStatus = registerResponse.statusCode();
                String regBody = registerResponse.body();
                System.out.println("Register Status: " + regStatus);
                System.out.println("Register Response: " + regBody);

                // Pour une inscription, l'API peut renvoyer 200 ou 201 (Created)
                if(regStatus < 200 || regStatus >= 300) {

                    //Erreur d'inscritption
                    String apiError = extractJsonField(regBody, "error");
                    if(apiError == null || apiError.isEmpty()) {
                        apiError = "Inscription refusée par le serveur (code " + regStatus + ").";
                    }
                    updateMessage(apiError); //=> stockage dans le Task le message
                    return null; // => on tente pas le login
                }

                // /auth/login
                String loginUrl = "http://localhost:8080/auth/login";

                String LoginJson = String.format(
                        "{\"email\":\"%s\",\"password\":\"%s\"}",
                        email, password
                );

                HttpRequest loginRequest = HttpRequest.newBuilder()
                        .uri(URI.create(loginUrl))
                        .header("Accept", "application/json")
                        .header ("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(LoginJson))
                        .build();

                HttpResponse<String> loginResponse = http.send(loginRequest, HttpResponse.BodyHandlers.ofString());

                int logStatus = loginResponse.statusCode();
                String logBody = loginResponse.body();
                System.out.println("Login Status: " + logStatus);
                System.out.println("Login Response: " + logBody);

                if(logStatus != 200){
                    //Erreur d'inscritption
                    String apiError = extractJsonField(logBody, "error");
                    if(apiError == null || apiError.isEmpty()) {
                        apiError = "Connexion automatique échouée (code " + logStatus + ").";
                    }
                    updateMessage(apiError); //=> stockage dans le Task le message
                    return null;
                }

                //récupération de token
                String token =  extractJsonField(logBody, "jwt");
                if(token == null || token.isEmpty()) {
                    updateMessage("Connexion réussi mais aucun token renvoyé par le serveur.");
                    return null;
                }

                //stocker le token dans la session
                //Session.setToken (token);

                return token;

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
                errorLabel1.setText("Erreur pendant l'inscription.");
            }

            Throwable ex = task.getException();
            if (ex != null) {
                ex.printStackTrace();
            }
        });


        //lancer le Task en arrière-plan
        new Thread(task).start();

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
