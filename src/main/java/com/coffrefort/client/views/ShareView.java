package com.coffrefort.client.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.function.Consumer;

public class ShareView {

    private final VBox root = new VBox();
    private final Label itemNameLabel = new Label("NomDeLElement");
    private final TextField recipientField = new TextField();
    private final TextField expiresField = new TextField();
    private final TextField maxUsesField = new TextField();
    private final CheckBox allowVersionsCheckBox = new CheckBox("Autoriser le téléchargement de versions spécifiques");
    private final Label errorLabel = new Label();
    private final Button cancelButton = new Button("Annuler");
    private final Button shareButton = new Button("Partager");

    private Consumer<ShareData> onShare;
    private Runnable onCancel;

    public ShareView() {
        buildUi();
    }

    private void buildUi() {
        root.setPrefSize(460, 350);
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

        // ========== ZONE DE MESSAGE ==========
        VBox messageBox = buildMessageBox();
        root.getChildren().add(messageBox);

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        root.getChildren().add(spacer);

        // ========== BOUTONS D'ACTION ==========
        HBox actionsBox = buildActionsBox();
        VBox.setMargin(actionsBox, new Insets(0, 0, 15, 0));
        root.getChildren().add(actionsBox);
    }

    private HBox buildHeader() {
        HBox header = new HBox(12);

        // Bande rouge avec icône
        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(49, 41);
        iconBox.setStyle("-fx-background-color: #980b0b; -fx-background-radius: 8;");
        iconBox.setPadding(new Insets(10));

        Text icon = new Text("🔗");
        icon.setFill(Color.WHITE);
        icon.setStyle("-fx-font-size: 24px;");
        icon.setWrappingWidth(24.0);
        iconBox.getChildren().add(icon);

        // Titre + sous-titre
        VBox titleBox = new VBox(4);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPrefSize(348, 55);

        Text title = new Text("Partager");
        title.setFill(Color.web("#980b0b"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");
        title.setWrappingWidth(139.97);

        Text subtitle = new Text("Choisissez un destinataire");
        subtitle.setFill(Color.web("#666666"));
        subtitle.setStyle("-fx-font-size: 12px;");

        titleBox.getChildren().addAll(title, subtitle);

        header.getChildren().addAll(iconBox, titleBox);
        return header;
    }

    private VBox buildMessageBox() {
        VBox messageBox = new VBox(10);

        // Label d'introduction
        Label introLabel = new Label("Vous êtes sur le point de partager l'élément suivant :");
        introLabel.setAlignment(Pos.CENTER);
        introLabel.setPrefWidth(410);
        introLabel.setWrapText(true);
        introLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        introLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 13px;");

        // Nom de l'élément
        itemNameLabel.setAlignment(Pos.CENTER);
        itemNameLabel.setPrefWidth(410);
        itemNameLabel.setWrapText(true);
        itemNameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        itemNameLabel.setStyle("-fx-text-fill: #980b0b; -fx-font-weight: bold; -fx-font-size: 13px;");

        // Champ destinataire
        VBox recipientBox = new VBox(6);
        Label recipientLabel = new Label("Destinataire (email ou nom d'utilisateur)");
        recipientLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 12px; -fx-font-weight: bold;");
        recipientField.setPromptText("ex: alice@domaine.com");
        recipientField.setStyle("-fx-background-radius: 6; -fx-padding: 8 10; -fx-border-radius: 6; -fx-border-color: #C8C8C8;");
        recipientBox.getChildren().addAll(recipientLabel, recipientField);

        // Champs Expiration et Max Uses
        HBox optionsBox = buildOptionsBox();

        // Label d'erreur
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        errorLabel.setStyle("-fx-text-fill: #980b0b; -fx-font-size: 12px; -fx-font-weight: bold;");

        messageBox.getChildren().addAll(introLabel, itemNameLabel, recipientBox, optionsBox, errorLabel);
        return messageBox;
    }

    private HBox buildOptionsBox() {
        HBox optionsBox = new HBox(12);

        // Expiration
        VBox expiresBox = new VBox(6);
        HBox.setHgrow(expiresBox, javafx.scene.layout.Priority.ALWAYS);
        Label expiresLabel = new Label("Expiration (jours)");
        expiresLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 12px; -fx-font-weight: bold;");
        expiresField.setPromptText("ex: 7 (vide = jamais)");
        expiresField.setStyle("-fx-background-radius: 6; -fx-padding: 8 10; -fx-border-radius: 6; -fx-border-color: #C8C8C8;");
        expiresBox.getChildren().addAll(expiresLabel, expiresField);

        // Max Uses
        VBox maxUsesBox = new VBox(6);
        HBox.setHgrow(maxUsesBox, javafx.scene.layout.Priority.ALWAYS);
        Label maxUsesLabel = new Label("Max téléchargements");
        maxUsesLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 12px; -fx-font-weight: bold;");
        maxUsesField.setPromptText("ex: 3 (vide = illimité)");
        maxUsesField.setStyle("-fx-background-radius: 6; -fx-padding: 8 10; -fx-border-radius: 6; -fx-border-color: #C8C8C8;");
        maxUsesBox.getChildren().addAll(maxUsesLabel, maxUsesField);

        // Options avancées
        VBox advancedBox = buildAdvancedOptionsBox();

        optionsBox.getChildren().addAll(expiresBox, maxUsesBox, advancedBox);
        return optionsBox;
    }

    private VBox buildAdvancedOptionsBox() {
        VBox advancedBox = new VBox(6);

        Label optionsLabel = new Label("Options de partage");
        optionsLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 12px; -fx-font-weight: bold;");

        allowVersionsCheckBox.setStyle("-fx-text-fill: #333333; -fx-font-size: 12px;");

        Label helpLabel = new Label("Si activé, le destinataire pourra choisir une version précise du fichier.");
        helpLabel.setWrapText(true);
        helpLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 10px; -fx-padding: 0 0 0 25;");

        advancedBox.getChildren().addAll(optionsLabel, allowVersionsCheckBox, helpLabel);
        return advancedBox;
    }

    private HBox buildActionsBox() {
        HBox actionsBox = new HBox(12);
        actionsBox.setAlignment(Pos.CENTER);

        // Bouton Annuler
        cancelButton.setCancelButton(true);
        cancelButton.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #333333; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8 20;");
        cancelButton.setFont(Font.font(12));
        cancelButton.setOnAction(e -> triggerCancel());

        // Bouton Partager
        shareButton.setDefaultButton(true);
        shareButton.setStyle("-fx-background-color: #980b0b; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8 24; -fx-font-weight: bold;");
        shareButton.setFont(Font.font(12));
        shareButton.setOnAction(e -> triggerShare());

        DropShadow shadow = new DropShadow(10.0, Color.rgb(13, 64, 140, 0.35));
        shareButton.setEffect(shadow);

        actionsBox.getChildren().addAll(cancelButton, shareButton);
        return actionsBox;
    }

    // ========== TRIGGERS ==========

    private void triggerShare() {
        String recipient = recipientField.getText();
        if (recipient == null || recipient.trim().isEmpty()) {
            showError("Veuillez renseigner un destinataire.");
            return;
        }

        Integer maxUses = parseIntegerOrNull(maxUsesField.getText());
        if (maxUses != null && maxUses < 1) {
            showError("Max uses doit être >= 1 ou vide (illimité)");
            return;
        }

        Integer expiresDays = parseIntegerOrNull(expiresField.getText());
        if (expiresDays != null && expiresDays < 1) {
            showError("Expiration doit être >= 1 ou vide (jamais)");
            return;
        }

        boolean allowVersions = allowVersionsCheckBox.isSelected();

        hideError();

        if (onShare != null) {
            ShareData data = new ShareData(recipient.trim(), maxUses, expiresDays, allowVersions);
            onShare.accept(data);
        }
    }

    private void triggerCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    // ========== SETTERS CALLBACKS ==========

    public void setOnShare(Consumer<ShareData> onShare) {
        this.onShare = onShare;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    // ========== MÉTHODES PUBLIQUES ==========

    public void setItemName(String name) {
        itemNameLabel.setText(name != null ? name : "");
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

    // ========== UTILITAIRES ==========

    private Integer parseIntegerOrNull(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ========== CLASSE INTERNE POUR LES DONNÉES ==========

    public static class ShareData {
        private final String recipient;
        private final Integer maxUses;
        private final Integer expiresDays;
        private final boolean allowVersions;

        public ShareData(String recipient, Integer maxUses, Integer expiresDays, boolean allowVersions) {
            this.recipient = recipient;
            this.maxUses = maxUses;
            this.expiresDays = expiresDays;
            this.allowVersions = allowVersions;
        }

        public String getRecipient() {
            return recipient;
        }

        public Integer getMaxUses() {
            return maxUses;
        }

        public Integer getExpiresDays() {
            return expiresDays;
        }

        public boolean isAllowVersions() {
            return allowVersions;
        }

        @Override
        public String toString() {
            return recipient + "|"
                    + (maxUses != null ? maxUses : "null") + "|"
                    + (expiresDays != null ? expiresDays : "null") + "|"
                    + allowVersions;
        }
    }
}