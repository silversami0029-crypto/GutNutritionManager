package com.example.gutnutritionmanager;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.List;

public class FoodAdviceRepository {
    private FoodAdviceDao foodAdviceDao;
    private LiveData<List<FoodAdvice>> allAdvice;

    public FoodAdviceRepository(Application application) {
        try {
            AppDatabase database = AppDatabase.getDatabase(application);
            foodAdviceDao = database.foodAdviceDao();

            Log.d("FoodAdviceRepository", "FoodAdviceDao initialized: " + (foodAdviceDao != null));

            allAdvice = foodAdviceDao.getAllAdvice();

            // Add debug logging to see if data exists
            allAdvice.observeForever(new Observer<List<FoodAdvice>>() {
                @Override
                public void onChanged(List<FoodAdvice> adviceList) {
                    Log.d("FoodAdviceRepository", "Advice data changed: " + adviceList.size() + " items");

                    // Test direct database access
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        try {
                            List<FoodAdvice> directAdvice = foodAdviceDao.getAllAdviceDirect();
                            Log.d("FoodAdviceRepository", "Direct database query: " + directAdvice.size() + " items");

                            if (directAdvice.isEmpty()) {
                                Log.d("FoodAdviceRepository", "Database is empty. Checking if initializer was called.");
                            }
                        } catch (Exception e) {
                            Log.e("FoodAdviceRepository", "Error querying database: " + e.getMessage());
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e("FoodAdviceRepository", "Error initializing repository: " + e.getMessage());
        }


    }

    public void insert(FoodAdvice advice) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            foodAdviceDao.insert(advice);
        });
    }

    public LiveData<List<FoodAdvice>> getAdviceForFood(String foodName, String adviceType) {
        Log.d("FoodAdviceRepository", "Getting advice for: " + foodName + ", type: " + adviceType);
        return foodAdviceDao.getAdviceForFood(foodName, adviceType);
    }

    public LiveData<List<FoodAdvice>> getAllAdviceForFood(String foodName) {
        return foodAdviceDao.getAllAdviceForFood(foodName);
    }

    public LiveData<List<FoodAdvice>> getAllAdvice() {
        return allAdvice;
    }
}