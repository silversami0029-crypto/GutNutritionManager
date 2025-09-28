package com.example.gutnutritionmanager;

import java.util.List;

// Track user milestones
public class AchievementTracker {
    public static String checkAchievements(List<LogEntry> entries, List<Symptom> symptoms) {
        if (entries.size() >= 7) return "1-Week Consistency";
        if (symptoms.size() >= 10) return "Symptom Tracker";
        if (entries.size() >= 30) return "1-Month Commitment";
        return null;
    }
}