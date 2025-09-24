package com.appfx.backend;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.sql.*;

public final class Database {

    // Schreibbarer Pfad für die echte App-Datenbank
    private static final Path DB_PATH =
            Paths.get(System.getProperty("user.home"), ".iotapp", "homedb.db");
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    /** Beim App-Start einmal aufrufen. Legt Schema (Variante B) sicher an und füllt Type-Tabelle. */
    public static void init() throws SQLException {
        try { Files.createDirectories(DB_PATH.getParent()); }
        catch (IOException e) {
            throw new RuntimeException("DB-Verzeichnis konnte nicht erstellt werden: " + DB_PATH.getParent(), e);
        }

        // wenn es noch keine Datei gibt: optional Vorlage aus /db/homedb.db kopieren
        if (Files.notExists(DB_PATH)) {
            try (InputStream in = Database.class.getResourceAsStream("/db/homedb.db")) {
                if (in != null) {
                    Files.copy(in, DB_PATH, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    // ansonsten wird gleich unten das Schema frisch angelegt
                }
            } catch (IOException copyErr) {
                // wenn Kopie fehlschlägt, legen wir trotzdem unten frisch an
            }
        }

        try (Connection c = getConnection()) {
            try (Statement st = c.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");

                // --- Schema (Variante B) robust sicherstellen ---
                st.execute("""
                    CREATE TABLE IF NOT EXISTS Room (
                      RoomID   INTEGER PRIMARY KEY AUTOINCREMENT,
                      RoomName TEXT NOT NULL UNIQUE
                    );
                """);

                st.execute("""
                    CREATE TABLE IF NOT EXISTS Type (
                      TypeID   INTEGER PRIMARY KEY AUTOINCREMENT,
                      TypeName TEXT NOT NULL UNIQUE
                    );
                """);

                st.execute("""
                    CREATE TABLE IF NOT EXISTS Sensor (
                      SensorID INTEGER PRIMARY KEY AUTOINCREMENT,
                      RoomID   INTEGER NOT NULL,
                      TypeID   INTEGER NOT NULL,
                      Name     TEXT,
                      UNIQUE(RoomID, TypeID, Name),
                      FOREIGN KEY (RoomID) REFERENCES Room(RoomID) ON DELETE CASCADE,
                      FOREIGN KEY (TypeID) REFERENCES Type(TypeID) ON DELETE CASCADE
                    );
                """);

                st.execute("""
                    CREATE TABLE IF NOT EXISTS Measurement (
                      MeasurementID INTEGER PRIMARY KEY AUTOINCREMENT,
                      SensorID      INTEGER NOT NULL,
                      Value         REAL NOT NULL,
                      Zeit          INTEGER NOT NULL,
                      FOREIGN KEY (SensorID) REFERENCES Sensor(SensorID) ON DELETE CASCADE
                    );
                """);
            }

            // --- 6 Sensortypen befüllen (entspricht deinen SensorType-Labels) ---
            try (PreparedStatement ps =
                         c.prepareStatement("INSERT OR IGNORE INTO Type(TypeName) VALUES (?)")) {
                for (SensorType t : SensorType.values()) {
                    ps.setString(1, t.label());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }

    private Database() {}
}
