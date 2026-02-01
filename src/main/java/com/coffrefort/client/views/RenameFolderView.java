package com.coffrefort.client.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.function.Consumer;

public class RenameFolderView {

    private final VBox root = new VBox();
    private final Label currentNameLabel = new Label("NomDuDossier");
    private final TextField nameField = new TextField();
    private final Label errorLabel = new Label("Erreur");
    private final Button cancelButton = new Button("Annuler");
    private final Button confirmButton = new Button("Renommer");

    private Consumer<String> onConfirm;
    private Runnable onCancel;

    public RenameFolderView() {
        buildUi();
    }

    private void buildUi() {
        root.setPrefSize(420, 240);
        root.setSpacing(15);
        root.setStyle("-fx-background-color: #E5E5E5; -fx-background-radius: 8;");
        root.setPadding(new Insets(20, 25, 20, 25));

        // ========== EN-TÊTE ==========
        HBox header = buildHeader();
        root.getChildren().add(header);

        // Séparateur
        Separator separator = new Separator();
        VBox.setMargin(separator, new Insets(5, 0, 10, 0));
        root.getChildren().add(separator);

        // ========== ZONE DE MESSAGE + INPUT ==========
        VBox messageBox = buildMessageBox();
        root.getChildren().add(messageBox);

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        root.getChildren().add(spacer);

        // ========== BOUTONS D'ACTION ==========
        HBox actionsBox = buildActionsBox();
        root.getChildren().add(actionsBox);
    }

    private HBox buildHeader() {
        HBox header = new HBox(12);

        // Bande rouge avec icône
        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefWidth(48);
        iconBox.setStyle("-fx-background-color: #980b0b; -fx-background-radius: 8;");
        iconBox.setPadding(new Insets(10));

        Text icon = new Text("✏️");
        icon.setFill(Color.WHITE);
        icon.setStyle("-fx-font-size: 24px;");
        iconBox.getChildren().add(icon);

        // Titre + sous-titre
        VBox titleBox = new VBox(4);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Text title = new Text("Renommer le dossier");
        title.setFill(Color.web("#980b0b"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");

        Text subtitle = new Text("Saisissez un nouveau nom.");
        subtitle.setFill(Color.web("#666666"));
        subtitle.setStyle("-fx-font-size: 12px;");

        titleBox.getChildren().addAll(title, subtitle);

        header.getChildren().addAll(iconBox, titleBox);
        return header;
    }

    private VBox buildMessageBox() {
        VBox messageBox = new VBox(10);

        // Label "Nom actuel :"
        Label currentLabel = new Label("Nom actuel :");
        currentLabel.setAlignment(Pos.CENTER);
        currentLabel.setPrefWidth(370);
        currentLabel.setWrapText(true);
        currentLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        currentLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 13px;");

        // Nom actuel du dossier
        currentNameLabel.setAlignment(Pos.CENTER);
        currentNameLabel.setPrefWidth(370);
        currentNameLabel.setWrapText(true);
        currentNameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        currentNameLabel.setStyle("-fx-text-fill: #980b0b; -fx-font-weight: bold; -fx-font-size: 13px;");

        // Champ de saisie
        nameField.setPromptText("Nouveau nom du dossier");
        nameField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #cccccc; -fx-padding: 8 10;");
        nameField.setOnAction(e -> triggerConfirm());

        // Label d'erreur (caché par défaut)
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setAlignment(Pos.CENTER);
        errorLabel.setPrefWidth(370);
        errorLabel.setWrapText(true);
        errorLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        errorLabel.setStyle("-fx-background-color: #ffe5e5; -fx-text-fill: #980b0b; -fx-font-weight: bold; -fx-padding: 8; -fx-background-radius: 6;");

        messageBox.getChildren().addAll(currentLabel, currentNameLabel, nameField, errorLabel);
        return messageBox;
    }

    private HBox buildActionsBox() {
        HBox actionsBox = new HBox(12);
        actionsBox.setAlignment(Pos.CENTER);

        // Bouton Annuler
        cancelButton.setCancelButton(true);
        cancelButton.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #333333; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8 20;");
        cancelButton.setFont(Font.font(12));
        cancelButton.setOnAction(e -> triggerCancel());

        // Bouton Renommer
        confirmButton.setDefaultButton(true);
        confirmButton.setStyle("-fx-background-color: #980b0b; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8 24; -fx-font-weight: bold;");
        confirmButton.setFont(Font.font(12));
        confirmButton.setOnAction(e -> triggerConfirm());

        DropShadow shadow = new DropShadow(10.0, Color.rgb(153, 11, 11, 0.35));
        confirmButton.setEffect(shadow);

        actionsBox.getChildren().addAll(cancelButton, confirmButton);
        return actionsBox;
    }

    // ========== TRIGGERS ==========

    private void triggerConfirm() {
        String newName = nameField.getText();
        if (newName == null || newName.trim().isEmpty()) {
            showError("Le nom ne peut pas être vide.");
            return;
        }
        hideError();
        if (onConfirm != null) {
            onConfirm.accept(newName.trim());
        }
    }

    private void triggerCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    // ========== SETTERS CALLBACKS ==========

    public void setOnConfirm(Consumer<String> onConfirm) {
        this.onConfirm = onConfirm;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    // ========== MÉTHODES PUBLIQUES ==========

    public void setCurrentName(String name) {
        currentNameLabel.setText(name != null ? name : "");
        nameField.setText(name); // Pré-remplir le champ
    }

    public void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    public void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    public Node getRoot() {
        return root;
    }
}