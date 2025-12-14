package com.coffrefort.client.controllers;

import com.coffrefort.client.ApiClient;
import com.coffrefort.client.model.FileEntry;
import com.coffrefort.client.model.NodeItem;
import com.coffrefort.client.model.Quota;
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
import javafx.scene.control.TableRow;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Optional;
import com.coffrefort.client.config.AppProperties;
import org.w3c.dom.Node;

public class MainController {

    //propriétés
    @FXML private TreeView<NodeItem> treeView;
    @FXML private TableView<FileEntry> table;
    @FXML private TableColumn<FileEntry, String> nameCol;
    @FXML private TableColumn<FileEntry, String> sizeCol;
    @FXML private TableColumn<FileEntry, String> dateCol;

    @FXML private ProgressBar quotaBar;
    @FXML private Label quotaLabel;
    private String quotaColor = "#5cb85c"; //=> pour la couleur persistante
    private boolean quotaStyleInitialized = false;
    private int quotaStyleRetries = 0;
    private static final int MAX_QUOTA_STYLE_RETRIES = 20;

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

    private Stage mainStage;

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
        setupTreeViewRootContextMenu();

        //mettre en place le listener
        // quand je clique sur un dossier => currentFolder <=> currentFolder= null
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

        // pour garantir le styles inline => éviter le  CSS externe
        // ✅ label bold inline
        quotaLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333; -fx-font-size: 12px;");

        quotaBar.setStyle("-fx-pref-height: 8px;");

        // IMPORTANT : on laisse JavaFX créer la skin, puis on stylise (avec retry)
        Platform.runLater(this::refreshQuotaBarStyleWithRetry);

        // Si la scene arrive / change -> restyle
        quotaBar.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                quotaStyleRetries = 0;
                Platform.runLater(this::refreshQuotaBarStyleWithRetry);
            }
        });

        // Si la skin change -> restyle
        quotaBar.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            quotaStyleRetries = 0;
            Platform.runLater(this::refreshQuotaBarStyleWithRetry);
        });

        // À chaque changement de progress, JavaFX peut reconstruire la bar -> restyle
        quotaBar.progressProperty().addListener((obs, oldV, newV) -> {
            Platform.runLater(this::refreshQuotaBarStyle);
        });

        // ✅ premier passage
        Platform.runLater(() -> {
            initQuotaBarStyleOnce();
            refreshQuotaBarStyle();
        });

    }


    /**
     * mettre à jour les colonnes dans TableView
     */
    private void setupTable() {
        // Configuration des colonnes
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));


        sizeCol.setCellValueFactory(new PropertyValueFactory<>("formattedSize"));
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
//        table.setOnMouseClicked(event -> {
//            if (event.getClickCount() == 2) {
//                FileEntry selected = table.getSelectionModel().getSelectedItem();
//                if (selected != null) {
//                    handleDownload(selected);
//                }
//            }
//        });

        //double cliq sur une ligne
        table.setRowFactory(tv -> {
            TableRow<FileEntry> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if(event.getClickCount() ==2 && !row.isEmpty()){
                    FileEntry selected = row.getItem();
                    handleDownload(selected);
                }
            });
            return row;
        });

    }


    /**
     * mise à jour : Listener sur le TreeView
     */
    private void setupTreeView() {

        // Style de l'arborescence
        treeView.setCellFactory(tv -> {
            TreeCell<NodeItem> cell = new TreeCell<>() {

                @Override
                protected void updateItem(NodeItem item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setContextMenu(null);
                    } else {
                        setText("📁 " + item.getName());

                        // ne pas proposer suppression sur la racine virtuelle => id=0
                        if (item.getId() == 0) {
                            setContextMenu(null);
                        } else {

                            // afficher le menu au clique droite => setContextMenu()
                            // rendre le clique droit active
                            setContextMenu(createFolderContextMenu(this));
                        }
                    }
                }
            };
            return cell;
        });
    }

    /**
     * création du menu contextuel pour un dossier donné
     * @param cell
     * @return
     */
    private ContextMenu createFolderContextMenu(TreeCell<NodeItem>  cell){
        ContextMenu menu = new ContextMenu();


        MenuItem createInside = new MenuItem("Nouveau dossier ici...");
        createInside.setOnAction(event -> {
            NodeItem folder = cell.getItem();
            if (folder != null){
                openCreateFolderDialog(folder); // => parent = dossier cliqué
            }
        });

        MenuItem deleteItem = new MenuItem("Supprimer ce dossier...");
        deleteItem.setOnAction(event -> {
            NodeItem folder = cell.getItem();
            TreeItem<NodeItem> treeItem = cell.getTreeItem();

            if (folder != null && treeItem != null) {
                handleDeleteFolder(folder, treeItem);
            }
        });

        menu.getItems().addAll(createInside, new SeparatorMenuItem(), deleteItem);
        return menu;
    }


    private void setupTreeViewRootContextMenu(){
        ContextMenu rootMenu = new ContextMenu();

        MenuItem createRootFolder = new MenuItem("Nouveau dossier à la racine...");
        createRootFolder.setOnAction(event -> openCreateFolderDialog(null));
        rootMenu.getItems().addAll(createRootFolder);


        treeView.setOnContextMenuRequested(event -> {

            // si la souris est au-dessus d'une TreeCell => menu du dossier
            TreeCell<?> hoveredCell = (TreeCell<?>) treeView.lookup(".tree-cell:hover");
            if(hoveredCell != null && hoveredCell.getItem() != null){
                return; //laisser le menu contextuel du TreeCell à fonctionner
            }

            //zone vide => affichage le menu racine
            rootMenu.show(treeView, event.getScreenX(), event.getScreenY());
            event.consume();
        });

        //si on fait clic gauche ailleurs => fermer le menu
        treeView.setOnMousePressed(event -> {
            if(rootMenu.isShowing()){
                rootMenu.hide();
            }

            if(event.isPrimaryButtonDown()){
                TreeCell<?> heveredCell = (TreeCell<?>) treeView.lookup(".tree-cell:hover");
                if(heveredCell == null && heveredCell.getItem() == null){
                    treeView.getSelectionModel().clearSelection();
                    currentFolder = null;
                    fileList.clear();
                    updateFileCount();
                    statusLabel.setText("Aucun dossier séléctionné");
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

//                    Sélectionner le premier dossier si disponible
//                    il ne faut plus séléctionner automatiquement le premier dossier!!
//                    if (!rootItem.getChildren().isEmpty()) {
//
//
//                        TreeItem<NodeItem> first = rootItem.getChildren().get(0);
//                        treeView.getSelectionModel().select(first);
//                        currentFolder = first.getValue();
//
//                        // charge les fichiers du 1er dossier
//                        loadFiles(currentFolder);
//                    }


                    treeView.getSelectionModel().clearSelection();
                    currentFolder = null;

                    //vider la table tant qu'aucun dossier n'est choisi
                    fileList.clear();
                    updateFileCount();
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

    private void initQuotaBarStyleOnce() {
        if (quotaStyleInitialized) return;
        quotaStyleInitialized = true;

        // Track (fond) : on le fixe une fois (sera réappliqué si skin change via refresh)
        var track = quotaBar.lookup(".track");  //=> cherche dans la ProgressBar le nœud interne CSS nommé .track
        if (track != null) {
            track.setStyle("-fx-background-color: #eeeeee; -fx-background-radius: 4px; -fx-background-insets: 0;");
        }
    }

    private void setQuotaColor(String hexColor) {
        quotaColor = hexColor;
        refreshQuotaBarStyleWithRetry();
    }

    private void refreshQuotaBarStyleWithRetry() {
        // Essayer d'appliquer, et si bar/track pas prêts, retenter quelques pulses
        if (!refreshQuotaBarStyle()) {
            if (quotaStyleRetries++ < MAX_QUOTA_STYLE_RETRIES) {
                Platform.runLater(this::refreshQuotaBarStyleWithRetry);
            } else {
                System.out.println("QuotaBar style: impossible de trouver .bar/.track après retries");
            }
        }
    }

    /**
     * @return true si .bar existe (style appliqué), false sinon
     */
    private boolean refreshQuotaBarStyle() {
        var track = quotaBar.lookup(".track");
        var bar = quotaBar.lookup(".bar");

        // si pas encore prêt, on ne fait rien
        if (track == null || bar == null) {
            return false;
        }

        track.setStyle("-fx-background-color: #eeeeee; -fx-background-radius: 4px; -fx-background-insets: 0;");
        bar.setStyle(
                "-fx-background-color: " + quotaColor + ";" +
                        "-fx-background-radius: 4px;" +
                        "-fx-background-insets: 0;"
        );

        return true;
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
                    if (quota == null) {
                        quotaBar.setProgress(0.0);
                        quotaLabel.setText("0 B / 0 B");

                        setQuotaColor("#d9534f"); //rouge
                        refreshQuotaBarStyleWithRetry();
                        return;
                    }

                    double ratio = quota.getUsageRatio();
                    if (ratio < 0) ratio = 0;
                    if (ratio > 1) ratio = 1;

                    // couleur
                    if (ratio >= 0.9) quotaColor = "#d9534f"; //rouge
                    else if (ratio >= 0.8) quotaColor = "#f0ad4e"; //orange
                    else quotaColor = "#5cb85c"; //vert

                    // progress
                    quotaBar.setProgress(ratio); // valeur entre 0 et 1

                    // texte
                    quotaLabel.setText(formatSize(quota.getUsed()) + " / " + formatSize(quota.getMax()));

                    // restyle (important après setProgress) => pour éviter que JavaFX reconstruite le noeud interne
                    quotaStyleRetries = 0;
                    refreshQuotaBarStyleWithRetry();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    quotaBar.setProgress(0.0);
                    quotaLabel.setText("Erreur quota");

                    setQuotaColor("#d9534f"); //rouge
                    quotaStyleRetries = 0;
                    refreshQuotaBarStyleWithRetry();
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


    /**
     * Gestion d'upload des fichiers
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

            if(currentFolder != null){
                controller.setTargetFolderId(currentFolder.getId());
            }

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
     * supprimer des fichiers
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

    /**
     * gestion de téléchargement
     * @param file
     */
    private void handleDownload(FileEntry file) {
//        statusLabel.setText("Téléchargement: " + file.getName());
//        // TODO: Implémenter le téléchargement
//        showInfo("Téléchargement", "Téléchargement de: " + file.getName());

        if(file == null) return;

        FileChooser chooser = new FileChooser();

        //à choisir où enregistrer
        chooser.setTitle("Enregistrer le fichier...");

        // définir le nom => par défaut
        chooser.setInitialFileName(file.getName());

        //au cas ou pour définir un filtre par extension
        // chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));

        File target = chooser.showSaveDialog(table.getScene().getWindow());
        if (target == null){
            statusLabel.setText("Le téléchargement est annulé");
            return;
        }

        statusLabel.setText("Téléchargement de " + file.getName() + "...");

        new Thread(() -> {
            try {
                apiClient.downloadFileTo(file.getId(), target);

                Platform.runLater(() -> {
                    statusLabel.setText("Téléchargé " + target.getAbsolutePath());
                    updateQuota();
                });
            }catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Téléchargement", "Impossible de télécharger: " + e.getMessage());
                    statusLabel.setText("Erreur de téléchargement");
                });

            }
        }).start();

    }

    /**
     * Gestion de cas de "création d'un folder"
     */
    @FXML
    private void handleNewFolder() {
        openCreateFolderDialog(currentFolder); // currentFolder peut être null => racine
    }

    /**
     * Création d'un Folder
     * @param name
     */
    private void createFolder(String name, NodeItem parentFolder) {
        statusLabel.setText("Création du dossier...");

        new Thread(() -> {
            try {
                boolean success = apiClient.createFolder(name, parentFolder); //=> parentFolder peut être null

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

    /**
     * ouvrir le dialog CreatFolder.fxml pour créer un dossier avec à la racine
     * @param parentFolder
     */
    private void openCreateFolderDialog(NodeItem parentFolder){
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/coffrefort/client/createFolder.fxml")
            );

            VBox root = loader.load();

            // Récupération du contrôleur
            CreateFolderController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Créer un nouveau dossier");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(treeView.getScene().getWindow());
            dialogStage.setScene(new Scene(root));

            controller.setDialogStage(dialogStage);

            controller.setOnCreateFolder(name -> createFolder(name, parentFolder));

            dialogStage.showAndWait();

        }catch (Exception e){
            System.err.println("Erreur lors du chargement de createFolder.fxml");
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir la fenêtre de création: " + e.getMessage());
        }
    }

    // à écrire!!!!
    @FXML
    private void handleOpenShares() {
        // TODO: Ouvrir la fenêtre des partages
        showInfo("Mes partages", "Fonctionnalité à venir");
    }

    /**
     * pour gérer la suppression d'un dossier
     * @param folder
     * @param treeItem
     */
    private void handleDeleteFolder(NodeItem folder, TreeItem<NodeItem> treeItem){

//        Alert confirm =  new Alert(Alert.AlertType.CONFIRMATION);
//        confirm.setTitle("Supprimer le dossier");
//        confirm.setHeaderText("Supprimer le dossier \"" + folder.getName() + "\" ?");
//        confirm.setContentText("Tous les fichiers et sous-dossiers de ce dossier seront supprimer.");

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/coffrefort/client/confirmDeleteFolder.fxml")
            );

            VBox root = loader.load();

            // Récupération du contrôleur
            ConfirmDeleteFolderController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Confirmer la suppresion du dossier");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(deleteButton.getScene().getWindow());
            dialogStage.setScene(new Scene(root));

            // Injection du stage et du nom de fichier
            controller.setDialogStage(dialogStage);
            controller.setFolderName(folder.getName());

            //callbacks
            controller.setOnConfirm(() -> deleteFolderOnServer(folder, treeItem));
            controller.setOnCancel(() -> statusLabel.setText("Suppression du dossier annulé"));
            dialogStage.showAndWait();

        } catch (Exception e){
            System.err.println("Erreur lors du chargement de confirmDeleteFolder.fxml");
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir la fenêtre de suppression: "+e.getMessage());
        }

//        Optional<ButtonType> result = confirm.showAndWait();
//        if(result.isPresent() && result.get() == ButtonType.OK){
//            deleteFolderOnServer(folder, treeItem);
//        }else{
//            statusLabel.setText("Suppression du dossier annulée");
//        }
    }

    /**
     * supprimer le dossier sur le serveur (via API) + mise à jour l'affichage
     * @param folder
     * @param treeItem
     */
    private void deleteFolderOnServer(NodeItem folder, TreeItem<NodeItem> treeItem){
        statusLabel.setText("Suppression du dossier en cours ...");

        new Thread(() -> {
            try{
                boolean success = apiClient.deleteFolder(folder.getId());

                Platform.runLater(() -> {
                    if(success){

                        //Si on est dans ce dossier => vider la table
                        if(currentFolder != null && currentFolder.getId() ==  folder.getId()){
                            fileList.clear();
                            updateFileCount();
                            currentFolder = null;
                        }

                        //recharger arborescence
                        loadData();
                        updateQuota();

                        statusLabel.setText("Dossier supprimé: " + folder.getName());
                    }else{
                        showError("Erreur", "Impossible de supprimer le dossier.");
                        statusLabel.setText("Erreur de suppression du dossier.");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Erreur", "Erreur lors de la suppression du dossier: " + e.getMessage());
                    statusLabel.setText("Erreur de suppression du dossier");
                });
            }
        }).start();
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