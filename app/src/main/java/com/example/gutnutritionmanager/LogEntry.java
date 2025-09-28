package com.example.gutnutritionmanager;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "log_entries")
public class LogEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public Date timestamp;
    public String foods;

    // Default constructor required by Room
    public LogEntry() {
    }
@Ignore
    public LogEntry(Date timestamp, String foods) {
        this.timestamp = timestamp;
        this.foods = foods;
    }
}