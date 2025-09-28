package com.example.gutnutritionmanager;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FoodDao {
    @Insert
    void insert(Food food);

    // You'll need to add this method to your FoodDao
    @Query("SELECT * FROM foods WHERE name = :name LIMIT 1")
    Food getFoodByName(String name);

    @Update
    void update(Food food);
    // New queries for FODMAP
    @Query("SELECT * FROM foods WHERE fodmap_status = :status ORDER BY name")
    LiveData<List<Food>> getFoodsByFodmapStatus(String status);

    @Query("SELECT * FROM foods WHERE LOWER(name) LIKE LOWER(:query) AND fodmap_status = :status ORDER BY name")
    LiveData<List<Food>> searchFoodsByFodmapStatus(String query, String status);
    // Get all foods
    @Query("SELECT * FROM foods")
    LiveData<List<Food>> getAllFoods();

    // Count low FODMAP foods
    @Query("SELECT COUNT(*) FROM foods WHERE fodmap_status = 'LOW'")
    int getLowFodmapCount();

    @Query("SELECT * FROM foods WHERE LOWER(name) LIKE LOWER(:query) ORDER BY common DESC, name ASC")
    LiveData<List<Food>> searchFoods(String query);

    @Query("SELECT * FROM foods WHERE common = 1 ORDER BY name ASC")
    LiveData<List<Food>> getCommonFoods();

    @Query("DELETE FROM foods")
    void deleteAll();
    @Query("SELECT * FROM foods WHERE LOWER(name) LIKE LOWER(:query) AND fodmap_status = 'LOW'")
    LiveData<List<Food>> searchLowFodmapFoods(String query);
    @Query("SELECT COUNT(*) FROM foods")
    int getFoodCount();

    @Query("SELECT * FROM foods LIMIT :limit")
    List<Food> getSampleFoods(int limit);




}


