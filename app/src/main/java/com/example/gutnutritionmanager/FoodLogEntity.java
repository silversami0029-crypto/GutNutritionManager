package com.example.gutnutritionmanager;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import java.util.Date;


@Entity(tableName = "food_logs")  // ✅ SIMPLE VERSION
public class FoodLogEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int foodId;
    public String foodName;
    public String preparation;
    public String portionSize;
    public String mealType;
    public String fodmapStatus;
    public Date timestamp;
    private String mood; // NEW: Add mood field
    private int stressLevel; // NEW: Add stress level (1-5)

    public FoodLogEntity(int foodId, String foodName, String preparation, String portionSize,
                         String mealType, String fodmapStatus, Date timestamp, String mood, int stressLevel) {
        this.foodId = foodId;
        this.foodName = foodName;
        this.preparation = preparation;
        this.portionSize = portionSize;
        this.mealType = mealType;
        this.fodmapStatus = fodmapStatus;
        this.mood =  mood;
        this.stressLevel =  stressLevel;
        this.timestamp = timestamp;
    }

    // Getters
    public int getId() { return id; }
    public int getFoodId() { return foodId; }
    public String getFoodName() { return foodName; }
    public String getPreparation() { return preparation; }
    public String getPortionSize() { return portionSize; }
    public String getMealType() { return mealType; }

    public String getFodmapStatus() { return fodmapStatus; }
    public Date getTimestamp() { return timestamp; }
    public String getMood() { return mood; }
    public int getStressLevel() { return stressLevel; }
}