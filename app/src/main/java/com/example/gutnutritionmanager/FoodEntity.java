package com.example.gutnutritionmanager;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "foods")
public class FoodEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String fodmapStatus; // "LOW", "HIGH", "MODERATE"
    public String category; // "Protein", "Vegetable", "Fruit", "Grain"
    public String emoji;
    public String commonPreparations; // "Raw,Cooked,Steamed"

    public FoodEntity(String name, String fodmapStatus, String category, String emoji, String commonPreparations) {
        this.name = name;
        this.fodmapStatus = fodmapStatus;
        this.category = category;
        this.emoji = emoji;
        this.commonPreparations = commonPreparations;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getFodmapStatus() { return fodmapStatus; }
    public String getCategory() { return category; }
    public String getEmoji() { return emoji; }
    public String getCommonPreparations() { return commonPreparations; }
}
