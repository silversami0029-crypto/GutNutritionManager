package com.example.gutnutritionmanager;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InsightsFragment extends Fragment {
    private RecyclerView symptomsRecyclerView;
    private SymptomsAdapter symptomsAdapter;
    private NutritionViewModel viewModel;
    private TextView insightsTextView;
    private ProgressBar progressBar;
    private Button refreshButton;

    public InsightsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_insights, container, false);

        // Initialize UI components
        insightsTextView = view.findViewById(R.id.insightsTextView);
        progressBar = view.findViewById(R.id.progressBar);
        refreshButton = view.findViewById(R.id.refreshButton);

        // ✅ ADD THESE LINES: Initialize symptoms RecyclerView
        symptomsRecyclerView = view.findViewById(R.id.symptomsRecyclerView);

        // TEST: Set initial text to verify TextView works
        insightsTextView.setText("Loading insights...");

        // Initialize ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(NutritionViewModel.class);

        // ✅ ADD THIS: Setup symptoms RecyclerView and start observing
        setupSymptomsRecyclerView();
        observeSymptoms();

        // Set up refresh button
        refreshButton.setOnClickListener(v -> loadBasicInsights());

        return view;
    }

    private void setupSymptomsRecyclerView() {
        symptomsAdapter = new SymptomsAdapter(new ArrayList<>());
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        symptomsRecyclerView.setLayoutManager(layoutManager);
        symptomsRecyclerView.setAdapter(symptomsAdapter);
    }

    private void observeSymptoms() {
        Log.d("InsightsFragment", "Starting to observe symptoms data...");

        viewModel.getEntriesWithSymptoms().observe(getViewLifecycleOwner(), entriesWithSymptoms -> {
            Log.d("InsightsFragment", "Symptoms data received: " +
                    (entriesWithSymptoms != null ? entriesWithSymptoms.size() : "null"));

            if (entriesWithSymptoms != null && !entriesWithSymptoms.isEmpty()) {
                // Filter only entries that have actual symptoms
                List<LogEntryWithSymptoms> entriesWithActualSymptoms = new ArrayList<>();
                for (LogEntryWithSymptoms entry : entriesWithSymptoms) {
                    if (entry.symptomName != null && !entry.symptomName.isEmpty()) {
                        entriesWithActualSymptoms.add(entry);
                    }
                }

                if (!entriesWithActualSymptoms.isEmpty()) {
                    symptomsAdapter.updateData(entriesWithActualSymptoms);
                    Log.d("InsightsFragment", "Displaying " + entriesWithActualSymptoms.size() + " symptoms");
                }
            }
        });
    }

    private void updateInsightsDisplay(List<Symptom> symptoms) {
        StringBuilder insights = new StringBuilder();

        insights.append("=== YOUR SYMPTOM INSIGHTS ===\n\n");
        insights.append("Total Symptoms Logged: ").append(symptoms.size()).append("\n\n");

        // Calculate symptom frequency
        Map<String, Integer> symptomCount = new HashMap<>();
        Map<String, Double> symptomAvgSeverity = new HashMap<>();

        for (Symptom symptom : symptoms) {
            symptomCount.put(symptom.name, symptomCount.getOrDefault(symptom.name, 0) + 1);

            // Calculate average severity
            double currentAvg = symptomAvgSeverity.getOrDefault(symptom.name, 0.0);
            int count = symptomCount.get(symptom.name);
            double newAvg = (currentAvg * (count - 1) + symptom.severity) / count;
            symptomAvgSeverity.put(symptom.name, newAvg);
        }

        insights.append("Most Common Symptoms:\n");
        for (Map.Entry<String, Integer> entry : symptomCount.entrySet()) {
            String symptomName = entry.getKey();
            int frequency = entry.getValue();
            double avgSeverity = symptomAvgSeverity.get(symptomName);

            insights.append("• ").append(symptomName)
                    .append(": ").append(frequency).append(" times")
                    .append(" (Avg severity: ").append(String.format("%.1f", avgSeverity)).append("/5)\n");
        }

        insights.append("\n=== RECENT SYMPTOMS ===\n");
        // Show last 5 symptoms
        int count = Math.min(symptoms.size(), 5);
        for (int i = 0; i < count; i++) {
            Symptom symptom = symptoms.get(i);
            insights.append("• ").append(symptom.name)
                    .append(" (Severity: ").append(symptom.severity).append("/5)\n");
        }

        // FORCE THIS TO APPEAR AT THE TOP
        String symptomInsights = insights.toString();

        // Get the current text and REPLACE or ADD symptom insights
        String currentText = insightsTextView.getText().toString();

        if (currentText.contains("=== YOUR SYMPTOM INSIGHTS ===")) {
            // Replace existing symptom insights
            currentText = currentText.replaceAll("=== YOUR SYMPTOM INSIGHTS ===.*?=== RECENT SYMPTOMS ===[\\s\\S]*?(?=\\n===|$)", symptomInsights);
        } else {
            // Add symptom insights at the beginning
            currentText = symptomInsights + "\n\n" + currentText;
        }

        insightsTextView.setText(currentText);
    }
/*
    private void updateInsightsDisplay(List<Symptom> symptoms) {
        String symptomText = "🎯 SYMPTOM INSIGHTS\n\n";
        symptomText += "You have " + symptoms.size() + " logged symptoms:\n\n";

        // Group by symptom name
        Map<String, Integer> symptomFreq = new HashMap<>();
        for (Symptom s : symptoms) {
            symptomFreq.put(s.name, symptomFreq.getOrDefault(s.name, 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : symptomFreq.entrySet()) {
            symptomText += "• " + entry.getKey() + ": " + entry.getValue() + " times\n";
        }

        // COMPLETELY REPLACE the text view content
        insightsTextView.setText(symptomText + "\n\n" +
                "=== OTHER INSIGHTS ===\n" +
                "Scroll down for food and mood insights...");
    }*/


/*
    private void setupObservers() {
        // Observe symptoms data - this will automatically update when data changes
        viewModel.getAllSymptoms().observe(getViewLifecycleOwner(), symptoms -> {
            Log.d("INSIGHTS_FRAGMENT", "Symptoms observed: " + (symptoms != null ? symptoms.size() : "null"));

            if (symptoms != null && !symptoms.isEmpty()) {
                Log.d("INSIGHTS_FRAGMENT", "First symptom: " + symptoms.get(0).name);
                updateInsightsDisplay(symptoms);
            } else {
                Log.d("INSIGHTS_FRAGMENT", "No symptoms to display");
                insightsTextView.setText("No symptom data available. Add some meals with symptoms to see insights.");
            }
        });

        // Also observe log entries with symptoms if you need that data
        viewModel.getEntriesWithSymptoms().observe(getViewLifecycleOwner(), entries -> {
            Log.d("INSIGHTS_FRAGMENT", "Entries with symptoms observed: " + (entries != null ? entries.size() : "null"));
        });
    }*/
private void setupObservers() {
    Log.d("INSIGHTS", "Setting up observers for symptoms");

    viewModel.getAllSymptoms().observe(getViewLifecycleOwner(), symptoms -> {
        Log.d("INSIGHTS", "GOT " + symptoms.size() + " SYMPTOMS - UPDATING UI");

        // Create the symptom text
        String symptomText = createSymptomText(symptoms);

        // Update UI on main thread
        updateUIText(symptomText);
    });
}

    private String createSymptomText(List<Symptom> symptoms) {
        StringBuilder text = new StringBuilder();
        text.append("🎯 YOUR SYMPTOM INSIGHTS\n\n");
        text.append("Total Symptoms: ").append(symptoms.size()).append("\n\n");

        text.append("All Your Symptoms:\n");
        for (Symptom symptom : symptoms) {
            text.append("• ").append(symptom.name)
                    .append(" (Severity: ").append(symptom.severity).append("/5)\n");
        }

        return text.toString();
    }

    private void updateUIText(String text) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (insightsTextView != null) {
                    insightsTextView.setText(text);
                    Log.d("INSIGHTS", "UI UPDATED WITH SYMPTOMS!");
                } else {
                    Log.e("INSIGHTS", "TextView is null - cannot update UI");
                }
            });
        }
    }


    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // SET UP OBSERVERS HERE - This is what you're missing!
        setupObservers();

        // Load insights initially
        loadBasicInsights();
    }



    private List<LogEntryWithSymptoms> convertSymptomsToDisplay(List<Symptom> symptoms) {
        List<LogEntryWithSymptoms> result = new ArrayList<>();
        for (Symptom symptom : symptoms) {
            LogEntryWithSymptoms entry = new LogEntryWithSymptoms();
            entry.symptomName = symptom.name;
            entry.symptomSeverity = symptom.severity;
            // Create a dummy log entry for display
            entry.logEntry = new LogEntry();
            entry.logEntry.timestamp = new Date(symptom.timestamp);
            entry.logEntry.foods = "Symptom Entry";
            result.add(entry);
        }
        return result;
    }
    private void debugBrainGutData(List<FoodLogEntity> foodLogs) {
        Log.d("BrainGutDebug", "=== BRAIN-GUT DATA ANALYSIS ===");
        Log.d("BrainGutDebug", "Total food logs: " + foodLogs.size());

        for (FoodLogEntity food : foodLogs) {
            Log.d("BrainGutDebug", "Food: " + food.getFoodName() +
                    " | Mood: " + food.getMood() +
                    " | Stress: " + food.getStressLevel());
        }

        // Call this in your loadBasicInsights after getting foodLogs
        debugBrainGutData(foodLogs);
    }

private String generateBasicInsights(List<FoodLogEntity> foodLogs) {
    StringBuilder insights = new StringBuilder();

    insights.append("🍽️ *YOUR FOOD INSIGHTS*\n\n");

    // Basic statistics
    insights.append("📊 *Statistics*\n");
    insights.append("• Total foods logged: ").append(foodLogs.size()).append("\n");

    // Count low FODMAP foods
    long lowFodmapCount = 0;
    for (FoodLogEntity food : foodLogs) {
        if ("LOW".equals(food.getFodmapStatus())) {
            lowFodmapCount++;
        }
    }
    insights.append("• Gut-safe foods: ").append(lowFodmapCount).append("/").append(foodLogs.size()).append("\n\n");

    // Most common foods
    insights.append("🏆 *Most Common Foods*\n");
    Map<String, Integer> foodCounts = new HashMap<>();
    for (FoodLogEntity food : foodLogs) {
        String foodName = food.getFoodName();
        foodCounts.put(foodName, foodCounts.getOrDefault(foodName, 0) + 1);
    }

    // Show top 3 foods
    foodCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(3)
            .forEach(entry -> {
                insights.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" times\n");
            });
    insights.append("\n");

    // Preparation methods
    insights.append("👨‍🍳 *Preparation Methods*\n");
    Map<String, Integer> prepCounts = new HashMap<>();
    for (FoodLogEntity food : foodLogs) {
        String prep = food.getPreparation();
        prepCounts.put(prep, prepCounts.getOrDefault(prep, 0) + 1);
    }

    for (Map.Entry<String, Integer> entry : prepCounts.entrySet()) {
        insights.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" times\n");
    }

    return insights.toString();
}



    private void loadBasicInsights() {
        showLoading(true);
        insightsTextView.setText("Analyzing your brain-gut connection...");

        AppDatabase database = AppDatabase.getDatabase(requireContext());
        FoodDao foodDao = database.foodDao();

        // Get both food logs and symptoms
        foodDao.getAllFoodLogs().observe(getViewLifecycleOwner(), foodLogs -> {
            // Get symptoms (you'll need to implement this method in your DAO)
            // foodDao.getAllSymptoms().observe(getViewLifecycleOwner(), symptoms -> {
            showLoading(false);

            if (foodLogs == null || foodLogs.isEmpty()) {
                insightsTextView.setText("No food data available yet.\n\nLog some meals with mood tracking to see brain-gut insights!");
                return;
            }

            // Generate combined insights
            String basicInsights = generateBasicInsights(foodLogs);
            // String brainGutInsights = generateBrainGutInsights(foodLogs, symptoms);
            String brainGutInsights = generateBrainGutInsights(foodLogs, new ArrayList<>()); // Temporary

            String allInsights = basicInsights + "\n" + brainGutInsights;
            insightsTextView.setText(allInsights);
            // });
        });
    }

    public void refreshInsights() {
        // Your refresh logic here
        if (refreshButton != null) {
            refreshButton.performClick(); // Simulate clicking the refresh button
        }
    }
    private String generateComprehensiveInsights(List<LogEntryWithSymptoms> entries) {
        StringBuilder insights = new StringBuilder();

        insights.append("=== DIGESTIVE HEALTH INSIGHTS ===\n\n");

        // 1. Basic correlation analysis
        insights.append("📊 FOOD-SYMPTOM CORRELATIONS\n\n");
        Map<String, InsightAnalyzer.CorrelationResult> correlations =
                InsightAnalyzer.analyzeCorrelations(entries);

        if (correlations.isEmpty()) {
            insights.append("No significant correlations found yet. Keep logging to identify patterns.\n\n");
        } else {
            for (InsightAnalyzer.CorrelationResult result : correlations.values()) {
                insights.append("🔍 ").append(result.symptom.toUpperCase()).append("\n");
                insights.append("   Total occurrences: ").append(result.totalOccurrences).append("\n");
                insights.append("   Confidence score: ").append(String.format("%.1f%%", result.confidenceScore * 100)).append("\n\n");

                List<String> topFoods = result.getTopFoods(3); // Get top 3 significant foods
                if (!topFoods.isEmpty()) {
                    insights.append("   Most associated foods:\n");
                    for (String food : topFoods) {
                        insights.append("   • ").append(food).append("\n");
                    }
                } else {
                    insights.append("   No strong correlations detected yet.\n");
                }

                // Add timing insight
                String timingInsight = result.getTimingInsight();
                insights.append("   ⏰ ").append(timingInsight).append("\n\n");
            }
        }

        // 2. Trend analysis
        insights.append("📈 TREND ANALYSIS\n\n");
        Map<String, InsightAnalyzer.TrendAnalysis> trends =
                InsightAnalyzer.analyzeTrends(entries);

        if (trends.isEmpty()) {
            insights.append("Need more data to analyze trends (minimum 3 entries with symptoms).\n\n");
        } else {
            for (Map.Entry<String, InsightAnalyzer.TrendAnalysis> entry : trends.entrySet()) {
                String symptom = entry.getKey();
                InsightAnalyzer.TrendAnalysis analysis = entry.getValue();

                insights.append("📊 ").append(symptom.toUpperCase()).append("\n");
                insights.append("   Severity trend: ").append(analysis.severityTrend).append("\n");
                insights.append("   Frequency trend: ").append(analysis.frequencyTrend).append("\n");

                // Interpret the weekly trend
                String trendDirection = analysis.weeklyTrend > 0.1 ? "📈 Worsening" :
                        analysis.weeklyTrend < -0.1 ? "📉 Improving" : "➡️ Stable";
                insights.append("   Weekly trend: ").append(trendDirection).append("\n\n");
            }
        }

        // 3. Pattern detection
        insights.append("🔍 PATTERN DETECTION\n\n");
        List<InsightAnalyzer.FoodPattern> patterns =
                InsightAnalyzer.detectFoodPatterns(entries);

        if (patterns.isEmpty()) {
            insights.append("No significant food patterns detected yet.\n\n");
        } else {
            insights.append("Detected food patterns:\n");
            for (InsightAnalyzer.FoodPattern pattern : patterns) {
                insights.append("• ").append(String.join(" + ", pattern.foods))
                        .append(" (occurred ").append(pattern.frequency).append(" times)\n");
            }
            insights.append("\n");
        }

        // 4. Overall statistics
        insights.append("📋 OVERALL STATISTICS\n\n");
        insights.append("Total entries analyzed: ").append(entries.size()).append("\n");

        long entriesWithSymptoms = entries.stream().filter(e -> e.symptomName != null).count();
        insights.append("Entries with symptoms: ").append(entriesWithSymptoms).append("\n");

        double symptomPercentage = (entriesWithSymptoms * 100.0) / entries.size();
        insights.append("Symptom frequency: ").append(String.format("%.1f%%", symptomPercentage)).append("\n\n");

        // 5. Recommendations based on insights
        insights.append("💡 RECOMMENDATIONS\n\n");
        insights.append(generateRecommendations(correlations, trends, entries.size()));

        return insights.toString();
    }

    private String generateRecommendations(Map<String, InsightAnalyzer.CorrelationResult> correlations,
                                           Map<String, InsightAnalyzer.TrendAnalysis> trends,
                                           int totalEntries) {
        StringBuilder recommendations = new StringBuilder();

        if (totalEntries < 5) {
            recommendations.append("• Continue logging for at least 5 more entries for better insights\n");
        }

        if (!correlations.isEmpty()) {
            recommendations.append("• Consider reducing intake of highly correlated foods\n");
            recommendations.append("• Monitor symptoms after consuming identified foods\n");
        }

        if (!trends.isEmpty()) {
            for (Map.Entry<String, InsightAnalyzer.TrendAnalysis> entry : trends.entrySet()) {
                String symptom = entry.getKey();
                InsightAnalyzer.TrendAnalysis analysis = entry.getValue();

                if ("Worsening".equals(analysis.severityTrend)) {
                    recommendations.append("• ").append(symptom).append(" appears to be worsening - consider consulting a healthcare provider\n");
                } else if ("Improving".equals(analysis.severityTrend)) {
                    recommendations.append("• Great! ").append(symptom).append(" appears to be improving\n");
                }
            }
        }

        if (recommendations.length() == 0) {
            recommendations.append("• Keep consistent logging to build more personalized insights\n");
            recommendations.append("• Try logging at different times of day to identify patterns\n");
            recommendations.append("• Note portion sizes and food preparation methods\n");
        }

        return recommendations.toString();
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (refreshButton != null) {
            refreshButton.setEnabled(!isLoading);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh insights when fragment becomes visible
        loadBasicInsights();
    }


    private void debugDataAvailability() {
        AppDatabase database = AppDatabase.getDatabase(requireContext());
        FoodDao foodDao = database.foodDao();

        // Observe food logs using LiveData
        foodDao.getAllFoodLogs().observe(getViewLifecycleOwner(), foodLogs -> {
            Log.d("InsightsDebug", "Food logs count: " + (foodLogs != null ? foodLogs.size() : 0));

            if (foodLogs != null && !foodLogs.isEmpty()) {
                for (int i = 0; i < Math.min(3, foodLogs.size()); i++) {
                    FoodLogEntity log = foodLogs.get(i);
                    Log.d("InsightsDebug", "Food " + i + ": " + log.getFoodName() + " | " + log.getPreparation());
                }
            } else {
                Log.d("InsightsDebug", "No food logs found");
            }
        });

        // If you have symptoms as LiveData, observe them too:
        // foodDao.getAllSymptoms().observe(getViewLifecycleOwner(), symptoms -> {
        //     Log.d("InsightsDebug", "Symptoms count: " + (symptoms != null ? symptoms.size() : 0));
        // });
    }

// ADD THESE NEW METHODS FOR BRAIN-GUT INSIGHTS:

    private String generateBrainGutInsights(List<FoodLogEntity> foodLogs, List<Symptom> symptoms) {
        StringBuilder insights = new StringBuilder();

        insights.append("🧠 BRAIN-GUT CONNECTION INSIGHTS\n\n");

        // 1. Mood-Symptom Correlation
        String moodInsight = analyzeMoodSymptomCorrelation(foodLogs, symptoms);
        insights.append(moodInsight).append("\n");

        // 2. Stress Impact Analysis
        String stressInsight = analyzeStressImpact(foodLogs, symptoms);
        insights.append(stressInsight).append("\n");

        // 3. Gut-Calming Recommendations
        String calmingInsights = generateGutCalmingRecommendations(foodLogs);
        insights.append(calmingInsights).append("\n");

        return insights.toString();
    }

    private String analyzeMoodSymptomCorrelation(List<FoodLogEntity> foodLogs, List<Symptom> symptoms) {
        if (foodLogs.isEmpty() || symptoms.isEmpty()) {
            return "📊 Mood-Symptom Patterns\nTrack more meals with symptoms to see mood correlations\n";
        }

        StringBuilder insight = new StringBuilder();
        insight.append("📊 Mood-Symptom Patterns\n");

        // Simple correlation: Count symptoms by mood
        Map<String, Integer> moodSymptomCount = new HashMap<>();
        Map<String, Integer> moodCount = new HashMap<>();

        // This is simplified - in real app, you'd match by timestamp
        for (FoodLogEntity food : foodLogs) {
            String mood = food.getMood();
            moodCount.put(mood, moodCount.getOrDefault(mood, 0) + 1);

            // If we have symptoms around this time, count them
            if (!symptoms.isEmpty()) {
                moodSymptomCount.put(mood, moodSymptomCount.getOrDefault(mood, 0) + 1);
            }
        }

        // Find mood with highest symptom correlation
        String highestMood = "";
        double highestRatio = 0;

        for (String mood : moodCount.keySet()) {
            int totalWithThisMood = moodCount.get(mood);
            int symptomsWithThisMood = moodSymptomCount.getOrDefault(mood, 0);
            double ratio = (double) symptomsWithThisMood / totalWithThisMood;

            if (ratio > highestRatio) {
                highestRatio = ratio;
                highestMood = mood;
            }

            insight.append("• ").append(mood).append(": ")
                    .append(symptomsWithThisMood).append("/").append(totalWithThisMood)
                    .append(" meals had symptoms\n");
        }

        if (highestRatio > 0.5 && !highestMood.isEmpty()) {
            insight.append("\n🔍 Insight: You're more likely to experience symptoms when eating while ")
                    .append(highestMood.toLowerCase()).append("\n");
        }

        return insight.toString();
    }

    private String analyzeStressImpact(List<FoodLogEntity> foodLogs, List<Symptom> symptoms) {
        if (foodLogs.isEmpty()) {
            return "😰 Stress Impact\nTrack stress levels to see gut impact\n";
        }

        StringBuilder insight = new StringBuilder();
        insight.append("😰 Stress Impact Analysis\n");

        // Calculate average stress for symptomatic vs non-symptomatic meals
        double totalStress = 0;
        double highStressMeals = 0; // stress level 4-5
        double lowStressMeals = 0;  // stress level 1-2

        for (FoodLogEntity food : foodLogs) {
            int stress = food.getStressLevel();
            totalStress += stress;

            if (stress >= 4) highStressMeals++;
            if (stress <= 2) lowStressMeals++;
        }

        double avgStress = totalStress / foodLogs.size();
        insight.append("• Average stress during meals: ").append(String.format("%.1f", avgStress)).append("/5\n");

        if (highStressMeals > 0) {
            double highStressPercent = (highStressMeals / foodLogs.size()) * 100;
            insight.append("• ").append(String.format("%.0f", highStressPercent))
                    .append("% of meals were during high stress\n");
        }

        if (avgStress > 3.0) {
            insight.append("💡 Tip: High stress eating can trigger gut symptoms. Try calming exercises before meals.\n");
        } else if (avgStress < 2.5) {
            insight.append("🌟 Great: You're generally eating in a relaxed state!\n");
        }

        return insight.toString();
    }

    private String generateGutCalmingRecommendations(List<FoodLogEntity> foodLogs) {
        StringBuilder recommendations = new StringBuilder();
        recommendations.append("🛌 Gut-Calming Strategies\n");

        // Analyze stress patterns to give personalized recommendations
        long highStressMeals = foodLogs.stream()
                .filter(food -> food.getStressLevel() >= 4)
                .count();

        if (highStressMeals > foodLogs.size() * 0.3) { // If 30%+ meals are high stress
            recommendations.append("• Try 2-minute breathing before meals\n");
            recommendations.append("• Practice mindful eating - no distractions\n");
            recommendations.append("• Consider gentle walks after stressful meals\n");
        } else {
            recommendations.append("• Continue your good stress management!\n");
            recommendations.append("• Try 4-7-8 breathing when feeling stressed: Inhale 4s, Hold 7s, Exhale 8s\n");
        }

        recommendations.append("• Vagal nerve exercise: Humming or gargling water\n");
        recommendations.append("• Progressive muscle relaxation before bed\n");

        return recommendations.toString();
    }

    // Temporary debug method - add this to InsightsFragment
    private void createTestData() {
        AppDatabase database = AppDatabase.getDatabase(requireContext());
        FoodDao foodDao = database.foodDao();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            // Clear existing test data if any
            // foodDao.deleteAllFoodLogs(); // Use carefully!

            // Add test food logs with different moods/stress
            FoodLogEntity test1 = new FoodLogEntity(0, "Chicken", "Cooked", "Medium", "Lunch", "LOW", new Date(), "😊 Calm", 2);
            FoodLogEntity test2 = new FoodLogEntity(0, "Rice", "Cooked", "Medium", "Lunch", "LOW", new Date(), "😰 Stressed", 4);
            FoodLogEntity test3 = new FoodLogEntity(0, "Carrots", "Cooked", "Medium", "Dinner", "LOW", new Date(), "😟 Anxious", 5);
            FoodLogEntity test4 = new FoodLogEntity(0, "Fish", "Cooked", "Medium", "Breakfast", "LOW", new Date(), "😊 Calm", 1);

            foodDao.insert(test1);
            foodDao.insert(test2);
            foodDao.insert(test3);
            foodDao.insert(test4);

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Test data added! Refresh insights.", Toast.LENGTH_SHORT).show();
            });
        });
    }

}