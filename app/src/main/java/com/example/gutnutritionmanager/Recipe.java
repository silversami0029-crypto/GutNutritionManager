package com.example.gutnutritionmanager;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recipes")
public class Recipe {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String description;
    public String ingredients; // JSON or comma-separated
    public String instructions;
    public String cookingTime;
    public String difficulty;
    public String category; // Breakfast, Lunch, Dinner, Snack
    public String fodmapLevel; // LOW, MODERATE, HIGH
    public String imageUrl;
    public int servings;
    public boolean isFavorite;

    public Recipe() {}

    public Recipe(String title, String description, String ingredients, String instructions,
                  String cookingTime, String difficulty, String category, String fodmapLevel,
                  String imageUrl, int servings) {
        this.title = title;
        this.description = description;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.cookingTime = cookingTime;
        this.difficulty = difficulty;
        this.category = category;
        this.fodmapLevel = fodmapLevel;
        this.imageUrl = imageUrl;
        this.servings = servings;
        this.isFavorite = false;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getCookingTime() { return cookingTime; }
    public void setCookingTime(String cookingTime) { this.cookingTime = cookingTime; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getFodmapLevel() { return fodmapLevel; }
    public void setFodmapLevel(String fodmapLevel) { this.fodmapLevel = fodmapLevel; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getServings() { return servings; }
    public void setServings(int servings) { this.servings = servings; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}