package com.appfx.backend;

public class TempSensor extends Sensor {

    // eigenes neues Attribut
    private double temperatur;


    // Konstruktor
    public TempSensor(String topic, String timestamp, double temperatur) {
        super(topic, timestamp);
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
                ", temperatur=" + temperatur +
                '}';
    }
}
