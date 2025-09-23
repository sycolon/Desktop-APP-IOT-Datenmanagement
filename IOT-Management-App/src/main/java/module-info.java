module com.appfx.iotmanagementapp {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.appfx.iotmanagementapp to javafx.fxml;
    exports com.appfx.iotmanagementapp;
}