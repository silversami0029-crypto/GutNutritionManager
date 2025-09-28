package com.example.gutnutritionmanager;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LogEntryDao {
    // Insert methods
    @Insert
    void insert(LogEntry logEntry);
    @Insert
    void insert(Symptom symptom);
    @Insert
    void insertSymptom(Symptom symptom);
    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    LiveData<List<LogEntry>> getAllEntries();


    @Query("SELECT le.*, s.name as symptom_name, s.severity as symptom_severity " +
            "FROM log_entries le " +
            "LEFT JOIN symptoms s ON le.id = s.logEntryId " +
            "ORDER BY le.timestamp DESC")
    LiveData<List<LogEntryWithSymptoms>> getAllEntriesWithSymptoms();

    // Make sure this method exists




    // In your LogEntryDao
    @Query("SELECT le.*, s.name as symptom_name, s.severity as symptom_severity " +
            "FROM log_entries le " +
            "LEFT JOIN symptoms s ON le.id = s.logEntryId " +
            "WHERE le.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY le.timestamp DESC")
    LiveData<List<LogEntryWithSymptoms>> getEntriesWithSymptomsByDate(long startDate, long endDate);

// Make sure this class is defined

    @Query("SELECT COUNT(*) FROM log_entries")
    int getLogEntryCount();

}

