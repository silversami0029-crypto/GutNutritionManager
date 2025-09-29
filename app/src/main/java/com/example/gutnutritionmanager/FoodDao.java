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
    @Insert
    void insert(FoodLogEntity foodLog);

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

    // Enhanced FODMAP lookup
    @Query("SELECT fodmap_status FROM foods WHERE LOWER(name) = LOWER(:foodName) LIMIT 1")
    String getFodmapStatusByName(String foodName);


    // Food Log methods
    @Query("SELECT * FROM food_logs WHERE date(timestamp/1000,'unixepoch') = date('now') ORDER BY timestamp DESC")
    LiveData<List<FoodLogEntity>> getTodayFoodLogs();

    @Query("SELECT * FROM food_logs ORDER BY timestamp DESC")
    LiveData<List<FoodLogEntity>> getAllFoodLogs();

    @Query("DELETE FROM food_logs WHERE id = :logId")
    void deleteFoodLog(int logId);

    @Update
    void updateFoodLog(FoodLogEntity foodLog);

    // symptom
    @Query("SELECT * FROM symptoms")
    List<Symptom> getAllSymptoms();

    @Query("SELECT s.* FROM symptoms s " +
            "INNER JOIN log_entries le ON s.logEntryId = le.id " +
            "WHERE date(le.timestamp/1000, 'unixepoch') = date('now')")
    List<Symptom> getTodaySymptoms();
// ADD THESE METHODS TO YOUR FoodDao:

    // Get food logs for a specific date
    @Query("SELECT * FROM food_logs WHERE date(timestamp/1000,'unixepoch') = date(:selectedDate/1000,'unixepoch') ORDER BY timestamp DESC")
    LiveData<List<FoodLogEntity>> getFoodLogsByDate(long selectedDate);

    // Get food logs for date range
    @Query("SELECT * FROM food_logs WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    LiveData<List<FoodLogEntity>> getFoodLogsByDateRange(long startDate, long endDate);

    // Get yesterday's food logs
    @Query("SELECT * FROM food_logs WHERE date(timestamp/1000,'unixepoch') = date('now', '-1 day') ORDER BY timestamp DESC")
    LiveData<List<FoodLogEntity>> getYesterdayFoodLogs();

    // Get last 7 days food logs
    @Query("SELECT * FROM food_logs WHERE date(timestamp/1000, 'unixepoch') >= date('now', '-7 days') ORDER BY timestamp DESC")
    LiveData<List<FoodLogEntity>> getLast7DaysFoodLogs();

    // Add these direct query methods (without LiveData)
    @Query("SELECT * FROM food_logs WHERE date(timestamp/1000, 'unixepoch') = date('now', '-1 day') ORDER BY timestamp DESC")
    List<FoodLogEntity> getYesterdayFoodLogsDirect();

    @Query("SELECT * FROM food_logs WHERE date(timestamp/1000, 'unixepoch') >= date('now', '-7 days') ORDER BY timestamp DESC")
    List<FoodLogEntity> getLast7DaysFoodLogsDirect();

    @Query("SELECT * FROM food_logs ORDER BY timestamp DESC")
    List<FoodLogEntity> getAllFoodLogsDirect();


}


