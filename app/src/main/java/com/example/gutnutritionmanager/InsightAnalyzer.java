package com.example.gutnutritionmanager;

import android.icu.util.Calendar;
import android.util.Log;

import java.util.*;
import java.util.stream.Collectors;

public class InsightAnalyzer {

    public static Map<String, CorrelationResult> analyzeCorrelations(List<LogEntryWithSymptoms> entries) {
        Map<String, CorrelationResult> results = new HashMap<>();

        if (entries == null || entries.isEmpty()) {
            return results;
        }

        try {
            // Phase 1: Basic frequency analysis
            Map<String, Integer> symptomFrequency = analyzeSymptomFrequency(entries);
            Map<String, Integer> foodFrequency = analyzeFoodFrequency(entries);

            // Phase 2: Advanced correlation analysis
            for (LogEntryWithSymptoms entry : entries) {
                if (entry.symptomName != null && !entry.symptomName.isEmpty()) {
                    String symptom = entry.symptomName;

                    if (!results.containsKey(symptom)) {
                        results.put(symptom, new CorrelationResult(symptom));
                    }

                    CorrelationResult result = results.get(symptom);
                    result.totalOccurrences++;

                    // Weight by severity (1-5 scale)
                    double severityWeight = entry.symptomSeverity / 5.0;

                    // Analyze foods for this symptom occurrence
                    if (entry.logEntry.foods != null) {
                        String[] foods = entry.logEntry.foods.split(",");
                        for (String food : foods) {
                            food = food.trim();
                            if (!food.isEmpty()) {
                                // Use weighted count based on severity
                                double currentCount = result.foodCounts.getOrDefault(food, 0.0);
                                result.foodCounts.put(food, currentCount + severityWeight);
                            }
                        }
                    }

                    // Track timing patterns
                    result.addTimingData(entry.logEntry.timestamp, entry.symptomSeverity);
                }
            }

            // Phase 3: Statistical significance calculation
            for (CorrelationResult result : results.values()) {
                calculateStatisticalSignificance(result, entries.size(), foodFrequency);
                result.calculateConfidenceScores();
            }

        } catch (Exception e) {
            Log.e("InsightAnalyzer", "Error in analysis: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    private static Map<String, Integer> analyzeSymptomFrequency(List<LogEntryWithSymptoms> entries) {
        Map<String, Integer> frequency = new HashMap<>();
        for (LogEntryWithSymptoms entry : entries) {
            if (entry.symptomName != null) {
                frequency.put(entry.symptomName, frequency.getOrDefault(entry.symptomName, 0) + 1);
            }
        }
        return frequency;
    }

    private static Map<String, Integer> analyzeFoodFrequency(List<LogEntryWithSymptoms> entries) {
        Map<String, Integer> frequency = new HashMap<>();
        for (LogEntryWithSymptoms entry : entries) {
            if (entry.logEntry.foods != null) {
                String[] foods = entry.logEntry.foods.split(",");
                for (String food : foods) {
                    food = food.trim();
                    if (!food.isEmpty()) {
                        frequency.put(food, frequency.getOrDefault(food, 0) + 1);
                    }
                }
            }
        }
        return frequency;
    }

    private static void calculateStatisticalSignificance(CorrelationResult result, int totalEntries,
                                                         Map<String, Integer> foodFrequency) {
        for (Map.Entry<String, Double> foodEntry : result.foodCounts.entrySet()) {
            String food = foodEntry.getKey();
            double symptomFoodCount = foodEntry.getValue();

            // Calculate expected frequency if no correlation
            double foodOverallFrequency = foodFrequency.getOrDefault(food, 1) / (double) totalEntries;
            double expectedCount = result.totalOccurrences * foodOverallFrequency;

            // Calculate chi-square like significance
            if (expectedCount > 0) {
                double significance = Math.abs(symptomFoodCount - expectedCount) / Math.sqrt(expectedCount);
                result.foodSignificance.put(food, significance);
            }
        }
    }

    // Enhanced trend analysis with multiple timeframes
    public static Map<String, TrendAnalysis> analyzeTrends(List<LogEntryWithSymptoms> entries) {
        Map<String, TrendAnalysis> trends = new HashMap<>();

        if (entries == null || entries.size() < 3) {
            return trends;
        }

        // Sort entries by date
        entries.sort(Comparator.comparing(entry -> entry.logEntry.timestamp));

        // Group by symptom
        Map<String, List<LogEntryWithSymptoms>> symptomGroups = entries.stream()
                .filter(entry -> entry.symptomName != null)
                .collect(Collectors.groupingBy(entry -> entry.symptomName));

        for (Map.Entry<String, List<LogEntryWithSymptoms>> group : symptomGroups.entrySet()) {
            String symptom = group.getKey();
            List<LogEntryWithSymptoms> symptomEntries = group.getValue();

            TrendAnalysis analysis = new TrendAnalysis(symptom);

            // Weekly trends
            analysis.weeklyTrend = calculateWeeklyTrend(symptomEntries);

            // Severity trends
            analysis.severityTrend = calculateSeverityTrend(symptomEntries);

            // Frequency trends
            analysis.frequencyTrend = calculateFrequencyTrend(symptomEntries);

            trends.put(symptom, analysis);
        }

        return trends;
    }

    private static double calculateWeeklyTrend(List<LogEntryWithSymptoms> entries) {
        if (entries.size() < 2) return 0;

        // Simple linear regression for trend
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = entries.size();

        for (int i = 0; i < n; i++) {
            double x = i; // Time index
            double y = entries.get(i).symptomSeverity;
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return slope; // Positive = increasing, Negative = decreasing
    }

    private static String calculateSeverityTrend(List<LogEntryWithSymptoms> entries) {
        if (entries.size() < 3) return "Insufficient data";

        double firstHalfAvg = entries.subList(0, entries.size()/2).stream()
                .mapToInt(e -> e.symptomSeverity)
                .average().orElse(0);

        double secondHalfAvg = entries.subList(entries.size()/2, entries.size()).stream()
                .mapToInt(e -> e.symptomSeverity)
                .average().orElse(0);

        double change = ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100;

        if (Math.abs(change) < 10) return "Stable";
        return change > 0 ? "Worsening" : "Improving";
    }

    private static String calculateFrequencyTrend(List<LogEntryWithSymptoms> entries) {
        if (entries.size() < 4) return "Insufficient data";

        // Analyze frequency over time (entries per week)
        Map<Integer, Long> weeklyCounts = entries.stream()
                .collect(Collectors.groupingBy(
                        entry -> getWeekOfYear(entry.logEntry.timestamp),
                        Collectors.counting()
                ));

        if (weeklyCounts.size() < 2) return "Need more weeks of data";

        // Simple trend analysis
        List<Long> counts = new ArrayList<>(weeklyCounts.values());
        long firstHalfAvg = (long) counts.subList(0, counts.size()/2).stream()
                .mapToLong(Long::longValue).average().orElse(0);
        long secondHalfAvg = (long) counts.subList(counts.size()/2, counts.size()).stream()
                .mapToLong(Long::longValue).average().orElse(0);

        return secondHalfAvg > firstHalfAvg ? "Increasing" : "Decreasing";
    }

    // Pattern detection algorithms
    public static List<FoodPattern> detectFoodPatterns(List<LogEntryWithSymptoms> entries) {
        List<FoodPattern> patterns = new ArrayList<>();

        // 1. Food combination analysis
        Map<Set<String>, Integer> combinationFrequency = new HashMap<>();

        for (LogEntryWithSymptoms entry : entries) {
            if (entry.logEntry.foods != null && entry.symptomName != null) {
                Set<String> foodSet = Arrays.stream(entry.logEntry.foods.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toSet());

                if (foodSet.size() >= 2) {
                    combinationFrequency.put(foodSet,
                            combinationFrequency.getOrDefault(foodSet, 0) + 1);
                }
            }
        }

        // Find significant combinations
        for (Map.Entry<Set<String>, Integer> combo : combinationFrequency.entrySet()) {
            if (combo.getValue() >= 3) { // Minimum 3 occurrences
                FoodPattern pattern = new FoodPattern();
                pattern.foods = new ArrayList<>(combo.getKey());
                pattern.frequency = combo.getValue();
                pattern.type = "Combination";
                patterns.add(pattern);
            }
        }

        return patterns;
    }

    // Helper methods
    private static int getWeekOfYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.WEEK_OF_YEAR);
    }

    // Enhanced CorrelationResult class
    public static class CorrelationResult {
        public String symptom;
        public int totalOccurrences;
        public Map<String, Double> foodCounts; // Changed to Double for weighted counts
        public Map<String, Double> foodPercentages;
        public Map<String, Double> foodSignificance;
        public List<TimingData> timingData;
        public double confidenceScore;

        public CorrelationResult(String symptom) {
            this.symptom = symptom;
            this.totalOccurrences = 0;
            this.foodCounts = new HashMap<>();
            this.foodPercentages = new HashMap<>();
            this.foodSignificance = new HashMap<>();
            this.timingData = new ArrayList<>();
        }

        public void addTimingData(Date timestamp, int severity) {
            this.timingData.add(new TimingData(timestamp, severity));
        }

        public void calculateConfidenceScores() {
            // Calculate percentages
            for (Map.Entry<String, Double> entry : foodCounts.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / totalOccurrences;
                foodPercentages.put(entry.getKey(), percentage);
            }

            // Calculate overall confidence based on data quality
            this.confidenceScore = Math.min(1.0, totalOccurrences / 10.0); // More occurrences = higher confidence
        }
        // Overloaded method with default significance
        public List<String> getTopFoods(int limit) {
            return getTopFoods(limit, 1.5); // Call the main method with default value
        }

        public List<String> getTopFoods(int limit, double minSignificance) {
            return foodSignificance.entrySet().stream()
                    .filter(entry -> entry.getValue() >= minSignificance)
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .limit(limit)
                    .map(entry -> {
                        String food = entry.getKey();
                        double percentage = foodPercentages.getOrDefault(food, 0.0);
                        double significance = entry.getValue();
                        return String.format("%s (%.1f%%, sig: %.2f)", food, percentage, significance);
                    })
                    .collect(Collectors.toList());
        }

        public String getTimingInsight() {
            if (timingData.size() < 3) return "Need more timing data";

            // Analyze time of day patterns
            Map<Integer, Double> hourSeverity = new HashMap<>();
            for (TimingData data : timingData) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(data.timestamp);
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                hourSeverity.put(hour, hourSeverity.getOrDefault(hour, 0.0) + data.severity);
            }

            // Find peak hours
            return hourSeverity.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(entry -> "Most common around " + entry.getKey() + ":00")
                    .orElse("No clear timing pattern");
        }
    }

    // Supporting data classes
    public static class TrendAnalysis {
        public String symptom;
        public double weeklyTrend;
        public String severityTrend;
        public String frequencyTrend;

        public TrendAnalysis(String symptom) {
            this.symptom = symptom;
        }
    }

    public static class FoodPattern {
        public List<String> foods;
        public int frequency;
        public String type;
    }

    public static class TimingData {
        public Date timestamp;
        public int severity;

        public TimingData(Date timestamp, int severity) {
            this.timestamp = timestamp;
            this.severity = severity;
        }
    }
}