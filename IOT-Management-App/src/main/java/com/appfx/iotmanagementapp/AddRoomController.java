package com.appfx.iotmanagementapp;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddRoomController {
    @FXML private TextField tfRoomName;
    @FXML private Button btnAdd;

    @FXML
    private void handleAdd() {
        String name = tfRoomName.getText();
        // TODO: Validieren, an Main-Controller übergeben, speichern usw.
        close();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) btnAdd.getScene().getWindow();
        stage.close();
    }
}
