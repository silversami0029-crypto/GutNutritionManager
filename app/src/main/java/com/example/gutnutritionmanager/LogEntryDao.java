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
    long insert(LogEntry logEntry);
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

    // Get all symptoms ordered by timestamp (newest first)
    @Query("SELECT * FROM symptoms ORDER BY timestamp DESC")
    LiveData<List<Symptom>> getAllSymptoms();

    // Get symptoms by date range
    @Query("SELECT * FROM symptoms WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    LiveData<List<Symptom>> getSymptomsByDateRange(long startTime, long endTime);

    // Get symptoms count
    @Query("SELECT COUNT(*) FROM symptoms")
    int getSymptomCount();

    // Get symptoms for today
    @Query("SELECT * FROM symptoms WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    LiveData<List<Symptom>> getTodaySymptoms(long startOfDay);

    // Get symptoms by severity
    @Query("SELECT * FROM symptoms WHERE severity = :severityLevel ORDER BY timestamp DESC")
    LiveData<List<Symptom>> getSymptomsBySeverity(int severityLevel);

    // Delete a symptom
    @Query("DELETE FROM symptoms WHERE id = :symptomId")
    void deleteSymptom(int symptomId);

    // Update a symptom
    @Query("UPDATE symptoms SET name = :name, severity = :severity WHERE id = :symptomId")
    void updateSymptom(int symptomId, String name, int severity);

    @Query("SELECT * FROM symptoms")
    List<Symptom> getAllSymptomsDirect();

    @Query("SELECT le.*, s.name as symptom_name, s.severity as symptom_severity " +
            "FROM log_entries le " +
            "LEFT JOIN symptoms s ON le.id = s.logEntryId " +
            "ORDER BY le.timestamp DESC")
    List<LogEntryWithSymptoms> getAllEntriesWithSymptomsDirect();

}

