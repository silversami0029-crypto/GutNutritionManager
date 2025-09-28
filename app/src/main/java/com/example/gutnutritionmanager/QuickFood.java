package com.example.gutnutritionmanager;

public class QuickFood {
    private String emoji;
    private String name;
    private String fodmapStatus;
    private String defaultPreparation;

    public QuickFood(String emoji, String name, String fodmapStatus, String defaultPreparation) {
        this.emoji = emoji;
        this.name = name;
        this.fodmapStatus = fodmapStatus;
        this.defaultPreparation = defaultPreparation;
    }

    // Getters
    public String getEmoji() { return emoji; }
    public String getName() { return name; }
    public String getFodmapStatus() { return fodmapStatus; }
    public String getDefaultPreparation(){return defaultPreparation;}
}
