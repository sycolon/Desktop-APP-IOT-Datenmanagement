package com.appfx.iotmanagementapp;


import com.appfx.backend.RoomModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class ShowRoomController {

    @FXML
    private Button btnClose;
    @FXML
    private void handleClose() {
        // Aktuelles Fenster über den Button holen
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }
    private RoomModel room;
    public void setRoom(RoomModel room) {
        this.room = room;
        System.out.println("Raum geöffnet: " + room.getName());
        // TODO: Sensoren in TableView laden
    }
}

