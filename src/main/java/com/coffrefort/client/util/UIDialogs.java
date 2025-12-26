package com.coffrefort.client.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;

public final class UIDialogs {

    private UIDialogs() {}


    /**
     * Afficher une information
     */
    public static void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        Label icon = new Label("i");
        icon.setStyle(
                "-fx-background-color: #980b0b;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-alignment: center;" +
                        "-fx-min-width: 28px;" +
                        "-fx-min-height: 28px;" +
                        "-fx-background-radius: 11px;" +
                        "-fx-font-size: 14px;"
        );
        alert.setGraphic(icon);

        styleOkButton(alert);

        alert.showAndWait();
    }


    /**
     * Afficher une erreur
     */
    public static void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        Label icon = new Label("!");
        icon.setStyle(
                "-fx-background-color: #980b0b;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-alignment: center;" +
                        "-fx-min-width: 28px;" +
                        "-fx-min-height: 28px;" +
                        "-fx-background-radius: 11px;" +
                        "-fx-font-size: 14px;"
        );
        alert.setGraphic(icon);

        styleOkButton(alert);

        alert.showAndWait();
    }

    private static void styleOkButton(Alert alert) {
        DialogPane pane = alert.getDialogPane();
        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.setStyle(
                    "-fx-background-color: #980b0b;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-cursor: hand;"
            );
        }
    }
}
