package com.appfx.backend;

public abstract class Sensor {

    // Attribute
    private String topic;
    private String timestamp;
    private String name;

    // Konstruktor
    public Sensor(String topic, String timestamp, String name) {
        this.topic = topic;
        this.timestamp = timestamp;
        this.name = name;
    }

    // getter/setter Methoden
    public String getTopic() {
        return topic;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getName() {
        return name;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setName(String name) {
        this.name = name;
    }

} // Class Sensor
