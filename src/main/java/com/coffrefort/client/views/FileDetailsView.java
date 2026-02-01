package com.coffrefort.client.views;

import com.coffrefort.client.model.VersionEntry;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.function.Consumer;

public class FileDetailsView {

    private final VBox root = new VBox();

    // En-tête
    private final Label fileNameLabel = new Label("NomDuFichier.ext");
    private final Label fileMetaLabel = new Label("0.0 MB • Modifié le 2025-01-01");
    private final Button replaceButton = new Button("Remplacer…");

    // Zone progression
    private final VBox progressBox = new VBox();
    private final Label uploadStatusLabel = new Label("Upload...");
    private final ProgressBar uploadProgressBar = new ProgressBar(0.0);
    private final Label errorLabel = new Label("Erreur");

    // Bloc versions
    private final Label versionsCountLabel = new Label("0 version(s)");
    private final TableView<VersionEntry> versionsTable = new TableView<>();
    private final TableColumn<VersionEntry, Integer> versionCol = new TableColumn<>("Version");
    private final TableColumn<VersionEntry, Long> sizeCol = new TableColumn<>("Taille");
    private final TableColumn<VersionEntry, String> dateCol = new TableColumn<>("Créée le");
    private final TableColumn<VersionEntry, String> checksumCol = new TableColumn<>("Checksum");

    // Boutons d'action
    private final Button copyChecksumButton = new Button("Copier checksum");
    private final Button openLocalFolderButton = new Button("Ouvrir dossier local");
    private final Button downloadVersionButton = new Button("Télécharger cette version");

    // Callbacks
    private Runnable onReplace;
    private Runnable onCopyChecksum;
    private Runnable onOpenLocalFolder;
    private Runnable onDownloadVersion;

    public FileDetailsView() {
        buildUi();
    }

    private void buildUi() {
        root.setPrefSize(720, 540);
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

        // ========== ZONE PROGRESSION ==========
        buildProgressBox();
        root.getChildren().add(progressBox);

        // ========== BLOC VERSIONS ==========
        HBox versionsHeader = buildVersionsHeader();
        root.getChildren().add(versionsHeader);

        buildVersionsTable();
        root.getChildren().add(versionsTable);

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        root.getChildren().add(spacer);

        // ========== ACTIONS VERSIONS ==========
        HBox actionsBox = buildActionsBox();
        root.getChildren().add(actionsBox);
    }

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        // Bande rouge avec icône
        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefWidth(48);
        iconBox.setStyle("-fx-background-color: #980b0b; -fx-background-radius: 8;");
        iconBox.setPadding(new Insets(10));

        Text icon = new Text("📄");
        icon.setFill(Color.WHITE);
        icon.setStyle("-fx-font-size: 24px;");
        iconBox.getChildren().add(icon);

        // Titre + sous-titre
        VBox titleBox = new VBox(4);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleBox, javafx.scene.layout.Priority.ALWAYS);

        Text title = new Text("Détail du fichier");
        title.setFill(Color.web("#980b0b"));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");

        fileNameLabel.setWrapText(true);
        fileNameLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 13px; -fx-font-weight: bold;");

        fileMetaLabel.setWrapText(true);
        fileMetaLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");

        titleBox.getChildren().addAll(title, fileNameLabel, fileMetaLabel);

        // Bouton remplacer
        replaceButton.setStyle("-fx-background-color: #980b0b; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8 18; -fx-font-weight: bold;");
        replaceButton.setFont(Font.font(12));
        replaceButton.setOnAction(e -> triggerReplace());

        DropShadow shadow = new DropShadow(10.0, Color.rgb(153, 11, 11, 0.35));
        replaceButton.setEffect(shadow);

        header.getChildren().addAll(iconBox, titleBox, replaceButton);
        return header;
    }

    private void buildProgressBox() {
        progressBox.setSpacing(8);
        progressBox.setVisible(false);
        progressBox.setManaged(false);

        uploadStatusLabel.setWrapText(true);
        uploadStatusLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");

        uploadProgressBar.setPrefWidth(660);
        uploadProgressBar.setPrefHeight(10);
        uploadProgressBar.setStyle("-fx-background-radius: 6;");

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setAlignment(Pos.CENTER);
        errorLabel.setPrefWidth(660);
        errorLabel.setWrapText(true);
        errorLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        errorLabel.setStyle("-fx-background-color: #ffe5e5; -fx-text-fill: #980b0b; -fx-font-weight: bold; -fx-padding: 8; -fx-background-radius: 6;");

        progressBox.getChildren().addAll(uploadStatusLabel, uploadProgressBar, errorLabel);
    }

    private HBox buildVersionsHeader() {
        HBox versionsHeader = new HBox(10);
        versionsHeader.setAlignment(Pos.CENTER_LEFT);

        Text versionsTitle = new Text("Historique des versions");
        versionsTitle.setFill(Color.web("#980b0b"));
        versionsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        versionsCountLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");

        versionsHeader.getChildren().addAll(versionsTitle, spacer, versionsCountLabel);
        return versionsHeader;
    }

    private void buildVersionsTable() {
        versionsTable.setPrefHeight(320);
        versionsTable.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #cccccc;");

        // Colonnes
        versionCol.setPrefWidth(90);
        versionCol.setCellValueFactory(new PropertyValueFactory<>("version"));

        sizeCol.setPrefWidth(120);
        sizeCol.setCellValueFactory(new PropertyValueFactory<>("size"));
        sizeCol.setCellFactory(col -> new TableCell<VersionEntry, Long>() {
            @Override
            protected void updateItem(Long size, boolean empty) {
                super.updateItem(size, empty);
                if (empty || size == null) {
                    setText(null);
                } else {
                    setText(formatSize(size));
                }
            }
        });

        dateCol.setPrefWidth(180);
        dateCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        checksumCol.setPrefWidth(270);
        checksumCol.setCellValueFactory(new PropertyValueFactory<>("checksumHex"));

        versionsTable.getColumns().addAll(versionCol, sizeCol, dateCol, checksumCol);

        // Placeholder
        VBox placeholder = new VBox(8);
        placeholder.setAlignment(Pos.CENTER);

        Text placeholderText1 = new Text("Aucune version disponible");
        placeholderText1.setFill(Color.web("#999999"));
        placeholderText1.setStyle("-fx-font-size: 13px;");

        Text placeholderText2 = new Text("Cliquez sur 'Remplacer…' pour créer une nouvelle version");
        placeholderText2.setFill(Color.web("#cccccc"));
        placeholderText2.setStyle("-fx-font-size: 12px;");

        placeholder.getChildren().addAll(placeholderText1, placeholderText2);
        versionsTable.setPlaceholder(placeholder);

        // Effet ombre
        DropShadow shadow = new DropShadow(2, 1, 1, Color.rgb(204, 204, 204, 0.30));
        versionsTable.setEffect(shadow);

        // Gestion de la sélection
        versionsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            copyChecksumButton.setDisable(!hasSelection);
            openLocalFolderButton.setDisable(!hasSelection);
            downloadVersionButton.setDisable(!hasSelection);

            if (hasSelection) {
                copyChecksumButton.setStyle("-fx-background-color: #980b0b; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8 16;");
                openLocalFolderButton.setStyle("-fx-background-color: #980b0b; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8 16;");
                downloadVersionButton.setStyle("-fx-background-color: #980b0b; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8 16;");
            } else {
                resetButtonStyles();
            }
        });
    }

    private HBox buildActionsBox() {
        HBox actionsBox = new HBox(12);
        actionsBox.setAlignment(Pos.CENTER);

        // Style par défaut (désactivé)
        copyChecksumButton.setDisable(true);
        openLocalFolderButton.setDisable(true);
        downloadVersionButton.setDisable(true);

        resetButtonStyles();

        copyChecksumButton.setFont(Font.font(12));
        openLocalFolderButton.setFont(Font.font(12));
        downloadVersionButton.setFont(Font.font(12));

        copyChecksumButton.setOnAction(e -> triggerCopyChecksum());
        openLocalFolderButton.setOnAction(e -> triggerOpenLocalFolder());
        downloadVersionButton.setOnAction(e -> triggerDownloadVersion());

        actionsBox.getChildren().addAll(copyChecksumButton, openLocalFolderButton, downloadVersionButton);
        return actionsBox;
    }

    private void resetButtonStyles() {
        String disabledStyle = "-fx-background-color: #cccccc; -fx-text-fill: #333333; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 8 16;";
        copyChecksumButton.setStyle(disabledStyle);
        openLocalFolderButton.setStyle(disabledStyle);
        downloadVersionButton.setStyle(disabledStyle);
    }

    // ========== TRIGGERS ==========

    private void triggerReplace() {
        if (onReplace != null) {
            onReplace.run();
        }
    }

    private void triggerCopyChecksum() {
        if (onCopyChecksum != null) {
            onCopyChecksum.run();
        }
    }

    private void triggerOpenLocalFolder() {
        if (onOpenLocalFolder != null) {
            onOpenLocalFolder.run();
        }
    }

    private void triggerDownloadVersion() {
        if (onDownloadVersion != null) {
            onDownloadVersion.run();
        }
    }

    // ========== SETTERS CALLBACKS ==========

    public void setOnReplace(Runnable onReplace) {
        this.onReplace = onReplace;
    }

    public void setOnCopyChecksum(Runnable onCopyChecksum) {
        this.onCopyChecksum = onCopyChecksum;
    }

    public void setOnOpenLocalFolder(Runnable onOpenLocalFolder) {
        this.onOpenLocalFolder = onOpenLocalFolder;
    }

    public void setOnDownloadVersion(Runnable onDownloadVersion) {
        this.onDownloadVersion = onDownloadVersion;
    }

    // ========== MÉTHODES PUBLIQUES ==========

    public void setFileName(String name) {
        fileNameLabel.setText(name != null ? name : "NomDuFichier.ext");
    }

    public void setFileMeta(String meta) {
        fileMetaLabel.setText(meta != null ? meta : "");
    }

    public void setVersionsCount(int count) {
        versionsCountLabel.setText(count + " version(s)");
    }

    public void showProgress(boolean visible) {
        progressBox.setVisible(visible);
        progressBox.setManaged(visible);
    }

    public void setProgress(double progress) {
        uploadProgressBar.setProgress(progress);
    }

    public void setUploadStatus(String status) {
        uploadStatusLabel.setText(status);
    }

    public void showError(String message) {
        if (message == null || message.isEmpty()) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        } else {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    public TableView<VersionEntry> getVersionsTable() {
        return versionsTable;
    }

    public VersionEntry getSelectedVersion() {
        return versionsTable.getSelectionModel().getSelectedItem();
    }

    public Node getRoot() {
        return root;
    }

    // ========== UTILITAIRES ==========

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}