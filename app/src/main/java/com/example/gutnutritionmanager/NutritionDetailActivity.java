package com.example.gutnutritionmanager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gutnutritionmanager.api.USDAFood;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class NutritionDetailActivity extends AppCompatActivity {

    private TextView foodName;
    private TextView caloriesView;
    private TextView proteinView;
    private TextView carbsView;
    private TextView fatView;
    private TextView fiberView;
    private TextView sugarView;
    private RecyclerView nutrientsList;
    private NutrientAdapter nutrientAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutrition_detail);

        // Initialize views
        foodName = findViewById(R.id.food_name);
        caloriesView = findViewById(R.id.calories);
        proteinView = findViewById(R.id.protein);
        carbsView = findViewById(R.id.carbs);
        fatView = findViewById(R.id.fat);
        fiberView = findViewById(R.id.fiber);
        sugarView = findViewById(R.id.sugar);
        nutrientsList = findViewById(R.id.nutrients_list);
        Button adviceButton = findViewById(R.id.advice_button);



        // Set up back button
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // Get food data from intent
        Food food = getIntent().getParcelableExtra("food");

        if (food != null) {
            displayNutritionData(food);
        } else {
            Log.e("NutritionDetail", "No food data received");
            finish(); // Close activity if no food data
        }

        adviceButton.setOnClickListener(v -> {
            Intent intent = new Intent(NutritionDetailActivity.this, FoodAdviceActivity.class);
            intent.putExtra("food_name", food.name);
            startActivity(intent);
        });
    }

    private void displayNutritionData(Food food) {
        foodName.setText(food.name);
        caloriesView.setText(String.format("%.1f kcal", food.calories));
        proteinView.setText(String.format("%.1fg", food.protein));
        carbsView.setText(String.format("%.1fg", food.carbs));
        fatView.setText(String.format("%.1fg", food.fat));
        fiberView.setText(String.format("%.1fg", food.fiber));
        sugarView.setText(String.format("%.1fg", food.sugar));

        // Display full nutrient list if available
        if (food.nutrientData != null && !food.nutrientData.isEmpty()) {
            try {
                Gson gson = new Gson();
                Type nutrientListType = new TypeToken<List<USDAFood.FoodNutrient>>(){}.getType();
                List<USDAFood.FoodNutrient> nutrients = gson.fromJson(food.nutrientData, nutrientListType);

                nutrientAdapter = new NutrientAdapter(nutrients);
                nutrientsList.setLayoutManager(new LinearLayoutManager(this));
                nutrientsList.setAdapter(nutrientAdapter);
            } catch (Exception e) {
                Log.e("NutritionDetail", "Error parsing nutrient data", e);
            }
        } else {
            Log.d("NutritionDetail", "No detailed nutrient data available");
        }
    }
}