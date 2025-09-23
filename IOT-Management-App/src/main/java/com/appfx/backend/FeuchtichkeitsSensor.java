package com.appfx.backend;

public class FeuchtichkeitsSensor extends Sensor {

    // eigenes neues Attribut
    private double humidity;


    // Konstruktor
    public FeuchtichkeitsSensor(String topic, String timestamp,String name, double humidity) {
        super(topic, timestamp,name);
        this.humidity = humidity;
    }
    // getter und setter Methoden
    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    @Override
    public String toString() {
        return "Feuchte{" +
                "topic=" + getTopic() +      // falls Sensor getter hat
                ", timestamp=" + getTimestamp() +
                ", timestamp=" + getName() +
                ", Feuchte=" + humidity +
                '}';
    }
}
