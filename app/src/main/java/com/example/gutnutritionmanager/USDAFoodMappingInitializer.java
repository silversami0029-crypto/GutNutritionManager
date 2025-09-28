package com.example.gutnutritionmanager;

import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class USDAFoodMappingInitializer {
    public static void initializeData(AppDatabase database) {
        USDAFoodMappingDao dao = database.usdaFoodMappingDao();

        // Common USDA food mappings
        List<USDAFoodMapping> initialMappings = Arrays.asList(
                new USDAFoodMapping("BUSH'S BAKED BEANS WITH ONION 16 OZ", "Onion"),
                new USDAFoodMapping("BUSH'S BAKED BEANS WITH ONION 28 OZ", "Onion"),
                new USDAFoodMapping("BUSH'S BAKED BEANS WITH ONION 12-16 OZ", "Onion"),
                new USDAFoodMapping("BUSH'S BAKED BEANS WITH ONION 12-28 OZ", "Onion"),
                new USDAFoodMapping("APPLE", "Apple"),
                new USDAFoodMapping("APPLE JUICE", "Apple"),
                new USDAFoodMapping("APPLE CIDER", "Apple"),
                new USDAFoodMapping("BANANA", "Banana"),
                new USDAFoodMapping("BANANA CHIPS", "Banana"),
                new USDAFoodMapping("BROCCOLI", "Broccoli"),
                new USDAFoodMapping("BROCCOLI FLORETS", "Broccoli"),
                new USDAFoodMapping("CAULIFLOWER", "Cauliflower"),
                new USDAFoodMapping("CAULIFLOWER RICE", "Cauliflower"),
                new USDAFoodMapping("MILK", "Milk"),
                new USDAFoodMapping("WHOLE MILK", "Milk"),
                new USDAFoodMapping("SKIM MILK", "Milk"),
                new USDAFoodMapping("CHEESE", "Cheese"),
                new USDAFoodMapping("CHEDDAR CHEESE", "Cheese"),
                new USDAFoodMapping("YOGURT", "Yogurt"),
                new USDAFoodMapping("GREEK YOGURT", "Yogurt"),
                new USDAFoodMapping("HONEY", "Honey"),
                new USDAFoodMapping("PURE HONEY", "Honey"),
                new USDAFoodMapping("CARROT", "Carrot"),
                new USDAFoodMapping("BABY CARROTS", "Carrot"),
                new USDAFoodMapping("CHOCOLATE", "Chocolate"),
                new USDAFoodMapping("MILK CHOCOLATE", "Chocolate"),
                new USDAFoodMapping("COFFEE", "Coffee"),
                new USDAFoodMapping("BREWED COFFEE", "Coffee"),
                new USDAFoodMapping("TEA", "Tea"),
                new USDAFoodMapping("GREEN TEA", "Tea")
        );

        for (USDAFoodMapping mapping : initialMappings) {
            dao.insert(mapping);
        }

        Log.d("USDAFoodMappingInitializer", "Initialized " + initialMappings.size() + " food mappings");
    }
}
