package com.example.gutnutritionmanager;


import android.util.Log;

import androidx.room.Query;

import java.util.Arrays;
import java.util.List;

public class FoodAdviceInitializer {
    public static void initializeData(AppDatabase database) {
        FoodAdviceDao dao = database.foodAdviceDao();

        Log.d("FoodAdviceInitializer", "Starting food advice initialization");

        // Clear existing data first (for development)
        dao.deleteAll();

         // Sample data
        List<FoodAdvice> adviceList = Arrays.asList(

                new FoodAdvice("Honey", "preparation",
                        "Avoid entirely, even in small amounts",
                        "Honey is high in fructose and should be avoided on low FODMAP diet",
                        "WORSE"),
                new FoodAdvice("Honey", "substitute",
                        "Use maple syrup or rice malt syrup",
                        "These are lower FODMAP alternatives to honey",
                        "BETTER"),
                new FoodAdvice("Honey", "substitute",
                        "Use golden syrup or glucose syrup",
                        "These syrups are low in FODMAPs",
                        "BETTER"),
                new FoodAdvice("Onion", "substitute", "Use garlic-infused oil",
                        "Fructans aren't oil-soluble, so garlic-infused oil is safe", "BETTER"),
                new FoodAdvice("Onion", "substitute", "Use chives or green onion tops",
                        "Green parts of alliums are lower in FODMAPs", "BETTER"),
                new FoodAdvice("Onion", "preparation", "Avoid entirely, even when cooked",
                        "Cooking doesn't reduce fructan content significantly", "WORSE"),
                new FoodAdvice("Garlic", "substitute", "Use garlic-infused oil",
                        "Fructans aren't oil-soluble, so garlic-infused oil is safe", "BETTER"),
                new FoodAdvice("Carrot", "preparation", "Eat cooked instead of raw",
                        "Cooking breaks down difficult-to-digest fibers", "BETTER"),
                new FoodAdvice("Carrot", "preparation", "Try pureed carrots",
                        "Pureeing makes carrots easier to digest", "BETTER"),
                new FoodAdvice("Apple", "substitute", "Try berries instead",
                        "Berries are generally low FODMAP in small portions", "BETTER"),
                new FoodAdvice("Milk", "substitute", "Use lactose-free milk",
                        "Lactose-free milk removes the problematic FODMAP", "BETTER"),
                new FoodAdvice("Broccoli", "preparation", "Eat the stalks instead of florets",
                        "Broccoli florets are high FODMAP, but stalks are low", "BETTER")
        );
        for (FoodAdvice advice : adviceList) {
            dao.insert(advice);
            Log.d("FoodAdviceInitializer", "Inserted advice: " + advice.foodName);
        }

        Log.d("FoodAdviceInitializer", "Food advice initialization completed");

        // Verify the data was inserted
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<FoodAdvice> allAdvice = dao.getAllAdviceDirect();
            Log.d("FoodAdviceInitializer", "Total advice items in DB: " + allAdvice.size());
        });
    }




}

