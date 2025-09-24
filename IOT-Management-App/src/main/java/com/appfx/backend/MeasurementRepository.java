package com.appfx.backend;

import java.sql.*;
import java.util.EnumSet;

public class MeasurementRepository {

    /** Sorgt dafür, dass der Raum existiert. Gibt RoomID zurück. */
    private int ensureRoom(Connection c, String roomName) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO Room(RoomName) VALUES (?)")) {
            ps.setString(1, roomName);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT RoomID FROM Room WHERE RoomName = ?")) {
            ps.setString(1, roomName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Room konnte nicht ermittelt werden: " + roomName);
    }

    /** Liefert TypeID aus Type-Tabelle. */
    private int getTypeId(Connection c, SensorType type) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT TypeID FROM Type WHERE TypeName = ?")) {
            ps.setString(1, type.label());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Type fehlt in Type-Tabelle: " + type.label());
    }

    /** Sorgt dafür, dass der konkrete Sensor (RoomID+TypeID+Name) existiert. Gibt SensorID zurück. */
    private int ensureSensor(Connection c, int roomId, int typeId, String sensorName) throws SQLException {
        // Name darf NULL sein – aber eindeutiger ist ein Name aus Telemetrie (z.B. "t-wohn-1")
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO Sensor(RoomID, TypeID, Name) VALUES (?, ?, ?)")) {
            ps.setInt(1, roomId);
            ps.setInt(2, typeId);
            ps.setString(3, sensorName);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT SensorID FROM Sensor WHERE RoomID=? AND TypeID=? AND Name IS ?")) {
            ps.setInt(1, roomId);
            ps.setInt(2, typeId);
            ps.setString(3, sensorName); // IS vergleicht NULL korrekt
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Sensor konnte nicht ermittelt werden (RoomID=" + roomId + ", TypeID=" + typeId + ", Name=" + sensorName + ")");
    }

    /** Messung speichern (epoch millis in `zeit`). */
    public void insertMeasurement(int sensorId, double value, long zeit) {
        String sql = "INSERT INTO Measurement(SensorID, Value, Zeit) VALUES (?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sensorId);
            ps.setDouble(2, value);
            ps.setLong(3, zeit);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Insert Measurement fehlgeschlagen", e);
        }
    }

    /** Convenience: Raum/Typ/SensorName sicherstellen und Messung speichern. */
    public int saveFromTelemetry(String roomName, SensorType type, String sensorName, double value, long zeit) {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            int roomId  = ensureRoom(c, roomName);
            int typeId  = getTypeId(c, type);
            int sensorId = ensureSensor(c, roomId, typeId, sensorName);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO Measurement(SensorID, Value, Zeit) VALUES (?, ?, ?)")) {
                ps.setInt(1, sensorId);
                ps.setDouble(2, value);
                ps.setLong(3, zeit);
                ps.executeUpdate();
            }
            c.commit();
            return sensorId;
        } catch (SQLException e) {
            throw new RuntimeException("saveFromTelemetry fehlgeschlagen", e);
        }
    }
}
