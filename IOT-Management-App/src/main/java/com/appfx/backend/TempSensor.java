package com.appfx.backend;

public class TempSensor extends Sensor {

    // eigenes neues Attribut
    private double temperatur;


    // Konstruktor
    public TempSensor(String topic, String timestamp,String name, double temperatur) {
        super(topic, timestamp,name);
        this.temperatur = temperatur;
    }
    // getter und setter Methoden
    public double getTemperatur() {
        return temperatur;
    }

    public void setTemperatur(double temperatur) {
        this.temperatur = temperatur;
    }

    @Override
    public String toString() {
        return "TempSensor{" +
                "topic=" + getTopic() +      // falls Sensor getter hat
                ", timestamp=" + getTimestamp() +
                ", timestamp=" + getName() +
                ", temperatur=" + temperatur +
                '}';
    }
}
