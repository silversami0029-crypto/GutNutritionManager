package com.example.gutnutritionmanager;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

// FoodAdviceDao.java
@Dao
public interface FoodAdviceDao {

    @Insert
    void insert(FoodAdvice advice);

    @Update
    void update(FoodAdvice advice);

    @Delete
    void delete(FoodAdvice advice);

    @Query("SELECT COUNT(*) FROM food_advice")
    int getAdviceCount();

    @Query("SELECT * FROM food_advice WHERE foodName = :foodName AND adviceType = :adviceType")
    LiveData<List<FoodAdvice>> getAdviceForFood(String foodName, String adviceType);

    @Query("SELECT * FROM food_advice WHERE foodName = :foodName")
    LiveData<List<FoodAdvice>> getAllAdviceForFood(String foodName);

    @Query("SELECT * FROM food_advice")
    List<FoodAdvice> getAllAdviceDirect();

    @Query("SELECT * FROM food_advice")
    LiveData<List<FoodAdvice>> getAllAdvice();

    // This method takes only ONE parameter
    @Query("SELECT * FROM food_advice WHERE foodName = :foodName")
    List<FoodAdvice> getAdviceForFoodDirect(String foodName);

    // ADD THIS METHOD - it takes TWO parameters
    @Query("SELECT * FROM food_advice WHERE foodName = :foodName AND adviceType = :adviceType")
    List<FoodAdvice> getAdviceForFoodDirect(String foodName, String adviceType);

    @Query("DELETE FROM food_advice")
    void deleteAll();
}


