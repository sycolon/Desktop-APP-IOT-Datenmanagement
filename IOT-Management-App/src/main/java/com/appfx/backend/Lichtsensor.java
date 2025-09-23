package com.appfx.backend;

public class Lichtsensor extends Sensor{

    // Attribute
    private boolean active;

    // Konstruktor
    public Lichtsensor(String topic, String timestamp, String name, boolean active) {
        super(topic, timestamp, name);
        this.active = active;
    }

    // getter/setter Methoden
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // Weitere Methoden
    @Override
    public String toString() {
        return "Lichtsensor{" +
                "topic=" + getTopic() +
                ", timestamp=" + getTimestamp() +
                ", name=" + getName() +
                ", active=" + isActive() +
                "}";
    } // toString()

} // Class Lichtsensor
