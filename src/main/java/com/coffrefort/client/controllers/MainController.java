package com.coffrefort.client.controllers;

import com.coffrefort.client.ApiClient;
import com.coffrefort.client.model.FileEntry;
import com.coffrefort.client.model.NodeItem;
import com.coffrefort.client.model.Quota;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainController {
    @FXML private TreeView<NodeItem> treeView;
    @FXML private TableView<FileEntry> table;
    @FXML private TableColumn<FileEntry, String> nameCol;
    @FXML private TableColumn<FileEntry, Long> sizeCol;
    @FXML private TableColumn<FileEntry, String> dateCol;
    @FXML private ProgressBar quotaBar;
    @FXML private Label quotaLabel;

    private ApiClient apiClient;

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
        // si initialize() a déjà été appelé, on peut charger les données
        if (treeView != null) {
            loadData();
        }
    }

    @FXML
    private void initialize() {
        // Configuration des colonnes
        if (nameCol != null) {
            nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        }
        if (sizeCol != null) {
            sizeCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("size"));
            sizeCol.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Long size, boolean empty) {
                    super.updateItem(size, empty);
                    setText(empty || size == null ? null : humanSize(size));
                }
            });
        }
        if (dateCol != null) {
            dateCol.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getUpdatedAt().atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            ));
        }

        if (treeView != null) {
            treeView.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
                if (sel != null) refreshFiles(sel.getValue());
            });
        }

        // Charger les données si l'apiClient est déjà injecté
        if (apiClient != null) {
            loadData();
        }
    }

    private void loadData() {
        // Arbre
        List<NodeItem> roots = apiClient.listRoot();
        TreeItem<NodeItem> hiddenRoot = new TreeItem<>(NodeItem.folder("root"));
        for (NodeItem n : roots) {
            hiddenRoot.getChildren().add(buildTree(n));
        }
        treeView.setRoot(hiddenRoot);
        if (!hiddenRoot.getChildren().isEmpty()) {
            treeView.getSelectionModel().select(hiddenRoot.getChildren().get(0));
        }

        // Quota
        Quota q = apiClient.getQuota();
        quotaBar.setProgress(q.getUsageRatio());
        quotaLabel.setText(humanSize(q.getUsed()) + " / " + humanSize(q.getMax()));
    }

    private TreeItem<NodeItem> buildTree(NodeItem node) {
        TreeItem<NodeItem> ti = new TreeItem<>(node);
        for (NodeItem child : node.getChildren()) {
            ti.getChildren().add(buildTree(child));
        }
        ti.setExpanded(true);
        return ti;
    }

    private void refreshFiles(NodeItem node) {
        table.getItems().setAll(node.getFiles());
    }

    @FXML
    private void handleUpload() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier à envoyer (simulation)");
        File file = chooser.showOpenDialog(table.getScene().getWindow());
        if (file != null) {
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setHeaderText("Upload simulé");
            ok.setContentText("Fichier sélectionné: " + file.getName());
            ok.showAndWait();
        }
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " o";
        double v = bytes;
        String[] units = {"Ko", "Mo", "Go", "To"};
        int i = -1;
        while (v >= 1024 && i < units.length - 1) { v /= 1024.0; i++; }
        return new DecimalFormat("0.##").format(v) + " " + units[i];
    }
}
