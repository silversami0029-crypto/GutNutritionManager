package com.example.gutnutritionmanager;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import androidx.annotation.NonNull;

import android.view.animation.AccelerateDecelerateInterpolator;

public class RecipeDetailsDialog extends Dialog {

    private Recipe recipe;
    private OnRecipeActionListener listener;

    // Timer variables
    private Chronometer cookingTimer;
    private Button startTimerBtn, pauseTimerBtn, resetTimerBtn;
    private boolean isTimerRunning = false;
    private long timeWhenStopped = 0;

    // Servings variables
    private TextView servingsCountText;
    private Button decreaseServingsBtn, increaseServingsBtn;
    private int currentServings;

    public interface OnRecipeActionListener {
        void onCookThis(Recipe recipe);
        void onAddToFavorites(Recipe recipe, boolean isFavorite);
    }

    public RecipeDetailsDialog(@NonNull Context context, Recipe recipe) {
        super(context);
        this.recipe = recipe;
    }

    public void setOnRecipeActionListener(OnRecipeActionListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_recipe_details);
        // Safe dimension setting
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        //Fix dialog width
        //getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);


        // Initialize ALL views after setContentView
        initializeViews();
        setupCookingTimer();
        setupServingsCalculator();
        setupRecipeData();
    }

    private void initializeViews() {
        // Initialize timer views
        cookingTimer = findViewById(R.id.cookingTimer);
        startTimerBtn = findViewById(R.id.startTimerBtn);
        pauseTimerBtn = findViewById(R.id.pauseTimerBtn);
        resetTimerBtn = findViewById(R.id.resetTimerBtn);

        // Initialize servings views
        servingsCountText = findViewById(R.id.servingsCountText);
        decreaseServingsBtn = findViewById(R.id.decreaseServingsBtn);
        increaseServingsBtn = findViewById(R.id.increaseServingsBtn);

        // Initialize other recipe views
        TextView recipeTitle = findViewById(R.id.recipeTitle);
        TextView recipeDescription = findViewById(R.id.recipeDescription);
        TextView recipeIngredients = findViewById(R.id.recipeIngredients);
        TextView recipeInstructions = findViewById(R.id.recipeInstructions);
        Button cookButton = findViewById(R.id.cookThisButton);

        // Set recipe data
        recipeTitle.setText(recipe.getTitle());
        recipeDescription.setText(recipe.getDescription());
        recipeIngredients.setText(recipe.getIngredients());
        recipeInstructions.setText(recipe.getInstructions());

        // Setup cook button
        cookButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCookThis(recipe);
            }
            dismiss();
        });
    }

    private void setupCookingTimer() {
        // Initially hide pause button
        pauseTimerBtn.setVisibility(View.GONE);

        startTimerBtn.setOnClickListener(v -> startTimer());
        pauseTimerBtn.setOnClickListener(v -> pauseTimer());
        resetTimerBtn.setOnClickListener(v -> resetTimer());
    }

    private void startTimer() {
        if (!isTimerRunning) {
            cookingTimer.setBase(SystemClock.elapsedRealtime() - timeWhenStopped);
            cookingTimer.start();
            isTimerRunning = true;
            startTimerBtn.setVisibility(View.GONE);
            pauseTimerBtn.setVisibility(View.VISIBLE);
        }
    }

    private void pauseTimer() {
        if (isTimerRunning) {
            timeWhenStopped = SystemClock.elapsedRealtime() - cookingTimer.getBase();
            cookingTimer.stop();
            isTimerRunning = false;
            pauseTimerBtn.setVisibility(View.GONE);
            startTimerBtn.setVisibility(View.VISIBLE);
        }
    }

    private void resetTimer() {
        cookingTimer.stop();
        cookingTimer.setBase(SystemClock.elapsedRealtime());
        timeWhenStopped = 0;
        isTimerRunning = false;
        startTimerBtn.setVisibility(View.VISIBLE);
        pauseTimerBtn.setVisibility(View.GONE);
    }

    private void setupServingsCalculator() {
        currentServings = recipe.getServings();
        updateServingsDisplay();

        decreaseServingsBtn.setOnClickListener(v -> {
            if (currentServings > 1) {
                currentServings--;
                updateServingsDisplay();
            }
        });

        increaseServingsBtn.setOnClickListener(v -> {
            if (currentServings < 10) {
                currentServings++;
                updateServingsDisplay();
            }
        });
    }

    private void updateServingsDisplay() {
        servingsCountText.setText(String.valueOf(currentServings));
    }

    private void setupRecipeData() {
        // Additional recipe setup if needed
    }

    @Override
    public void show() {
        super.show();

        // Apply fade in animation programmatically
        if (getWindow() != null && getWindow().getDecorView() != null) {
            getWindow().getDecorView().setAlpha(0f);
            getWindow().getDecorView().animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .start();
        }
    }

    @Override
    public void dismiss() {
        // Apply fade out animation before dismissing
        if (getWindow() != null && getWindow().getDecorView() != null) {
            getWindow().getDecorView().animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            RecipeDetailsDialog.super.dismiss();
                        }
                    })
                    .start();
        } else {
            super.dismiss();
        }
    }
}