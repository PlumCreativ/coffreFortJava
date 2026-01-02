package com.coffrefort.client.controllers;


import com.coffrefort.client.ApiClient;
import com.coffrefort.client.model.FileEntry;
import com.coffrefort.client.model.VersionEntry;
import com.coffrefort.client.util.UIDialogs;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Provider;
import java.util.List;

public class FileDetailsController {

    @FXML private Label fileNameLabel;
    @FXML private Label fileMetaLabel;

    @FXML private Button replaceButton;

    @FXML private VBox progressBox;
    @FXML private Label uploadStatusLabel;
    @FXML private ProgressBar uploadProgressBar;
    @FXML private Label errorLabel;
    @FXML private Label versionsCountLabel;

    @FXML private TableView<VersionEntry> versionsTable;
    @FXML private TableColumn<VersionEntry, Number> versionCol;
    @FXML private TableColumn<VersionEntry, String> sizeCol;
    @FXML private TableColumn<VersionEntry, String> dateCol;
    @FXML private TableColumn<VersionEntry, String> checksumCol;

    @FXML private Button copyChecksumButton;
    @FXML private Button openLocalFolderButton;
    @FXML private Button downloadVersionButton;

    private final ObservableList<VersionEntry> versions = FXCollections.observableArrayList();

    private ApiClient apiClient;
    private FileEntry file;
    private Stage stage;

    private Runnable onVersionUploaded;

    private Service<Void> uploadService;

    // pour garder une “trace” locale des téléchargements pour activer "ouvrir dossier local"
    // ObservableMap pour que les bindings JavaFX se mettent à jour
    // Map key = "fileId:v{versionNumber}" -> downloaded file path
    private final ObservableMap<String, Path> downloadedPaths = FXCollections.observableHashMap();


    /**
     * Injecte l’ApiClient utilisé pour charger les versions et effectuer les uploads/téléchargements
     * @param apiClient
     */
    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
        maybeRefresh();
    }

    /**
     * Injecte le Stage courant afin d’ouvrir des FileChooser et gérer la fenêtre.
     * @param stage
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Définit le fichier courant dont les versions sont affichées
     * @param file
     */
    public void setFile(FileEntry file) {
        this.file = file;
        maybeRefresh();
    }

    //refresh quand apiclient et file sont injectés!!
    private void maybeRefresh() {
        if(this.apiClient != null && this.file != null) {
            hydrateThenRefresh();
        }
    }

    private void  hydrateThenRefresh(){
        if(apiClient == null || file == null) return;

        versionsCountLabel.setText("Chargement...");

        new Thread(() -> {
            try {
                FileEntry fresh = apiClient.getFile(file.getId());

                Platform.runLater(() -> {
                    this.file = fresh; // => mise à jour les valeurs de header
                    refresh();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    versions.clear();
                    versionsCountLabel.setText("Erreur");
                    UIDialogs.showError("Erreur", null, "Impossible de charger: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Rafraîchit l’en-tête du fichier et recharge la liste des versions depuis l’API
     */
    public void refresh(){
        refreshHeader();
        loadVersions();
    }

    /**
     * Met à jour les informations affichées du fichier (nom, taille, date de modification)
     */
    private void refreshHeader(){

        if(file == null) return;

        fileNameLabel.setText(file.getName() != null ? file.getName() : "");

        String size = file.getFormattedSize() != null ? file.getFormattedSize() : "";
        String date = file.getUpdatedAtFormatted() != null ? file.getUpdatedAtFormatted() : "";
        String meta = size;

        if (!size.isBlank() && !date.isBlank()) {
            meta += " • ";
        }
        if (!date.isBlank()) {
            meta += "Modifié le " + date;
        }

        fileMetaLabel.setText(meta.trim());
    }

    /**
     * Charge les versions du fichier en tâche de fond et met à jour la table et le compteur
     */
    private void loadVersions(){
        if(apiClient == null || file == null) return;

        //feedback UI
        versionsCountLabel.setText("Chargement...");

        new Thread(() -> {
            try{
                //page => 1 , limit => 10µ
                List<VersionEntry> list = apiClient.listFileVersions(file.getId(), 1, 100);

                Platform.runLater(() -> {
                    versions.setAll(list);
                    versionsCountLabel.setText(versions.size() + " version(s)");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    versions.clear();
                    versionsCountLabel.setText("Erreur");
                    UIDialogs.showError("Erreur", null,"Impossible de charger les versions " + e.getMessage());
                });
            }
        }).start();
    }


    /**
     * Met à jour le label affichant le nom du fichier courant
     * @param name
     */
    public void setCurrentName(String name){
        if(fileNameLabel != null){
            fileNameLabel.setText(name != null ? name : "");
        }
    }

    /**
     * Définit le callback exécuté après l’upload réussi d’une nouvelle version
     * @param onVersionUploaded
     */
    public void setOnVersionUploaded(Runnable onVersionUploaded) {
        this.onVersionUploaded = onVersionUploaded;
    }


    @FXML
    /**
     * Initialise la table des versions, configure les bindings des boutons et prépare l’UI
     */
    private void initialize(){

        //Table setup
        setupVersionTable();

        versionsTable.setItems(versions);

        //activation d'un bouton si (que) un bouton est séléctionné
        copyChecksumButton.disableProperty().bind(
                Bindings.isNull(versionsTable.getSelectionModel().selectedItemProperty())
        );

        downloadVersionButton.disableProperty().bind(
                Bindings.isNull(versionsTable.getSelectionModel().selectedItemProperty())
        );

        // ouvrir le dossier local que s'il y a un chemin enregistré
        openLocalFolderButton.disableProperty().bind(
                Bindings.createBooleanBinding(() -> {
                    VersionEntry sel = versionsTable.getSelectionModel().getSelectedItem();
                    if(sel == null || file == null) return true;

                    return !downloadedPaths.containsKey(key(file.getId(), sel.getVersion()));
                }, versionsTable.getSelectionModel().selectedItemProperty(), downloadedPaths)
        );

        // le progress UI caché au début
        setProgressVisible(false);

        //utilisateur change de sélection => effacer les messages d'erreur
        versionsTable.getSelectionModel().selectedItemProperty().addListener((obs, v, n) -> hideError());


    }



    /**
     * Configure les colonnes de la table des versions et le double-clic pour copier le checksum
     */
    private void setupVersionTable(){

        //colonne versin
        versionCol.setCellValueFactory(new PropertyValueFactory<>("version"));

        //taille formatée
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("formattedSize"));

        // date formatée
        dateCol.setCellValueFactory(new PropertyValueFactory<>("createdAtFormatted"));

        //checksum short
        checksumCol.setCellValueFactory(new PropertyValueFactory<>("checksumShort"));

        // avec double clique => copier le checksum
        versionsTable.setRowFactory(tv -> {
            TableRow<VersionEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if(event.getClickCount() == 2 && !row.isEmpty()) {
                    onCopyChecksum();
                }
            });
            return row;
        });
    }

    //=============== Action UI ==============
    @FXML
    /**
     * Permet de sélectionner un fichier local et d’uploader une nouvelle version avec suivi de progression
     */
    private void onReplace(){

        if(uploadService != null && uploadService.isRunning()){
            UIDialogs.showError("Erreur", null,"Un upload est deja en cours");
            return;
        }

        if(apiClient == null || file == null) {
            UIDialogs.showError("Erreur", null,"API ou fichier non initilalisé");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier pour remplacer");
        chooser.setInitialFileName(file.getName());

        File selected = chooser.showOpenDialog(stage);
        if(selected == null) return;

        hideError();
        setProgressVisible(true);
        uploadStatusLabel.setText("Preparation upload...");
        uploadProgressBar.setProgress(0);

        uploadService =  new UploadVersionService(apiClient, file.getId(), selected);

        uploadProgressBar.progressProperty().bind(uploadService.progressProperty());
        uploadStatusLabel.textProperty().bind(uploadService.messageProperty());

        // désactiver le bouton pendant l'upload
        replaceButton.disableProperty().bind(uploadService.runningProperty());

        uploadService.setOnSucceeded(event -> {

            //débind
            uploadProgressBar.progressProperty().unbind();
            uploadStatusLabel.textProperty().unbind();
            replaceButton.disableProperty().unbind();

            replaceButton.setDisable(false);

            setProgressVisible(false);

            //refresh version et header
            hydrateThenRefresh();

            //reload liste fichiers et quota ... => callback vers MainController ???????
            if(onVersionUploaded != null) {
                onVersionUploaded.run();
            }
        });

        uploadService.setOnFailed(event -> {
            Throwable ex = uploadService.getException();

            uploadProgressBar.progressProperty().unbind();
            uploadStatusLabel.textProperty().unbind();
            replaceButton.disableProperty().unbind();

            replaceButton.setDisable(false);

            //Masquer la progression et nettoyer l'état
            setProgressVisible(false);

            UIDialogs.showError("Upload echoue: ", null, (ex != null ? ex.getMessage() : "Erreur inconnu"));
        });

        uploadService.start();
    }

    @FXML
    /**
     * Copie le checksum complet de la version sélectionnée dans le presse-papiers
     */
    private void onCopyChecksum(){

        VersionEntry sel = versionsTable.getSelectionModel().getSelectedItem();

        if(sel == null) return;

        String checksum = sel.getChecksum();
        if(checksum == null || checksum.isBlank()){
            UIDialogs.showError("Erreur", null, "Checksum indisponible");
            return;
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(checksum.trim());
        Clipboard.getSystemClipboard().setContent(content);

        //feedback
        UIDialogs.showInfo("Checksum copie", null, "Le checksum a ete copie dans le presse-papiers.");
    }

    @FXML
    /**
     * Ouvre le dossier local contenant la version téléchargée sélectionnée.
     */
    private void onOpenLocalFolder(){
        VersionEntry sel = versionsTable.getSelectionModel().getSelectedItem();

        if(sel == null) return;

        if(file == null) {
            UIDialogs.showError("Erreur", null, "Fichier non initialise.");
            return;
        }

        //chercher le chemin du fichier téléchargé associé au couple (fileId, selId)
        Path path = downloadedPaths.get(key(file.getId(), sel.getVersion()));
        if(path == null){
            UIDialogs.showError("Erreur", null, "Cette version n'a pas encore ete telecharge sur ce PC");
            return;
        }

        try {
            //si "path" est fichier => ouvrir son dossier parent
            // si "path" est un dossier => ouvrir ce dossier
            Path dir = Files.isDirectory(path) ? path : path.getParent();

            //vérifier si le dossier existe => p.ex fichier supprimé, disque externe débranché ...
            if (dir == null || !Files.exists(dir)) {
                UIDialogs.showError("Erreur", null, "Dossier local introuvable");
                return;
            }

            if(Desktop.isDesktopSupported()){

                //ouvrir le dossier dans l’explorateur de fichiers du système p.exFinder/Explorer
                Desktop.getDesktop().open(dir.toFile());
            }else{

                //p.ex. VM, certaines plateformes
                UIDialogs.showError("Erreur", null, "Ouverture du dossier non supportée sur cette plateforme.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            UIDialogs.showError("Erreur", null, "Impossible d'ouvrir le dossier: " + e.getMessage());
        }
    }


    @FXML
    /**
     * Télécharge une version spécifique du fichier avec affichage de la progression
     */
    private void onDownloadVersion(){
        VersionEntry sel = versionsTable.getSelectionModel().getSelectedItem();

        if(sel == null || file == null) return;

        //blockage pour l'instant car le backend n'a pas encore ce endpoint !!!!!!!!!!!!!!!!!!!!!!!!!!!
//        if(!apiClient.isVersionDownloadSupported()){
//            UIDialogs.showError("Pas encore disponible", "Le telechargement d'une version concrete n'est pas ecrit par le backend");
//            return;
//        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer la version " + sel.getVersion());
        //chooser.setInitialFileName(file.getName());
        chooser.setInitialFileName(file.getName() + "_v" + sel.getVersion());

        //pour définir un filtre par extension
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Images (*.jpg, *.jpeg, *.png, *.webp)", "*.jpg", "*.jpeg", "*.png", "*.webp"),
                new FileChooser.ExtensionFilter("Documents Word (*.doc, *.docx)", "*.doc", "*.docx"),
                new FileChooser.ExtensionFilter("Tous les fichiers (*.*)", "*.*"));

        //filtre séléctionné par défaut
        chooser.setSelectedExtensionFilter(chooser.getExtensionFilters().get(0));

        File target = chooser.showSaveDialog(stage);
        if(target == null) return;

        hideError();
        setProgressVisible(true);
        uploadStatusLabel.setText("Telechargement");
        uploadProgressBar.setProgress(0);

        //utilisation d'un Service pour télécharger en tâche de fond
        Service<Void> downloadService = new Service<>() {

            @Override
            protected Task<Void> createTask() {
                return new Task<>(){

                    @Override
                    protected Void call() throws Exception {
                        updateMessage("Downloading version " + sel.getVersion() + "...");

                        apiClient.downloadFileVersionTo(file.getId(), sel.getVersion(), target, (done, total) -> {
                            if(total > 0) {
                                updateProgress(done, total);
                            }
                        });
                        updateMessage("Download complete");
                        updateProgress(1, 1);
                        return null;
                    }
                };
            }
        };

        uploadProgressBar.progressProperty().bind(downloadService.progressProperty());
        uploadStatusLabel.textProperty().bind(downloadService.messageProperty());

        downloadVersionButton.disableProperty().bind(downloadService.runningProperty());
        replaceButton.disableProperty().bind(downloadService.runningProperty());

        downloadService.setOnSucceeded(event -> {
            uploadProgressBar.progressProperty().unbind();
            uploadStatusLabel.textProperty().unbind();
            downloadVersionButton.disableProperty().unbind();
            replaceButton.disableProperty().unbind();

            replaceButton.setDisable(false);
            downloadVersionButton.setDisable(false);


            setProgressVisible(false);

            //enregistrer le chemin local pour activer "ouvrir dossier local"
            downloadedPaths.put(key(file.getId(), sel.getVersion()), target.toPath());

            // optionnel (UI) ?????????
            versionsTable.refresh();

            // afficher le chemin (sur FX thread)
            Platform.runLater(() ->
                    UIDialogs.showInfo("Téléchargement", null, "Version téléchargée :\n" + target.getAbsolutePath())
            );
        });

        downloadService.setOnFailed(event -> {
            Throwable ex = downloadService.getException();

            uploadProgressBar.progressProperty().unbind();
            uploadStatusLabel.textProperty().unbind();
            downloadVersionButton.disableProperty().unbind();
            replaceButton.disableProperty().unbind();

            replaceButton.setDisable(false);
            downloadVersionButton.setDisable(false);

            setProgressVisible(false);

            UIDialogs.showError("Téléchargement échoué: ", null,  (ex != null ? ex.getMessage() : "Erreur inconnu"));
        });

        downloadService.start();
    }




    //================ Helper UI ===============

    /**
     * Affiche ou masque la zone de progression et réinitialise l’état d’erreur si nécessaire
     * @param visible
     */
    private void setProgressVisible(boolean visible){
        progressBox.setVisible(visible);
        progressBox.setManaged(visible);
        if(!visible){
            hideError();
        }
    }

    /**
     * Masque le message d’erreur affiché dans l’interface.
     */
    private void hideError(){
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /**
     * énère une clé unique pour associer un fichier et une version à un chemin local téléchargé
     * @param fileId
     * @param versionNumber
     * @return
     */
    private static String key(long fileId, int versionNumber){
        return fileId + ":v" + versionNumber;
    }

    //encapsulation Service/Task lié à cette écran

    /**
     * Service JavaFX encapsulant l’upload d’une nouvelle version en tâche de fond
     */
    private static class UploadVersionService extends Service<Void> {

        private final ApiClient api;
        private final int fileId;
        private final File selectedFile;

        UploadVersionService(ApiClient api, int fileId, File selectedFile) {
            this.api = api;
            this.fileId = fileId;
            this.selectedFile = selectedFile;
        }

        @Override
        /**
         * Crée la tâche d’upload avec suivi de progression et mise à jour des messages d’état
         */
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() throws Exception {
                    updateMessage("Upload en cours...");
                    updateProgress(-1, 1); // indeterminate au début

                    api.uploadNewVersion(fileId, selectedFile, (sent, total) -> {
                        if (isCancelled()) return;
                        if (total > 0) updateProgress(sent, total);
                        else updateProgress(-1, 1);
                    });

                    updateProgress(1, 1);
                    updateMessage("Upload terminé");
                    return null;
                }
            };
        }
    }


}
