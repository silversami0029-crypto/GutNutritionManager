package com.example.gutnutritionmanager;

import java.util.ArrayList;
import java.util.List;

// FODMAPDataInitializer.java
public class FODMAPDataInitializer {
    public static List<Food> getCommonFodmapFoods() {
        List<Food> foods = new ArrayList<>();

        // Low FODMAP foods
        foods.add(new Food(0, "Banana", "Fruit", true, "LOW", "",
                89.0, 1.1, 22.8, 0.3, 2.6, 12.2, null));
        foods.add(new Food(0, "Blueberries",  "Fruit", true, "LOW", "",
                89.0, 1.1, 22.8, 0.3, 2.6, 12.2, null));
        foods.add(new Food(0, "Carrot", "Vegetable", true, "LOW", "",
                89.0, 1.1, 22.8, 0.3, 2.6, 12.2, null));

        foods.add(new Food(0, "Cucumber", "Vegetable", true, "LOW", "",
                89.0, 1.1, 22.8, 0.3, 2.6, 12.2, null));

        foods.add(new Food(0, "Egg", "Protein", true, "LOW", "",
                89.0, 1.1, 22.8, 0.3, 2.6, 12.2, null));

        foods.add(new Food(0, "Chicken", "Protein",true, "LOW", "",
                89.0, 1.1, 22.8, 0.3, 2.6, 12.2, null));

        foods.add(new Food(0, "Rice", "Grain", true, "LOW", "",
                89.0, 1.1, 22.8, 0.3, 2.6, 12.2, null));

        foods.add(new Food(0, "Potato", "Vegetable", true, "LOW", "",
                89.0, 1.1, 22.8, 0.3, 2.6, 12.2, null));


        // High FODMAP foods
        foods.add(new Food(0, "Onion", "Vegetable", true, "LOW", "",
                89.0, 1.1, 22.8, 0.3, 2.6, 12.2, null));

        foods.add(new Food(0, "Garlic", "Vegetable", true, "LOW", "",
                89.0, 1.1, 22.8, 0.3, 2.6, 12.2, null));

        foods.add(new Food(0, "Apple", "Fruit", true, "LOW", "",
                89.0, 1.1, 22.8, 0.3, 2.6, 12.2, null));

        foods.add(new Food(0, "Milk", "Dairy", true, "LOW", "",
                89.0, 1.1, 22.8, 0.3, 2.6, 12.2, null));

        foods.add(new Food(0, "Wheat", "Grain", true, "LOW", "",
                89.0, 1.1, 22.8, 0.3, 2.6, 12.2, null));

        foods.add(new Food(0, "Beans", "Protein", true, "LOW", "",
                89.0, 1.1, 22.8, 0.3, 2.6, 12.2, null));


        // Moderate FODMAP foods
        foods.add(new Food(0, "Avocado", "Fruit", true, "LOW", "",
                89.0, 1.1, 22.8, 0.3, 2.6, 12.2, null));

        foods.add(new Food(0, "Sweet potato", "Vegetable", true, "LOW", "",
                89.0, 1.1, 22.8, 0.3, 2.6, 12.2, null));



        return foods;
    }
}
