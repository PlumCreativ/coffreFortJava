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
        setupTable();
        setupTreeView();
        //loadData();
        updateFileCount();

        String email = AppProperties.get("auth.email");
        if(email != null && !email.isEmpty()){
            userEmailLabel.setText(email);
        }

        System.out.println("userEmail: " + userEmailLabel.getText());
    }

    private void setupTable() {
        // Configuration des colonnes
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("sizeFormatted"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));

        table.setItems(fileList);

        // Activer/désactiver les boutons selon la sélection
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
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

        // Double-clic pour télécharger
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                FileEntry selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    handleDownload(selected);
                }
            }
        });
    }

    private void setupTreeView() {
        // Configuration de l'arborescence
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentFolder = newVal.getValue();
                loadFiles(currentFolder);
            }
        });

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

//    private void loadData() {
//        new Thread(() -> {
//            try {
//                // Charger l'arborescence
//                NodeItem root = apiClient.listRoot();
//
//                Platform.runLater(() -> {
//                    TreeItem<NodeItem> rootItem = buildTree(root);
//                    treeView.setRoot(rootItem);
//
//                    // Sélectionner le premier dossier
//                    if (!rootItem.getChildren().isEmpty()) {
//                        treeView.getSelectionModel().select(rootItem.getChildren().get(0));
//                    }
//                });
//
//                // Charger les quotas
//                updateQuota();
//
//                Platform.runLater(() -> {
//                    statusLabel.setText("Données chargées");
//                });
//
//            } catch (Exception e) {
//                Platform.runLater(() -> {
//                    showError("Erreur de chargement", "Impossible de charger les données: " + e.getMessage());
//                    statusLabel.setText("Erreur de chargement");
//                });
//            }
//        }).start();
//    }

    private TreeItem<NodeItem> buildTree(NodeItem node) {
        TreeItem<NodeItem> item = new TreeItem<>(node);
        item.setExpanded(true);

        for (NodeItem child : node.getChildren()) {
            item.getChildren().add(buildTree(child));
        }

        return item;
    }

    private void loadFiles(NodeItem folder) {
        fileList.clear();
        fileList.addAll(folder.getFiles());
        updateFileCount();
        statusLabel.setText("Dossier: " + folder.getName());
    }

//    private void updateQuota() {
//        new Thread(() -> {
//            try {
//                var quota = apiClient.getQuota();
//
//                Platform.runLater(() -> {
//                    double ratio = quota.ratio();
//                    quotaBar.setProgress(ratio);
//
//                    String used = formatSize(quota.used());
//                    String total = formatSize(quota.total());
//                    quotaLabel.setText(used + " / " + total);
//
//                    // Changer la couleur selon l'utilisation
//                    if (ratio >= 0.9) {
//                        quotaBar.setStyle("-fx-accent: #d9534f;"); // Rouge
//                    } else if (ratio >= 0.8) {
//                        quotaBar.setStyle("-fx-accent: #f0ad4e;"); // Orange
//                    } else {
//                        quotaBar.setStyle("-fx-accent: #980b0b;"); // Rouge normal
//                    }
//                });
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }).start();
//    }

    private void updateFileCount() {
        int count = fileList.size();
        fileCountLabel.setText(count + " fichier" + (count > 1 ? "s" : ""));
    }

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


    @FXML
    private void handleShare() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // TODO: Ouvrir le dialog de partage
        showInfo("Partage", "Fonctionnalité de partage pour: " + selected.getName());
    }

    @FXML
    private void handleDelete() {
        FileEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le fichier ?");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer \"" + selected.getName() + "\" ?");

        Optional<ButtonType> result = confirm.showAndWait();
//        if (result.isPresent() && result.get() == ButtonType.OK) {
//            deleteFile(selected);
//        }
    }

//    private void deleteFile(FileEntry file) {
//        statusLabel.setText("Suppression en cours...");
//
//        new Thread(() -> {
//            try {
//                boolean success = apiClient.deleteFile(file.getId());
//
//                Platform.runLater(() -> {
//                    if (success) {
//                        fileList.remove(file);
//                        updateFileCount();
//                        updateQuota();
//                        statusLabel.setText("Fichier supprimé: " + file.getName());
//                    } else {
//                        showError("Erreur", "Impossible de supprimer le fichier.");
//                        statusLabel.setText("Erreur de suppression");
//                    }
//                });
//            } catch (Exception e) {
//                Platform.runLater(() -> {
//                    showError("Erreur", "Erreur: " + e.getMessage());
//                    statusLabel.setText("Erreur de suppression");
//                });
//            }
//        }).start();
//    }

    private void handleDownload(FileEntry file) {
        statusLabel.setText("Téléchargement: " + file.getName());
        // TODO: Implémenter le téléchargement
        showInfo("Téléchargement", "Téléchargement de: " + file.getName());
    }

    @FXML
    private void handleNewFolder() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nouveau dossier");
        dialog.setHeaderText("Créer un nouveau dossier");
        dialog.setContentText("Nom du dossier:");

        Optional<String> result = dialog.showAndWait();
//        result.ifPresent(name -> {
//            if (!name.trim().isEmpty()) {
//                createFolder(name.trim());
//            }
//        });
    }

//    private void createFolder(String name) {
//        statusLabel.setText("Création du dossier...");
//
//        new Thread(() -> {
//            try {
//                boolean success = apiClient.createFolder(name, currentFolder);
//
//                Platform.runLater(() -> {
//                    if (success) {
//                        loadData(); // Recharger l'arborescence
//                        statusLabel.setText("Dossier créé: " + name);
//                    } else {
//                        showError("Erreur", "Impossible de créer le dossier.");
//                        statusLabel.setText("Erreur de création");
//                    }
//                });
//            } catch (Exception e) {
//                Platform.runLater(() -> {
//                    showError("Erreur", "Erreur: " + e.getMessage());
//                    statusLabel.setText("Erreur de création");
//                });
//            }
//        }).start();
//    }

    @FXML
    private void handleOpenShares() {
        // TODO: Ouvrir la fenêtre des partages
        showInfo("Mes partages", "Fonctionnalité à venir");
    }

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

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), unit);
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}