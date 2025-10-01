package com.example.gutnutritionmanager;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.gutnutritionmanager.api.USDARepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class NutritionViewModel extends AndroidViewModel {

    private LogEntryDao logEntryDao;
    private USDAFoodMappingRepository usdaFoodMappingRepository;
    private FoodDao foodDao; // Make sure this is declared

    private FoodAdviceDao foodAdviceDao;
    private LiveData<List<LogEntry>> allEntries;
    private LiveData<List<LogEntryWithSymptoms>> entriesWithSymptoms;
    private LiveData<List<Food>> commonFoods;
    private USDARepository usdaRepository;
    private FoodMatchingService foodMatchingService; // Add this
    private String usdaCategory; // Add this if you need to store category

    private Application application;
    private FoodAdviceRepository foodAdviceRepository;
    private LiveData<List<FoodAdvice>> foodAdvice;
    private USDAFoodMappingDao usdaFoodMappingDao; // Add this
    private final ExecutorService executorService; // Add this line


    private final ExecutorService databaseWriteExecutor = Executors.newSingleThreadExecutor();

    public NutritionViewModel(@NonNull Application application) {
        super(application);
        this.executorService = Executors.newSingleThreadExecutor(); // Initialize it
        try {
            AppDatabase db = AppDatabase.getDatabase(application);
            logEntryDao = db.logEntryDao();
            foodDao = db.foodDao();
            foodAdviceDao = db.foodAdviceDao();
            usdaFoodMappingDao = db.usdaFoodMappingDao(); // Add this

            this.foodMatchingService = new FoodMatchingService(usdaFoodMappingDao, foodAdviceDao);
            usdaRepository = new USDARepository(application);
            foodAdviceRepository = new FoodAdviceRepository(application);
            // Get the FoodDao from your database
            // Test the USDA API
            usdaRepository.testUSDAAPI();
            AppDatabase database = AppDatabase.getDatabase(application);
            FoodDao foodDao = database.foodDao();

            // Pass the FoodDao to the repository
            usdaRepository = new USDARepository(application);

            // Initialize LiveData objects
            allEntries = logEntryDao.getAllEntries();
            entriesWithSymptoms = logEntryDao.getAllEntriesWithSymptoms();
            commonFoods = foodDao.getCommonFoods(); // This should use foodDao, not logEntryDao

        } catch (Exception e) {
            Log.e("NutritionViewModel", "Error initializing ViewModel: " + e.getMessage());
            // Initialize with empty LiveData to prevent null references
            allEntries = new MutableLiveData<>(Collections.emptyList());
            commonFoods = new MutableLiveData<>(Collections.emptyList());
        }
    }

    public String findAdviceFoodName(String usdaFoodName) {
        // First try exact match in mapping table
        String adviceName = usdaFoodMappingRepository.getAdviceFoodName(usdaFoodName);

        if (adviceName != null) {
            Log.d("NutritionViewModel", "Found mapping: " + usdaFoodName + " -> " + adviceName);
            return adviceName;
        }

        // If no mapping found, use smart pattern matching
        return smartMatchFoodName(usdaFoodName);
    }
    private String smartMatchFoodName(String usdaFoodName) {
        // Common patterns for matching USDA names to simple food names
        Map<String, String> foodPatterns = new HashMap<>();
        foodPatterns.put("(?i).onion.", "Onion");
        foodPatterns.put("(?i).garlic.", "Garlic");
        foodPatterns.put("(?i).apple.", "Apple");
        foodPatterns.put("(?i).banana.", "Banana");
        foodPatterns.put("(?i).broccoli.", "Broccoli");
        foodPatterns.put("(?i).cauliflower.", "Cauliflower");
        foodPatterns.put("(?i).milk.", "Milk");
        foodPatterns.put("(?i).cheese.", "Cheese");
        foodPatterns.put("(?i).yogurt.", "Yogurt");
        foodPatterns.put("(?i).wheat.", "Wheat");
        foodPatterns.put("(?i).honey.", "Honey");
        foodPatterns.put("(?i).carrot.", "Carrot");
        foodPatterns.put("(?i).bean.", "Beans");
        foodPatterns.put("(?i).chocolate.", "Chocolate");
        foodPatterns.put("(?i).coffee.", "Coffee");
        foodPatterns.put("(?i).tea.", "Tea");

        // Try to match patterns
        for (Map.Entry<String, String> entry : foodPatterns.entrySet()) {
            if (usdaFoodName.matches(entry.getKey())) {
                String matchedName = entry.getValue();

                Log.d("NutritionViewModel", "Pattern matched: " + usdaFoodName + " -> " + matchedName);

                // Auto-create mapping for future use
                learnFoodMapping(usdaFoodName, matchedName);

                return matchedName;
            }
        }

        // Fallback: extract main food name
        return extractMainFoodName(usdaFoodName);
    }
    private String extractMainFoodName(String usdaFoodName) {
        // Simple extraction logic
        String[] words = usdaFoodName.split("\\s+");
        if (words.length > 0) {
            // Return the first word as the main food name
            String mainName = words[0];
            Log.d("NutritionViewModel", "Extracted main name: " + usdaFoodName + " -> " + mainName);
            return mainName;
        }

        return usdaFoodName; // Fallback to original name
    }
    public void learnFoodMapping(String usdaFoodName, String adviceFoodName) {
        executorService.execute(() -> {
            try {
                // Use the 4-parameter constructor with default values
                USDAFoodMapping mapping = new USDAFoodMapping(usdaFoodName, "", adviceFoodName, 0.5);
                usdaFoodMappingDao.insert(mapping);
                Log.d("NutritionViewModel", "Learned new mapping: " + usdaFoodName + " -> " + adviceFoodName);
            } catch (Exception e) {
                Log.e("NutritionViewModel", "Error learning mapping: " + e.getMessage());
            }
        });
    }

    public LiveData<List<FoodAdvice>> getAdviceForUSDAFood(String usdaFoodName, String usdaCategory, String adviceType) {
        MutableLiveData<List<FoodAdvice>> result = new MutableLiveData<>();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Find the best matching food name
            String adviceFoodName = foodMatchingService.findBestMatch(usdaFoodName, usdaCategory);

            Log.d("NutritionViewModel", "USDA: '" + usdaFoodName + "' -> Advice: '" + adviceFoodName + "'");

            // Get advice for the matched food name
            List<FoodAdvice> advice = foodAdviceDao.getAdviceForFoodDirect(adviceFoodName, adviceType);

            if (advice.isEmpty()) {
                // Fallback to general advice - now with correct parameters
                advice = getGeneralAdvice(usdaFoodName, adviceFoodName, adviceType);
            }

            result.postValue(advice);
        });

        return result;
    }

    private List<FoodAdvice> getGeneralAdvice(String usdaFoodName, String adviceFoodName, String adviceType) {
        List<FoodAdvice> generalAdvice = new ArrayList<>();

        String description = "No specific advice found for:\n\"" + usdaFoodName + "\"\n\n" +
                "Matched to: " + adviceFoodName + "\n\n" +
                "General tips:\n• Start with small portions\n• Cook thoroughly\n• Monitor symptoms";

        generalAdvice.add(new FoodAdvice(adviceFoodName, adviceType,
                "General Dietary Advice", description, "GENERAL"));

        return generalAdvice;
    }

    // Debug method to view all mappings
    public void debugMappings() {
        executorService.execute(() -> {
            List<USDAFoodMapping> mappings = usdaFoodMappingRepository.getAllMappings();
            Log.d("NutritionViewModel", "Total mappings: " + (mappings != null ? mappings.size() : 0));

            if (mappings != null) {
                for (USDAFoodMapping mapping : mappings) {
                    Log.d("NutritionViewModel", "Mapping: " + mapping.usdaFoodName + " -> " + mapping.adviceFoodName);
                }
            }
        });
    }






    public LiveData<List<FoodAdvice>> getAdviceForFood(String foodName, String adviceType) {
        Log.d("NutritionViewModel", "Getting advice for: " + foodName + ", type: " + adviceType);
        return foodAdviceDao.getAdviceForFood(foodName, adviceType);
    }

    public LiveData<List<FoodAdvice>> getAllAdviceForFood(String foodName) {
        return foodAdviceRepository.getAllAdviceForFood(foodName);
    }

    public void insertFoodAdvice(FoodAdvice advice) {
        foodAdviceRepository.insert(advice);
    }

    public void checkUSDAApi() {
        // Test the USDA API with a simple query
        LiveData<List<Food>> testLiveData = usdaRepository.searchFoods("apple");
        testLiveData.observeForever(new Observer<List<Food>>() {
            @Override
            public void onChanged(List<Food> foods) {
                Log.d("NutritionViewModel", "USDA API test completed. Found " + foods.size() + " items.");
                testLiveData.removeObserver(this);
            }
        });
    }

    public void testFoodAdviceDao() {
        if (foodAdviceDao == null) {
            Log.e("NutritionViewModel", "foodAdviceDao is still null!");
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Test with a simple insert
                FoodAdvice testAdvice = new FoodAdvice("Test", "substitute",
                        "Test advice", "This is a test", "BETTER");
                foodAdviceDao.insert(testAdvice);

                // Test retrieval
                List<FoodAdvice> allAdvice = foodAdviceDao.getAllAdviceDirect();
                Log.d("NutritionViewModel", "Test successful! Total advice: " + allAdvice.size());

            } catch (Exception e) {
                Log.e("NutritionViewModel", "Test failed: " + e.getMessage());
            }
        });
    }
    public void debugFoodAdvice(String foodName) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<FoodAdvice> allAdvice = foodAdviceDao.getAdviceForFoodDirect(foodName);
            Log.d("NutritionViewModel", "Debug - All advice for " + foodName + ": " + allAdvice.size());

            for (FoodAdvice advice : allAdvice) {
                Log.d("NutritionViewModel", "Debug - Advice: " + advice.foodName +
                        " | Type: " + advice.adviceType +
                        " | Desc: " + advice.description);
            }
        });


        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<FoodAdvice> allAdvice = foodAdviceDao.getAllAdviceDirect();
                Log.d("NutritionViewModel", "Total advice items: " + allAdvice.size());

                for (FoodAdvice advice : allAdvice) {
                    Log.d("NutritionViewModel", "Advice: " + advice.foodName +
                            " | Type: " + advice.adviceType +
                            " | Desc: " + advice.description);
                }
            } catch (Exception e) {
                Log.e("NutritionViewModel", "Error debugging food advice: " + e.getMessage());
            }

        });
    }

    public void debugDatabase() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Check food count
            int foodCount = foodDao.getFoodCount();
            Log.d("NutritionViewModel", "Foods in database: " + foodCount);

            // Check log entries count
            int logEntryCount = logEntryDao.getLogEntryCount();
            Log.d("NutritionViewModel", "Log entries in database: " + logEntryCount);

            // Get some sample foods
            List<Food> sampleFoods = foodDao.getSampleFoods(5);
            for (Food food : sampleFoods) {
                Log.d("NutritionViewModel", "Food: " + food.name + " | Category: " + food.category);
            }
        });
    }



    private MutableLiveData<List<Food>> usdaSearchResults = new MutableLiveData<>();



    // ... your existing methods

    public LiveData<List<Food>> getUsdaSearchResults() {
        return usdaSearchResults;
    }
/*
    public LiveData<List<Food>> searchUSDAFoods(String query) {
        return usdaRepository.searchFoods(query);
    }
*/

    // Add a method to get the search results
    public LiveData<List<Food>> getUSDAFoods() {
        return usdaSearchResults;
    }

    public void searchUSDAFoods(String query) {
        // Clear previous results
        usdaSearchResults.postValue(new ArrayList<>());

        // Perform search and observe the results
        LiveData<List<Food>> searchResults = usdaRepository.searchFoods(query);
        searchResults.observeForever(new Observer<List<Food>>() {
            @Override
            public void onChanged(List<Food> foods) {
                // Post the results to our LiveData
                usdaSearchResults.postValue(foods);
                // Remove the observer to avoid memory leaks
                searchResults.removeObserver(this);
            }
        });
    }
    // In NutritionViewModel.java
    public void updateFoodFodmapStatus(String foodName, String status) {
        databaseWriteExecutor.execute(() -> {
            Food food = foodDao.getFoodByName(foodName);
            if (food != null) {
                food.setFodmapStatus(status);
                foodDao.update(food);
            }
        });
    }

    public LiveData<List<Food>> getLowFodmapFoods() {
        return foodDao.getFoodsByFodmapStatus("LOW");
    }

    public LiveData<List<Food>> searchLowFodmapFoods(String query) {
        return foodDao.searchFoodsByFodmapStatus("%" + query + "%", "LOW");
    }

    public void importUSDAFood(Food food) {
        // Try to classify FODMAP status based on food name
        String fodmapStatus = classifyFodmapStatus(food.name);
        food.fodmapStatus = fodmapStatus;
        // The USDA ID should already be set in the food object
        insertFood(food);

    }
    private String classifyFodmapStatus(String foodName) {
        // Simple classification logic - you might want to make this more sophisticated
        if (foodName.toLowerCase().contains("onion") ||
                foodName.toLowerCase().contains("garlic") ||
                foodName.toLowerCase().contains("chicory") ||
                foodName.toLowerCase().contains("asparagus")) {
            return "HIGH";
        } else if (foodName.toLowerCase().contains("orange") ||
                foodName.toLowerCase().contains("banana") ||
                foodName.toLowerCase().contains("strawberry")) {
            return "LOW";
        } else {
            return "UNKNOWN";
        }
    }
    public LiveData<List<LogEntry>> getAllEntries() {
        if (allEntries == null) {
            // Return empty LiveData if null
            return new MutableLiveData<>(Collections.emptyList());
        }
        return allEntries;
    }
    public void checkFoodAdviceData() {
        if (foodAdviceDao == null) {
            Log.e("NutritionViewModel", "foodAdviceDao is null!");
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Check if any data exists
                int count = foodAdviceDao.getAdviceCount();
                Log.d("NutritionViewModel", "Food advice count: " + count);

                if (count == 0) {
                    Log.d("NutritionViewModel", "No food advice data found. Initializing now.");

                    // Manually initialize data
                    FoodAdviceInitializer.initializeData(AppDatabase.getDatabase(getApplication()));

                    // Check again
                    count = foodAdviceDao.getAdviceCount();
                    Log.d("NutritionViewModel", "Food advice count after initialization: " + count);
                }

                // List all advice
                List<FoodAdvice> allAdvice = foodAdviceDao.getAllAdviceDirect();
                for (FoodAdvice advice : allAdvice) {
                    Log.d("NutritionViewModel", "Advice: " + advice.foodName + " - " + advice.description);
                }

            } catch (Exception e) {
                Log.e("NutritionViewModel", "Error checking food advice data: " + e.getMessage());
            }
        });
    }


    public LiveData<List<Food>> getCommonFoods() {
        if (foodDao != null) {
            return foodDao.getCommonFoods();
        } else {
            return new MutableLiveData<>(Collections.emptyList());
        }
    }

    // Add these methods
    public LiveData<List<Food>> searchFoods(String query) {
        if (foodDao != null && query != null) {
            return foodDao.searchFoods("%" + query + "%");
        } else {
            return new MutableLiveData<>(Collections.emptyList());
        }
    }

    public void insertFood(Food food) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            foodDao.insert(food);
        });
    }


    // Make sure this method exists


    private LiveData<List<Food>> currentUSDAsearch;

    public LiveData<List<Food>> getCurrentUSDAsearch() {
        return currentUSDAsearch;
    }


    public void insertLogEntry(LogEntry entry) {
        databaseWriteExecutor.execute(() -> {
            logEntryDao.insert(entry);
        });
    }
    // Add this method
    public LiveData<List<LogEntryWithSymptoms>> getEntriesWithSymptoms() {
        return logEntryDao.getAllEntriesWithSymptoms();
    }


    public void insertSymptom(Symptom symptom) {
        databaseWriteExecutor.execute(() -> {
            logEntryDao.insertSymptom(symptom);
        });
    }
    public LiveData<List<Food>> getFoodsWithFodmapFilter(String query, boolean lowFodmapOnly) {

        if (lowFodmapOnly) {
            return usdaRepository.searchLowFodmapFoods(query, lowFodmapOnly);
        } else {
            return usdaRepository.searchFoods(query);
        }
    }
    // Add a debug method to your NutritionViewModel


    public LiveData<List<Food>> getCommonFoodsWithFilter(boolean fodmapOnly) {
        Log.d("NutritionViewModel", "Getting common foods with filter - Low FODMAP Only: " + fodmapOnly);
        // For empty queries, search for common staples
        return searchUSDAFoodsWithFilter("chicken rice beef egg carrot potato", fodmapOnly);

    }
    private MutableLiveData<Boolean> fodmapFilterEnabled = new MutableLiveData<>(false);

    public void setFodmapFilterEnabled(boolean enabled) {
        fodmapFilterEnabled.setValue(enabled);
    }

    public LiveData<Boolean> isFodmapFilterEnabled() {
        return fodmapFilterEnabled;
    }

    // Simplified method for common use

    public LiveData<List<Food>> getFoodsWithFilter(String query, boolean fodmapOnly) {
        return searchUSDAFoodsWithFilter(query, fodmapOnly);
    }

    public void insertLogEntryWithSymptom(LogEntry logEntry, String symptomName, int severity) {
        databaseWriteExecutor.execute(() -> {
            try {
                // First insert the log entry and get its ID
                long logEntryId = logEntryDao.insert(logEntry);
                Log.d("NutritionViewModel", "Inserted log entry with ID: " + logEntryId);

                // Then create and insert symptom linked to this log entry
                Symptom symptom = new Symptom();
                symptom.logEntryId = (int) logEntryId; // Link to the log entry
                symptom.name = symptomName;
                symptom.severity = severity;
                symptom.timestamp = System.currentTimeMillis();

                logEntryDao.insertSymptom(symptom);
                Log.d("NutritionViewModel", "Inserted symptom: " + symptomName + " linked to log entry: " + logEntryId);

            } catch (Exception e) {
                Log.e("NutritionViewModel", "Error inserting symptom with log entry: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void debugSymptoms() {
        databaseWriteExecutor.execute(() -> {
            try {
                // Check all symptoms in database
                List<Symptom> allSymptoms = logEntryDao.getAllSymptomsDirect();
                Log.d("DEBUG", "Total symptoms in DB: " + allSymptoms.size());

                for (Symptom symptom : allSymptoms) {
                    Log.d("DEBUG", "Symptom: " + symptom.name +
                            ", Severity: " + symptom.severity +
                            ", LogEntryId: " + symptom.logEntryId);
                }

                // Check log entries with symptoms
                List<LogEntryWithSymptoms> entriesWithSymptoms = logEntryDao.getAllEntriesWithSymptomsDirect();
                Log.d("DEBUG", "Log entries with symptoms: " + entriesWithSymptoms.size());

            } catch (Exception e) {
                Log.e("DEBUG", "Error debugging symptoms: " + e.getMessage());
            }
        });
    }
    public LiveData<List<Food>> searchUSDAFoodsWithFilter(String query, boolean fodmapOnly) {
        MutableLiveData<List<Food>> result = new MutableLiveData<>();

        usdaRepository.searchFoods(query).observeForever(usdaFoods -> {
            if (fodmapOnly) {
                // Filter to only LOW FODMAP foods
                List<Food> filteredFoods = new ArrayList<>();
                for (Food food : usdaFoods) {
                    if ("LOW".equals(food.getFodmapStatus())) {
                        filteredFoods.add(food);
                    }
                }
                result.setValue(filteredFoods);
                Log.d("NutritionViewModel", "IBS ON - Showing " + filteredFoods.size() + " LOW FODMAP foods");
            } else {
                // Show all USDA results
                result.setValue(usdaFoods);
                Log.d("NutritionViewModel", "IBS OFF - Showing all " + usdaFoods.size() + " foods");
            }
        });

        return result;
    }


    // Recipe methods
    public LiveData<List<Recipe>> getAllRecipes() {
        return foodDao.getAllRecipes();
    }

    public LiveData<List<Recipe>> getLowFodmapRecipes() {
        return foodDao.getLowFodmapRecipes();
    }

    public LiveData<List<Recipe>> getRecipesByCategory(String category) {
        return foodDao.getRecipesByCategory(category);
    }

    public LiveData<List<Recipe>> getFavoriteRecipes() {
        return foodDao.getFavoriteRecipes();
    }

    public LiveData<List<Recipe>> searchRecipes(String query) {
        return foodDao.searchRecipes(query);
    }

    public void setRecipeFavorite(int recipeId, boolean isFavorite) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            foodDao.setRecipeFavorite(recipeId, isFavorite);
        });
    }

    public void insertRecipe(Recipe recipe) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            foodDao.insertRecipe(recipe);
        });
    }

    // Add this method to your NutritionViewModel class
    public void loadRecipesFromJson(Context context) {
        databaseWriteExecutor.execute(() -> {
            try {
                // Check if recipes already exist to avoid duplicates
                int existingRecipeCount = foodDao.getRecipeCount();

                if (existingRecipeCount == 0) {
                    Log.d("NutritionViewModel", "Loading recipes from JSON...");
                    RecipeLoader.loadRecipesIntoDatabase(context, AppDatabase.getDatabase(application));
                    Log.d("NutritionViewModel", "Recipes loaded successfully from JSON");
                } else {
                    Log.d("NutritionViewModel", "Recipes already exist in database: " + existingRecipeCount);
                }
            } catch (Exception e) {
                Log.e("NutritionViewModel", "Error loading recipes from JSON: " + e.getMessage());
            }
        });
    }

    // Add this method to check if database is empty
    public LiveData<Integer> getRecipeCount() {
        MutableLiveData<Integer> result = new MutableLiveData<>();
        databaseWriteExecutor.execute(() -> {
            int count = foodDao.getRecipeCount();
            result.postValue(count);
        });
        return result;
    }

    // Add these methods to NutritionViewModel.java
    public LiveData<List<Symptom>> getAllSymptoms() {
        return logEntryDao.getAllSymptoms();
    }







}