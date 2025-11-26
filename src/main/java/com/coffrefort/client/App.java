package com.coffrefort.client;

import com.coffrefort.client.controllers.LoginController;
import com.coffrefort.client.controllers.MainController;
import com.coffrefort.client.controllers.RegisterController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Mini client lourd JavaFX d'exemple pour le projet « Coffre‑fort numérique ».
 * Objectif pédagogique: fournir une base exécutable, simple à lire, sur laquelle
 * les étudiants peuvent s'appuyer pour intégrer de vrais appels REST.
 */
public class App extends Application {

    private final ApiClient apiClient = new ApiClient();


    //Connexion
    @Override
    public void start(Stage stage) {
        stage.setTitle("Coffre‑fort numérique — Mini client");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/coffrefort/client/login2.fxml"));

            // Controller factory pour injecter ApiClient et callback
            loader.setControllerFactory(type -> {
                if (type == LoginController.class) {
                    LoginController c = new LoginController();
                    c.setApiClient(apiClient);
                    c.setOnSuccess(() -> {

                        // Ouvre la fenêtre principale via FXML
                        openMainAndClose(stage);
                    });
                    return c;
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Parent root = loader.load();
            Scene scene = new Scene(root, 420, 600);
            stage.setTitle("Coffre-fort numérique — Connexion");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de charger login.fxml", e);
        }
    }


    //Inscription
    private void openRegister(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/coffrefort/client/register.fxml"));

            loader.setControllerFactory(type -> {
                if (type == RegisterController.class) {
                    RegisterController c = new RegisterController();
                    c.setApiClient(apiClient);

                    // Après inscription réussie → retour à l'écran de login
                    c.setOnRegisterSuccess(() -> start(stage));

                    // Clique sur "Se connecter" → retour à l'écran de login
                    c.setOnGoToLogin(() -> start(stage));

                    return c;
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Parent root = loader.load();
            Scene scene = new Scene(root, 420, 650);
            stage.setTitle("Coffre-fort numérique — Inscription");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de charger register.fxml", e);
        }
    }

    // accès au tableau de bord
    private void openMainAndClose(Stage loginStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/coffrefort/client/main.fxml"));
            loader.setControllerFactory(type -> {
                if (type == MainController.class) {
                    MainController c = new MainController();
                    c.setApiClient(apiClient);
                    return c;
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Parent root = loader.load();
            Stage mainStage = new Stage();
            mainStage.setTitle("Coffre‑fort — Espace personnel");
            mainStage.setScene(new Scene(root, 1024, 640));
            mainStage.show();

            // Fermer la fenêtre de login
            loginStage.close();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de charger main.fxml", e);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
