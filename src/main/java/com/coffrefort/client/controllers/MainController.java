package com.coffrefort.client.controllers;

import com.coffrefort.client.ApiClient;
import com.coffrefort.client.model.FileEntry;
import com.coffrefort.client.model.NodeItem;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Optional;
import com.coffrefort.client.config.AppProperties;

public class MainController {

    //propriétés
    @FXML private TreeView<NodeItem> treeView;
    @FXML private TableView<FileEntry> table;
    @FXML private TableColumn<FileEntry, String> nameCol;
    @FXML private TableColumn<FileEntry, String> sizeCol;
    @FXML private TableColumn<FileEntry, String> dateCol;

    @FXML private ProgressBar quotaBar;
    @FXML private Label quotaLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label statusLabel;
    @FXML private Label fileCountLabel;

    @FXML private Button uploadButton;
    @FXML private Button shareButton;
    @FXML private Button deleteButton;
    @FXML private Button newFolderButton;
    @FXML private Button logoutButton;

    private ApiClient apiClient;
    private Runnable onLogout;
    private ObservableList<FileEntry> fileList = FXCollections.observableArrayList();
    private NodeItem currentFolder;

    //méthodes
    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setOnLogout(Runnable callback) {
        this.onLogout = callback;
    }

    public void setUserEmail(String email) {
        if (userEmailLabel != null) {
            userEmailLabel.setText(email);
        }
    }

    @FXML
    private void initialize() {

        //préparation l'interface
        setupTable();
        setupTreeView();

        //mettre en place le listener
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null && newItem.getValue() != null) {
                currentFolder = newItem.getValue();
                loadFiles(currentFolder);
            }
        });

        //charger les données
        loadData();         // => charger les données au démarrage

        //mise à jour compteur
        updateFileCount();

        //mise à jour le quota
        updateQuota();

        //mise à jour email d'utilisateur
        String email = AppProperties.get("auth.email");
        if(email != null && !email.isEmpty()){
            userEmailLabel.setText(email);
        }

        System.out.println("userEmail: " + userEmailLabel.getText());

    }


    /**
     * mettre à jour les colonnes dans TableView
     */
    private void setupTable() {
        // Configuration des colonnes
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("sizeFormatted"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("updatedAtFormatted"));

        table.setItems(fileList);

        // Activer/désactiver les boutons selon la sélection
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;  //=> true  //newVal = la valeur sélectionnée dans une TableView/ListView
            shareButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);

            // Changer la couleur des boutons
            if (hasSelection) {
                shareButton.setStyle("-fx-background-color: #980b0b; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 6 14;");
                deleteButton.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 6 14;");
            } else {
                shareButton.setStyle("-fx-background-color: #666666; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 6 14;");
                deleteButton.setStyle("-fx-background-color: #666666; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 6 14;");
            }
        });

        // Double-clic pour télécharger => pour plus tard
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                FileEntry selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    handleDownload(selected);
                }
            }
        });
    }


    /**
     * mise à jour : Listener sur le TreeView
     */
    private void setupTreeView() {

        // Listener sur la sélection => à supprimer???
//        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
//            if (newVal != null && newVal.getValue() != null) {
//                currentFolder = newVal.getValue();
//                loadFiles(currentFolder);
//            }
//        });

        // Style de l'arborescence
        treeView.setCellFactory(tv -> new TreeCell<NodeItem>() {

            @Override
            protected void updateItem(NodeItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText("📁 " + item.getName());
                }
            }
        });
    }


    /**
     * Chargement les données, l'arborescence
     */
    private void loadData() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                // Charger l'arborescence depuis l'API
                NodeItem root = apiClient.listRoot();

                Platform.runLater(() -> {
                    TreeItem<NodeItem> rootItem = buildTree(root);
                    treeView.setRoot(rootItem);

                    // Sélectionner le premier dossier si disponible
                    if (!rootItem.getChildren().isEmpty()) {

                        TreeItem<NodeItem> first = rootItem.getChildren().get(0);
                        treeView.getSelectionModel().select(first);
                        currentFolder = first.getValue();

                        // charge les fichiers du 1er dossier
                        loadFiles(currentFolder);
                    }
                    statusLabel.setText("Données chargées");
                });

                // Charger les quotas avec endpoint
                //updateQuota();

//                Platform.runLater(() -> {
//                    statusLabel.setText("Données chargées");
//                });

            } catch (Exception e) {
                e.printStackTrace();

                Platform.runLater(() -> {
                    showError("Erreur de chargement", "Impossible de charger les données: " + e.getMessage());
                    statusLabel.setText("Erreur de chargement");
                });
            }
        }).start();
    }


    /**
     * Construction visuelle de l'arbre
     * @param node
     * @return
     */
    private TreeItem<NodeItem> buildTree(NodeItem node) {

        TreeItem<NodeItem> item = new TreeItem<>(node);
        item.setExpanded(true);

        for (NodeItem child : node.getChildren()) {
            item.getChildren().add(buildTree(child));
        }

        return item;
    }


    /**
     * Chargement des fichiers d'un dossier
     * @param folder
     */
    private void loadFiles(NodeItem folder) {

        if(folder == null) {
            return;
        }

        statusLabel.setText("Chargement des fichiers ...");
        fileList.clear();

        new Thread(() -> {
            try{
                var files = apiClient.listFiles(folder.getId());

                Platform.runLater(() -> {
                    fileList.setAll(files);
                    updateFileCount();
                    statusLabel.setText("Dossier: " + folder.getName());
                });

            }catch(Exception e){
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Erreur", "Impossible de charger les fichiers: " + e.getMessage());
                    statusLabel.setText("Erreur de chargement des fichiers");
                });
            }
        }).start();
    }

    /**
     * mettre à jour le quota
     */
    private void updateQuota() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                var quota = apiClient.getQuota();

                Platform.runLater(() -> {

                    if(quota == null){
                        quotaBar.setProgress(0.0);
                        quotaLabel.setText("0 B / 0 B");
                        quotaBar.setStyle("-fx-accent: #980b0b;");
                        return;
                    }
                    double ratio = quota.getUsageRatio();

                    if(ratio < 0){ ratio = 0;}
                    if(ratio > 1){ ratio = 1;}

                    quotaBar.setProgress(ratio);

                    String used = formatSize(quota.getUsed());
                    String total = formatSize(quota.getMax());
                    quotaLabel.setText(used + " / " + total);

                    // Changer la couleur selon l'utilisation
                    if (ratio >= 0.9) {
                        quotaBar.setStyle("-fx-accent: #d9534f;"); // Rouge
                    } else if (ratio >= 0.8) {
                        quotaBar.setStyle("-fx-accent: #f0ad4e;"); // Orange
                    } else {
                        quotaBar.setStyle("-fx-accent: #980b0b;"); // Rouge normal
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    quotaBar.setProgress(0);
                    quotaLabel.setText("Erreur quota");
                    quotaBar.setStyle("-fx-accent: #d9534f;");
                });
            }
        }).start();
    }

    // à écrire!!!!
    private void updateFileCount() {

        int count = (fileList == null) ? 0 : fileList.size();

        if (fileCountLabel != null) {
            fileCountLabel.setText(count + " fichier" + (count > 1 ? "s" : ""));
        }

    }


    /**à compléter la méthode upload!!!!
     * Gestion d'upload
     */
    @FXML
    private void handleUpload() {
        try{
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/coffrefort/client/uploadDialog.fxml")
            );

            Parent root = loader.load();

            //récupération du contrôleur
            UploadDialogController controller = loader.getController();
            controller.setApiClient(apiClient);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Uploader des fichiers");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(uploadButton.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            controller.setDialogStage(dialogStage);

            //callback pour rafraîchir après upload
            controller.setOnUploadSuccess(() ->{

                if(currentFolder != null){
                    loadFiles(currentFolder);
                }
                statusLabel.setText("Upload terminé");
            });

            dialogStage.showAndWait();

        }catch(Exception e){
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir la fenêtre d'upload "+e.getMessage());
        }
    }


    // à écrire!!!!
    @FXML
    private void handleShare() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // TODO: Ouvrir le dialog de partage
        showInfo("Partage", "Fonctionnalité de partage pour: " + selected.getName());
    }

    /**
     * supprimer des fichiers ou folders
     */
    @FXML
    private void handleDelete() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        deleteButton.setDisable(true);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/coffrefort/client/confirmDelete.fxml")
            );

            VBox root = loader.load();

            // Récupération du contrôleur
            ConfirmDeleteController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Confirmer la suppresion du fichier");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(deleteButton.getScene().getWindow());
            dialogStage.setScene(new Scene(root));

            // Injection du stage et du nom de fichier
            controller.setDialogStage(dialogStage);
            controller.setFileName(selected.getName());

            //callbacks
            controller.setOnConfirm(() -> deleteFile(selected));
            controller.setOnCancel(() -> statusLabel.setText("Suppression annulé"));
            dialogStage.showAndWait();

        } catch (Exception e){
            System.err.println("Erreur lors du chargement de confirmDelete.fxml");
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir la fenêtre de suppression: "+e.getMessage());
        } finally {
            deleteButton.setDisable(false);
        }
    }

    /**
     * supprimer un file
     * @param file
     */
    private void deleteFile(FileEntry file) {
        statusLabel.setText("Suppression en cours...");

        new Thread(() -> {
            try {
                boolean success = apiClient.deleteFile(file.getId());

                Platform.runLater(() -> {
                    if (success) {

                        fileList.remove(file);  // => ça n'enleve  que localement

                        if(currentFolder != null){ //=> recharger complètement le dossier
                            loadFiles(currentFolder);
                        }

                        updateFileCount();
                        updateQuota();

                        //Désactiver les boutons de partage et supprime
                        shareButton.setDisable(true);
                        deleteButton.setDisable(true);

                        shareButton.setStyle("-fx-background-color: #666666; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 6 14;");
                        deleteButton.setStyle("-fx-background-color: #666666; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 6 14;");

                        statusLabel.setText("Fichier supprimé: " + file.getName());
                    } else {
                        showError("Erreur", "Impossible de supprimer le fichier.");
                        statusLabel.setText("Erreur de suppression");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Erreur", "Erreur: " + e.getMessage());
                    statusLabel.setText("Erreur de suppression");
                });
            }
        }).start();
    }

    // à écrire!!!!
    private void handleDownload(FileEntry file) {
        statusLabel.setText("Téléchargement: " + file.getName());
        // TODO: Implémenter le téléchargement
        showInfo("Téléchargement", "Téléchargement de: " + file.getName());
    }

    /**
     * Gestion de cas de "création d'un folder"
     */
    @FXML
    private void handleNewFolder() {
        newFolderButton.setDisable(true);
        try{
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/coffrefort/client/createFolder.fxml")
            );

            VBox root = loader.load();

            // Récupération du contrôleur
            CreateFolderController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Créer un nouveau dossier");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(newFolderButton.getScene().getWindow());
            dialogStage.setScene(new Scene(root));

            // Injection du stage et de la logique de déconnexion
            controller.setDialogStage(dialogStage);

            //quand user clique sur "Créer"
            controller.setOnCreateFolder(this::createFolder);

            dialogStage.showAndWait();
        }catch(Exception e){
            System.err.println("Erreur lors du chargement de createFolder.fxml");
            e.printStackTrace();
        }finally {
            // Réactiver le bouton après fermeture du dialogue
            newFolderButton.setDisable(false);
        }
    }

    /**
     * Création d'un Folder
     * @param name
     */
    private void createFolder(String name) {
        statusLabel.setText("Création du dossier...");

        new Thread(() -> {
            try {
                boolean success = apiClient.createFolder(name, currentFolder);

                Platform.runLater(() -> {
                    if (success) {
                        loadData(); // Recharger l'arborescence
                        statusLabel.setText("Dossier créé: " + name);
                    } else {
                        showError("Erreur", "Impossible de créer le dossier.");
                        statusLabel.setText("Erreur de création");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Erreur", "Erreur: " + e.getMessage());
                    statusLabel.setText("Erreur de création");
                });
            }
        }).start();
    }

    // à écrire!!!!
    @FXML
    private void handleOpenShares() {
        // TODO: Ouvrir la fenêtre des partages
        showInfo("Mes partages", "Fonctionnalité à venir");
    }


    /**
     * Gestion de déconnexion
     */
    @FXML
    private void handleLogout() {
        logoutButton.setDisable(true);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/coffrefort/client/confirmLogout.fxml")
            );

            VBox root = loader.load();

            // Récupération du contrôleur
            ConfirmLogoutController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Confirmer la déconnexion");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(logoutButton.getScene().getWindow());
            dialogStage.setScene(new Scene(root));

            // Injection du stage et de la logique de déconnexion
            controller.setDialogStage(dialogStage);
            controller.setOnLogoutConfirmed(() -> {

                // Déconnexion (suppression du token)
                apiClient.logout();
                System.out.println("Déconnexion effectuée. Retour à l'écran de connexion...");

                // Fermer la fenêtre de dialogue AVANT de changer de scène
                dialogStage.close();

                // Utiliser Platform.runLater pour changer de scène de manière sûre
                Platform.runLater(() -> {
                    try {
                        FXMLLoader loginLoader = new FXMLLoader(
                                getClass().getResource("/com/coffrefort/client/login2.fxml")
                        );
                        Parent loginRoot = loginLoader.load();

                        // Récupérer le contrôleur du login
                        LoginController loginController = loginLoader.getController();

                        // Injecter l'ApiClient existant
                        loginController.setApiClient(apiClient);

                        // Récupérer la fenêtre principale (Stage)
                        Stage stage = (Stage) logoutButton.getScene().getWindow();

                        // Remplacer la scène par celle du login
                        Scene loginScene = new Scene(loginRoot);
                        stage.setScene(loginScene);
                        stage.setTitle("Connexion - CryptoVault");
                        stage.show();

                        System.out.println("Redirection vers la page de connexion réussie.");

                    } catch (IOException e) {
                        System.err.println("Erreur lors du chargement de login2.fxml");
                        e.printStackTrace();

                        // Afficher un message d'erreur à l'utilisateur
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Erreur");
                        alert.setHeaderText("Erreur de déconnexion");
                        alert.setContentText("Impossible de charger l'écran de connexion.");
                        alert.showAndWait();
                    }
                });
            });

            dialogStage.showAndWait();

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de confirmLogout.fxml");
            e.printStackTrace();

            // Afficher un message d'erreur
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur de déconnexion");
            alert.setContentText("Impossible de charger la fenêtre de confirmation.");
            alert.showAndWait();

        } catch (Exception e) {
            System.err.println("Erreur inattendue lors de la déconnexion");
            e.printStackTrace();

        } finally {
            // Réactiver le bouton après fermeture du dialogue
            logoutButton.setDisable(false);
        }
    }

    // à écrire!!!! => il est dans le FileEntry
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), unit);
    }

    /**
     * afficher les erreurs
     * @param title
     * @param content
     */
    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * afficher les informations
     * @param title
     * @param content
     */
    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}