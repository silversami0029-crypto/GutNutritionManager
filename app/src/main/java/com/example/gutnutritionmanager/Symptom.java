package com.example.gutnutritionmanager;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(tableName = "symptoms",
        foreignKeys = @ForeignKey(entity = LogEntry.class,
                parentColumns = "id",
                childColumns = "logEntryId",
                onDelete = CASCADE),
        indices = {@Index("logEntryId")})
public class Symptom {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int logEntryId;
    public String name;
    public int severity;

    public Symptom() {
        // Default constructor required by Room
    }

    public Symptom(int logEntryId, String name, int severity) {
        this.logEntryId = logEntryId;
        this.name = name;
        this.severity = severity;
    }
}