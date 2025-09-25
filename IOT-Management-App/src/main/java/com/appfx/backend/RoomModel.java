package com.appfx.backend;

import java.util.EnumSet;
import java.util.Set;

public class RoomModel {
    private final String name;
    private final EnumSet<SensorType> sensors;

    public RoomModel(Object id, String name, Set<SensorType> sensors){
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Room name required");
        this.name = name.trim();
        this.sensors = sensors.isEmpty() ? EnumSet.noneOf(SensorType.class) : EnumSet.copyOf(sensors);
    }

    public String getName(){ return name; }
    public EnumSet<SensorType> getSensors(){ return EnumSet.copyOf(sensors); }

    @Override
    public String toString() {
        String sensorList = sensors.isEmpty() ? "keine Sensoren" :
                sensors.stream().map(SensorType::label).sorted().reduce((a,b) -> a + ", " + b).orElse("");
        return name + " (" + sensorList + ")";
    }

    public Object getId() {
    return null;}
}
