package com.coffrefort.client;

import com.coffrefort.client.controllers.MainController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class MainView {

    private final BorderPane root;
    private final MainController controller;

    public MainView(ApiClient apiClient, String userEmail) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/coffrefort/client/main.fxml")
            );
            this.root = loader.load();
            this.controller = loader.getController();

            // Injecter ApiClient dans le contrôleur
            if (apiClient != null) {
                controller.setApiClient(apiClient);
            }

            // Injecter l'email (affiché dans userEmailLabel)
            if (userEmail != null && !userEmail.isBlank()) {
                controller.setUserEmail(userEmail);
            }

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du chargement de main.fxml", e);
        }
    }

    // Surcharge pratique si tu n'as pas encore l'email
    public MainView(ApiClient apiClient) {
        this(apiClient, null);
    }

    // Comme LoginView.getRoot()
    public Node getRoot() {
        return root;
    }

    // Accès au contrôleur, si besoin
    public MainController getController() {
        return controller;
    }
}