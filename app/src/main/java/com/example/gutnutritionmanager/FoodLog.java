package com.example.gutnutritionmanager;

import java.util.Date;

public class FoodLog {
    private String foodName;
    private String preparation;
    private String mealType;
    private String fodmapStatus;
    private Date timestamp;

    public FoodLog(String foodName, String preparation, String mealType, String fodmapStatus) {
        this.foodName = foodName;
        this.preparation = preparation;
        this.mealType = mealType;
        this.fodmapStatus = fodmapStatus;
        this.timestamp = new Date();
    }

    // Getters
    public String getFoodName() { return foodName; }
    public String getPreparation() { return preparation; }
    public String getMealType() { return mealType; }
    public String getFodmapStatus() { return fodmapStatus; }
    public Date getTimestamp() { return timestamp; }
}
