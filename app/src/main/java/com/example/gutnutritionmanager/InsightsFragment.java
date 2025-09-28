package com.example.gutnutritionmanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;
import java.util.Map;

public class InsightsFragment extends Fragment {

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

        // Initialize ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(NutritionViewModel.class);

        // Set up refresh button
        refreshButton.setOnClickListener(v -> loadInsights());

        // Load insights initially
        loadInsights();

        return view;
    }

    private void loadInsights() {
        showLoading(true);
        insightsTextView.setText("Analyzing your data...");

        // Observe the log entries with symptoms
        viewModel.getEntriesWithSymptoms().observe(getViewLifecycleOwner(), entries -> {
            showLoading(false);

            if (entries == null || entries.isEmpty()) {
                insightsTextView.setText("No data available yet.\n\nLog some meals and symptoms to see insights about your digestive health.");
                return;
            }

            try {
                // Generate insights using the enhanced analyzer
                String insights = generateComprehensiveInsights(entries);
                insightsTextView.setText(insights);
            } catch (Exception e) {
                insightsTextView.setText("Error generating insights: " + e.getMessage());
            }
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
        loadInsights();
    }
}