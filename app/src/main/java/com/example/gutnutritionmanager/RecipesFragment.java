package com.example.gutnutritionmanager;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import com.example.gutnutritionmanager.RecipeDetailsDialog;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecipesFragment extends Fragment {

    private RecyclerView recipesRecyclerView;
    private RecipeAdapter recipeAdapter;
    private NutritionViewModel viewModel;
    private androidx.appcompat.widget.SearchView searchView;
    private View emptyState;
    private Spinner categorySpinner;

    // Recipe of the Day views
    private com.google.android.material.card.MaterialCardView recipeOfTheDayCard;
    private TextView recipeOfTheDayTitle, recipeOfTheDayName;
    private Button viewRecipeOfTheDayBtn;
    private Recipe featuredRecipe;

    // Category list
    private String[] categories = {"All Recipes", "Breakfast", "Lunch", "Dinner", "Snack"};

    public RecipesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipes, container, false);

        // Initialize UI
        recipesRecyclerView = view.findViewById(R.id.recipesRecyclerView);
        searchView = view.findViewById(R.id.recipeSearchView);
        emptyState = view.findViewById(R.id.emptyState);
        categorySpinner = view.findViewById(R.id.categorySpinner);

        // Initialize Recipe of the Day views
        recipeOfTheDayCard = view.findViewById(R.id.recipeOfTheDayCard);
        recipeOfTheDayTitle = view.findViewById(R.id.recipeOfTheDayTitle);
        recipeOfTheDayName = view.findViewById(R.id.recipeOfTheDayName);
        viewRecipeOfTheDayBtn = view.findViewById(R.id.viewRecipeOfTheDayBtn);

        // Setup category spinner
        setupCategorySpinner();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(NutritionViewModel.class);

        // Setup Recipe of the Day click listener
        viewRecipeOfTheDayBtn.setOnClickListener(v -> {
            if (featuredRecipe != null) {
                showRecipeDetails(featuredRecipe);
            }
        });

        // Load recipes
        loadRecipes();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupSearch();
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        // Set category filter listener
        categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = categories[position];
                filterRecipesByCategory(selectedCategory);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupRecyclerView() {
        recipeAdapter = new RecipeAdapter(new ArrayList<>(), new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                showRecipeDetails(recipe);
            }

            @Override
            public void onRecipeFavorite(Recipe recipe, boolean isFavorite) {
                viewModel.setRecipeFavorite(recipe.getId(), isFavorite);
            }
        });

        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        recipesRecyclerView.setLayoutManager(layoutManager);
        recipesRecyclerView.setAdapter(recipeAdapter);
    }

    private void loadRecipes() {
        // First, check if we need to load recipes from JSON
        viewModel.getRecipeCount().observe(getViewLifecycleOwner(), count -> {
            Log.d("RecipesFragment", "Recipe count in database: " + count);

            if (count == 0) {
                Log.d("RecipesFragment", "Database empty, loading recipes from JSON...");
                viewModel.loadRecipesFromJson(requireContext());
            }
        });

        // Then observe the recipes from database
        viewModel.getAllRecipes().observe(getViewLifecycleOwner(), recipes -> {
            Log.d("RecipesFragment", "Recipes observed: " + (recipes != null ? recipes.size() : 0));

            if (recipes != null && !recipes.isEmpty()) {
                recipeAdapter.updateData(recipes);
                showEmptyState(false);
                setupRecipeOfTheDay(recipes);
                Log.d("RecipesFragment", "Successfully loaded " + recipes.size() + " recipes into UI");
            } else {
                showEmptyState(true);
                Log.d("RecipesFragment", "No recipes found - waiting for JSON loading...");
            }
        });
    }

    private void filterRecipesByCategory(String category) {
        if ("All Recipes".equals(category)) {
            // Show all recipes
            viewModel.getAllRecipes().observe(getViewLifecycleOwner(), recipes -> {
                if (recipes != null) {
                    recipeAdapter.updateData(recipes);
                    Log.d("RecipesFragment", "Showing all " + recipes.size() + " recipes");
                }
            });
        } else {
            // Filter by specific category
            viewModel.getRecipesByCategory(category).observe(getViewLifecycleOwner(), recipes -> {
                if (recipes != null) {
                    recipeAdapter.updateData(recipes);
                    Log.d("RecipesFragment", "Showing " + recipes.size() + " " + category + " recipes");
                }
            });
        }
    }

    private void filterRecipes(String query) {
        if (recipeAdapter != null) {
            recipeAdapter.getFilter().filter(query);
        }
    }

    private void setupSearch() {
        if (searchView != null) {
            searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filterRecipes(newText);
                    return true;
                }
            });
        } else {
            Log.e("RecipesFragment", "SearchView not found!");
        }
    }

    private void setupRecipeOfTheDay(List<Recipe> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            recipeOfTheDayCard.setVisibility(View.GONE);
            return;
        }

        // Sort recipes by title for consistent ordering across devices
        List<Recipe> sortedRecipes = new ArrayList<>(recipes);
        Collections.sort(sortedRecipes, (r1, r2) -> r1.getTitle().compareToIgnoreCase(r2.getTitle()));

        // Simple algorithm: Get recipe based on day of year for consistency
        int dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR);
        int recipeIndex = dayOfYear % sortedRecipes.size();

        featuredRecipe = sortedRecipes.get(recipeIndex);

        // Update UI
        recipeOfTheDayName.setText(featuredRecipe.getTitle());
        recipeOfTheDayCard.setVisibility(View.VISIBLE);

        Log.d("RecipesFragment", "Recipe of the Day: " + featuredRecipe.getTitle() + " (Index: " + recipeIndex + ")");
    }




    private void showRecipeDetails(Recipe recipe) {
        RecipeDetailsDialog dialog = new RecipeDetailsDialog(requireContext(), recipe);

        dialog.setOnRecipeActionListener(new RecipeDetailsDialog.OnRecipeActionListener() {
            @Override
            public void onCookThis(Recipe recipe) {
                // TODO: Implement "Cook This" functionality
                // This could auto-log the recipe ingredients
            }

            @Override
            public void onAddToFavorites(Recipe recipe, boolean isFavorite) {
                viewModel.setRecipeFavorite(recipe.getId(), isFavorite);
            }
        });
         // Add this after setOnRecipeActionListener but before show()
       // dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.show(); // This should work now!
    }

    private void showEmptyState(boolean show) {
        if (emptyState != null && recipesRecyclerView != null) {
            if (show) {
                emptyState.setVisibility(View.VISIBLE);
                recipesRecyclerView.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                recipesRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }




}