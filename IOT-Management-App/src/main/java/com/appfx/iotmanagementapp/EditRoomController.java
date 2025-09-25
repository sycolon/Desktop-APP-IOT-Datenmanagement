package com.appfx.iotmanagementapp;

import com.appfx.backend.RoomModel;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class EditRoomController {

    @FXML private ComboBox<RoomModel> cbRooms;

    private Consumer<RoomModel> onChoose;

    /** Räume in die ComboBox setzen. */
    public void setRooms(ObservableList<RoomModel> rooms) {
        cbRooms.setItems(rooms);

        // Anzeige: nur den Namen zeigen
        cbRooms.setCellFactory((ListView<RoomModel> lv) -> new ListCell<>() {
            @Override protected void updateItem(RoomModel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });
        cbRooms.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(RoomModel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });

        if (!rooms.isEmpty()) cbRooms.getSelectionModel().selectFirst();
    }

    /** Callback registrieren, wird bei Klick auf "Edit" mit dem gewählten Room aufgerufen. */
    public void setOnChoose(Consumer<RoomModel> onChoose) {
        this.onChoose = onChoose;
    }

    @FXML
    private void handleEdit() {
        RoomModel selected = cbRooms.getSelectionModel().getSelectedItem();
        if (selected == null) {
            // optional: nichts gewählt → einfach zu
            handleCancel();
            return;
        }
        if (onChoose != null) onChoose.accept(selected);
        close();
    }

    @FXML
    private void handleCancel() { close(); }

    private void close() {
        Stage s = (Stage) cbRooms.getScene().getWindow();
        s.close();
    }
}
