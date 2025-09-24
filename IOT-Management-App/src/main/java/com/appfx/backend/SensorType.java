package com.appfx.backend;

public enum SensorType {
    TEMPERATURE("Temperature"),
    HUMIDITY("HumiditySensor"),
    LIGHT("Light"),
    SMOKE("Smoke"),
    MOVE("MoveSensor"),
    DOOR("DoorSensor");

    private final String label;
    SensorType(String label){ this.label = label; }
    public String label(){ return label; }
}
