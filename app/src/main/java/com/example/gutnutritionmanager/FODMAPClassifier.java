package com.example.gutnutritionmanager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FODMAPClassifier {

    // Comprehensive FODMAP knowledge base
    private static final Set<String> HIGH_FODMAP_KEYWORDS = new HashSet<>(Arrays.asList(
            "onion", "garlic", "asparagus", "cauliflower", "artichoke", "leek", "shallot",
            "milk", "yogurt", "soft cheese", "ricotta", "cottage cheese", "whey",
            "wheat", "rye", "barley", "pasta", "bread", "cereal", "flour",
            "apple", "pear", "watermelon", "peach", "mango", "cherry",
            "honey", "agave", "high fructose corn syrup", "sorbitol", "mannitol", "xylitol",
            "beans", "lentils", "chickpeas", "soy", "tofu", "hummus",
            "cashew", "pistachio", "avocado"
    ));

    private static final Set<String> LOW_FODMAP_KEYWORDS = new HashSet<>(Arrays.asList(
            "rice", "chicken", "beef", "fish", "salmon", "tuna", "egg", "egg",
            "carrot", "potato", "cucumber", "zucchini", "spinach", "lettuce", "tomato",
            "orange", "banana", "strawberry", "blueberry", "raspberry", "grape",
            "hard cheese", "cheddar", "parmesan", "swiss", "feta",
            "oat", "quinoa", "corn", "potato", "almond", "walnut", "peanut",
            "maple syrup", "sugar", "dark chocolate", "coffee", "tea", "water"
    ));

    private static final Set<String> HIGH_FODMAP_CATEGORIES = new HashSet<>(Arrays.asList(
            "dairy", "milk", "cheese", "yogurt", "wheat", "bread", "pasta",
            "legume", "bean", "lentil", "onion", "garlic", "fruit", "apple", "pear"
    ));

    public static String classifyFood(String foodName, String category) {
        if (foodName == null) return "MODERATE";

        String lowerName = foodName.toLowerCase();
        String lowerCategory = category != null ? category.toLowerCase() : "";

        // Check for high FODMAP indicators
        if (containsHighFODMAP(lowerName, lowerCategory)) {
            return "HIGH";
        }

        // Check for low FODMAP indicators
        if (containsLowFODMAP(lowerName, lowerCategory)) {
            return "LOW";
        }

        return "MODERATE"; // Default for unknown foods
    }

    private static boolean containsHighFODMAP(String foodName, String category) {
        // Check food name keywords
        for (String keyword : HIGH_FODMAP_KEYWORDS) {
            if (foodName.contains(keyword)) {
                return true;
            }
        }

        // Check category
        for (String catKeyword : HIGH_FODMAP_CATEGORIES) {
            if (category.contains(catKeyword)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsLowFODMAP(String foodName, String category) {
        // Check food name keywords
        for (String keyword : LOW_FODMAP_KEYWORDS) {
            if (foodName.contains(keyword)) {
                return true;
            }
        }

        // Favor protein and staple categories
        if (category.contains("meat") || category.contains("poultry") ||
                category.contains("fish") || category.contains("egg") ||
                category.contains("grain") || category.contains("vegetable")) {
            return true;
        }

        return false;
    }

    // Get confidence level for classification
    public static double getConfidence(String foodName, String category) {
        int matches = 0;
        int totalChecks = 0;

        String lowerName = foodName.toLowerCase();
        String lowerCategory = category != null ? category.toLowerCase() : "";

        // Check high FODMAP matches
        for (String keyword : HIGH_FODMAP_KEYWORDS) {
            if (lowerName.contains(keyword)) {
                matches++;
            }
            totalChecks++;
        }

        // Check low FODMAP matches
        for (String keyword : LOW_FODMAP_KEYWORDS) {
            if (lowerName.contains(keyword)) {
                matches++;
            }
            totalChecks++;
        }

        return totalChecks > 0 ? (double) matches / totalChecks : 0.5;
    }
}