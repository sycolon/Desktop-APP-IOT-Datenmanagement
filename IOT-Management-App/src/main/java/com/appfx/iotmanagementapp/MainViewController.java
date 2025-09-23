package com.appfx.iotmanagementapp;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;


import java.io.IOException;
import java.util.Objects;


public class MainViewController {

    @FXML
    private Button btnAddRoom;

    @FXML
    private Button btnConnect;

    @FXML
    private TextField tfHost;

    @FXML
    private TextField tfPort;

    @FXML
    private void handleAddRoom(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/com/appfx/iotmanagementapp/addRoom.fxml"))
            );
            Parent root = loader.load();

            // (Optional) AddRoomController controller = loader.getController();
            // controller.init(...);

            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.setTitle("Raum hinzufügen");
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialog.initOwner(btnAddRoom.getScene().getWindow());
            dialog.setScene(new javafx.scene.Scene(root));
            dialog.show(); // oder showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            // new Alert(Alert.AlertType.ERROR, "Fenster konnte nicht geladen werden:\n" + e.getMessage()).showAndWait();
        }
    }
}