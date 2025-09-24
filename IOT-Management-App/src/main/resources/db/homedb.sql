PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS Room (
    RoomID   INTEGER PRIMARY KEY AUTOINCREMENT,
    RoomName TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS Type (
    TypeID   INTEGER PRIMARY KEY AUTOINCREMENT,
    TypeName TEXT NOT NULL UNIQUE
);

-- Konkreter Sensor (optional mit Name/DeviceId)
CREATE TABLE IF NOT EXISTS Sensor (
    SensorID INTEGER PRIMARY KEY AUTOINCREMENT,
    RoomID   INTEGER NOT NULL,
    TypeID   INTEGER NOT NULL,
    Name     TEXT,           -- optional z.B. "t-wohn-1"
    UNIQUE(RoomID, TypeID, Name),
    FOREIGN KEY (RoomID) REFERENCES Room(RoomID) ON DELETE CASCADE,
    FOREIGN KEY (TypeID) REFERENCES Type(TypeID) ON DELETE CASCADE
    );

-- Messwerte
CREATE TABLE IF NOT EXISTS Measurement (
    MeasurementID INTEGER PRIMARY KEY AUTOINCREMENT,
    SensorID      INTEGER NOT NULL,
    Value         REAL NOT NULL,
    Zeit          INTEGER NOT NULL,  -- Unix epoch (ms oder s)
    FOREIGN KEY (SensorID) REFERENCES Sensor(SensorID) ON DELETE CASCADE
    );

INSERT OR IGNORE INTO Type(TypeName) VALUES
 ('Temperature'), ('HumiditySensor'), ('Light'),
 ('Smoke'), ('MoveSensor'), ('DoorSensor');
