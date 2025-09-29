package com.example.gutnutritionmanager;

import java.util.Date;

public class FoodLog {
    private String foodName;
    private String preparation;
    private String mealType;
    private String fodmapStatus;
    private Date timestamp;
    private String mood; // NEW: Add mood field
    private int stressLevel; // NEW: Add stress level (1-5)

    public FoodLog(String foodName, String preparation, String mealType, String fodmapStatus, Date timestamp, String mood, int stressLevel) {
        this.foodName = foodName;
        this.preparation = preparation;
        this.mealType = mealType;
        this.fodmapStatus = fodmapStatus;
        this.mood =  mood;
        this.stressLevel =  stressLevel;
        this.timestamp = new Date();
    }

    // Getters
    public String getFoodName() { return foodName; }
    public String getPreparation() { return preparation; }
    public String getMealType() { return mealType; }
    public String getFodmapStatus() { return fodmapStatus; }
    public Date getTimestamp() { return timestamp; }
    public String getMood() { return mood; }
    public int getStressLevel() { return stressLevel; }
}
