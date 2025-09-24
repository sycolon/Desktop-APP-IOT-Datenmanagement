module com.appfx.iotmanagementapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.eclipse.paho.client.mqttv3;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires com.google.gson;

    opens com.appfx.iotmanagementapp to javafx.fxml;
    exports com.appfx.iotmanagementapp;
    exports com.appfx.backend;

}