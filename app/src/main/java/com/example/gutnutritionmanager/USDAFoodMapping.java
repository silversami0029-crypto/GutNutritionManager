package com.example.gutnutritionmanager;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "usda_food_mapping")
public class USDAFoodMapping {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String usdaFoodName;
    public String adviceFoodName;
    public double confidence; // Add this field
    public String usdaCategory; // Add this field

    // Constructor for 2 parameters (existing)
    // Annotate the constructor you DON'T want Room to use with @Ignore
    @Ignore
    public USDAFoodMapping(String usdaFoodName, String adviceFoodName) {
        this.usdaFoodName = usdaFoodName;
        this.adviceFoodName = adviceFoodName;
        this.confidence = 0.5; // Default confidence
        this.usdaCategory = ""; // Default category
    }

    // Add constructor for 4 parameters
    public USDAFoodMapping(String usdaFoodName, String usdaCategory, String adviceFoodName, double confidence) {
        this.usdaFoodName = usdaFoodName;
        this.usdaCategory = usdaCategory;
        this.adviceFoodName = adviceFoodName;
        this.confidence = confidence;
    }

    // Getters and setters
    public int getId() { return id; }
    public String getUsdaFoodName() { return usdaFoodName; }
    public String getAdviceFoodName() { return adviceFoodName; }
    public double getConfidence() { return confidence; }
    public String getUsdaCategory() { return usdaCategory; }

    public void setId(int id) { this.id = id; }
    public void setUsdaFoodName(String usdaFoodName) { this.usdaFoodName = usdaFoodName; }
    public void setAdviceFoodName(String adviceFoodName) { this.adviceFoodName = adviceFoodName; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public void setUsdaCategory(String usdaCategory) { this.usdaCategory = usdaCategory; }
}