package com.coffrefort.client.controllers;

import com.coffrefort.client.ApiClient;
import com.coffrefort.client.model.UserQuota;
import com.coffrefort.client.util.FileUtils;
import com.coffrefort.client.util.UIDialogs;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class QuotaManagementController {

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button refreshButton;
    @FXML private Button modifyQuotaButton;
    @FXML private Button closeButton;
    @FXML private Label infoLabel;

    @FXML private TableView<UserQuota> usersTable;
    @FXML private TableColumn<UserQuota, Integer> idCol;
    //@FXML private TableColumn<UserQuota, String> usernameCol;
    @FXML private TableColumn<UserQuota, String> emailCol;
    @FXML private TableColumn<UserQuota, String> quotaUsedCol;
    @FXML private TableColumn<UserQuota, String> quotaMaxCol;
    @FXML private TableColumn<UserQuota, String> percentCol;
    @FXML private TableColumn<UserQuota, String> roleCol;

    private final ObservableList<UserQuota> userList = FXCollections.observableArrayList();

    private ApiClient apiClient;
    private Stage dialogStage;

    @FXML
    private void initialize() {

        //config des colonnes
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        quotaUsedCol.setCellValueFactory(new PropertyValueFactory<>("quotaUsed"));
        quotaMaxCol.setCellValueFactory(new PropertyValueFactory<>("quotaMax"));
        percentCol.setCellValueFactory(new PropertyValueFactory<>("percent"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

        centerColumn(idCol);
        centerColumn(quotaUsedCol);
        centerColumn(quotaMaxCol);
        centerColumn(percentCol);
        centerColumn(roleCol);

        usersTable.setItems(userList);

        modifyQuotaButton.setDisable(true);
        // activer le bouton "modifier" => si une ligne est séléctionné
        usersTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            modifyQuotaButton.setDisable(newValue == null);
        });

        //double clic pour modifier
        usersTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && usersTable.getSelectionModel().getSelectedItem() != null) {
                handleModifyQuota();
            }
        });
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void refreshNow(){
        usersTable.setItems(userList);
        handleRefresh();
    }

    private <T> void centerColumn(TableColumn<UserQuota, T> col) {
        col.setCellFactory(column -> new TableCell<>(){
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                }else{
                    setText(item.toString());
                }
                setAlignment(Pos.CENTER);
            }
        });
    }

    /**
     * charge la liste de user depuis l'API
     */
    @FXML
    private void handleRefresh(){
        if(apiClient == null){
            showError("ApiClient non initialisé");
            return;
        }

        refreshButton.setDisable(true);
        showInfo("Chargement en cours...");

        new Thread(()->{

            try{
                //appel api pour récuperer tous les users avec leurs quotas
                java.util.List<UserQuota> users = apiClient.getAllUsersWithQuota();

                Platform.runLater(()->{
                    userList.clear();
                    userList.addAll(users);

                    refreshButton.setDisable(false);
                    hideInfo();
                    usersTable.setItems(userList);
                    if(users.isEmpty()){
                        showInfo("Aucun utilisateur trouvé");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(()->{
                    refreshButton.setDisable(false);
                    hideInfo();
                    UIDialogs.showError("Erreur", null, "Impossible de charger les utilisateurs " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Recherche d'utilisateurs par email
     */
    @FXML
    private void handleSearch(){
        String query = searchField.getText();
        if(query == null || query.trim().isEmpty()){
            usersTable.setItems(userList);
            hideInfo();
            return;
        }

        String lowerQuery = query.toLowerCase();
        ObservableList<UserQuota> filtered = userList.filtered(user ->
                //user.getName().toLowerCase().contains(lowerQuery) ||
                user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerQuery)
        );

        usersTable.setItems(filtered);

        if(filtered.isEmpty()){
            showInfo("Aucun résultat pour : " + query);
        } else {
            showInfo(filtered.size() + "résultat(s) trouvé(s)");
        }
    }

    /**
     * ouvre le dialogue pour modfier le quota d'un user
     */
    @FXML
    private void handleModifyQuota(){
        UserQuota selected = usersTable.getSelectionModel().getSelectedItem();
        if(selected == null){
            return;
        }

        try{
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/coffrefort/client/modifyQuota.fxml")
            );

            Scene scene = new Scene(loader.load());

            // Récupération du contrôleur
            ModifyQuotaController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Modifier le quota");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(dialogStage);
            stage.setResizable(false);
            stage.setScene(scene);

            controller.setDialogStage(stage);
            controller.setApiClient(apiClient);
            controller.setUser(selected);

            controller.setOnSuccess(() -> {
                //rafraichir après modif
                refreshNow();
            });

            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            UIDialogs.showError("Erreur", null, "Impossible d'ouvrir le dialogue" + e.getMessage());
        }
    }

    @FXML
    private void handleClose(){
        if(dialogStage != null){
            dialogStage.close();
        }
    }

    private void showInfo(String message){
        infoLabel.setText(message);
        infoLabel.setVisible(true);
        infoLabel.setManaged(true);
    }

    private void hideInfo(){
        infoLabel.setVisible(false);
        infoLabel.setManaged(false);
    }

    private void showError(String message){
        UIDialogs.showError("Erreur", null, message);
    }





}
