package com.coffrefort.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import com.coffrefort.client.ApiClient;
import com.coffrefort.client.model.ShareItem;
import javafx.stage.Stage;

public class MySharesController {

    @FXML private TableView<ShareItem> sharesTable;
    @FXML private TableColumn<ShareItem, String> resourceCol;
    @FXML private TableColumn<ShareItem, String> labelCol;
    @FXML private TableColumn<ShareItem, String> expiresCol;
    @FXML private TableColumn<ShareItem, String> remainingCol;
    @FXML private TableColumn<ShareItem, String> statusCol;
    @FXML private TableColumn<ShareItem, Void> actionCol; // ✅ Changé de Object à Void

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
            showError("Erreur", "Impossible de charger les partages: " + e.getMessage());
        }
    }

    /**
     * Gestion du bouton "Révoquer"
     */
    private void initActionColumn() {

        actionCol.setCellFactory(col -> new TableCell<ShareItem, Void>() {

            private final Button revokeBtn = new Button("Révoquer");

            {
                // Initialisation du bouton
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
                    setGraphic(new HBox(revokeBtn));
                }
            }
        });
    }

    /**
     * Révoquer un partage
     */
    private void revokeShare(ShareItem item) {
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Voulez-vous vraiment révoquer ce partage ?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirmation.setTitle("Confirmer la révocation");
        confirmation.setHeaderText("Révoquer le partage de: " + item.getResource());

        confirmation.showAndWait().ifPresent(button -> {
            if (button == ButtonType.YES) {
                try {
                    apiClient.revokeShare(item.getId());
                    loadShares(); // Recharger la liste
                    showInfo("Succès", "Le partage a été révoqué.");
                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Erreur", "Impossible de révoquer le partage: " + e.getMessage());
                }
            }
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Afficher une erreur
     */
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Afficher une information
     */
    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}