package com.example.gutnutritionmanager;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;

public class RecipeLoader {
    private static final String TAG = "RecipeLoader";

    public static class RecipeList {
        public List<Recipe> recipes;
    }

    public static List<Recipe> loadRecipesFromJson(Context context) {
        try {
            // Read JSON file from assets
            InputStream inputStream = context.getAssets().open("recipes.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            String json = new String(buffer, "UTF-8");

            // Parse JSON using Gson
            Gson gson = new Gson();
            Type recipeListType = new TypeToken<RecipeList>(){}.getType();
            RecipeList recipeList = gson.fromJson(json, recipeListType);

            Log.d(TAG, "Successfully loaded " + recipeList.recipes.size() + " recipes from JSON");
            return recipeList.recipes;

        } catch (Exception e) {
            Log.e(TAG, "Error loading recipes from JSON: " + e.getMessage());
            return null;
        }
    }

    public static void loadRecipesIntoDatabase(Context context, AppDatabase database) {
        List<Recipe> recipes = loadRecipesFromJson(context);
        if (recipes != null && !recipes.isEmpty()) {
            // Insert recipes into database
            for (Recipe recipe : recipes) {
                database.foodDao().insertRecipe(recipe);
            }
            Log.d(TAG, "Successfully loaded " + recipes.size() + " recipes into database");
        }
    }
}