package com.appfx.backend;

public class Tuersensor extends Sensor{

    // Attribut
    private boolean doorOpen;

    // Konstruktor
    public Tuersensor(String topic, String timestamp, String name, boolean doorOpen) {
        super(topic, timestamp, name);
        this.doorOpen = doorOpen;
    }

    // getter/setter Methoden
    public boolean isDoorOpen() {
        return doorOpen;
    }

    public void setDoorOpen(boolean doorOpen) {
        this.doorOpen = doorOpen;
    }

    // Weitere Methoden
    @Override
    public String toString() {
        return "Lichtsensor{" +
                "topic=" + getTopic() +
                ", timestamp=" + getTimestamp() +
                ", name=" + getName() +
                ", doorOpen=" + isDoorOpen() +
                "}";
    } // toString()
}
