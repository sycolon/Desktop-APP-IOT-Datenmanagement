package com.appfx.iotmanagementapp;

import com.appfx.backend.Database;
import com.appfx.backend.MQTTClient;
import com.appfx.backend.MeasurementRepository;
import com.appfx.backend.RoomModel;
import com.appfx.backend.RoomRepository;
import com.appfx.backend.SensorType;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainViewController {

    // --- FXML-Elemente ---
    @FXML private Button btnConnect;
    @FXML private Button btnAddRoom;
    @FXML private TextField tfHost;
    @FXML private TextField tfPort;
    @FXML private TextField tfTopic;
    @FXML private GridPane gridRooms;
    @FXML private TextArea taLog;

    // --- State/Repos ---
    private final MQTTClient mqtt = new MQTTClient();
    private final ObservableList<RoomModel> rooms = FXCollections.observableArrayList();
    private final RoomRepository repo = new RoomRepository();
    private final MeasurementRepository mRepo = new MeasurementRepository();

    // --- Parser für die Zeilen der Textdatei ---
    // Beispielzeile:
    // Gesendet: Topic='home/sensor/wohnzimmer/temperature', Payload='{"timestamp": "...", "value": 24.4, "unit": "°C"}'
    // Gruppe 1 Topic zwischen Topic='....'
    //Gruppe 2  JSON-Payload zwischen PAyload='...'
    // Ein Regex (kurz für regulärer Ausdruck) ist eine Zeichenfolge, die als Muster dient, um Text zu durchsuchen
    // zu validieren, zu filtern oder zu manipulieren.
    // Es handelt sich um eine spezielle Syntax, die mithilfe von Zeichen und Regeln Muster
    // in Texten beschreibt, anstatt feste Wörter zu suchen.
    private static final Pattern LINE =
            Pattern.compile("^Gesendet:\\s*Topic='([^']+)',\\s*Payload='(\\{.*\\})'\\s*$");
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // --- Init ---DB wird Initialisiert (legt Tabellen an, füllt Typen
    @FXML
    private void initialize() {
        try {
            Database.init();                 // legt ~/.iotapp/homedb.db an bzw. initialisiert
            rooms.setAll(repo.findAll());    // Räume + zugeordnete Typen laden
            log("DB ready: ~/.iotapp/homedb.db");
        } catch (Exception e) {
            log("! DB init/load failed: " + e.getMessage());
        }

        if (tfHost.getText() == null || tfHost.getText().isBlank()) tfHost.setText("broker.hivemq.com");
        if (tfPort.getText() == null || tfPort.getText().isBlank()) tfPort.setText("1883");

        renderRooms();
    }

    // --- Connect / Disconnect ---
    @FXML
    private void handleConnect(ActionEvent event) {
        if (mqtt.isConnected()) {
            btnConnect.setDisable(true);
            log("… Disconnecting");
            CompletableFuture.runAsync(mqtt::disconnect)
                    .whenComplete((v, ex) -> Platform.runLater(() -> {
                        btnConnect.setText("Connect");
                        btnConnect.setDisable(false);
                        if (ex != null) log("✖ Disconnect failed: " + ex.getMessage());
                        else            log("✖ Disconnected");
                    }));
            return;
        }

        final String host = tfHost.getText() == null ? "" : tfHost.getText().trim();
        final String portStr = tfPort.getText() == null ? "" : tfPort.getText().trim();

        // --- DATEI-MODUS: Host beginnt mit file: oder classpath: → statt MQTT wird Datei importiert
        if (host.startsWith("file:") || host.startsWith("classpath:")) {
            btnConnect.setDisable(true);
            log("… Importing from " + host);

            CompletableFuture.runAsync(() -> {
                if (host.startsWith("file:")) {
                    Path p = Path.of(host.substring("file:".length()));
                    importFromFile(p);
                } else { // classpath:
                    String res = host.substring("classpath:".length());
                    if (!res.startsWith("/")) res = "/" + res;
                    importFromResource(res);
                }
            }).whenComplete((v, ex) -> Platform.runLater(() -> {
                btnConnect.setDisable(false);
                if (ex != null) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    log("✖ Import failed: " + cause.getMessage());
                    btnConnect.setText("Connect");
                } else {
                    log("✔ Import finished");
                    btnConnect.setText("Load again");
                }
            }));
            return; // normalen MQTT-Connect überspringen
        }

        // --- normaler MQTT-Connect (unverändert) ---
        if (host.isEmpty() || portStr.isEmpty()) { log("! Bitte Host und Port eingeben"); return; }

        final int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException nfe) {
            log("! Port muss eine Zahl sein (z. B. 1883)");
            return;
        }

        btnConnect.setDisable(true);
        log("… Connecting to " + host + ":" + port);

        CompletableFuture.runAsync(() -> {
            try {
                mqtt.connect(host, port, null, null);
                mqtt.setOnMessage((topic, payload) -> handleTelemetry(topic, payload));
                mqtt.subscribe("house/#", 0);         // nach Bedarf anpassen
                // mqtt.subscribe("demo/nedim/#", 0);

                // Abonniere deine Topics (nach Bedarf anpassen)
                if(isValidMqttTopic(tfTopic.getText())) {
                    System.out.println(isValidMqttTopic(tfTopic.getText()));
                    mqtt.subscribe(tfTopic.getText(), 0);
                } else {
//                    System.out.println("FEHLER AUSGABE ELSE");
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Topic error");
                        alert.setHeaderText(null);
                        alert.setContentText("Topic don't match the specifications");
                        alert.showAndWait();
                    });
                }
                        // Haus-Schema: house/<room>/<type>/<id>
//                mqtt.subscribe("demo/nedim/#", 0);    // optional: dein Demo-Topic

            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }).whenComplete((v, ex) -> Platform.runLater(() -> {
            btnConnect.setDisable(false);
            if (ex != null) {
                btnConnect.setText("Connect");
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                log("✖ Connect failed: " + cause.getMessage());
            } else {
                btnConnect.setText("Disconnect");
                log("✔ Connected to " + host + ":" + port);
            }
        }));
    }

    // --- Raum hinzufügen (Dialog) ---
    @FXML
    private void handleAddRoom(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/com/appfx/iotmanagementapp/addRoom.fxml"))
            );
            Parent root = loader.load();

            AddRoomController ctrl = loader.getController();
            ctrl.setOnSave(room -> {
                try {
                    repo.saveRoom(room);  // persistieren
                    rooms.add(room);      // UI
                    renderRooms();
                    log("Saved room '" + room.getName() + "' to DB");

                    // Optional: Auto-Subscribe für diesen Raum
                    if (mqtt.isConnected()) {
                        String base = "house/" + room.getName().toLowerCase().replace(' ', '-');
                        mqtt.subscribe(base + "/#", 0);
                        log("SUB " + base + "/#");
                    }
                } catch (Exception ex) {
                    log("! Save failed: " + ex.getMessage());
                }
            });

            Stage dialog = new Stage();
            dialog.setTitle("Raum hinzufügen");
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.initOwner(btnAddRoom.getScene().getWindow());
            dialog.setScene(new Scene(root));
            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            log("! Dialog konnte nicht geladen werden: " + e.getMessage());
        }
    }

    // --- Rendering ---
    /** Zeichnet die Räume in ein 2-Spalten-Grid. */
    private void renderRooms() {
        gridRooms.getChildren().clear();
        final int cols = 2;

        for (int i = 0; i < rooms.size(); i++) {
            int row = i / cols;
            int col = i % cols;

            RoomModel r = rooms.get(i);

            VBox cell = new VBox(4);
            cell.setPadding(new Insets(8));
            cell.setStyle("-fx-border-color: #b5b5b5; -fx-border-radius: 6; -fx-background-color: #fafafa;");

            Label name = new Label(r.getName());
            name.setStyle("-fx-font-weight: bold;");

            String sensorsText = r.getSensors().isEmpty()
                    ? "(keine Sensoren)"
                    : r.getSensors().stream().map(SensorType::label).sorted().reduce((a, b) -> a + ", " + b).orElse("");

            Label sensors = new Label(sensorsText);

            cell.getChildren().addAll(name, sensors);
            gridRooms.add(cell, col, row);
        }
    }

    // --- Telemetry & Logging ---
    /** Verarbeitet eingehende MQTT-/Import-Payloads und speichert Messwerte. */
    private void handleTelemetry(String topic, String payload) {
        try {
            // Erwartetes JSON:
            // {"id":"t-wohn-1","type":"temperature","value":22.4,"ts":1710000000000,"room":"wohnzimmer"}
            JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();

            String sensorIdStr = obj.has("id")   ? obj.get("id").getAsString()   : null;
            String typeStr     = obj.has("type") ? obj.get("type").getAsString() : null;
            Double value       = obj.has("value")? obj.get("value").getAsDouble(): null;
            Long ts            = obj.has("timestamp")   ? obj.get("timestamp").getAsLong()     : System.currentTimeMillis();
            String roomName    = obj.has("unit") ? obj.get("unit").getAsString() : null;

            // Fallbacks aus Topic: house/<room>/<type>/<id>
            if (roomName == null || typeStr == null || sensorIdStr == null) {
                String[] p = topic.split("/");
                if (p.length >= 4) {
                    if (roomName == null)    roomName = p[1];
                    if (typeStr == null)     typeStr  = p[2];
                    if (sensorIdStr == null) sensorIdStr = p[3];
                }
            }

            if (value == null) { log("! Telemetry ohne 'value' ignoriert: " + payload); return; }
            if (roomName == null || typeStr == null) {
                log("! Telemetry ohne room/type: topic=" + topic + " payload=" + payload);
                return;
            }

            SensorType type = mapType(typeStr);
            String roomNorm = roomName.trim().toLowerCase().replace(' ', '-');
            String sensorName = sensorIdStr; // darf null sein

            int sensorId = mRepo.saveFromTelemetry(roomNorm, type, sensorName, value, ts);
            log(String.format("💾 %s/%s [%s]=%.2f @%d (SensorID=%d)",
                    roomNorm, type.name(), sensorName, value, ts, sensorId));

        } catch (Exception e) {
            log("! parse/save error: " + e.getMessage() + " | " + payload);
        }
    }

    private SensorType mapType(String raw) {
        String t = raw.trim().toLowerCase();
        return switch (t) {
            case "temperature", "temp"          -> SensorType.TEMPERATURE;
            case "humidity", "humiditysensor"   -> SensorType.HUMIDITY;
            case "light", "lux"                 -> SensorType.LIGHT;
            case "smoke"                        -> SensorType.SMOKE;
            case "move", "motion", "movesensor" -> SensorType.MOVE;
            case "door", "doorsensor"           -> SensorType.DOOR;
            default -> throw new IllegalArgumentException("Unknown type: " + raw);
        };
    }

    /** Thread-sicheres UI-Log. */
    private void log(String s) {
        Platform.runLater(() -> {
            if (taLog == null) return;
            taLog.appendText(s + System.lineSeparator());
            taLog.setScrollTop(Double.MAX_VALUE);
        });
    }

    /** Optional von der Application beim Beenden aufrufen. */
    public void mqttDisconnect() {
        mqtt.disconnect();
        log("✖ Disconnected");
    }

    // --- Datei-Import (Resource oder File); ruft pro Zeile parseAndDispatchLine() auf ---
    private void importFromResource(String resourcePath) {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) { log("! Resource nicht gefunden: " + resourcePath); return; }
            try (var br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                br.lines().forEach(this::parseAndDispatchLine);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void importFromFile(Path path) {
        try (var lines = Files.lines(path, StandardCharsets.UTF_8)) {
            lines.forEach(this::parseAndDispatchLine);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    // Eine Zeile „Gesendet: …“ parsen und in die bestehende Pipeline geben
    private void parseAndDispatchLine(String line) {
        Matcher m = LINE.matcher(line);
        if (!m.find()) return; // ignoriert "Simulation gestartet." etc.

        String originalTopic = m.group(1); // z. B. home/sensor/wohnzimmer/temperature
        String originalJson  = m.group(2); // {"timestamp":"...","value":..., "unit":"..."}

        try {
            String[] p = originalTopic.split("/");
            if (p.length < 4) { log("! Ungültiges Topic: " + originalTopic); return; }
            String roomRaw = p[2];
            String typeRaw = p[3];

            JsonObject in = JsonParser.parseString(originalJson).getAsJsonObject();

            long ts;
            if (in.has("timestamp")) {
                LocalDateTime ldt = LocalDateTime.parse(in.get("timestamp").getAsString(), TS_FMT);
                ts = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } else {
                ts = System.currentTimeMillis();
            }

            double value;
            if (in.get("value").isJsonPrimitive() && in.get("value").getAsJsonPrimitive().isBoolean()) {
                value = in.get("value").getAsBoolean() ? 1.0 : 0.0;
            } else {
                value = in.get("value").getAsDouble();
            }

            // Typ-Namen aus Datei → interne Typen
            String mappedType = switch (typeRaw.toLowerCase()) {
                case "temperature"     -> "temperature";
                case "humidity"        -> "humidity";
                case "light"           -> "light";
                case "movement"        -> "move";
                case "smoke-detektor"  -> "smoke";
                case "door-sensor"     -> "door";
                default -> null; // window-sensor/sound-level aktuell ignoriert
            };
            if (mappedType == null) { log("↷ übersprungen (nicht unterstützt): " + originalTopic); return; }

            String room = roomRaw.trim().toLowerCase().replace(' ', '-');
            String id   = "sim-" + mappedType + "-" + room;

            // In das Schema bringen, das handleTelemetry erwartet
            String newTopic = "house/" + room + "/" + mappedType + "/" + id;

            JsonObject out = new JsonObject();
            out.addProperty("id",   id);
            out.addProperty("type", mappedType);
            out.addProperty("value", value);
            out.addProperty("ts", ts);
            out.addProperty("room", room);

            handleTelemetry(newTopic, out.toString());

        } catch (Exception ex) {
            log("! Import-Fehler: " + ex.getMessage() + " | " + originalTopic);
        }
    }

    public static boolean isValidMqttTopic(String topic) {
        if (topic == null || topic.isEmpty()) {
            return false;
        }

        // darf nicht mit Leerzeichen starten oder enden
        if (!topic.equals(topic.trim())) {
            return false;
        }

        // darf keine leeren Levels enthalten ("//")
        if (topic.contains("//")) {
            return false;
        }

        // '#' darf nur einmal vorkommen und nur am Ende als eigenes Level
        if (topic.contains("#")) {
            if (topic.chars().filter(ch -> ch == '#').count() > 1) {
                return false;
            }
            if (!(topic.equals("#") || topic.endsWith("/#"))) {
                return false;
            }
        }

        // '+' darf nur als eigenes Level vorkommen
        if (topic.contains("+")) {
            String[] parts = topic.split("/");
            for (String part : parts) {
                if (part.contains("+") && !part.equals("+")) {
                    return false;
                }
            }
        }

        return true;
    }
}
