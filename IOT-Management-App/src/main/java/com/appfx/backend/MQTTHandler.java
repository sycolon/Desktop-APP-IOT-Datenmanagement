package com.appfx.backend;

import java.util.HashMap;
import java.util.Map;

public class MQTTHandler {

    private final MQTTClient mqttClient;

    // Map: Raum -> Sensorname -> Sensorobjekt
    private final Map<String, Map<String, Sensor>> sensorMap = new HashMap<>();

    public MQTTHandler() {
        mqttClient = new MQTTClient();

        // MQTT-Nachrichten weiterleiten
        mqttClient.setOnMessage(this::handleMessage);

        mqttClient.setOnConnectionLost(cause ->
                System.err.println("Verbindung verloren: " + cause.getMessage()));
    }

    /** Verbindung herstellen und alle Sensoren abonnieren */
    public void connect() throws Exception {
        mqttClient.connect("broker.hivemq.com", 1883, null, null);
        mqttClient.subscribe("home/sensor/+/+", 0);
    }

    /** Verarbeitung der eingehenden MQTT-Nachrichten */
    private void handleMessage(String topic, String payload) {
        String[] parts = topic.split("/");

        if (parts.length == 4) {
            String raum = parts[2];
            String sensorName = parts[3];

            String timestamp = extractJsonValue(payload, "timestamp");
            String valueStr = extractJsonValue(payload, "value");

            Sensor sensor = null;

            try {
                // Boolean-Sensoren
                if (sensorName.matches("movement|door-sensor|window-sensor|smoke-detektor|gas-sensor|light")) {
                    boolean value = Boolean.parseBoolean(valueStr);
                    switch (sensorName) {
                        case "movement" -> sensor = new Bewegungsmelder(topic, timestamp, sensorName, value);
                        case "door-sensor", "window-sensor" -> sensor = new Tuersensor(topic, timestamp, sensorName, value);
                        case "smoke-detektor", "gas-sensor" -> sensor = new RauchSensor(topic, timestamp, sensorName, value);
                        case "light" -> sensor = new Lichtsensor(topic, timestamp, sensorName, value);
                    }
                }
                // Numerische Sensoren
                else if (sensorName.matches("temperature|humidity|sound-level")) {
                    double value = Double.parseDouble(valueStr);
                    switch (sensorName) {
                        case "temperature" -> sensor = new TempSensor(topic, timestamp, sensorName, value);
                        case "humidity" -> sensor = new FeuchtichkeitsSensor(topic, timestamp, sensorName, value);
                        case "sound-level" -> sensor = new TempSensor(topic, timestamp, sensorName, value);
                    }
                } else {
                    System.err.println("Unbekannter Sensor: " + sensorName + " im Raum " + raum);
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Erstellen des Sensors: " + e.getMessage());
                return;
            }

            if (sensor != null) {
                // Sensor in Map speichern
                sensorMap.computeIfAbsent(raum, k -> new HashMap<>()).put(sensorName, sensor);
                System.out.println(sensor);
            }

        } else {
            // Fehlerfall, Topic unerwartet
            System.err.println("Unerwartetes Topic: " + topic);
        }
    }

    /** Hilfsmethode: Wert aus JSON-ähnlichem String extrahieren */
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*";
        int index = json.indexOf(pattern);
        if (index == -1) return "";

        index += pattern.length();
        char startChar = json.charAt(index);

        if (startChar == '"') { // String-Wert
            int endIndex = json.indexOf('"', index + 1);
            return json.substring(index + 1, endIndex);
        } else { // Zahl oder boolean
            int endIndex = json.indexOf(',', index);
            if (endIndex == -1) endIndex = json.indexOf('}', index);
            return json.substring(index, endIndex).trim();
        }
    }

    /** Zugriff auf die Sensoren pro Raum */
    public Map<String, Map<String, Sensor>> getSensorMap() {
        return sensorMap;
    }

}
