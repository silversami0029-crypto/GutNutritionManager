package com.example.gutnutritionmanager;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface USDAFoodMappingDao {
    @Insert
    void insert(USDAFoodMapping mapping);

    @Update
    void update(USDAFoodMapping mapping);

    @Query("SELECT * FROM usda_food_mapping") // Make sure table name matches
    List<USDAFoodMapping> getAllMappings();

    // Fix this query - make sure column name matches exactly
    @Query("SELECT adviceFoodName FROM usda_food_mapping WHERE usdaFoodName = :usdaName")
    String getAdviceFoodName(String usdaName);

    @Query("SELECT * FROM usda_food_mapping WHERE usdaFoodName = :usdaName")
    USDAFoodMapping getMapping(String usdaName);

    @Query("SELECT * FROM usda_food_mapping WHERE usdaFoodName LIKE :pattern")
    List<USDAFoodMapping> findMatchingUSDAFoods(String pattern);

    // Fix this query - make sure column name matches exactly
    @Query("SELECT * FROM usda_food_mapping WHERE adviceFoodName = :adviceName")
    List<USDAFoodMapping> findByAdviceFoodName(String adviceName);

    @Query("DELETE FROM usda_food_mapping WHERE usdaFoodName = :usdaName")
    void deleteByUsdaName(String usdaName);

    @Query("SELECT COUNT(*) FROM usda_food_mapping")
    int getMappingCount();
}