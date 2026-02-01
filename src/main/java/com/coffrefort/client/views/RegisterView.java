package com.coffrefort.client.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

//extends Application => ??????
public class RegisterView  {

    private final GridPane root = new GridPane();

    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmPasswordField = new PasswordField();

    private final Label emailErrorLabel = new Label();
    private final Label passwordErrorLabel = new Label();
    private final Label confirmErrorLabel = new Label();
    private final Label globalErrorLabel = new Label();
    private final Label successLabel = new Label();


    private final Button registerButton = new Button("S'inscrire");
    private final Hyperlink loginLink = new Hyperlink("Se connecter");
    private final Hyperlink legalLink = new Hyperlink("Mentions Légales");

    /** Callback (email, password, confirmPassword) */
    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    private TriConsumer<String, String, String> onRegister;
    private Runnable onGoToLogin;

    // callback quand l'utilisateur clique sur "Mentions Légales"
    private Runnable onOpenLegal;


    public RegisterView() {
        buildUi();
    }

    private void buildUi() {
        root.setPadding(new Insets(20));
        root.setHgap(10);
        root.setVgap(10);

        // Titre
        Text title = new Text("Inscription");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        root.add(title, 0, 0, 2, 1);

        // Sous-titre
        Text subtitle = new Text("Créez votre compte");
        subtitle.setStyle("-fx-font-size: 12px; -fx-fill: #666666;");
        root.add(subtitle, 0, 1, 2, 1);

        // Email
        Label emailLabel = new Label("Email");
        emailField.setPromptText("Email");
        root.add(emailLabel, 0, 2);
        root.add(emailField, 1, 2);

        emailErrorLabel.setStyle("-fx-text-fill: #b00020; -fx-font-size: 10px;");
        root.add(emailErrorLabel, 1, 3);

        // Mot de passe
        Label pwdLabel = new Label("Mot de passe");
        passwordField.setPromptText("Mot de passe (min. 8 caractères)");
        root.add(pwdLabel, 0, 4);
        root.add(passwordField, 1, 4);

        passwordErrorLabel.setStyle("-fx-text-fill: #b00020; -fx-font-size: 10px;");
        root.add(passwordErrorLabel, 1, 5);

        // Confirmation mot de passe
        Label confirmLabel = new Label("Confirmer le mot de passe");
        confirmPasswordField.setPromptText("Confirmer le mot de passe");
        root.add(confirmLabel, 0, 6);
        root.add(confirmPasswordField, 1, 6);

        confirmErrorLabel.setStyle("-fx-text-fill: #b00020; -fx-font-size: 10px;");
        root.add(confirmErrorLabel, 1, 7);

        // Bouton inscription
        registerButton.setDefaultButton(true);
        registerButton.setOnAction(e -> triggerRegister());

        HBox actions = new HBox(10, registerButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        root.add(actions, 1, 8);

        // Labels global erreur / succès
        globalErrorLabel.setStyle("-fx-text-fill: #b00020;");
        successLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

        root.add(globalErrorLabel, 1, 9);
        root.add(successLabel, 1, 10);

        // Lien "Se connecter"
        Text question = new Text("Déjà inscrit ?");
        question.setStyle("-fx-fill: #666666; -fx-font-size: 12px;");

        loginLink.setStyle("-fx-text-fill: #980b0b; -fx-cursor: hand;");
        loginLink.setOnAction(e -> triggerGoToLogin());

        HBox bottom = new HBox(5, question, loginLink);
        bottom.setAlignment(Pos.CENTER_LEFT);
        root.add(bottom, 1, 11);

        // Lien "Mentions Légales"
        legalLink.setStyle("-fx-text-fill: #980b0b; -fx-cursor: hand;");
        legalLink.setOnAction(e -> triggerOpenLegal());

        HBox legalBox = new HBox(legalLink);
        legalBox.setAlignment(Pos.CENTER_LEFT);
        root.add(legalBox, 1, 6);

        clearMessages();

        root.setFocusTraversable(true);

        root.setOnMouseClicked(event -> { //ca ne marche pas !!!
            Object target = event.getTarget();

            //ne pas défocuser si je clique sur un des TextField ou PasswordField
            if (!(target instanceof TextField) && !(target instanceof PasswordField)) {
                root.requestFocus();
            }

        });
    }

    private void triggerRegister() {
        clearMessages();

        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String password = passwordField.getText() != null ? passwordField.getText() : "";
        String confirm = confirmPasswordField.getText() != null ? confirmPasswordField.getText() : "";

        boolean hasError = false;

        // Validation email
        if (email.isEmpty()) {
            emailErrorLabel.setText("L'email est obligatoire.");
            hasError = true;
        } else if (!email.contains("@") || !email.contains(".")) {
            emailErrorLabel.setText("Email invalide.");
            hasError = true;
        }

        // Validation mot de passe
        if (password.isEmpty()) {
            passwordErrorLabel.setText("Le mot de passe est obligatoire.");
            hasError = true;
        } else if (password.length() < 8) {
            passwordErrorLabel.setText("Au moins 8 caractères.");
            hasError = true;
        }

        // Validation confirmation
        if (confirm.isEmpty()) {
            confirmErrorLabel.setText("Veuillez confirmer le mot de passe.");
            hasError = true;
        } else if (!confirm.equals(password)) {
            confirmErrorLabel.setText("Les mots de passe ne correspondent pas.");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        if (onRegister != null) {
            onRegister.accept(email, password, confirm);
        }
    }


    private void triggerGoToLogin() {
        if (onGoToLogin != null) {
            onGoToLogin.run();
        }
    }

    private void triggerOpenLegal() {
        if (onOpenLegal != null) {
            onOpenLegal.run();
        }
    }


    public void setOnRegister(TriConsumer<String, String, String> onRegister) {
        this.onRegister = onRegister;
    }

    public void setOnGoToLogin(Runnable onGoToLogin) {
        this.onGoToLogin = onGoToLogin;
    }

    public void setOnOpenLegal(Runnable onOpenLegal) {
        this.onOpenLegal = onOpenLegal;
    }

    public void showGlobalError(String message) {
        globalErrorLabel.setText(message == null ? "" : message);
    }

    public void showSuccess(String message) {
        successLabel.setText(message == null ? "" : message);
    }

    public void clearMessages() {
        emailErrorLabel.setText("");
        passwordErrorLabel.setText("");
        confirmErrorLabel.setText("");
        globalErrorLabel.setText("");
        successLabel.setText("");
    }

    public Node getRoot() {
        return root;
    }
}
