package com.coffrefort.client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.util.function.BiConsumer;


public class LoginView {

    private final GridPane root = new GridPane();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Label errorLabel = new Label();

    private final Hyperlink registerLink = new Hyperlink("S'inscrire");
    private final Hyperlink legalLink = new Hyperlink("Mentions Légales");

    // callback (email, password) → logique métier
    private BiConsumer<String, String> onLogin;

    // callback quand l'utilisateur clique sur "S'inscrire"
    private Runnable onGoToRegister;

    // callback quand l'utilisateur clique sur "Mentions Légales"
    private Runnable onOpenLegal;


    public LoginView() {
        buildUi();
    }

    private void buildUi() {
        root.setPadding(new Insets(20));
        root.setHgap(10);
        root.setVgap(10);

        Text title = new Text("Connexion");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        //Place le titre sur 2 colonnes
        root.add(title, 0, 0, 2, 1);

        //email
        Label emailLabel = new Label("Email");
        emailField.setPromptText("Email");
        root.add(emailLabel, 0, 1);
        root.add(emailField, 1, 1);

        //mot de passe
        Label pwdLabel = new Label("Mot de passe");
        passwordField.setPromptText("Mot de passe");
        root.add(pwdLabel, 0, 2);
        root.add(passwordField, 1, 2);

        // Bouton connexion
        Button loginBtn = new Button("Se connecter");
        loginBtn.setDefaultButton(true);
        loginBtn.setOnAction(e -> triggerLogin());

        HBox actions = new HBox(10, loginBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        root.add(actions, 1, 3);

        //label d'erreur
        errorLabel.setStyle("-fx-text-fill: #b00020;");
        root.add(errorLabel, 1, 4);

        // Lien "Vous n'êtes pas inscrit(e) ? S'inscrire"
        Text question = new Text("Vous n'êtes pas inscrit(e) ?");
        question.setStyle("-fx-fill: #666666; -fx-font-size: 12px;");

        registerLink.setStyle("-fx-text-fill: #980b0b; -fx-cursor: hand;");
        registerLink.setOnAction(e -> triggerGoToRegister());

        HBox registerBox = new HBox(5, question, registerLink);
        registerBox.setAlignment(Pos.CENTER_LEFT);
        root.add(registerBox, 1, 5);

        // Lien "Mentions Légales"
        legalLink.setStyle("-fx-text-fill: #980b0b; -fx-cursor: hand;");
        legalLink.setOnAction(e -> triggerOpenLegal());

        HBox legalBox = new HBox(legalLink);
        legalBox.setAlignment(Pos.CENTER_LEFT);
        root.add(legalBox, 1, 6);

        root.setOnMouseClicked(event -> root.requestFocus());

    }

    private void triggerLogin() {
        if (onLogin != null) {
            showError("");
            onLogin.accept(emailField.getText(), passwordField.getText());
        }
    }

    private void triggerGoToRegister() {
        if (onGoToRegister != null) {
            onGoToRegister.run();
        }
    }

    private void triggerOpenLegal() {
        if (onOpenLegal != null) {
            onOpenLegal.run();
        }
    }


    public void setOnLogin(BiConsumer<String, String> onLogin) {
        this.onLogin = onLogin;
    }

    public void setOnGoToRegister(Runnable onGoToRegister) {
        this.onGoToRegister = onGoToRegister;
    }

    public void setOnOpenLegal(Runnable onOpenLegal) {
        this.onOpenLegal = onOpenLegal;
    }

    public void showError(String message) {
        errorLabel.setText(message == null ? "" : message);
    }

    public Node getRoot() {
        return root;
    }
}
