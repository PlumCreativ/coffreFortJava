package com.coffrefort.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;

import com.coffrefort.client.ApiClient;
import com.coffrefort.client.model.ShareItem;
import com.coffrefort.client.util.UIDialogs;

import javafx.stage.Stage;

public class MySharesController {

    @FXML private TableView<ShareItem> sharesTable;
    @FXML private TableColumn<ShareItem, String> resourceCol;
    @FXML private TableColumn<ShareItem, String> labelCol;
    @FXML private TableColumn<ShareItem, String> expiresCol;
    @FXML private TableColumn<ShareItem, String> remainingCol;
    @FXML private TableColumn<ShareItem, String> statusCol;
    @FXML private TableColumn<ShareItem, Void> actionCol; // Changé de Object à Void

    private ApiClient apiClient;
    private Stage stage;

    @FXML
    private void initialize() {
        resourceCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getResource()));

        labelCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getLabel()));

        expiresCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getExpiresAt()));

        remainingCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getRemainingUses() == null
                                ? "∞"
                                : data.getValue().getRemainingUses().toString()));

        statusCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().isRevoked() ? "Révoqué" : "Actif"
                ));
    }

    public void setApiClient(ApiClient apiClient) {
        System.out.println("MySharesController - setApiClient() appelée");
        this.apiClient = apiClient;
        initActionColumn();
        loadShares();
    }

    /**
     * Charge les partages
     */
    private void loadShares() {
        System.out.println("MySharesController - loadShares() démarrage...");

        try {
            var shares = apiClient.listShares();

            System.out.println("MySharesController - Nombre de partages reçus: " + shares.size());

            // Debug: afficher chaque partage
            for (int i = 0; i < shares.size(); i++) {
                ShareItem share = shares.get(i);
                System.out.println("Partage " + i + ": " +
                        "id=" + share.getId() +
                        ", resource=" + share.getResource() +
                        ", label=" + share.getLabel() +
                        ", expires=" + share.getExpiresAt() +
                        ", remaining=" + share.getRemainingUses() +
                        ", revoked=" + share.isRevoked());
            }

            sharesTable.getItems().setAll(shares);
            System.out.println("MySharesController - Données ajoutées à la table");

        } catch (Exception e) {
            System.err.println("MySharesController - ERREUR lors du chargement: " + e.getMessage());

            e.printStackTrace();
            UIDialogs.showError("Erreur", "Impossible de charger les partages: " + e.getMessage());
        }
    }

    /**
     * Gestion du bouton "Révoquer"
     */
    private void initActionColumn() {

        actionCol.setCellFactory(col -> new TableCell<ShareItem, Void>() {

            private final Button copyBtn = new Button("Copier");
            private final Button revokeBtn = new Button("Révoquer");
            private final HBox box = new HBox(6,  copyBtn, revokeBtn);

            {
                copyBtn.setStyle("-fx-background-color: #b00909; -fx-text-fill: white; -fx-cursor: hand;");
                copyBtn.setOnAction(event -> {
                    ShareItem item = getTableView().getItems().get(getIndex());
                    String url = item.getUrl();
                    if(url == null || url.isBlank()) return;

                    javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                    content.putString(url);
                    javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Information");
                    alert.setHeaderText(null);
                    alert.setContentText("Lien copié dans le presse-papier !");

                    //Remplacer l'icône par un "i" bordeaux
                    Label icon = new Label("i");
                    icon.setStyle(
                            "-fx-background-color: #980b0b;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-alignment: center;" +
                            "-fx-min-width: 28px;" +
                            "-fx-min-height: 28px;" +
                            "-fx-background-radius: 11px;" +
                            "-fx-font-size: 14px;"
                    );
                    alert.setGraphic(icon);

                    DialogPane pane = alert.getDialogPane();

                    // Bouton OK
                    Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
                    if (okBtn != null) {
                        okBtn.setStyle(
                                "-fx-background-color: #980b0b;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-cursor: hand;"
                        );
                    }
                    alert.showAndWait();
                });

                // Initialisation du bouton révoquer
                revokeBtn.setStyle("-fx-background-color: #980b0b; -fx-text-fill: white; -fx-cursor: hand;");
                revokeBtn.setOnAction(event -> {
                    ShareItem item = getTableView().getItems().get(getIndex());
                    revokeShare(item);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    ShareItem share = getTableView().getItems().get(getIndex());
                    revokeBtn.setDisable(share.isRevoked());
                    setGraphic(box);
                }
            }
        });
    }

    /**
     * Révoquer un partage
     */
    private void revokeShare(ShareItem item) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmer la revocation");
        confirmation.setHeaderText("Revoquer le partage de: " + item.getResource());
        confirmation.setContentText("\"Voulez-vous vraiment revoquer ce partage ?\"");

        //Icône bordeaux
        Label icon = new Label("!");
        icon.setStyle(
                "-fx-background-color: #980b0b;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-alignment: center;" +
                "-fx-min-width: 28px;" +
                "-fx-min-height: 28px;" +
                "-fx-background-radius: 11px;" +
                "-fx-font-size: 14px;"
        );
        confirmation.setGraphic(icon);

        DialogPane pane = confirmation.getDialogPane();
        pane.setStyle("-fx-background-color: #E5E5E5;");

        //les boutons annuler/révoquer
        ButtonType revoqueType = new ButtonType("Révoquer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(revoqueType, cancelType);

        Button revokeBtn = (Button) pane.lookupButton(revoqueType);
        if(revokeBtn != null) {
            revokeBtn.setStyle(
                    "-fx-background-color: #980b0b;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-weight: bold;" +
                    "-fx-cursor: hand;"
            );
        }

        Button cancelBtn = (Button) pane.lookupButton(cancelType);
        if(cancelBtn != null) {
            cancelBtn.setStyle(
                    "-fx-background-color: #cccccc;" +
                    "-fx-text-fill: #333333;" +
                    "-fx-cursor: hand;"
            );
        }

        confirmation.showAndWait().ifPresent(button -> {
            if (button == revoqueType) {
                try {
                    apiClient.revokeShare(item.getId());
                    loadShares(); // Recharger la liste
                    UIDialogs.showInfo("Succès", "Le partage a ete revoque.");
                } catch (Exception e) {
                    e.printStackTrace();
                    UIDialogs.showError("Erreur", "Impossible de revoquer le partage: " + e.getMessage());
                }
            }
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

//    /**
//     * Afficher une erreur
//     */
//    private void showError(String title, String content) {
//        Alert alert = new Alert(Alert.AlertType.ERROR);
//        alert.setTitle(title);
//        alert.setHeaderText(null);
//        alert.setContentText(content);
//
//        //Remplacer l'icône par un "i" bordeaux
//        Label icon = new Label("!");
//        icon.setStyle(
//                "-fx-background-color: #980b0b;" +
//                "-fx-text-fill: white;" +
//                "-fx-font-weight: bold;" +
//                "-fx-alignment: center;" +
//                "-fx-min-width: 28px;" +
//                "-fx-min-height: 28px;" +
//                "-fx-background-radius: 11px;" +
//                "-fx-font-size: 14px;"
//        );
//        alert.setGraphic(icon);
//
//        // Style bouton OK
//        DialogPane pane = alert.getDialogPane();
//        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
//        if (okBtn != null) {
//            okBtn.setStyle(
//                    "-fx-background-color: #980b0b;" +
//                    "-fx-text-fill: white;" +
//                    "-fx-font-weight: bold;" +
//                    "-fx-cursor: hand;"
//            );
//        }
//        alert.showAndWait();
//    }
//
//    /**
//     * Afficher une information
//     */
//    private void showInfo(String title, String content) {
//        Alert alert = new Alert(Alert.AlertType.INFORMATION);
//        alert.setTitle(title);
//        alert.setHeaderText(null);
//        alert.setContentText(content);
//
//        //Remplacer l'icône par un "i" bordeaux
//        Label icon = new Label("i");
//        icon.setStyle(
//                "-fx-background-color: #980b0b;" +
//                "-fx-text-fill: white;" +
//                "-fx-font-weight: bold;" +
//                "-fx-alignment: center;" +
//                "-fx-min-width: 28px;" +
//                "-fx-min-height: 28px;" +
//                "-fx-background-radius: 11px;" +
//                "-fx-font-size: 14px;"
//        );
//        alert.setGraphic(icon);
//
//        // Style bouton OK
//        DialogPane pane = alert.getDialogPane();
//        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
//        if (okBtn != null) {
//            okBtn.setStyle(
//                    "-fx-background-color: #980b0b;" +
//                    "-fx-text-fill: white;" +
//                    "-fx-font-weight: bold;" +
//                    "-fx-cursor: hand;"
//            );
//        }
//        alert.showAndWait();
//    }
}

