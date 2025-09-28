package com.example.gutnutritionmanager;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "food_advice")
public class FoodAdvice {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String foodName;
    public String adviceType; // "substitute" or "preparation"
    public String description;
    public String explanation;
    public String fodmapImpact; // "BETTER", "WORSE", "SAME"

    public FoodAdvice() {}

@Ignore
    public FoodAdvice(String foodName, String adviceType, String description,
                      String explanation, String fodmapImpact) {
        this.foodName = foodName;
        this.adviceType = adviceType;
        this.description = description;
        this.explanation = explanation;
        this.fodmapImpact = fodmapImpact;
    }
}
