package com.appfx.backend;

import java.sql.*;
import java.util.*;

public class RoomRepository {

    /** Sorgt dafür, dass die 6 Typen in der Type-Tabelle existieren. */
    public void ensureTypes() {
        String upsert = "INSERT OR IGNORE INTO Type(TypeName) VALUES (?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(upsert)) {
            for (SensorType t : SensorType.values()) {
                ps.setString(1, t.label());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("ensureTypes fehlgeschlagen", e);
        }
    }

    /** Lädt alle Räume inkl. Sensor-Typen. */
    public List<RoomModel> findAll() {
        ensureTypes();

        String sql = """
            SELECT r.RoomID, r.RoomName, t.TypeName
            FROM Room r
            LEFT JOIN Sensor s ON s.RoomID = r.RoomID
            LEFT JOIN Type t    ON t.TypeID = s.TypeID
            ORDER BY r.RoomID
        """;

        Map<Integer, String> rooms = new LinkedHashMap<>();
        Map<Integer, EnumSet<SensorType>> sensors = new HashMap<>();

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int roomId = rs.getInt("RoomID");
                String roomName = rs.getString("RoomName");
                rooms.putIfAbsent(roomId, roomName);

                String typeName = rs.getString("TypeName");
                if (typeName != null) {
                    SensorType st = fromLabel(typeName);
                    sensors.computeIfAbsent(roomId, k -> EnumSet.noneOf(SensorType.class)).add(st);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden der Räume", e);
        }

        List<RoomModel> result = new ArrayList<>();
        for (var entry : rooms.entrySet()) {
            int id = entry.getKey();
            result.add(new RoomModel(entry.getValue(), sensors.getOrDefault(id, EnumSet.noneOf(SensorType.class))));
        }
        return result;
    }

    /** Speichert einen Raum + zugehörige Sensor-Typen. */
    public void saveRoom(RoomModel room) {
        ensureTypes();

        String insertRoom = "INSERT INTO Room(RoomName) VALUES (?)";
        String selectType = "SELECT TypeID FROM Type WHERE TypeName = ?";
        String insertSensor = "INSERT INTO Sensor(RoomID, TypeID) VALUES (?, ?)";

        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);

            int roomId;
            try (PreparedStatement ps = c.prepareStatement(insertRoom, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, room.getName());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); roomId = keys.getInt(1); }
            }

            try (PreparedStatement psSel = c.prepareStatement(selectType);
                 PreparedStatement psIns = c.prepareStatement(insertSensor)) {
                for (SensorType t : room.getSensors()) {
                    psSel.setString(1, t.label());
                    try (ResultSet rs = psSel.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Type nicht gefunden: " + t.label());
                        int typeId = rs.getInt(1);
                        psIns.setInt(1, roomId);
                        psIns.setInt(2, typeId);
                        psIns.addBatch();
                    }
                }
                psIns.executeBatch();
            }

            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Speichern des Raums", e);
        }
    }

    private SensorType fromLabel(String label) {
        for (SensorType t : SensorType.values()) {
            if (t.label().equalsIgnoreCase(label)) return t;
        }
        // Fallback: versuchen Enum-Name
        return SensorType.valueOf(label.toUpperCase());
    }
}
