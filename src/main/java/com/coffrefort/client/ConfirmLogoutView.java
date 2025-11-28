package com.coffrefort.client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class ConfirmLogoutView {

    private final VBox root = new VBox(15);
    private final Label infoLabel = new Label(
            "Voulez-vous vraiment vous déconnecter ?\nToutes les opérations en cours seront interrompues."
    );

    private final Button cancelButton = new Button("Annuler");
    private final Button confirmButton = new Button("Se déconnecter");

    // callback quand l’utilisateur clique sur "Annuler"
    private Runnable onCancel;

    // callback quand l’utilisateur confirme la déconnexion
    private Runnable onConfirm;

    public ConfirmLogoutView() {
        buildUi();
    }


    private void buildUi() {
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #E5E5E5; -fx-background-radius: 8;");

        // En-tête
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefWidth(48);
        iconBox.setStyle("-fx-background-color: #980b0b; -fx-background-radius: 8;");
        Text icon = new Text("🚪");
        icon.setStyle("-fx-font-size: 24px; -fx-fill: white;");
        iconBox.getChildren().add(icon);

        VBox titleBox = new VBox(4);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Text title = new Text("Confirmer la déconnexion");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-fill: #980b0b;");
        Text subtitle = new Text("Vous allez être déconnecté de CryptoVault.");
        subtitle.setStyle("-fx-font-size: 11px; -fx-fill: #666666;");
        titleBox.getChildren().addAll(title, subtitle);

        header.getChildren().addAll(iconBox, titleBox);

        // Message
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 12px;");

        // Boutons
        cancelButton.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #333333; "
                + "-fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8 20;");
        cancelButton.setOnAction(e -> triggerCancel());

        confirmButton.setStyle("-fx-background-color: #980b0b; -fx-text-fill: white; "
                + "-fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8 24; -fx-font-weight: bold;");
        confirmButton.setDefaultButton(true);
        confirmButton.setOnAction(e -> triggerConfirm());

        HBox actions = new HBox(12, cancelButton, confirmButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        // Construction finale
        root.getChildren().addAll(header, infoLabel, actions);
    }

    private void triggerCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    private void triggerConfirm() {
        if (onConfirm != null) {
            onConfirm.run();
        }
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public void setOnConfirm(Runnable onConfirm) {
        this.onConfirm = onConfirm;
    }

    public Node getRoot() {
        return root;
    }
}
