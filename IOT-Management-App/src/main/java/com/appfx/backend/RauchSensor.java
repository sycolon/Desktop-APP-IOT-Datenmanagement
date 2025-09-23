package com.appfx.backend;

public class RauchSensor extends Sensor {

    // eigenes neues Attribut
    private boolean rauch;


    // Konstruktor
    public RauchSensor(String topic, String timestamp,String name, boolean rauch) {
        super(topic, timestamp, name);
        this.rauch = rauch;
    }
    // getter und setter Methoden
    public boolean getRauch() {
        return rauch;
    }

    public void setRauch(boolean rauch) {
        this.rauch = rauch;
    }

    @Override
    public String toString() {
        return "RauchSensor{" +
                "topic=" + getTopic() +      // falls Sensor getter hat
                ", timestamp=" + getTimestamp() +
                ", name=" + getName() +
                ", Rauchentwicklung=" + rauch +
                '}';
    }
}