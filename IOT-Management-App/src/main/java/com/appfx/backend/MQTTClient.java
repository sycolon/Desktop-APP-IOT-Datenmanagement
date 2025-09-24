package com.appfx.backend;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Einfacher MQTT-Client-Wrapper (Eclipse Paho, Async).
 * - connect(host, port, user, pass)
 * - publish(topic, payload, qos, retain)
 * - subscribe(topic, qos)
 * - unsubscribe(topic)
 * - disconnect()
 * Optional: setOnMessage((topic, payload) -> {...}),
 *           setOnConnectionLost(cause -> {...})
 *
 * Hinweis: Aufrufe wie connect()/publish() blockieren kurz (waitForCompletion).
 * Bitte NICHT im JavaFX-UI-Thread aufrufen, sondern in einem Hintergrund-Thread.
 */
public class MQTTClient {

    private MqttAsyncClient client;
    private final String clientId = "iotapp-" + UUID.randomUUID();

    private BiConsumer<String, String> onMessage;   // (topic, payload)
    private Consumer<Throwable> onConnectionLost;   // Verbindungsverlust

    /** Verbindung herstellen. Wirft MqttException bei Fehler. */
    public synchronized void connect(String host, int port, String username, String password) throws MqttException {
        if (host == null || host.isBlank()) throw new IllegalArgumentException("Host darf nicht leer sein");
        if (port <= 0) throw new IllegalArgumentException("Port muss > 0 sein");
        if (client != null && client.isConnected()) return;

        String brokerUrl = "tcp://" + host.trim() + ":" + port;
        client = new MqttAsyncClient(brokerUrl, clientId);

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(true);
        opts.setKeepAliveInterval(60);

        if (username != null && !username.isBlank()) opts.setUserName(username);
        if (password != null && !password.isBlank()) opts.setPassword(password.toCharArray());

        // Last Will (falls Client wegbricht)
        String lwtTopic = clientId + "/status";
        opts.setWill(lwtTopic, "offline".getBytes(StandardCharsets.UTF_8), 0, true);

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                if (onConnectionLost != null) onConnectionLost.accept(cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                if (onMessage != null) onMessage.accept(topic, payload);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // optional: Logging/Metriken
            }
        });

        client.connect(opts).waitForCompletion();

        // Online-Status setzen (retain)
        MqttMessage online = new MqttMessage("online".getBytes(StandardCharsets.UTF_8));
        online.setQos(0);
        online.setRetained(true);
        client.publish(lwtTopic, online).waitForCompletion();
    }

    /** Veröffentlichen. */
    public synchronized void publish(String topic, String payload, int qos, boolean retain) throws MqttException {
        ensureConnected();
        if (topic == null || topic.isBlank()) throw new IllegalArgumentException("Topic darf nicht leer sein");
        if (qos < 0 || qos > 2) qos = 0;

        MqttMessage msg = new MqttMessage(payload == null ? new byte[0] : payload.getBytes(StandardCharsets.UTF_8));
        msg.setQos(qos);
        msg.setRetained(retain);
        client.publish(topic, msg).waitForCompletion();
    }

    /** Abonnieren. */
    public synchronized void subscribe(String topic, int qos) throws MqttException {
        ensureConnected();
        if (topic == null || topic.isBlank()) throw new IllegalArgumentException("Topic darf nicht leer sein");
        if (qos < 0 || qos > 2) qos = 0;
        client.subscribe(topic, qos).waitForCompletion();
    }

    /** Abo aufheben. */
    public synchronized void unsubscribe(String topic) throws MqttException {
        ensureConnected();
        client.unsubscribe(topic).waitForCompletion();
    }

    /** Verbindung sauber schließen. */
    public synchronized void disconnect() {
        try {
            if (client != null) {
                if (client.isConnected()) client.disconnect().waitForCompletion(2000);
                client.close();
            }
        } catch (Exception ignored) {
        } finally {
            client = null;
        }
    }

    public synchronized boolean isConnected() {
        return client != null && client.isConnected();
    }

    public void setOnMessage(BiConsumer<String, String> onMessage) {
        this.onMessage = onMessage;
    }

    public void setOnConnectionLost(Consumer<Throwable> onConnectionLost) {
        this.onConnectionLost = onConnectionLost;
    }

    private void ensureConnected() throws MqttException {
        if (client == null || !client.isConnected()) {
            throw new MqttException(new Exception("MQTT nicht verbunden"));
        }
    }


}
