package com.example.gutnutritionmanager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class DiaryFragment_backup extends Fragment {
    private AutoCompleteTextView foodEditText;
    private TextView diaryLogTextView;
    private NutritionViewModel viewModel;
    private FoodAdapter foodAdapter;
    private boolean searchingUSDA = false;

    // Add handler for debouncing USDA searches
    private Handler handler = new Handler();
    private Runnable usdaSearchRunnable;
    private static final int USDA_SEARCH_DELAY = 500; // 500ms delay
    Switch fodmapSwitch;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diary, container, false);

        // Initialize UI components
        foodEditText = view.findViewById(R.id.foodSearchInput);
        diaryLogTextView = view.findViewById(R.id.diaryLogTextView);
        MaterialButton logButton = view.findViewById(R.id.logFoodButton);
        fodmapSwitch = view.findViewById(R.id.ibsModeSwitch);
        // Add Symptom button - navigate to Symptoms tab
        MaterialButton addSymptomButton = view.findViewById(R.id.addSymptomButton);

        addSymptomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Symptoms tab (index 1)
                ViewPager2 viewPager = requireActivity().findViewById(R.id.viewPager);
                if (viewPager != null) {
                    viewPager.setCurrentItem(1, true); // 0=Diary, 1=Symptoms, 2=Insights
                }
            }
        });
        // Doctor export functionality
        View exportCard = view.findViewById(R.id.exportCard);
        exportCard.setOnClickListener(v -> {
            shareWithDoctor();
        });
        // RecyclerView setup
        RecyclerView quickAddGrid = view.findViewById(R.id.quickAddGrid);

        // Set GridLayoutManager with 4 columns
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 4);
        quickAddGrid.setLayoutManager(layoutManager);

        MaterialButton insight = view.findViewById(R.id.insightButton);
        //set onclick on insight button
        insight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to the Insights tab (index 2) in the ViewPager
                ViewPager2 viewPager = requireActivity().findViewById(R.id.viewPager);
                if (viewPager != null) {
                    viewPager.setCurrentItem(2); // 0=Diary, 1=Symptoms, 2=Insights
                    // Optional: Trigger a refresh in InsightsFragment
                    // You'll need to implement this communication
                    triggerInsightsRefresh();
                }
            }
        });
        List<QuickFood> quickFoods = Arrays.asList(
                new QuickFood("🍗", "Chicken", "LOW", "Cooked"),
                new QuickFood("🐟", "Fish", "LOW", "Cooked"),
                new QuickFood("🥚", "Eggs", "LOW", "Cooked"),
                new QuickFood("🧀", "Cheese", "LOW", "Raw"),
                new QuickFood("🍚", "Rice", "LOW", "Cooked"),
                new QuickFood("🌾", "Oats", "LOW", "Cooked"),
                new QuickFood("🥕", "Carrots", "LOW", "Cooked"), // Cooked is often better for IBS
                new QuickFood("🍆", "Eggplant", "LOW", "Cooked"),
                new QuickFood("🍅", "Tomatoes", "LOW", "Raw"), // Many prefer raw tomatoes
                new QuickFood("🍓", "Berries", "LOW", "Raw"),
                new QuickFood("🍇", "Grapes", "LOW", "Raw"),
                new QuickFood("🍊", "Oranges", "LOW", "Raw")
        );
        //adapter setup
        // In your adapter setup in DiaryFragment:
        QuickFoodRecyclerAdapter adapter = new QuickFoodRecyclerAdapter(
                requireContext(),
                quickFoods,
                new QuickFoodRecyclerAdapter.OnFoodClickListener() {
                    @Override
                    public void onFoodClick(QuickFood food) {
                        if (requiresPreparationChoice(food)) {
                            showPreparationDialog(food);
                        } else {
                            addFoodWithPreparation(food, food.getDefaultPreparation());
                        }
                    }
                }
        );

        quickAddGrid.setAdapter(adapter);

        // Initialize ViewModel
        try {
            NutritionViewModelFactory factory = new NutritionViewModelFactory(requireActivity().getApplication());
            viewModel = new ViewModelProvider(requireActivity(), factory).get(NutritionViewModel.class);
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error initializing ViewModel: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return view;
        }

        debugDatabaseState();
        debugFoodData();
        debugLocalFoods();
        checkAndInitializeData();
        testFODMAPClassification();



        // Initialize FoodAdapter
        foodAdapter = new FoodAdapter(getContext(), android.R.layout.simple_dropdown_item_1line, viewModel);
        foodEditText.setAdapter(foodAdapter);
        foodEditText.setThreshold(1); // Show suggestions after 1 character



        // Set up text watcher for automatic USDA searching
        foodEditText.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();
            private final long DELAY = 500; // milliseconds

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Remove any pending USDA search requests
                if (usdaSearchRunnable != null) {
                    handler.removeCallbacks(usdaSearchRunnable);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                timer.cancel();
                timer = new Timer();

                String query = s.toString().trim();
                Log.d("DiaryFragment", "Text changed: " + query);

                if (query.length() > 2) {
                    Log.d("DiaryFragment", "Triggering USDA search for: " + query);
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            requireActivity().runOnUiThread(() -> {
                                // Get current IBS filter state
                                boolean ibsFilterEnabled = fodmapSwitch.isChecked();

                                Log.d("DiaryFragment", "Calling viewModel.getFoodsWithFilter with IBS: " + ibsFilterEnabled);

                                // Use the NEW method with FODMAP filtering
                                viewModel.getFoodsWithFilter(query, ibsFilterEnabled).observe(getViewLifecycleOwner(), foods -> {
                                    Log.d("DiaryFragment", "Filtered USDA results: " + foods.size() + " items");

                                    // Log FODMAP status for debugging
                                    for (Food food : foods) {
                                        Log.d("DiaryFragment", "Food: " + food.getName() + " | FODMAP: " + food.getFodmapStatus());
                                    }

                                    foodAdapter.setLocalFoodList(foods);
                                });

                                searchingUSDA = true;
                            });
                        }
                    }, DELAY);
                } else if (query.isEmpty()) {
                    Log.d("DiaryFragment", "Query empty, switching to local foods");
                    searchingUSDA = false;
                    viewModel.getCommonFoods().observe(getViewLifecycleOwner(), foods -> {
                        foodAdapter.setLocalFoodList(foods);
                    });
                }
            }
        });


       // MaterialSwitch fodmapSwitch = view.findViewById(R.id.fodmap_filter_switch);

        fodmapSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d("DiaryFragment", "=== TOGGLE TRIGGERED ===");
            Log.d("DiaryFragment", "IBS-Friendly Mode: " + isChecked);
            viewModel.setFodmapFilterEnabled(isChecked);


            String query = foodEditText.getText().toString();
            Log.d("DiaryFragment", "Current query: " + query);

            if (!query.isEmpty()) {
                Log.d("DiaryFragment", "Performing search with filter");
               // performRealTimeUSDAsearch(query);
                performSearch(query, isChecked);  // This might be using old method
            }
        });

        // Observe the filter state
        viewModel.isFodmapFilterEnabled().observe(getViewLifecycleOwner(), enabled -> {
            fodmapSwitch.setChecked(enabled);
        });



        // Set up item click listener for the AutoCompleteTextView
        foodEditText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Food selectedFood = (Food) parent.getItemAtPosition(position);
                if (foodAdapter.isShowingUSDAResults()) {
                    // Import from USDA
                    viewModel.importUSDAFood(selectedFood);
                    foodEditText.setText(selectedFood.name);
                    Toast.makeText(getContext(), "Imported from USDA: " + selectedFood.name, Toast.LENGTH_SHORT).show();

                    // Switch back to local foods
                    viewModel.getCommonFoods().observe(getViewLifecycleOwner(), new Observer<List<Food>>() {
                        @Override
                        public void onChanged(List<Food> foods) {
                            foodAdapter.setLocalFoodList(foods);
                            searchingUSDA = false;
                        }
                    });
                } else {
                    // Local food selection
                    foodEditText.setText(selectedFood.name);
                }
            }
        });

        // Observe USDA search results
        viewModel.getUsdaSearchResults().observe(getViewLifecycleOwner(), new Observer<List<Food>>() {
            @Override
             public void onChanged(List<Food> foods) {
                Log.d("DiaryFragment", "Received USDA results: " + foods.size() + " items");
                for (Food food : foods) {
                    Log.d("DiaryFragment", "Food: " + food.name + ", USDA ID: " + food.usdaId);
                }

                if (searchingUSDA) {
                    foodAdapter.setUSDAFoodList(foods);
                    Log.d("DiaryFragment", "No results found in USDA database");
                    // Only show toast if no results were found AND we're still searching USDA
                    if (foods.isEmpty() && searchingUSDA) {
                       // Toast.makeText(getContext(), "No results found in USDA database", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });

        // Load common foods initially
        if (viewModel != null) {
            viewModel.getCommonFoods().observe(getViewLifecycleOwner(), new Observer<List<Food>>() {
                @Override
                public void onChanged(List<Food> foods) {
                    if (foodAdapter != null) {
                        foodAdapter.setFoodList(foods);
                    }
                }
            });
        }

        logButton.setOnClickListener(v -> logFoodEntry());

        // Set up observers for log entries
        if (viewModel != null) {
            setupObservers();
        } else {
            diaryLogTextView.setText("Error: ViewModel not initialized");
        }

        updateDateDisplay(view);
        displayTodaysFoodLogs(view);

        return view;


    }// end onCreateView()


    //share insight with doctor
    private void shareWithDoctor() {
        // Simple doctor report (will be enhanced later)
        String doctorReport = generateDoctorReport();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My IBS Food Diary Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, doctorReport);

        startActivity(Intent.createChooser(shareIntent, "Share with Doctor"));
    }

    private String generateDoctorReport() {
        // Basic report - will be enhanced with real data later
        return "MY IBS FOOD DIARY REPORT\n" +
                "Generated: " + new Date().toString() + "\n\n" +
                "TRACKED FOODS:\n" +
                "• Rice (Cooked) - LOW FODMAP\n" +
                "• Chicken (Cooked) - LOW FODMAP\n" +
                "• Carrots (Cooked) - LOW FODMAP\n\n" +
                "INSIGHTS:\n" +
                "• Good tolerance to cooked vegetables\n" +
                "• Tracking preparation methods\n" +
                "• Monitoring portion sizes\n\n" +
                "Generated by Gut Nutrition Manager App";
    }

    private void showPreparationDialog(QuickFood food) {
        // Preparation options based on Reddit insights
        final String[] preparations = {"Raw", "Cooked", "Steamed", "Roasted", "Boiled", "Fried", "Mashed", "Pureed"};

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("How was the " + food.getName() + " prepared?");
        builder.setItems(preparations, (dialog, which) -> {
            String selectedPreparation = preparations[which];
            addFoodWithPreparation(food, selectedPreparation);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    private void addFoodWithPreparation(QuickFood food, String preparation) {
        // Auto-fill the search input with preparation info
        AutoCompleteTextView searchInput = getView().findViewById(R.id.foodSearchInput);
        String searchText = food.getName() + " (" + preparation + ")";
        searchInput.setText(searchText);

        // Show confirmation with preparation info
        String message = "Added " + food.getName() + " (" + preparation + ") to search";
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

        // Store preparation info for future correlations
        logFoodWithPreparation(food.getName(), preparation, food.getFodmapStatus());
    }

    private void logFoodWithPreparation(String foodName, String preparation, String fodmapStatus) {
        // Log for debugging and future correlation analysis
        Log.d("FoodTracking", "Food: " + foodName +
                ", Preparation: " + preparation +
                ", FODMAP: " + fodmapStatus);

        // TODO: Store in database for doctor reports and insights
        // This will be the foundation for our export feature
    }
    private void quickAddFood(QuickFood food) {
        // Show preparation dialog for relevant foods
        if (requiresPreparationChoice(food)) {
            showPreparationDialog(food);
        } else {
            // Use default preparation for foods that don't need choice
            addFoodWithPreparation(food, food.getDefaultPreparation());
        }
    }

    private boolean requiresPreparationChoice(QuickFood food) {
        // Foods where preparation significantly affects FODMAPs
        String[] choiceFoods = {"Carrots", "Eggplant", "Tomatoes", "Onions", "Garlic", "Broccoli"};
        return Arrays.asList(choiceFoods).contains(food.getName());
    }
/*
    private void addFoodWithPreparation(QuickFood food, String preparation) {
        // Auto-fill the search input with preparation info
        AutoCompleteTextView searchInput = getView().findViewById(R.id.foodSearchInput);
        String searchText = food.getName() + " (" + preparation + ")";
        searchInput.setText(searchText);

        // Show confirmation with preparation info
        String message = "Added " + food.getName() + " (" + preparation + ") to search";
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

        // Optional: Store preparation method for correlation tracking
        storePreparationInfo(food.getName(), preparation);
    }*/

    private void storePreparationInfo(String foodName, String preparation) {
        // This will be used later for food-symptom correlations
        Log.d("FoodPrep", "Food: " + foodName + ", Preparation: " + preparation);

        // TODO: Store in database for correlation analysis
        // For now, we'll just log it
    }
    private void triggerInsightsRefresh() {
        // This is one way to communicate between fragments
        // You can use ViewModel, EventBus, or Interface

        // Simple approach: Find the InsightsFragment and call a method
        FragmentManager fragmentManager = getParentFragmentManager();
        Fragment insightsFragment = fragmentManager.findFragmentByTag("f" + 2); // Assuming tag format

        if (insightsFragment instanceof InsightsFragment) {
            ((InsightsFragment) insightsFragment).refreshInsights();
        }
    }
    private void performRealTimeUSDAsearch(String query) {

        Log.d("USDA-Search", "Real-time search: " + query);

        // Get current IBS filter state
        boolean ibsFilterEnabled = fodmapSwitch.isChecked();

        viewModel.getFoodsWithFilter(query, ibsFilterEnabled).observe(getViewLifecycleOwner(), foods -> {
            Log.d("USDA-Search", "Results: " + foods.size() + " foods");

            // Show confidence information
            for (Food food : foods) {
                Log.d("USDA-Search", food.getName() + " - FODMAP: " + food.getFodmapStatus());
            }

            foodAdapter.setLocalFoodList(foods);
        });
    }
    private void performSearch(String query, boolean fodmapOnly) {
        Log.d("DiaryFragment", "performSearch - Query: " + query + ", FODMAP Only: " + fodmapOnly);

        // Use the NEW USDA method with FODMAP filtering
        viewModel.getFoodsWithFilter(query, fodmapOnly).observe(getViewLifecycleOwner(), foods -> {
            Log.d("DiaryFragment", "Search results: " + foods.size() + " items");

            // Log each food with FODMAP status
            for (Food food : foods) {
                Log.d("DiaryFragment", "Result - " + food.getName() + " (FODMAP: " + food.getFodmapStatus() + ")");
            }

            if (searchingUSDA) {
                foodAdapter.setUSDAFoodList(foods);
            } else {
                foodAdapter.setLocalFoodList(foods);
            }
        });
    }
        @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove any pending callbacks to prevent memory leaks
        if (usdaSearchRunnable != null) {
            handler.removeCallbacks(usdaSearchRunnable);
        }
    }

    private void setupObservers() {
        LiveData<List<LogEntry>> entriesLiveData = viewModel.getAllEntries();
        if (entriesLiveData != null) {
            entriesLiveData.observe(getViewLifecycleOwner(), new Observer<List<LogEntry>>() {
                @Override
                public void onChanged(List<LogEntry> logEntries) {
                    updateDiaryLogDisplay(logEntries);
                }
            });
        } else {
            diaryLogTextView.setText("Error: No data available");
        }
    }

    private void logFoodEntry() {
        String foodInput = foodEditText.getText().toString().trim();
        if (foodInput.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter a food", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LogEntry newEntry = new LogEntry(new Date(), foodInput);
            viewModel.insertLogEntry(newEntry);
            foodEditText.setText(""); // Clear input
            Toast.makeText(getActivity(), "Food logged!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error logging food: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDiaryLogDisplay(List<LogEntry> logEntries) {
        if (logEntries != null && !logEntries.isEmpty()) {
            StringBuilder logText = new StringBuilder("Today's Log:\n");
            for (LogEntry entry : logEntries) {
                logText.append("-> ").append(entry.foods).append("\n");
            }
            diaryLogTextView.setText(logText.toString());
        } else {
            diaryLogTextView.setText("Today's Log:\n(No entries yet)");
        }
    }

    // Add a method to check your food data
    private void debugFoodData() {
        viewModel.getCommonFoods().observe(getViewLifecycleOwner(), foods -> {
            if (viewModel == null) {
                Log.e("DiaryFragment", "ViewModel is null in debugFoodData!");
                return;
            }

            Log.d("FODMAPDebug", "=== FOOD DATA FODMAP STATUS ===");
            for (Food food : foods) {
                Log.d("FODMAPDebug", food.name + " - FODMAP: " + food.fodmapStatus);
            }
        });
    }

    private void debugLocalFoods() {
        viewModel.getCommonFoods().observe(getViewLifecycleOwner(), foods -> {
            Log.d("LocalFoods", "=== LOCAL FOODS IN DATABASE ===");
            for (Food food : foods) {
                Log.d("LocalFoods", "Food: " + food.name + " | FODMAP: " + food.fodmapStatus);
            }
        });
    }
    private void debugDatabaseState() {
        Log.d("DatabaseDebug", "=== COMPREHENSIVE DATABASE DEBUG ===");

        // 1. Check common foods count
        viewModel.getCommonFoods().observe(getViewLifecycleOwner(), foods -> {
            Log.d("DatabaseDebug", "1. Common foods count: " + (foods != null ? foods.size() : 0));
            if (foods != null && !foods.isEmpty()) {
                for (Food food : foods) {
                    Log.d("DatabaseDebug", "   - " + food.name + " | FODMAP: " + food.fodmapStatus);
                }
            } else {
                Log.d("DatabaseDebug", "   ⚠️ NO COMMON FOODS FOUND!");
            }
        });

        // 2. Test search functionality directly
        viewModel.getFoodsWithFilter("rice", false).observe(getViewLifecycleOwner(), foods -> {
            Log.d("DatabaseDebug", "2. Search 'rice' (all foods) results: " + (foods != null ? foods.size() : 0));
        });

        viewModel.getFoodsWithFilter("rice", true).observe(getViewLifecycleOwner(), foods -> {
            Log.d("DatabaseDebug", "3. Search 'rice' (LOW FODMAP only) results: " + (foods != null ? foods.size() : 0));
        });

        viewModel.getFoodsWithFilter("onion", true).observe(getViewLifecycleOwner(), foods -> {
            Log.d("DatabaseDebug", "4. Search 'onion' (LOW FODMAP only) results: " + (foods != null ? foods.size() : 0));
        });

        // 3. Check database initialization logs
        Log.d("DatabaseDebug", "5. Check AppDatabase logs for initialization status");
    }

    private void checkAndInitializeData() {
        viewModel.getCommonFoods().observe(getViewLifecycleOwner(), foods -> {
            if (foods == null || foods.isEmpty()) {
                Log.e("DataCheck", "❌ DATABASE IS EMPTY - Manual initialization needed");

                // Try to force initialization
                new Thread(() -> {
                    try {
                        AppDatabase database = AppDatabase.getDatabase(requireContext());
                        database.foodDao().insert(new Food(0, "Test Rice", "Grains", true, "LOW", "", 0, 0, 0, 0, 0, 0, null));
                        Log.d("DataCheck", "Manual test insertion attempted");
                    } catch (Exception e) {
                        Log.e("DataCheck", "Manual insertion failed: " + e.getMessage());
                    }
                }).start();
            } else {
                Log.d("DataCheck", "✅ Database has data: " + foods.size() + " foods");
            }
        });
    }

    private void setupUSDARealTimeSearch() {
        foodEditText.addTextChangedListener(new TextWatcher() {
            private Handler handler = new Handler();
            private Runnable runnable;
            private static final int SEARCH_DELAY = 300; // ms

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(runnable);

                String query = s.toString().trim();
                if (query.length() >= 2) { // Start searching after 2 characters
                    runnable = () -> performRealTimeUSDAsearch(query);
                    handler.postDelayed(runnable, SEARCH_DELAY);
                } else if (query.isEmpty()) {
                    // Clear results when empty
                    foodAdapter.setLocalFoodList(new ArrayList<>());
                }
            }
        });
    }


    private void testFODMAPClassification() {
        Log.d("FODMAP-TEST", "=== TESTING FODMAP CLASSIFICATION ===");

        // Test foods that should have different FODMAP statuses
        String[] testFoods = {"onion", "rice", "chicken", "apple", "garlic"};

        for (String food : testFoods) {
            viewModel.getFoodsWithFilter(food, false).observe(getViewLifecycleOwner(), foods -> {
                Log.d("FODMAP-TEST", "=== " + food.toUpperCase() + " RESULTS ===");
                Log.d("FODMAP-TEST", "Found " + foods.size() + " items");

                for (Food f : foods) {
                    Log.d("FODMAP-TEST",
                            f.getName() + " | " +
                                    "FODMAP: " + f.getFodmapStatus() + " | " +
                                    "Category: " + f.getCategory());
                }
            });
        }
    }

    private void updateDateDisplay(View view) {
        TextView todayDate = getView().findViewById(R.id.todayDate);

        if (todayDate != null) {
            // Get current date with proper formatting
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            String currentDate = sdf.format(new Date());

            todayDate.setText(currentDate);
            Log.d("DateDebug", "Date updated to: " + currentDate); // For debugging
        } else {
            Log.e("DateDebug", "todayDate TextView not found!");
        }
    }

    private void displayTodaysFoodLogs(View view) {
        LinearLayout logContainer = getView().findViewById(R.id.todayLogContainer);
        LinearLayout emptyState = getView().findViewById(R.id.emptyLogState);
        LinearLayout logSummary = getView().findViewById(R.id.logSummary);

        // TODO: Replace with actual database data
        List<FoodLog> todaysFoods = getTodaysFoodLogs(); // Mock data for now

        logContainer.removeAllViews();

        if (todaysFoods.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            logSummary.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            logSummary.setVisibility(View.VISIBLE);

            for (FoodLog foodLog : todaysFoods) {
                View logItemView = createFoodLogItem(foodLog);
                logContainer.addView(logItemView);
            }

            updateLogSummary(todaysFoods);
        }
    }

    private View createFoodLogItem(FoodLog foodLog) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View logItemView = inflater.inflate(R.layout.food_log_item, null);

        TextView emojiView = logItemView.findViewById(R.id.foodEmoji);
        TextView nameView = logItemView.findViewById(R.id.foodName);
        TextView detailsView = logItemView.findViewById(R.id.foodDetails);
        TextView badgeView = logItemView.findViewById(R.id.fodmapBadge);

        // Set data - you'll need to map foods to emojis
        emojiView.setText(getFoodEmoji(foodLog.getFoodName()));
        nameView.setText(foodLog.getFoodName());

        String details = foodLog.getPreparation() + " • " + foodLog.getMealType();
        detailsView.setText(details);

        badgeView.setText(foodLog.getFodmapStatus());
        if ("LOW".equals(foodLog.getFodmapStatus())) {
            badgeView.setBackgroundResource(R.drawable.fodmap_low_badge);
        } else {
            badgeView.setBackgroundResource(R.drawable.fodmap_high_badge);
        }

        return logItemView;
    }

    private String getFoodEmoji(String foodName) {
        // Simple emoji mapping - enhance as needed
        switch (foodName.toLowerCase()) {
            case "chicken": return "🍗";
            case "fish": return "🐟";
            case "eggs": return "🥚";
            case "rice": return "🍚";
            case "carrots": return "🥕";
            default: return "🍽️";
        }
    }

    private List<FoodLog> getTodaysFoodLogs() {
        // Mock data for testing - replace with database data later
        List<FoodLog> foodLogs = new ArrayList<>();

        // Add some sample foods
        foodLogs.add(new FoodLog("Chicken", "Cooked", "Lunch", "LOW"));
        foodLogs.add(new FoodLog("Rice", "Cooked", "Lunch", "LOW"));
        foodLogs.add(new FoodLog("Carrots", "Cooked", "Lunch", "LOW"));

        return foodLogs;
    }

    private void updateLogSummary(List<FoodLog> foods) {
        TextView totalFoodsText = getView().findViewById(R.id.totalFoodsText);
        TextView gutSafeText = getView().findViewById(R.id.gutSafeText);
        TextView triggersText = getView().findViewById(R.id.triggersText);

        int totalFoods = foods.size();
        int gutSafe = 0;

        for (FoodLog food : foods) {
            if ("LOW".equals(food.getFodmapStatus())) {
                gutSafe++;
            }
        }

        int triggers = totalFoods - gutSafe;

        // Update the UI - you'll need to add these IDs to your XML
        if (totalFoodsText != null) totalFoodsText.setText(String.valueOf(totalFoods));
        if (gutSafeText != null) gutSafeText.setText(String.valueOf(gutSafe));
        if (triggersText != null) triggersText.setText(String.valueOf(triggers));
    }



}