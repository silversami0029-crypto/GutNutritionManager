package com.example.gutnutritionmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class FoodAdviceActivity extends AppCompatActivity {

    private NutritionViewModel viewModel;
    private ProgressBar progressBar;
    private TextView foodNameTextView, adviceForTextView;
    private RecyclerView substitutesRecyclerView, preparationRecyclerView;
    private FoodAdviceAdapter substitutesAdapter, preparationAdapter;

    private String usdaFoodName;
    private String foodCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_advice);

        // Get data from intent
        usdaFoodName = getIntent().getStringExtra("FOOD_NAME");
        foodCategory = getIntent().getStringExtra("FOOD_CATEGORY");

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(NutritionViewModel.class);

        // Initialize UI components
        initViews();
        setupRecyclerViews();
        setupClickListeners();

        // Load advice
        loadFoodAdvice();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        foodNameTextView = findViewById(R.id.food_name);
        adviceForTextView = findViewById(R.id.advice_for_text);

        substitutesRecyclerView = findViewById(R.id.substitutes_list);
        preparationRecyclerView = findViewById(R.id.preparation_list);

        // Set food name
        if (usdaFoodName != null) {
            foodNameTextView.setText(usdaFoodName);
        }
    }

    private void setupRecyclerViews() {
        // Setup substitutes RecyclerView
        substitutesAdapter = new FoodAdviceAdapter(new ArrayList<>());
        substitutesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        substitutesRecyclerView.setAdapter(substitutesAdapter);

        // Setup preparation advice RecyclerView
        preparationAdapter = new FoodAdviceAdapter(new ArrayList<>());
        preparationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        preparationRecyclerView.setAdapter(preparationAdapter);
    }

    private void setupClickListeners() {
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }

    private void loadFoodAdvice() {
        showLoadingIndicator();

        if (usdaFoodName == null) {
            showError("No food name provided");
            return;
        }

        // Find the best matching advice food name
        String adviceFoodName = viewModel.findAdviceFoodName(usdaFoodName);

        // Show which USDA food we're getting advice for
        adviceForTextView.setText("Advice for: " + usdaFoodName);
        adviceForTextView.setVisibility(View.VISIBLE);

        Log.d("FoodAdviceActivity", "USDA Food: " + usdaFoodName + " → Advice Food: " + adviceFoodName);

        // Load substitutes
        viewModel.getAdviceForFood(adviceFoodName, "substitute").observe(this, substitutes -> {
            if (substitutes != null && !substitutes.isEmpty()) {
                substitutesAdapter.updateData(substitutes);
                Log.d("FoodAdviceActivity", "Loaded " + substitutes.size() + " substitutes");
            } else {
                showGeneralAdvice(usdaFoodName, adviceFoodName, "substitute");
            }
        });

        // Load preparation advice
        viewModel.getAdviceForFood(adviceFoodName, "preparation").observe(this, preparationAdvice -> {
            if (preparationAdvice != null && !preparationAdvice.isEmpty()) {
                preparationAdapter.updateData(preparationAdvice);
                Log.d("FoodAdviceActivity", "Loaded " + preparationAdvice.size() + " preparation tips");
            } else {
                // Don't show general advice for preparation if we already have substitutes
                // Just hide the loading indicator
                hideLoadingIndicator();
            }
        });
    }

    private void showGeneralAdvice(String usdaFoodName, String adviceFoodName, String adviceType) {
        runOnUiThread(() -> {
            List<FoodAdvice> generalAdvice = new ArrayList<>();

            if ("substitute".equals(adviceType)) {
                // Create general substitute advice
                FoodAdvice generalSubstitute = new FoodAdvice(
                        adviceFoodName,
                        "substitute",
                        "General Substitutes",
                        "No specific substitutes found for " + usdaFoodName + ". Try these general approaches:\n\n" +
                                "• Start with small portions\n" +
                                "• Consider similar foods from the same category\n" +
                                "• Cook thoroughly to improve digestibility\n" +
                                "• Combine with low-FODMAP ingredients",
                        "GENERAL"
                );
                generalAdvice.add(generalSubstitute);
                substitutesAdapter.updateData(generalAdvice);
            } else {
                // Create general preparation advice
                FoodAdvice generalPreparation = new FoodAdvice(
                        adviceFoodName,
                        "preparation",
                        "General Preparation Tips",
                        "General preparation advice for " + adviceFoodName + ":\n\n" +
                                "• Cook thoroughly to break down complex carbohydrates\n" +
                                "• Avoid raw preparation if you have sensitivity\n" +
                                "• Start with small serving sizes\n" +
                                "• Monitor your body's response",
                        "GENERAL"
                );
                generalAdvice.add(generalPreparation);
                preparationAdapter.updateData(generalAdvice);
            }

            hideLoadingIndicator();
        });
    }

    private void showLoadingIndicator() {
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            // Optionally hide RecyclerViews while loading
            substitutesRecyclerView.setVisibility(View.GONE);
            preparationRecyclerView.setVisibility(View.GONE);
        });
    }

    private void hideLoadingIndicator() {
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            // Show RecyclerViews after loading
            substitutesRecyclerView.setVisibility(View.VISIBLE);
            preparationRecyclerView.setVisibility(View.VISIBLE);
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            // You can show a toast or error message
            Log.e("FoodAdviceActivity", message);
            hideLoadingIndicator();

            // Show error in the UI
            TextView errorText = findViewById(R.id.advice_for_text);
            if (errorText != null) {
                errorText.setText("Error: " + message);
                errorText.setVisibility(View.VISIBLE);
            }
        });
    }
}