package com.example.gutnutritionmanager.api;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.gutnutritionmanager.AppDatabase;
import com.example.gutnutritionmanager.FODMAPClassifier;
import com.example.gutnutritionmanager.Food;
import com.example.gutnutritionmanager.FoodDao;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class USDARepository {
    private static final String USDA_API_KEY = "MHsqz8Z1PHUyeyhKO3qUXak458oyhZbft4dzUYBo";
    private USDAApiService service;
    private Context context;
    private Map<String, List<Food>> searchCache = new HashMap<>();
    private FoodDao foodDao;
    private final Context applicationContext;
    public String fodmapStatus;

    public USDARepository(Application application) {
        this.applicationContext = application.getApplicationContext();
        AppDatabase database = AppDatabase.getDatabase(application);
        this.foodDao = database.foodDao();
        this.service = ApiClient.getUSDAApiService();
    }

    public LiveData<List<Food>> getLowFodmapFoods() {
        return foodDao.getFoodsByFodmapStatus("LOW");
    }

    public LiveData<List<Food>> getCommonFoods() {
        return foodDao.getCommonFoods();
    }

    public LiveData<List<Food>> searchLowFodmapFoods(String query, boolean isLowFodmap) {
        if (isLowFodmap) {
            return foodDao.searchFoodsByFodmapStatus("%" + query + "%", "LOW");
        } else {
            return foodDao.searchFoods("%" + query + "%");
        }
    }

    public LiveData<List<Food>> searchFoods(String query) {
        MutableLiveData<List<Food>> liveData = new MutableLiveData<>();
        Log.d("USDARepository", "Searching for: " + query);

        // Check cache first
        if (searchCache.containsKey(query.toLowerCase())) {
            liveData.postValue(searchCache.get(query.toLowerCase()));
            return liveData;
        }

        // Check if device is online
        ConnectivityManager cm = (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            liveData.postValue(new ArrayList<>());
            return liveData;
        }

        Log.d("USDARepository", "Making API call to USDA");

        // Proceed with API call
        service.searchFoods(USDA_API_KEY, query, 25, 1).enqueue(new Callback<USDAFoodSearchResponse>() {
            @Override
            public void onResponse(Call<USDAFoodSearchResponse> call, Response<USDAFoodSearchResponse> response) {
                try {
                    // Log the full response for debugging
                    String rawResponse = new Gson().toJson(response.body());
                    Log.d("USDARepository", "Full response: " + rawResponse);
                } catch (Exception e) {
                    Log.e("USDARepository", "Error logging full response", e);
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<Food> foods = new ArrayList<>();
                    //List<USDAFood> foods = response.body().getFoods();
                    Log.d("USDARepository", "Raw response: " + response.body().toString());



                    for (USDAFood usdaFood : response.body().foods) {
                        // Extract nutritional information
                        double calories = 0;
                        double protein = 0;
                        double carbs = 0;
                        double fat = 0;
                        double fiber = 0;
                        double sugar = 0;

                        // Parse nutrients from the response
                        if (usdaFood.foodNutrients != null) {
                            for (USDAFood.FoodNutrient nutrient : usdaFood.foodNutrients) {
                                switch (nutrient.nutrientId) {
                                    case 1008: // Calories
                                        calories = nutrient.value;
                                        break;
                                    case 1003: // Protein
                                        protein = nutrient.value;
                                        break;
                                    case 1005: // Carbs
                                        carbs = nutrient.value;
                                        break;
                                    case 1004: // Fat
                                        fat = nutrient.value;
                                        break;
                                    case 1079: // Fiber
                                        fiber = nutrient.value;
                                        break;
                                    case 2000: // Sugar
                                        sugar = nutrient.value;
                                        break;
                                }
                            }
                        }

                        // Convert int fdcId to String for the Food constructor
                        String usdaId = String.valueOf(usdaFood.fdcId);

                        // Convert nutrient data to JSON string for full details
                        Gson gson = new Gson();
                        String nutrientJson = gson.toJson(usdaFood.foodNutrients);

                        // DETERMINE CATEGORY AND CLASSIFY FODMAP
                        String category = usdaFood.foodCategory != null ? usdaFood.foodCategory : "Unknown";
                        String fodmapStatus = FODMAPClassifier.classifyFood(usdaFood.description, category);

                        // Create food with nutritional information
                        Food food = new Food(
                                0, // id will be auto-generated by Room
                                usdaFood.description,
                                category,
                                false, // common
                                fodmapStatus, // ← NOW CLASSIFIED!
                                usdaId,  // USDA ID as String
                                calories,
                                protein,
                                carbs,
                                fat,
                                fiber,
                                sugar,
                                nutrientJson
                        );
                        foods.add(food);

                        // Log the created Food object with nutritional info
                        Log.d("USDARepository", "Created Food: " + food.name +
                                ", Calories: " + food.calories +
                                ", Protein: " + food.protein + "g" +
                                ", Carbs: " + food.carbs + "g" +
                                ", Fat: " + food.fat + "g");
                    }

                    Log.d("USDARepository", "API response: " + foods.size() + " items");

                    // Cache the results
                    searchCache.put(query.toLowerCase(), foods);
                    liveData.postValue(foods);
                } else {
                    Log.d("USDARepository", "API response unsuccessful: " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            Log.d("USDARepository", "Error body: " + response.errorBody().string());
                        } catch (IOException e) {
                            Log.e("USDARepository", "Error reading error body", e);
                        }
                    }
                    liveData.postValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<USDAFoodSearchResponse> call, Throwable t) {
                Log.e("USDARepository", "API call failed: " + t.getMessage());
                liveData.postValue(new ArrayList<>());
            }
        });

        return liveData;
    }

    public void testUSDAAPI() {
        Log.d("USDARepository", "Testing USDA API with simple query: apple");
        searchFoods("apple").observeForever(new Observer<List<Food>>() {
            @Override
            public void onChanged(List<Food> foods) {
                Log.d("USDARepository", "Test API result: " + foods.size() + " foods");
                for (Food food : foods) {
                    Log.d("USDARepository", "Test Food: " + food.name +
                            ", USDA ID: " + food.usdaId +
                            ", Calories: " + food.calories);
                }
            }
        });
    }

    private List<Food> convertAndClassifyUSDAFoods(List<USDAFood> usdaFoods) {
        List<Food> classifiedFoods = new ArrayList<>();

        Log.d("USDARepository", "=== STARTING FODMAP CLASSIFICATION ===");
        Log.d("USDARepository", "Processing " + usdaFoods.size() + " USDA foods");

        for (USDAFood usdaFood : usdaFoods) {
            try {
                Food food = convertToFood(usdaFood);

                String originalName = food.getName();
                String category = food.getCategory();

                // Auto-classify FODMAP status
                String fodmapStatus = FODMAPClassifier.classifyFood(originalName, category);
                food.setFodmapStatus(fodmapStatus);

                // DEBUG LOGGING
                Log.d("USDARepository", "CLASSIFIED: " + originalName +
                        " | Category: " + category +
                        " | FODMAP: " + fodmapStatus);

                classifiedFoods.add(food);
            } catch (Exception e) {
                Log.e("USDARepository", "Error converting USDA food: " + e.getMessage());
            }
        }

        Log.d("USDARepository", "=== COMPLETED FODMAP CLASSIFICATION ===");
        return classifiedFoods;
    }
    private Food convertToFood(USDAFood usdaFood) {
        Food food = new Food();

        // Set basic food information
        food.setName(usdaFood.getDescription());
        food.setUsdaId(String.valueOf(usdaFood.getFdcId()));
        food.setCategory(determineCategory(usdaFood));
        food.setCommon(false);

        // Set default values for other required fields
        food.setFodmapStatus("UNKNOWN"); // Will be classified later
        food.setCalories(0.0);
        food.setProtein(0.0);
        food.setCarbs(0.0);
        food.setFat(0.0);
        food.setFiber(0.0);
        food.setSugar(0.0);
        food.setNutrientData("");

        // Extract nutrients if available
        if (usdaFood.getFoodNutrients() != null) {
            extractNutrients(food, usdaFood.getFoodNutrients());
        }

        return food;
    }

    private void extractNutrients(Food food, List<USDAFood.FoodNutrient> nutrients) {
        for (USDAFood.FoodNutrient nutrient : nutrients) {
            String nutrientName = nutrient.getNutrientName();
            double value = nutrient.getValue();

            if (nutrientName != null) {
                switch (nutrientName.toLowerCase()) {
                    case "energy":
                    case "calories":
                        food.setCalories(value);
                        break;
                    case "protein":
                        food.setProtein(value);
                        break;
                    case "carbohydrate, by difference":
                    case "carbohydrates":
                    case "carbohydrate":
                        food.setCarbs(value);
                        break;
                    case "total lipid (fat)":
                    case "fat":
                    case "total fat":
                        food.setFat(value);
                        break;
                    case "fiber, total dietary":
                    case "fiber":
                    case "dietary fiber":
                        food.setFiber(value);
                        break;
                    case "sugars, total including nlea":
                    case "sugars":
                    case "sugar":
                        food.setSugar(value);
                        break;
                }
            }
        }
    }

        private String determineCategory(USDAFood usdaFood) {
        String description = usdaFood.getDescription();
        if (description == null) return "Other";

        String lowerDescription = description.toLowerCase();

        if (lowerDescription.contains("chicken") || lowerDescription.contains("beef") ||
                lowerDescription.contains("pork") || lowerDescription.contains("fish") ||
                lowerDescription.contains("meat") || lowerDescription.contains("steak")) {
            return "Protein";
        } else if (lowerDescription.contains("apple") || lowerDescription.contains("banana") ||
                lowerDescription.contains("orange") || lowerDescription.contains("berry") ||
                lowerDescription.contains("fruit") || lowerDescription.contains("grape")) {
            return "Fruit";
        } else if (lowerDescription.contains("carrot") || lowerDescription.contains("broccoli") ||
                lowerDescription.contains("spinach") || lowerDescription.contains("potato") ||
                lowerDescription.contains("vegetable") || lowerDescription.contains("lettuce") ||
                lowerDescription.contains("tomato") || lowerDescription.contains("onion")) {
            return "Vegetable";
        } else if (lowerDescription.contains("milk") || lowerDescription.contains("cheese") ||
                lowerDescription.contains("yogurt") || lowerDescription.contains("dairy") ||
                lowerDescription.contains("cream")) {
            return "Dairy";
        } else if (lowerDescription.contains("bread") || lowerDescription.contains("pasta") ||
                lowerDescription.contains("rice") || lowerDescription.contains("cereal") ||
                lowerDescription.contains("wheat") || lowerDescription.contains("oat")) {
            return "Grain";
        }

        return "Other";
    }



}