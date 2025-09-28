package com.example.gutnutritionmanager;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gutnutritionmanager.api.ApiClient;
import com.example.gutnutritionmanager.api.USDAApiService;
import com.example.gutnutritionmanager.api.USDAFood;
import com.example.gutnutritionmanager.api.USDAFoodSearchResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class USDARepository {
    private Application application;
    private USDAApiService usdaApiService;

    public USDARepository(Application application) {
        this.application = application;
        this.usdaApiService = ApiClient.getApiService();
    }

    public LiveData<List<Food>> searchFoods(String query) {
        MutableLiveData<List<Food>> result = new MutableLiveData<>();

        if (query == null || query.trim().isEmpty()) {
            result.setValue(new ArrayList<>());
            return result;
        }

        Log.d("USDARepository", "Searching USDA for: " + query);

        // Make real USDA API call
        usdaApiService.searchFoods(
                ApiClient.API_KEY,
                query,
                25,  // pageSize
                1    // pageNumber
        ).enqueue(new Callback<USDAFoodSearchResponse>() {
            @Override
            public void onResponse(Call<USDAFoodSearchResponse> call, Response<USDAFoodSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    USDAFoodSearchResponse searchResponse = response.body();
                    List<USDAFood> usdaFoods = searchResponse.getFoods();

                    if (usdaFoods != null && !usdaFoods.isEmpty()) {
                        List<Food> classifiedFoods = convertAndClassifyUSDAFoods(usdaFoods);
                        result.setValue(classifiedFoods);
                        Log.d("USDARepository", "USDA API success: " + classifiedFoods.size() + " foods found");
                    } else {
                        result.setValue(new ArrayList<>());
                        Log.d("USDARepository", "USDA API: No foods found");
                    }
                } else {
                    result.setValue(new ArrayList<>());
                    Log.e("USDARepository", "USDA API error: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<USDAFoodSearchResponse> call, Throwable t) {
                result.setValue(new ArrayList<>());
                Log.e("USDARepository", "USDA API failure: " + t.getMessage());

                // Fallback to mock data for testing
                List<Food> mockResults = createMockUSDAFoods(query);
                result.setValue(mockResults);
                Log.d("USDARepository", "Using mock data due to API failure");
            }
        });

        return result;
    }

    private List<Food> convertAndClassifyUSDAFoods(List<USDAFood> usdaFoods) {
        List<Food> classifiedFoods = new ArrayList<>();

        for (USDAFood usdaFood : usdaFoods) {
            try {
                Food food = convertToFood(usdaFood);

                // Auto-classify FODMAP status
                String fodmapStatus = FODMAPClassifier.classifyFood(
                        food.getName(),
                        food.getCategory()
                );
                food.setFodmapStatus(fodmapStatus);

                // Calculate and store confidence
                double confidence = FODMAPClassifier.getConfidence(food.getName(), food.getCategory());
                food.setNutrientData("Confidence: " + (int)(confidence * 100) + "%");

                classifiedFoods.add(food);
                Log.d("USDARepository", "Classified: " + food.getName() + " as " + fodmapStatus);
            } catch (Exception e) {
                Log.e("USDARepository", "Error converting USDA food: " + e.getMessage());
            }
        }

        return classifiedFoods;
    }

    private Food convertToFood(USDAFood usdaFood) {
        Food food = new Food();

        // Use getter methods
        food.setName(usdaFood.getDescription());
        food.setUsdaId(String.valueOf(usdaFood.getFdcId()));
        food.setCategory(determineCategory(usdaFood));
        food.setCommon(false);

        // Extract nutrients using getter
        if (usdaFood.getFoodNutrients() != null) {
            extractNutrients(food, usdaFood.getFoodNutrients());
        }

        return food;
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

    // Fallback mock data for testing
    private List<Food> createMockUSDAFoods(String query) {
        List<Food> mockFoods = new ArrayList<>();

        if (query.toLowerCase().contains("chicken")) {
            mockFoods.add(createMockFood("Chicken breast", "Protein", "451234"));
            mockFoods.add(createMockFood("Chicken thigh", "Protein", "451235"));
            mockFoods.add(createMockFood("Chicken soup", "Processed", "451236"));
        } else if (query.toLowerCase().contains("apple")) {
            mockFoods.add(createMockFood("Apple", "Fruit", "451237"));
            mockFoods.add(createMockFood("Apple juice", "Beverage", "451238"));
        } else if (query.toLowerCase().contains("onion")) {
            mockFoods.add(createMockFood("Onion", "Vegetable", "451240"));
            mockFoods.add(createMockFood("Onion soup", "Processed", "451241"));
        } else if (query.toLowerCase().contains("rice")) {
            mockFoods.add(createMockFood("White rice", "Grain", "451242"));
            mockFoods.add(createMockFood("Brown rice", "Grain", "451243"));
        } else {
            mockFoods.add(createMockFood(query + " product", "Food", "451244"));
        }

        return classifyUSDAFoods(mockFoods);
    }

    private List<Food> classifyUSDAFoods(List<Food> foods) {
        List<Food> classified = new ArrayList<>();
        for (Food food : foods) {
            String fodmapStatus = FODMAPClassifier.classifyFood(food.getName(), food.getCategory());
            food.setFodmapStatus(fodmapStatus);
            classified.add(food);
        }
        return classified;
    }

    private Food createMockFood(String name, String category, String usdaId) {
        Food food = new Food();
        food.setName(name);
        food.setCategory(category);
        food.setUsdaId(usdaId);
        food.setCommon(false);

        // Mock nutrient data
        food.setCalories(150.0);
        food.setProtein(20.0);
        food.setCarbs(5.0);
        food.setFat(8.0);
        food.setFiber(2.0);
        food.setSugar(3.0);

        return food;
    }

    public void testUSDAAPI() {
        searchFoods("chicken").observeForever(foods -> {
            Log.d("USDARepository", "API Test - Found: " + foods.size() + " foods");
            for (Food food : foods) {
                Log.d("USDARepository", " - " + food.getName() + " | FODMAP: " + food.getFodmapStatus());
            }
        });
    }
}