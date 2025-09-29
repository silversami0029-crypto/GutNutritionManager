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
        Toast.makeText(requireContext(), "Generating custom date range report...", Toast.LENGTH_SHORT).show();

        AppDatabase database = AppDatabase.getDatabase(requireContext());
        FoodDao foodDao = database.foodDao();

        // Convert dates to timestamps
        long startTimestamp = startDate.getTimeInMillis();
        long endTimestamp = endDate.getTimeInMillis();

        // Get food logs for the custom date range
        foodDao.getFoodLogsByDateRange(startTimestamp, endTimestamp).observe(getViewLifecycleOwner(), foodLogs -> {
            showLoading(false);

            // Format date range for display
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateRangeStr = sdf.format(startDate.getTime()) + " to " + sdf.format(endDate.getTime());

            String report = generateEnhancedDoctorReport(foodLogs, dateRangeStr);
            shareReport(report);
        });
    }

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

        // Get food logs for the custom date range
        foodDao.getFoodLogsByDateRange(startTimestamp, endTimestamp).observe(getViewLifecycleOwner(), foodLogs -> {
            createAndSharePdf(foodLogs, dateRangeStr);
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

        Log.d("PDFDebug", "Database initialized, getting food logs for: " + period);

        // Get data based on period
        if ("Yesterday".equals(period)) {
            Log.d("PDFDebug", "Getting yesterday's logs");
            foodDao.getYesterdayFoodLogs().observe(getViewLifecycleOwner(), foodLogs -> {
                Log.d("PDFDebug", "Yesterday logs received: " + (foodLogs != null ? foodLogs.size() : 0));
                createAndSharePdf(foodLogs, period);
            });
        } else if ("Last 7 Days".equals(period)) {
            Log.d("PDFDebug", "Getting last 7 days logs");
            foodDao.getLast7DaysFoodLogs().observe(getViewLifecycleOwner(), foodLogs -> {
                Log.d("PDFDebug", "Last 7 days logs received: " + (foodLogs != null ? foodLogs.size() : 0));
                createAndSharePdf(foodLogs, period);
            });
        } else {
            Log.d("PDFDebug", "Getting all time logs");
            foodDao.getAllFoodLogs().observe(getViewLifecycleOwner(), foodLogs -> {
                Log.d("PDFDebug", "All time logs received: " + (foodLogs != null ? foodLogs.size() : 0));

                createAndSharePdf(foodLogs, period);
            });
        }
    }

    private void createAndSharePdf(List<FoodLogEntity> foodLogs, String period) {
        showLoading(false);

        // Generate PDF
        PdfReportGenerator pdfGenerator = new PdfReportGenerator(requireContext());
        File pdfFile = pdfGenerator.generateDoctorReport(foodLogs, period);

        if (pdfFile != null && pdfFile.exists()) {
            // Share the PDF file
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

        foodDao.getYesterdayFoodLogs().observe(getViewLifecycleOwner(), foodLogs -> {
            showLoading(false);
            String report = generateEnhancedDoctorReport(foodLogs, "Yesterday");
            shareReport(report);
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

        foodDao.getLast7DaysFoodLogs().observe(getViewLifecycleOwner(), foodLogs -> {
            showLoading(false);
            String report = generateEnhancedDoctorReport(foodLogs, "Last 7 Days");
            shareReport(report);
        });
    }
    private void generateAndShareAllTimeReport() {
        showLoading(true);

        AppDatabase database = AppDatabase.getDatabase(requireContext());
        FoodDao foodDao = database.foodDao();

        foodDao.getAllFoodLogs().observe(getViewLifecycleOwner(), foodLogs -> {
            showLoading(false);
            String report = generateEnhancedDoctorReport(foodLogs, "All Time");
            shareReport(report);
        });
    }


    private String generateEnhancedDoctorReport(List<FoodLogEntity> foodLogs, String period) {
        StringBuilder report = new StringBuilder();

        report.append("🩺 GUT NUTRITION MANAGER - MEDICAL REPORT\n");
        report.append("============================================\n\n");

        // Header with period
        report.append("REPORT PERIOD: ").append(period).append("\n");
        report.append("GENERATED: ").append(new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(new Date())).append("\n\n");

        if (foodLogs == null || foodLogs.isEmpty()) {
            report.append("No food data available for the selected period.\n");
            return report.toString();
        }

        // 1. SUMMARY STATISTICS
        report.append("SUMMARY STATISTICS:\n");
        report.append("-------------------\n");
        report.append("• Total meals logged: ").append(foodLogs.size()).append("\n");

        // Calculate statistics
        int lowFodmapCount = 0;
        int totalStress = 0;
        Map<String, Integer> moodCounts = new HashMap<>();
        Map<String, Integer> mealTypeCounts = new HashMap<>();

        for (FoodLogEntity food : foodLogs) {
            // Count FODMAP status
            if ("LOW".equals(food.getFodmapStatus())) {
                lowFodmapCount++;
            }

            // Sum stress for average
            totalStress += food.getStressLevel();

            // Count moods
            String mood = food.getMood();
            moodCounts.put(mood, moodCounts.getOrDefault(mood, 0) + 1);

            // Count meal types
            String mealType = food.getMealType();
            mealTypeCounts.put(mealType, mealTypeCounts.getOrDefault(mealType, 0) + 1);
        }

        double avgStress = (double) totalStress / foodLogs.size();
        int gutSafePercentage = (lowFodmapCount * 100) / foodLogs.size();

        report.append("• Gut-safe foods: ").append(lowFodmapCount).append("/").append(foodLogs.size())
                .append(" (").append(gutSafePercentage).append("%)\n");
        report.append("• Average meal stress: ").append(String.format("%.1f/5", avgStress)).append("\n");

        // 2. MOOD & STRESS INSIGHTS
        report.append("\nMOOD & STRESS ANALYSIS:\n");
        report.append("------------------------\n");

        if (!moodCounts.isEmpty()) {
            report.append("Mood distribution:\n");
            moodCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(entry -> {
                        int percentage = (entry.getValue() * 100) / foodLogs.size();
                        report.append("• ").append(entry.getKey()).append(": ").append(entry.getValue())
                                .append(" meals (").append(percentage).append("%)\n");
                    });
        }

        // Brain-gut insight
        if (avgStress > 3.5) {
            report.append("🔍 High stress levels during meals may impact digestion\n");
        } else if (avgStress < 2.5) {
            report.append("🌟 Generally eating in a relaxed state\n");
        }

        // 3. DETAILED FOOD LOG
        report.append("\nDETAILED FOOD LOG:\n");
        report.append("------------------\n");

        // Group by date for better organization
        Map<String, List<FoodLogEntity>> logsByDate = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        for (FoodLogEntity food : foodLogs) {
            String dateKey = dateFormat.format(food.getTimestamp());
            logsByDate.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(food);
        }

        for (Map.Entry<String, List<FoodLogEntity>> dateEntry : logsByDate.entrySet()) {
            report.append("\n").append(dateEntry.getKey()).append(":\n");

            for (FoodLogEntity food : dateEntry.getValue()) {
                report.append("• ").append(food.getFoodName())
                        .append(" (").append(food.getPreparation()).append(")")
                        .append(" - ").append(food.getMealType())
                        .append(" [").append(food.getFodmapStatus()).append(" FODMAP]")
                        .append(" - Mood: ").append(food.getMood())
                        .append(", Stress: ").append(food.getStressLevel()).append("/5\n");
            }
        }

        // 4. RECOMMENDATIONS
        report.append("\nRECOMMENDATIONS:\n");
        report.append("----------------\n");

        if (gutSafePercentage < 70) {
            report.append("• Consider increasing gut-safe (LOW FODMAP) food intake\n");
        }

        if (avgStress > 3.0) {
            report.append("• Practice stress-reduction techniques before meals\n");
            report.append("• Try mindful eating in calm environments\n");
        }

        report.append("• Continue tracking for personalized insights\n");
        report.append("• Share this report with your healthcare provider\n");

        report.append("\n============================================\n");
        report.append("Generated by Gut Nutrition Manager App\n");

        return report.toString();
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


    private void testSimplePdf() {
        Log.d("PDFDebug", "Testing simple PDF generation");

        try {
            PdfReportGenerator pdfGenerator = new PdfReportGenerator(requireContext());

            // Create empty list for testing
            List<FoodLogEntity> testLogs = new ArrayList<>();

            File pdfFile = pdfGenerator.generateDoctorReport(testLogs, "Test Period");

            if (pdfFile != null && pdfFile.exists()) {
                Log.d("PDFDebug", "Simple PDF test SUCCESS - File: " + pdfFile.getAbsolutePath());
                Toast.makeText(requireContext(), "PDF test successful!", Toast.LENGTH_LONG).show();
            } else {
                Log.e("PDFDebug", "Simple PDF test FAILED");
                Toast.makeText(requireContext(), "PDF test failed", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Log.e("PDFDebug", "Simple PDF test ERROR: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "PDF error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}