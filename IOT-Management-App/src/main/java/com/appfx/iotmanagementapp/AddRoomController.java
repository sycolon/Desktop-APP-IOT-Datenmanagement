package com.appfx.iotmanagementapp;

import com.appfx.backend.RoomModel;
import com.appfx.backend.SensorType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

public class AddRoomController {

    @FXML private TextField tfRoomName;
    @FXML private CheckBox cbTemperature, cbHumidity, cbLight, cbSmoke, cbMove, cbDoor;
    @FXML private Button btnAdd;

    private Consumer<RoomModel> onSave;

    public void setOnSave(Consumer<RoomModel> onSave) {
        this.onSave = onSave;
    }

    /** EDIT-Fall: nur vorbefüllen und Button-Text ändern – KEINE ID nötig */
    public void setExistingRoom(RoomModel room) {
        if (room == null) return;
        setInitialData(room.getName(), room.getSensors());
        setSaveButtonText("Save");
    }

    /** Kannst du auch fürs Adden nutzen (z. B. Defaults ankreuzen) */
    public void setInitialData(String roomName, Set<SensorType> sensors) {
        if (roomName != null) tfRoomName.setText(roomName);

        cbTemperature.setSelected(sensors != null && sensors.contains(SensorType.TEMPERATURE));
        cbHumidity.setSelected(   sensors != null && sensors.contains(SensorType.HUMIDITY));
        cbLight.setSelected(      sensors != null && sensors.contains(SensorType.LIGHT));
        cbSmoke.setSelected(      sensors != null && sensors.contains(SensorType.SMOKE));
        cbMove.setSelected(       sensors != null && sensors.contains(SensorType.MOVE));
        cbDoor.setSelected(       sensors != null && sensors.contains(SensorType.DOOR));
    }

    public void setSaveButtonText(String text) {
        if (btnAdd != null) btnAdd.setText(text);
    }

    @FXML
    private void handleAdd() {
        String name = tfRoomName.getText() == null ? "" : tfRoomName.getText().trim();
        if (name.isEmpty()) { tfRoomName.requestFocus(); return; }

        Set<SensorType> sensors = EnumSet.noneOf(SensorType.class);
        if (cbTemperature.isSelected()) sensors.add(SensorType.TEMPERATURE);
        if (cbHumidity.isSelected())    sensors.add(SensorType.HUMIDITY);
        if (cbLight.isSelected())       sensors.add(SensorType.LIGHT);
        if (cbSmoke.isSelected())       sensors.add(SensorType.SMOKE);
        if (cbMove.isSelected())        sensors.add(SensorType.MOVE);
        if (cbDoor.isSelected())        sensors.add(SensorType.DOOR);

        // Ohne ID zurückgeben – der MainViewController fügt die ID beim Update hinzu
        RoomModel room = new RoomModel(null, name, sensors);

        if (onSave != null) onSave.accept(room);
        close();
    }

    @FXML
    private void handleCancel() { close(); }

    private void close() {
        Stage s = (Stage) btnAdd.getScene().getWindow();
        s.close();
    }
}
