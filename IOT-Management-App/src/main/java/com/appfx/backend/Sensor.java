package com.appfx.backend;

public abstract class Sensor {

    // Attribute

    private String topic;
    private String timestamp;

    // Konstruktor

    public Sensor(String topic, String timestamp) {
        this.topic = topic;
        this.timestamp = timestamp;
    }

    // getter Methoden

    public String getTopic() {
        return topic;
    }

    public String getTimestamp() {
        return timestamp;
    }

    // setter Methoden

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

} // Class Sensor
