package com.example.gutnutritionmanager;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class DiaryFragment extends Fragment {

    private AppDatabase database;
    private List<FoodLog> foodLogs = new ArrayList<>();
    private FoodDao foodDao;
    private AutoCompleteTextView foodEditText;
    private NutritionViewModel viewModel;
    private RecyclerView foodLogRecyclerView;
    private FoodLogAdapter foodLogAdapter;
    private FoodAdapter foodAdapter;
    private boolean searchingUSDA = false;

    // Handler for debouncing USDA searches
    private Handler handler = new Handler();
    private Runnable usdaSearchRunnable;
    private static final int USDA_SEARCH_DELAY = 500;
    private Switch fodmapSwitch;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViewModel();
        // Initialize database
        database = AppDatabase.getDatabase(requireContext());
        foodDao = database.foodDao();

        // Initialize UI components
        foodEditText = view.findViewById(R.id.foodSearchInput);
        MaterialButton logButton = view.findViewById(R.id.logFoodButton);
        fodmapSwitch = view.findViewById(R.id.ibsModeSwitch);
        foodLogRecyclerView = view.findViewById(R.id.foodLogRecyclerView);

        // Setup RecyclerView for food logs
        setupFoodLogRecyclerView();

        // Setup other UI components
        setupQuickAddFoods(view);
        setupButtons(view);
        setupSearchFunctionality();


        // Load initial data
        updateDateDisplay();
        loadTodaysFoodLogs();
    }
// Helper method to convert FoodLog to FoodLogEntity
    private FoodLogEntity convertToFoodLogEntity(FoodLog foodLog) {
        return new FoodLogEntity(
                0, // temporary ID - you'll need the real ID from database
                foodLog.getFoodName(),
                foodLog.getPreparation(),
                "Medium", // default portion
                foodLog.getMealType(),
                foodLog.getFodmapStatus(),
                foodLog.getTimestamp(),
                foodLog.getMood(),
                foodLog.getStressLevel()
               // new Date() // current date
        );
    }
    private void showEditFoodLogDialog(FoodLog foodLog) {
        // Create a simple edit dialog for FoodLog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Food: " + foodLog.getFoodName());

        // For now, show a simple message - you can enhance this later
        builder.setMessage("Editing functionality coming soon!\n\n" +
                "Food: " + foodLog.getFoodName() + "\n" +
                "Preparation: " + foodLog.getPreparation() + "\n" +
                "Meal Type: " + foodLog.getMealType() + "\n" +
                "FODMAP: " + foodLog.getFodmapStatus());

        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void deleteFoodLogFromDatabase(String foodName) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                // Get today's logs and find the matching one to delete
                List<FoodLogEntity> entities = foodDao.getTodayFoodLogs().getValue();
                if (entities != null) {
                    for (FoodLogEntity entity : entities) {
                        if (entity.getFoodName().equals(foodName)) {
                            foodDao.deleteFoodLog(entity.getId());
                            Log.d("DeleteFood", "Deleted from database: " + foodName);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("DeleteFood", "Error deleting food: " + e.getMessage());
            }
        });
    }
    private void deleteFoodLog(FoodLog foodLog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Food");
        builder.setMessage("Are you sure you want to delete " + foodLog.getFoodName() + "?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Remove from local list immediately for better UX
            foodLogs.remove(foodLog);
            displayTodaysFoodLogs();

            // Delete from database using the food name
            deleteFoodLogFromDatabase(foodLog.getFoodName());

            Toast.makeText(requireContext(), "Deleted: " + foodLog.getFoodName(), Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    private void showSwipeDeleteConfirmation(FoodLog foodLog, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Food")
                .setMessage("Delete " + foodLog.getFoodName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Remove from adapter and database
                    foodLogAdapter.deleteItem(position);
                    deleteFoodLogFromDatabase(foodLog.getFoodName());
                    Toast.makeText(requireContext(), "Deleted: " + foodLog.getFoodName(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Restore the item if cancelled
                    foodLogAdapter.notifyItemChanged(position);
                })
                .setOnCancelListener(dialog -> {
                    // Restore the item if dialog is cancelled
                    foodLogAdapter.notifyItemChanged(position);
                })
                .show();
    }
    private void setupFoodLogRecyclerView() {
        foodLogRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Create the adapter with both parameters
        foodLogAdapter = new FoodLogAdapter(new ArrayList<>(), new FoodLogAdapter.OnFoodLogClickListener() {
            @Override
            public void onFoodLogClick(FoodLog foodLog) {
                showEditFoodLogDialog(foodLog);
            }

            @Override
            public void onFoodLogDelete(FoodLog foodLog) {
                deleteFoodLog(foodLog);
            }
        });

        foodLogRecyclerView.setAdapter(foodLogAdapter);

        // ✅ ADD SWIPE TO DELETE FUNCTIONALITY
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        return false; // We don't support drag & drop
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();

                        // Show confirmation dialog before deleting
                        FoodLog foodLogToDelete = foodLogs.get(position);
                        showSwipeDeleteConfirmation(foodLogToDelete, position);
                    }
                }
        );

        itemTouchHelper.attachToRecyclerView(foodLogRecyclerView);
    }

    /*
    private void setupFoodLogRecyclerView() {
        foodLogRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Create the adapter with both parameters - use FoodLog directly
        foodLogAdapter = new FoodLogAdapter(new ArrayList<>(), new FoodLogAdapter.OnFoodLogClickListener() {
            @Override
            public void onFoodLogClick(FoodLog foodLog) {
                // Handle edit click - use FoodLog directly
                showEditFoodLogDialog(foodLog);
            }

            @Override
            public void onFoodLogDelete(FoodLog foodLog) {
                // Handle delete click - use FoodLog directly
                deleteFoodLog(foodLog);
            }
        });

        foodLogRecyclerView.setAdapter(foodLogAdapter);
    }*/

    private void setupQuickAddFoods(View view) {
        RecyclerView quickAddGrid = view.findViewById(R.id.quickAddGrid);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 4);
        quickAddGrid.setLayoutManager(layoutManager);

        List<QuickFood> quickFoods = Arrays.asList(
                new QuickFood("🍗", "Chicken", "LOW", "Cooked"),
                new QuickFood("🐟", "Fish", "LOW", "Cooked"),
                new QuickFood("🥚", "Eggs", "LOW", "Cooked"),
                new QuickFood("🧀", "Cheese", "LOW", "Raw"),
                new QuickFood("🍚", "Rice", "LOW", "Cooked"),
                new QuickFood("🌾", "Oats", "LOW", "Cooked"),
                new QuickFood("🥕", "Carrots", "LOW", "Cooked"),
                new QuickFood("🍆", "Eggplant", "LOW", "Cooked"),
                new QuickFood("🍅", "Tomatoes", "LOW", "Raw"),
                new QuickFood("🍓", "Berries", "LOW", "Raw"),
                new QuickFood("🍇", "Grapes", "LOW", "Raw"),
                new QuickFood("🍊", "Oranges", "LOW", "Raw")
        );

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
    }

    private void setupButtons(View view) {
        // Add Symptom button
        MaterialButton addSymptomButton = view.findViewById(R.id.addSymptomButton);
        addSymptomButton.setOnClickListener(v -> {
            ViewPager2 viewPager = requireActivity().findViewById(R.id.viewPager);
            if (viewPager != null) {
                viewPager.setCurrentItem(1, true);
            }
        });

        // Doctor export functionality
        View exportCard = view.findViewById(R.id.exportCard);
        exportCard.setOnClickListener(v -> shareWithDoctor());

        // Insights button
        MaterialButton insight = view.findViewById(R.id.insightButton);
        insight.setOnClickListener(v -> {
            ViewPager2 viewPager = requireActivity().findViewById(R.id.viewPager);
            if (viewPager != null) {
                viewPager.setCurrentItem(2);
                triggerInsightsRefresh();
            }
        });

        // Log Food button
        MaterialButton logFoodButton = view.findViewById(R.id.logFoodButton);
        logFoodButton.setOnClickListener(v -> logCurrentFood());
    }

    private void setupSearchFunctionality() {
        // Initialize FoodAdapter
        foodAdapter = new FoodAdapter(getContext(), android.R.layout.simple_dropdown_item_1line, viewModel);
        foodEditText.setAdapter(foodAdapter);
        foodEditText.setThreshold(1);

        // Set up text watcher for automatic searching
        foodEditText.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();
            private final long DELAY = 500;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (usdaSearchRunnable != null) {
                    handler.removeCallbacks(usdaSearchRunnable);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                timer.cancel();
                timer = new Timer();

                String query = s.toString().trim();
                if (query.length() > 2) {
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            requireActivity().runOnUiThread(() -> {
                                boolean ibsFilterEnabled = fodmapSwitch.isChecked();
                                viewModel.getFoodsWithFilter(query, ibsFilterEnabled).observe(getViewLifecycleOwner(), foods -> {
                                    foodAdapter.setLocalFoodList(foods);
                                });
                                searchingUSDA = true;
                            });
                        }
                    }, DELAY);
                } else if (query.isEmpty()) {
                    searchingUSDA = false;
                    viewModel.getCommonFoods().observe(getViewLifecycleOwner(), foods -> {
                        foodAdapter.setLocalFoodList(foods);
                    });
                }
            }
        });

        // FODMAP switch listener
        fodmapSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setFodmapFilterEnabled(isChecked);
            String query = foodEditText.getText().toString();
            if (!query.isEmpty()) {
                performSearch(query, isChecked);
            }
        });

        // Observe the filter state
        viewModel.isFodmapFilterEnabled().observe(getViewLifecycleOwner(), enabled -> {
            fodmapSwitch.setChecked(enabled);
        });

        // Set up item click listener for AutoCompleteTextView
        foodEditText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Food selectedFood = (Food) parent.getItemAtPosition(position);
                if (foodAdapter.isShowingUSDAResults()) {
                    viewModel.importUSDAFood(selectedFood);
                    foodEditText.setText(selectedFood.name);
                    Toast.makeText(getContext(), "Imported from USDA: " + selectedFood.name, Toast.LENGTH_SHORT).show();
                    viewModel.getCommonFoods().observe(getViewLifecycleOwner(), foods -> {
                        foodAdapter.setLocalFoodList(foods);
                        searchingUSDA = false;
                    });
                } else {
                    foodEditText.setText(selectedFood.name);
                }
            }
        });
    }

    private void setupViewModel() {
        try {
            NutritionViewModelFactory factory = new NutritionViewModelFactory(requireActivity().getApplication());
            viewModel = new ViewModelProvider(requireActivity(), factory).get(NutritionViewModel.class);
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Error initializing ViewModel: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

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
    }

    private void logCurrentFood() {
        String foodText = foodEditText.getText().toString().trim();
        if (!foodText.isEmpty()) {
            String foodName = foodText;
            String preparation = "Not specified";
            String mealType = "Lunch";

            if (foodText.contains("(") && foodText.contains(")")) {
                int start = foodText.indexOf("(") + 1;
                int end = foodText.indexOf(")");
                preparation = foodText.substring(start, end);
                foodName = foodText.substring(0, foodText.indexOf("(")).trim();
            }
            // Show mood selection instead of directly logging
            showMoodSelectionDialog(foodName, preparation, mealType);
           // addFoodToDatabase(foodName, preparation, mealType);
            foodEditText.setText("");
        } else {
            Toast.makeText(requireContext(), "Please select a food first", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTodaysFoodLogs() {
        Log.d("UIDebug", "Setting up LiveData observer for food logs");

        foodDao.getTodayFoodLogs().observe(getViewLifecycleOwner(), foodLogEntities -> {
            Log.d("UIDebug", "LiveData update: " + (foodLogEntities != null ? foodLogEntities.size() : 0) + " food logs");

            // Clear the existing FoodLog list
            foodLogs.clear();

            if (foodLogEntities != null && !foodLogEntities.isEmpty()) {
                Log.d("UIDebug", "Processing " + foodLogEntities.size() + " food logs");
                for (FoodLogEntity entity : foodLogEntities) {
                    // Convert each FoodLogEntity to FoodLog
                    FoodLog foodLog = new FoodLog(
                            entity.getFoodName(),
                            entity.getPreparation(),
                            entity.getMealType(),
                            entity.getFodmapStatus(),
                            entity.getTimestamp(),
                            entity.getMood(),           // Add mood
                            entity.getStressLevel()
                    );
                    foodLogs.add(foodLog);
                    Log.d("UIDebug", "Added to UI: " + entity.getFoodName() + " (" + entity.getPreparation() + ")");
                }
            } else {
                Log.d("UIDebug", "No food logs found in database");
            }

            // Now display the FoodLog list (not FoodLogEntity)
            displayTodaysFoodLogs();
        });
    }

    private void displayTodaysFoodLogs() {
        View view = getView();
        if (view == null) return;

        LinearLayout emptyState = view.findViewById(R.id.emptyLogState);
        LinearLayout logSummary = view.findViewById(R.id.logSummary);

        if (foodLogs.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            foodLogRecyclerView.setVisibility(View.GONE);
            logSummary.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            foodLogRecyclerView.setVisibility(View.VISIBLE);
            logSummary.setVisibility(View.VISIBLE);

            // ✅ This should now pass List<FoodLog> to the adapter
            if (foodLogAdapter != null) {
                foodLogAdapter.updateData(foodLogs);
            }
            updateSummaryCounts(foodLogs);
        }
    }

    private List<FoodLogEntity> convertToFoodLogEntityList(List<FoodLog> foodLogs) {
        List<FoodLogEntity> entities = new ArrayList<>();
        for (FoodLog log : foodLogs) {
            // Create a temporary FoodLogEntity - you might need to adjust this
            FoodLogEntity entity = new FoodLogEntity(
                    0, // temporary ID
                    log.getFoodName(),
                    log.getPreparation(),
                    "Medium", // default portion
                    log.getMealType(),
                    log.getFodmapStatus(),
                    log.getTimestamp(),
                    log.getMood(),
                    log.getStressLevel()
                    //new Date() // current date
            );
            entities.add(entity);
        }
        return entities;
    }

    private void updateSummaryCounts(List<FoodLog> foodLogs) {
        TextView totalFoodsText = getView().findViewById(R.id.totalFoodsText);
        TextView gutSafeText = getView().findViewById(R.id.gutSafeText);
        TextView triggersText = getView().findViewById(R.id.triggersText);

        int totalFoods = foodLogs.size();
        int gutSafeCount = 0;
        int triggerCount = 0;

        for (FoodLog foodLog : foodLogs) {
            String status = foodLog.getFodmapStatus();
            if (status != null && status.equalsIgnoreCase("low")) {
                gutSafeCount++;
            } else {
                triggerCount++;
            }
        }

        if (totalFoodsText != null) totalFoodsText.setText(String.valueOf(totalFoods));
        if (gutSafeText != null) gutSafeText.setText(String.valueOf(gutSafeCount));
        if (triggersText != null) triggersText.setText(String.valueOf(triggerCount));
    }

    // Helper methods (keep your existing implementations)
    private String determineFODMAPStatus(String foodName) {
        String[] lowFodmapFoods = {"chicken", "fish", "eggs", "rice", "oats", "carrots", "eggplant", "tomatoes", "grapes", "oranges", "cheese"};
        for (String food : lowFodmapFoods) {
            if (foodName.toLowerCase().contains(food)) {
                return "LOW";
            }
        }
        return "HIGH";
    }

    private void shareWithDoctor() {
        // Show date selection dialog first
        showDateSelectionDialog();
    }

    private void showDateSelectionDialog() {
        final String[] dateOptions = {
                "📅 Yesterday's Report",
                "📊 Last 7 Days",
                "📈 Custom Date Range",
                "📋 All Time Summary",
                "🖨️ Generate PDF Report"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Report Period");
        builder.setItems(dateOptions, (dialog, which) -> {
            switch (which) {
                case 0: // Yesterday Text
                    generateAndShareYesterdaysReport();
                    break;
                case 1: // Last 7 Days Text
                    generateAndShareLast7DaysReport();
                    break;
                case 2: // All Time Text
                    showCustomDateRangePicker(); // Using last 7 days as default
                    break;
                case 3: // All Time text
                    generateAndShareAllTimeReport();
                    break;
                case 4: // PDF Report
                    showPdfDateSelectionDialog();
                    //testSimplePdf();
                    break;

            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    private void showCustomDateRangePicker() {
        // Get current date
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create date picker for start date
        DatePickerDialog startDatePicker = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar startDate = Calendar.getInstance();
                    startDate.set(selectedYear, selectedMonth, selectedDay);

                    // After selecting start date, show end date picker
                    showEndDatePicker(startDate);

                }, year, month, day);

        startDatePicker.setTitle("Select Start Date");
        startDatePicker.show();
    }

    private void showEndDatePicker(final Calendar startDate) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog endDatePicker = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar endDate = Calendar.getInstance();
                    endDate.set(selectedYear, selectedMonth, selectedDay);

                    // Generate report for the custom date range
                    generateCustomDateRangeReport(startDate, endDate);

                }, year, month, day);

        endDatePicker.setTitle("Select End Date");
        endDatePicker.show();
    }

    private void generateCustomDateRangeReport(Calendar startDate, Calendar endDate) {
        showLoading(true);

        AppDatabase database = AppDatabase.getDatabase(requireContext());
        FoodDao foodDao = database.foodDao();

        // Convert dates to timestamps
        long startTimestamp = startDate.getTimeInMillis();
        long endTimestamp = endDate.getTimeInMillis();

        // Format date range for display - FIX: Define dateRangeStr here
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String dateRangeStr = sdf.format(startDate.getTime()) + " to " + sdf.format(endDate.getTime());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<FoodLogEntity> foodLogs = foodDao.getFoodLogsByDateRangeSync(startTimestamp, endTimestamp);
                List<Symptom> symptoms = foodDao.getSymptomsByDateRangeSync(startTimestamp, endTimestamp);

                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    // FIX: Add symptoms parameter and use dateRangeStr
                    String report = generateEnhancedDoctorReport(foodLogs, symptoms, dateRangeStr);
                    shareReport(report);
                });

            } catch (Exception e) {
                // Error handling
            }
        });

    }

    private String generateEnhancedDoctorReport(List<FoodLogEntity> foodLogs, List<Symptom> symptoms, String period) {
        StringBuilder report = new StringBuilder();

        // ===== PROFESSIONAL HEADER =====
        report.append("🩺 GUT NUTRITION MANAGER - CLINICAL REPORT\n");
        report.append("============================================\n\n");

        report.append("REPORT PERIOD: ").append(period).append("\n");
        report.append("GENERATED: ").append(new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(new Date())).append("\n");
        report.append("PATIENT: [User's Data]\n\n");

        // ===== EXECUTIVE SUMMARY =====
        report.append("1. EXECUTIVE SUMMARY\n");
        report.append("====================\n");
        report.append("• Total meals logged: ").append(foodLogs.size()).append("\n");
        report.append("• Total symptoms recorded: ").append(symptoms.size()).append("\n");

        // Calculate gut-safe percentage
        long lowFodmapCount = foodLogs.stream().filter(f -> "LOW".equals(f.getFodmapStatus())).count();
        int gutSafePercentage = foodLogs.isEmpty() ? 0 : (int) ((lowFodmapCount * 100) / foodLogs.size());
        report.append("• Gut-safe food consumption: ").append(gutSafePercentage).append("%\n\n");

        // ===== SYMPTOM ANALYSIS =====
        report.append("2. SYMPTOM ANALYSIS\n");
        report.append("===================\n");

        if (symptoms.isEmpty()) {
            report.append("No symptoms recorded for this period.\n\n");
        } else {
            // Use your existing symptom analysis logic
            Map<String, Integer> symptomCount = new HashMap<>();
            Map<String, Double> symptomAvgSeverity = new HashMap<>();

            for (Symptom symptom : symptoms) {
                symptomCount.put(symptom.name, symptomCount.getOrDefault(symptom.name, 0) + 1);

                double currentAvg = symptomAvgSeverity.getOrDefault(symptom.name, 0.0);
                int count = symptomCount.get(symptom.name);
                double newAvg = (currentAvg * (count - 1) + symptom.severity) / count;
                symptomAvgSeverity.put(symptom.name, newAvg);
            }

            for (Map.Entry<String, Integer> entry : symptomCount.entrySet()) {
                String symptomName = entry.getKey();
                int frequency = entry.getValue();
                double avgSeverity = symptomAvgSeverity.get(symptomName);

                report.append("• ").append(symptomName)
                        .append(": ").append(frequency).append(" occurrences")
                        .append(" (Avg severity: ").append(String.format("%.1f/5", avgSeverity)).append(")\n");
            }
            report.append("\n");
        }

        // ===== BRAIN-GUT CONNECTION ANALYSIS =====
        report.append("3. BRAIN-GUT CONNECTION\n");
        report.append("========================\n");

        // Stress Impact Analysis (from your generateBrainGutInsights)
        if (!foodLogs.isEmpty()) {
            double totalStress = 0;
            double highStressMeals = 0;

            for (FoodLogEntity food : foodLogs) {
                int stress = food.getStressLevel();
                totalStress += stress;
                if (stress >= 4) highStressMeals++;
            }

            double avgStress = totalStress / foodLogs.size();
            double highStressPercent = (highStressMeals / foodLogs.size()) * 100;

            report.append("Stress Analysis:\n");
            report.append("• Average meal stress: ").append(String.format("%.1f/5", avgStress)).append("\n");
            report.append("• High-stress meals: ").append(String.format("%.0f", highStressPercent)).append("%\n");

            // Clinical interpretation
            if (avgStress > 3.5) {
                report.append("• CLINICAL NOTE: Elevated stress during meals may exacerbate GI symptoms\n");
            } else if (avgStress < 2.5) {
                report.append("• CLINICAL NOTE: Good stress management during meals observed\n");
            }
            report.append("\n");
        }

        // ===== MOOD-SYMPTOM PATTERNS =====
        report.append("4. MOOD-SYMPTOM PATTERNS\n");
        report.append("=========================\n");
        if (!foodLogs.isEmpty()) {
            Map<String, Integer> moodCount = new HashMap<>();
            Map<String, Integer> moodSymptomCount = new HashMap<>();

            // Use text-only mood names for PDF compatibility
            Map<String, String> moodMapping = new HashMap<>();
            moodMapping.put("😊 Calm", "Calm");
            moodMapping.put("😐 Neutral", "Neutral");
            moodMapping.put("😰 Stressed", "Stressed");
            moodMapping.put("😟 Anxious", "Anxious");
            moodMapping.put("😴 Tired", "Tired");

            for (FoodLogEntity food : foodLogs) {
                String originalMood = food.getMood();
                String cleanMood = moodMapping.getOrDefault(originalMood, originalMood);

                moodCount.put(cleanMood, moodCount.getOrDefault(cleanMood, 0) + 1);
                moodSymptomCount.put(cleanMood, moodSymptomCount.getOrDefault(cleanMood, 0) + 1);
            }

            report.append("Mood Distribution:\n");
            for (Map.Entry<String, Integer> entry : moodCount.entrySet()) {
                String mood = entry.getKey();
                int totalMeals = entry.getValue();
                int symptomaticMeals = moodSymptomCount.getOrDefault(mood, 0);
                int percentage = totalMeals == 0 ? 0 : (symptomaticMeals * 100) / totalMeals;

                report.append("• ").append(mood).append(": ")
                        .append(symptomaticMeals).append("/").append(totalMeals)
                        .append(" meals had symptoms (").append(percentage).append("%)\n");
            }

            // FIXED: Proper clinical insight check
            boolean allMoodsSymptomatic = true;
            for (Map.Entry<String, Integer> entry : moodCount.entrySet()) {
                String mood = entry.getKey();
                int totalMeals = entry.getValue();
                int symptomaticMeals = moodSymptomCount.getOrDefault(mood, 0);

                if (symptomaticMeals != totalMeals) {
                    allMoodsSymptomatic = false;
                    break;
                }
            }

            if (allMoodsSymptomatic) {
                report.append("\nCLINICAL NOTE: Symptoms occur consistently across all emotional states.\n");
                report.append("Suggests potential food triggers rather than psychological factors.\n");
            }
        } else {
            report.append("No mood data available for analysis.\n");
        }
        report.append("\n");

       /* if (!foodLogs.isEmpty() && !symptoms.isEmpty()) {
            Map<String, Integer> moodCount = new HashMap<>();
            Map<String, Integer> moodSymptomCount = new HashMap<>();

            for (FoodLogEntity food : foodLogs) {
                String mood = food.getMood();
                moodCount.put(mood, moodCount.getOrDefault(mood, 0) + 1);

                // Simplified correlation - in real app, match by timestamp
                moodSymptomCount.put(mood, moodSymptomCount.getOrDefault(mood, 0) + 1);
            }

            report.append("Mood Distribution:\n");
            for (Map.Entry<String, Integer> entry : moodCount.entrySet()) {
                String mood = entry.getKey();
                int totalMeals = entry.getValue();
                int symptomaticMeals = moodSymptomCount.getOrDefault(mood, 0);
                int percentage = totalMeals == 0 ? 0 : (symptomaticMeals * 100) / totalMeals;

                report.append("• ").append(mood).append(": ")
                        .append(symptomaticMeals).append("/").append(totalMeals)
                        .append(" meals had symptoms (").append(percentage).append("%)\n");
            }
            report.append("\n");
        }*/

        // ===== FOOD ANALYSIS =====
        report.append("5. FOOD CONSUMPTION PATTERNS\n");
        report.append("=============================\n");

        if (!foodLogs.isEmpty()) {
            // Most common foods (from your generateBasicInsights)
            Map<String, Integer> foodCounts = new HashMap<>();
            for (FoodLogEntity food : foodLogs) {
                foodCounts.put(food.getFoodName(), foodCounts.getOrDefault(food.getFoodName(), 0) + 1);
            }

            report.append("Top Foods Consumed:\n");
            foodCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> {
                        report.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" times\n");
                    });
            report.append("\n");

            // FODMAP Status Summary
            report.append("FODMAP Status Summary:\n");
            report.append("• Gut-safe (LOW FODMAP): ").append(lowFodmapCount).append(" foods\n");
            report.append("• Potential triggers (HIGH FODMAP): ").append(foodLogs.size() - lowFodmapCount).append(" foods\n\n");
        }

        // ===== CLINICAL RECOMMENDATIONS =====
        report.append("6. CLINICAL RECOMMENDATIONS\n");
        report.append("============================\n");

        // Generate personalized recommendations based on the data
        if (!foodLogs.isEmpty()) {
            double avgStress = foodLogs.stream().mapToInt(FoodLogEntity::getStressLevel).average().orElse(0);
            long highFodmapCount = foodLogs.size() - lowFodmapCount;

            if (avgStress > 3.0) {
                report.append("• Implement stress-reduction techniques before meals\n");
                report.append("• Consider mindful eating practices\n");
                report.append("• Explore relaxation exercises (4-7-8 breathing)\n");
            }

            if (highFodmapCount > lowFodmapCount) {
                report.append("• Consider reducing HIGH FODMAP food intake\n");
                report.append("• Monitor symptoms after consuming identified trigger foods\n");
            }

            if (symptoms.size() > 5) {
                report.append("• Continue detailed symptom tracking for pattern identification\n");
                report.append("• Consider food-symptom correlation analysis\n");
            }

            if (report.toString().contains("CLINICAL RECOMMENDATIONS\n")) {
                // If no specific recommendations were added
                report.append("• Continue current tracking regimen\n");
                report.append("• Maintain balanced diet with stress management\n");
            }
        }

        report.append("\n");

        // ===== DETAILED LOGS (Optional - for comprehensive review) =====
        report.append("7. DETAILED ACTIVITY LOG\n");
        report.append("=========================\n");
        report.append("[Detailed food and symptom logs available in app]\n");
        report.append("• Total entries: ").append(foodLogs.size()).append(" foods, ").append(symptoms.size()).append(" symptoms\n");
        report.append("• Time range: ").append(period).append("\n\n");

        // ===== FOOTER =====
        report.append("============================================\n");
        report.append("Generated by Gut Nutrition Manager\n");
        report.append("For clinical use - Consult healthcare provider\n");
        report.append("for personalized medical advice\n");

        return report.toString();
    }
    /*
    private String generateEnhancedDoctorReport(List<FoodLogEntity> foodLogs, List<Symptom> symptoms, String period) {
        StringBuilder report = new StringBuilder();

        report.append("🩺 GUT NUTRITION MANAGER - MEDICAL REPORT\n");
        report.append("============================================\n\n");

        // Header with period
        report.append("REPORT PERIOD: ").append(period).append("\n");
        report.append("GENERATED: ").append(new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(new Date())).append("\n\n");

        // 1. SYMPTOM SUMMARY SECTION - NEW!
        report.append("SYMPTOM SUMMARY:\n");
        report.append("----------------\n");

        if (symptoms == null || symptoms.isEmpty()) {
            report.append("No symptoms logged for this period.\n\n");
        } else {
            report.append("Total symptoms logged: ").append(symptoms.size()).append("\n");

            // Group symptoms by type and calculate averages
            Map<String, List<Integer>> symptomSeverities = new HashMap<>();
            for (Symptom symptom : symptoms) {
                symptomSeverities.computeIfAbsent(symptom.getName(), k -> new ArrayList<>())
                        .add(symptom.getSeverity());
            }

            for (Map.Entry<String, List<Integer>> entry : symptomSeverities.entrySet()) {
                String symptomName = entry.getKey();
                List<Integer> severities = entry.getValue();
                double avgSeverity = severities.stream().mapToInt(Integer::intValue).average().orElse(0);

                report.append("• ").append(symptomName)
                        .append(": ").append(severities.size()).append(" times")
                        .append(", Avg severity: ").append(String.format("%.1f/5", avgSeverity)).append("\n");
            }
            report.append("\n");
        }

        // 2. FOOD SUMMARY (your existing code)
        if (foodLogs == null || foodLogs.isEmpty()) {
            report.append("No food data available for the selected period.\n");
            return report.toString();
        }

        report.append("FOOD SUMMARY:\n");
        report.append("-------------\n");
        report.append("• Total meals logged: ").append(foodLogs.size()).append("\n");

        // ... rest of your existing food analysis code ...

        return report.toString();
    }*/

    private void showPdfDateSelectionDialog() {
        final String[] pdfOptions = {
                "📅 PDF: Yesterday's Report",
                "📊 PDF: Last 7 Days",
                "📋 PDF: All Time Summary",
                "📅 PDF: Custom Date Range"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Generate PDF Report");
        builder.setItems(pdfOptions, (dialog, which) -> {
            switch (which) {
                case 0: // PDF Yesterday
                    generatePdfReport("Yesterday");
                    break;
                case 1: // PDF Last 7 Days
                    generatePdfReport("Last 7 Days");
                    break;
                case 2: // PDF All Time
                    generatePdfReport("All Time");
                    break;
                case 3: // PDF Custom Date Range - NEW!
                    showCustomDateRangePickerForPdf();
                    break;

            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void generatePdfForCustomDateRange(Calendar startDate, Calendar endDate) {
        showLoading(true);

        // Format dates for display
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String dateRangeStr = sdf.format(startDate.getTime()) + " to " + sdf.format(endDate.getTime());

        Toast.makeText(requireContext(), "Generating PDF for " + dateRangeStr, Toast.LENGTH_SHORT).show();

        AppDatabase database = AppDatabase.getDatabase(requireContext());
        FoodDao foodDao = database.foodDao();

        long startTimestamp = startDate.getTimeInMillis();
        long endTimestamp = endDate.getTimeInMillis();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<FoodLogEntity> foodLogs = foodDao.getFoodLogsByDateRangeSync(startTimestamp, endTimestamp);
                List<Symptom> symptoms = foodDao.getSymptomsByDateRangeSync(startTimestamp, endTimestamp);

                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    createAndSharePdf(foodLogs, symptoms, dateRangeStr);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });

    }
    private void showEndDatePickerForPdf(final Calendar startDate) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog endDatePicker = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar endDate = Calendar.getInstance();
                    endDate.set(selectedYear, selectedMonth, selectedDay, 23, 59, 59);

                    if (endDate.before(startDate)) {
                        Toast.makeText(requireContext(), "End date cannot be before start date", Toast.LENGTH_SHORT).show();
                        showEndDatePickerForPdf(startDate);
                        return;
                    }

                    generatePdfForCustomDateRange(startDate, endDate);

                }, year, month, day);

        endDatePicker.setTitle("Select End Date (PDF)");
        endDatePicker.getDatePicker().setMinDate(startDate.getTimeInMillis());
        endDatePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
        endDatePicker.show();
    }

    private void showCustomDateRangePickerForPdf() {
        // Get current date
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog startDatePicker = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar startDate = Calendar.getInstance();
                    startDate.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0);

                    showEndDatePickerForPdf(startDate);

                }, year, month, day);

        startDatePicker.setTitle("Select Start Date (PDF)");
        startDatePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
        startDatePicker.show();
    }


    private void generatePdfReport(String period) {
        Log.d("PDFDebug", "=== STARTING PDF GENERATION ===");
        Log.d("PDFDebug", "Period selected: " + period);

        showLoading(true);
        Toast.makeText(requireContext(), "Generating PDF report...", Toast.LENGTH_SHORT).show();

        AppDatabase database = AppDatabase.getDatabase(requireContext());
        FoodDao foodDao = database.foodDao();

        Log.d("PDFDebug", "Database initialized, getting data for: " + period);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<FoodLogEntity> foodLogs;
                List<Symptom> symptoms;

                // Get data based on period using sync methods
                if ("Yesterday".equals(period)) {
                    Log.d("PDFDebug", "Getting yesterday's data");
                    foodLogs = foodDao.getYesterdayFoodLogsSync();
                    symptoms = foodDao.getSymptomsByDateRangeSync(getYesterdayStartTime(), getYesterdayEndTime());
                } else if ("Last 7 Days".equals(period)) {
                    Log.d("PDFDebug", "Getting last 7 days data");
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DATE, -7);
                    long sevenDaysAgo = cal.getTimeInMillis();

                    foodLogs = foodDao.getLast7DaysFoodLogsSync(sevenDaysAgo);
                    symptoms = foodDao.getSymptomsByDateRangeSync(sevenDaysAgo, System.currentTimeMillis());
                } else {
                    Log.d("PDFDebug", "Getting all time data");
                    foodLogs = foodDao.getAllFoodLogsSync();
                    symptoms = foodDao.getAllSymptomsSync();
                }

                Log.d("PDFDebug", "Data received - Foods: " + foodLogs.size() + ", Symptoms: " + symptoms.size());

                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    createAndSharePdf(foodLogs, symptoms, period);
                });

            } catch (Exception e) {
                Log.e("PDFDebug", "Error generating PDF: " + e.getMessage(), e);
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void createAndSharePdf(List<FoodLogEntity> foodLogs, List<Symptom> symptoms, String period) {
        showLoading(false);

        // Generate the comprehensive report text
        String reportText = generateEnhancedDoctorReport(foodLogs, symptoms, period);

        // Generate PDF with the comprehensive report
        PdfReportGenerator pdfGenerator = new PdfReportGenerator(requireContext());
        File pdfFile = pdfGenerator.generateDoctorReport(reportText, period); // We need to update PdfReportGenerator

        if (pdfFile != null && pdfFile.exists()) {
            sharePdfFile(pdfFile);
            Toast.makeText(requireContext(), "PDF report generated successfully!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(requireContext(), "Failed to generate PDF report", Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePdfFile(File pdfFile) {
        try {
            // Use FileProvider for secure file sharing
            Uri contentUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    pdfFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Gut Nutrition Manager Report");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Please find attached my gut health report.");

            // Grant temporary read permission
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share PDF Report"));

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error sharing PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("PDFShare", "Error sharing PDF", e);
        }
    }

    private void shareReport(String report) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My IBS Food Diary Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, report);
        startActivity(Intent.createChooser(shareIntent, "Share with Doctor"));
    }

    private void showLoading(boolean loading) {
        // You can add a progress dialog here if needed
        if (loading) {
            Toast.makeText(requireContext(), "Generating report...", Toast.LENGTH_SHORT).show();
        }
    }
    private void generateAndShareYesterdaysReport() {
        showLoading(true);

        AppDatabase database = AppDatabase.getDatabase(requireContext());
        FoodDao foodDao = database.foodDao();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<FoodLogEntity> foodLogs = foodDao.getYesterdayFoodLogsSync();
                List<Symptom> symptoms = foodDao.getSymptomsByDateRangeSync(getYesterdayStartTime(), getYesterdayEndTime());

                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    // FIX: Add symptoms parameter
                    String report = generateEnhancedDoctorReport(foodLogs, symptoms, "Yesterday");
                    shareReport(report);
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });

    }

    private void generateAndShareLast7DaysReport() {
        showLoading(true);

        AppDatabase database = AppDatabase.getDatabase(requireContext());
        FoodDao foodDao = database.foodDao();

        // Calculate 7 days ago timestamp
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -7);
        long sevenDaysAgo = cal.getTimeInMillis();


        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<FoodLogEntity> foodLogs = foodDao.getLast7DaysFoodLogsSync(sevenDaysAgo);
                List<Symptom> symptoms = foodDao.getSymptomsByDateRangeSync(getLast7DaysStartTime(), getLast7DaysEndTime());

                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    // FIX: Add symptoms parameter
                    String report = generateEnhancedDoctorReport(foodLogs, symptoms, "Last 7 Days");
                    shareReport(report);
                });

            } catch (Exception e) {
                // Error handling
            }
        });

    }
    private void generateAndShareAllTimeReport() {
        showLoading(true);

        AppDatabase database = AppDatabase.getDatabase(requireContext());
        FoodDao foodDao = database.foodDao();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<FoodLogEntity> foodLogs = foodDao.getAllFoodLogsSync();
                List<Symptom> symptoms = foodDao.getAllSymptomsSync();

                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    // FIX: Add symptoms parameter
                    String report = generateEnhancedDoctorReport(foodLogs, symptoms, "All Time");
                    shareReport(report);
                });

            } catch (Exception e) {
                // Error handling
            }
        });



    }



    // Add these helper methods for date calculations
    private long getYesterdayStartTime() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getYesterdayEndTime() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        return cal.getTimeInMillis();
    }
    // Add these helper methods to DiaryFragment.java




    private long getLast7DaysStartTime() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -7);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getLast7DaysEndTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        return cal.getTimeInMillis();
    }


    private void showPreparationDialog(QuickFood food) {
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
        AutoCompleteTextView searchInput = getView().findViewById(R.id.foodSearchInput);
        String searchText = food.getName() + " (" + preparation + ")";
        searchInput.setText(searchText);
        Toast.makeText(requireContext(), "Ready to log: " + food.getName() + " (" + preparation + ")", Toast.LENGTH_SHORT).show();
        logFoodWithPreparation(food.getName(), preparation, food.getFodmapStatus());
    }

    private void logFoodWithPreparation(String foodName, String preparation, String fodmapStatus) {
        Log.d("FoodTracking", "Food: " + foodName + ", Preparation: " + preparation + ", FODMAP: " + fodmapStatus);
    }

    private boolean requiresPreparationChoice(QuickFood food) {
        String[] choiceFoods = {"Carrots", "Eggplant", "Tomatoes", "Onions", "Garlic", "Broccoli"};
        return Arrays.asList(choiceFoods).contains(food.getName());
    }

    private void triggerInsightsRefresh() {
        FragmentManager fragmentManager = getParentFragmentManager();
        Fragment insightsFragment = fragmentManager.findFragmentByTag("f" + 2);
        if (insightsFragment instanceof InsightsFragment) {
            ((InsightsFragment) insightsFragment).refreshInsights();
        }
    }

    private void performSearch(String query, boolean fodmapOnly) {
        viewModel.getFoodsWithFilter(query, fodmapOnly).observe(getViewLifecycleOwner(), foods -> {
            if (searchingUSDA) {
                foodAdapter.setUSDAFoodList(foods);
            } else {
                foodAdapter.setLocalFoodList(foods);
            }
        });
    }

    private void updateDateDisplay() {
        TextView todayDate = getView().findViewById(R.id.todayDate);
        if (todayDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            String currentDate = sdf.format(new Date());
            todayDate.setText(currentDate);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (usdaSearchRunnable != null) {
            handler.removeCallbacks(usdaSearchRunnable);
        }
    }
    private void showMoodSelectionDialog(String foodName, String preparation, String mealType) {
        final String[] moods = {"😊 Calm", "😐 Neutral", "😰 Stressed", "😟 Anxious", "😴 Tired"};
        final int[] selectedStress = {2}; // Default: Medium stress
        final String[] selectedMood = {moods[1]}; // Default: Neutral

        // Create bottom sheet dialog
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_mood_selection, null);
        dialog.setContentView(dialogView);

        // Setup mood buttons
        LinearLayout moodLayout = dialogView.findViewById(R.id.moodLayout);

        for (String mood : moods) {
            Button moodButton = new Button(requireContext());
            moodButton.setText(mood);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 4, 8, 4);

            moodButton.setLayoutParams(params);
            moodButton.setPadding(16, 12, 16, 12);
            moodButton.setTextSize(14);
            moodButton.setBackgroundResource(R.drawable.mood_button_unselected);

            moodButton.setOnClickListener(v -> {
                // Reset all buttons
                for (int i = 0; i < moodLayout.getChildCount(); i++) {
                    Button btn = (Button) moodLayout.getChildAt(i);
                    btn.setBackgroundResource(R.drawable.mood_button_unselected);
                }
                // Select current button
                moodButton.setBackgroundResource(R.drawable.mood_button_selected);
                selectedMood[0] = mood;
            });

            moodLayout.addView(moodButton);
        }

        // Setup stress level seekbar
        SeekBar stressSeekbar = dialogView.findViewById(R.id.stressSeekbar);
        TextView stressText = dialogView.findViewById(R.id.stressText);

        stressSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedStress[0] = progress + 1;
                stressText.setText("Stress Level: " + selectedStress[0] + "/5");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Set positive button (Log Food)
        dialogView.findViewById(R.id.positiveButton).setOnClickListener(v -> {
            addFoodToDatabase(foodName, preparation, mealType, selectedMood[0], selectedStress[0]);
            dialog.dismiss();
        });

        // Set negative button (Cancel)
        dialogView.findViewById(R.id.negativeButton).setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }
/*
    private void showMoodSelectionDialog(String foodName, String preparation, String mealType) {
        final String[] moods = {"😊 Calm", "😐 Neutral", "😰 Stressed", "😟 Anxious", "😴 Tired"};
        final Integer[] stressLevels = {1, 2, 3, 4, 5}; // 1=Low stress, 5=High stress

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("How are you feeling?");
        builder.setMessage("Select your mood and stress level before eating");

        // Inflate custom layout for mood selection
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_mood_selection, null);
        builder.setView(dialogView);

        // Setup mood buttons
        LinearLayout moodLayout = dialogView.findViewById(R.id.moodLayout);
        final String[] selectedMood = {moods[1]}; // Default: Neutral
        final int[] selectedStress = {2}; // Default: Medium stress

        for (String mood : moods) {
            Button moodButton = new Button(requireContext());
            moodButton.setText(mood);
            // Set layout parameters for vertical layout
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, // FULL WIDTH
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 2, 8, 2); // Vertical margins

            moodButton.setLayoutParams(params);
            moodButton.setPadding(16, 8, 16, 8); // More padding for easier tapping
            moodButton.setTextSize(14); // Comfortable text size

            moodButton.setBackgroundResource(R.drawable.mood_button_unselected);
            moodButton.setOnClickListener(v -> {

                // Reset all buttons
                for (int i = 0; i < moodLayout.getChildCount(); i++) {
                    Button btn = (Button) moodLayout.getChildAt(i);
                    btn.setBackgroundResource(R.drawable.mood_button_unselected);
                }
                // Select current button
                moodButton.setBackgroundResource(R.drawable.mood_button_selected);
                selectedMood[0] = mood;
            });
            moodLayout.addView(moodButton);
        }

        // Setup stress level seekbar
        SeekBar stressSeekbar = dialogView.findViewById(R.id.stressSeekbar);
        TextView stressText = dialogView.findViewById(R.id.stressText);

        stressSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedStress[0] = progress + 1; // Convert 0-4 to 1-5
                stressText.setText("Stress Level: " + selectedStress[0] + "/5");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        builder.setPositiveButton("Log Food", (dialog, which) -> {
            // Create FoodLog with mood and stress data
           // FoodLog newFoodLog = new FoodLog(foodName, preparation, mealType,
              //      determineFODMAPStatus(foodName), selectedMood[0], selectedStress[0]);

            // Add to database and update UI
            addFoodToDatabase(foodName, preparation, mealType, selectedMood[0], selectedStress[0]);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }*/


    private void addFoodToDatabase(String foodName, String preparation, String mealType, String mood, int stressLevel) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                String fodmapStatus = foodDao.getFodmapStatusByName(foodName);
                if (fodmapStatus == null) {
                    fodmapStatus = determineFODMAPStatus(foodName);
                }

                FoodLogEntity foodLog = new FoodLogEntity(
                        0,
                        foodName,
                        preparation,
                        "Medium",
                        mealType,
                        fodmapStatus,
                        new Date(),
                        mood,        // NEW: Store mood
                        stressLevel  // NEW: Store stress level
                );

                foodDao.insert(foodLog);
                requireActivity().runOnUiThread(() -> {
                    loadTodaysFoodLogs();
                    Toast.makeText(requireContext(), "Logged: " + foodName + " (Mood: " + mood + ")", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error logging food: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }


}