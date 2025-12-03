package com.coffrefort.client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class ConfirmDeleteFolderView {

    private final VBox root = new VBox(15);

    private final Label questionLabel = new Label("Voulez-vous vraiment supprimer ce dossier ?\nTous les fichiers et sous-dossiers de ce dossier seront supprimés.");
    private final Label dossierNameLabel = new Label("NomDuDossier.ext");
    private final Label warningLabel = new Label("Impossible d'annuler après validation.");

    private final Button cancelButton = new Button("Annuler");
    private final Button confirmButton = new Button("Supprimer");

    // callbacks
    private Runnable onCancel;
    private Runnable onConfirm;



    public ConfirmDeleteFolderView() {
        buildUi();
    }

    private void buildUi() {
        root.setPadding(new Insets(20, 25, 20, 25));
        root.setPrefSize(420, 220);
        root.setSpacing(15);
        root.setStyle("-fx-background-color: #E5E5E5; -fx-background-radius: 8;");

        // ===== En-tête =====
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefWidth(48);
        iconBox.setStyle("-fx-background-color: #980b0b; -fx-background-radius: 8;");
        iconBox.setPadding(new Insets(10));

        Text icon = new Text("🗑️");
        icon.setStyle("-fx-font-size: 24px; -fx-fill: white;");
        iconBox.getChildren().add(icon);

        VBox titleBox = new VBox(4);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Text title = new Text("Confirmer la suppression du dossier");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-fill: #980b0b;");

        Text subtitle = new Text("Cette action est définitive.");
        subtitle.setStyle("-fx-font-size: 12px; -fx-fill: #666666;");

        titleBox.getChildren().addAll(title, subtitle);

        header.getChildren().addAll(iconBox, titleBox);


        // ===== Message =====
        VBox messageBox = new VBox(8);
        messageBox.setAlignment(Pos.CENTER);

        questionLabel.setWrapText(true);
        questionLabel.setAlignment(Pos.CENTER);
        questionLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        questionLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 13px;");

        dossierNameLabel.setWrapText(true);
        dossierNameLabel.setAlignment(Pos.CENTER);
        dossierNameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        dossierNameLabel.setStyle("-fx-text-fill: #980b0b; -fx-font-weight: bold; -fx-font-size: 13px;");

        warningLabel.setWrapText(true);
        warningLabel.setAlignment(Pos.CENTER);
        warningLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        warningLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 13px;");

        messageBox.getChildren().addAll(questionLabel, dossierNameLabel, warningLabel);

        // Spacer pour pousser les boutons en bas
        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);


        // ===== Boutons (centrés) =====
        cancelButton.setStyle(
                "-fx-background-color: #cccccc; -fx-text-fill: #333333; " +
                        "-fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8 20;"
        );
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(e -> triggerCancel());

        confirmButton.setStyle(
                "-fx-background-color: #d9534f; -fx-text-fill: white; " +
                        "-fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8 24; " +
                        "-fx-font-weight: bold;"
        );
        confirmButton.setDefaultButton(true);
        confirmButton.setOnAction(e -> triggerConfirm());

        HBox actions = new HBox(12, cancelButton, confirmButton);
        actions.setAlignment(Pos.CENTER);

        // ===== Construction finale =====
        root.getChildren().addAll(header, messageBox, spacer, actions);
    }


    private void triggerCancel() {
        if (onCancel != null) onCancel.run();
    }

    private void triggerConfirm() {
        if (onConfirm != null) onConfirm.run();
    }

    // ===== API publique =====

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public void setOnConfirm(Runnable onConfirm) {
        this.onConfirm = onConfirm;
    }

    /** Permet d'injecter le nom du dossier sélectionné */
    public void setFolderName(String folderName) {
        dossierNameLabel.setText(folderName != null ? folderName : "");
    }

    public Node getRoot() {
        return root;
    }
}


