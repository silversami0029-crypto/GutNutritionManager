package com.example.gutnutritionmanager;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

class LogEntryWithSymptoms {
    @Embedded
    public LogEntry logEntry;

    @ColumnInfo(name = "symptom_name")
    public String symptomName;

    @ColumnInfo(name = "symptom_severity")
    public int symptomSeverity;
}
