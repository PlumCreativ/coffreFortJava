package com.coffrefort.client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class RenameFolderController {

    @FXML private Label currentNameLabel;
    @FXML private TextField nameField;
    @FXML private Label errorLabel;
    @FXML private Button cancelButton;
    @FXML private Button confirmButton;

    private Stage dialogStage;
    private Consumer<String> onConfirm;

    public void setStage(Stage stage){
        this.dialogStage = stage;
    }

    public void setCurrentName(String name){
        currentNameLabel.setText(name);
        nameField.setText(name);
        nameField.requestFocus();
        nameField.selectAll();
    }

    public void setOnConfirm(Consumer<String> callback){
        this.onConfirm = callback;
    }

    @FXML
    private void handleCancel(){
        if(dialogStage != null){
            dialogStage.close();
        }
    }

    @FXML
    private void handleConfirm(){
        String newName = nameField.getText() == null ? "" : nameField.getText().trim();

        if(newName.isBlank()){
            showError("Le nom ne peut pas être vide");
            return;
        }

        hideError();

        if(onConfirm != null){
            onConfirm.accept(newName);
        }

        if(dialogStage != null){
            dialogStage.close();
        }
    }

    private void showError(String message){
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void hideError(){
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

}
