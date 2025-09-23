package com.appfx.backend;

public class Bewegungsmelder extends Sensor {

    // Attribute
    private boolean motionDetected;

    // Konstruktor
    public Bewegungsmelder(String topic, String timestamp, String name, boolean motionDetection) {
        super(topic, timestamp, name);
        this.motionDetected = motionDetection;
    }

    // getter/setter Methoden
    public boolean isMotionDetected() {
        return motionDetected;
    }

    public void setMotionDetected(boolean motionDetected) {
        this.motionDetected = motionDetected;
    }

    // Weitere Methoden
    @Override
    public String toString() {
        return "Lichtsensor{" +
                "topic=" + getTopic() +
                ", timestamp=" + getTimestamp() +
                ", name=" + getName() +
                ", motionDetected=" + isMotionDetected() +
                "}";
    } // toString()

} // Class Bewegungsmelder
