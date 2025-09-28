package com.example.gutnutritionmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodMatchingService {
    private USDAFoodMappingDao usdaFoodMappingDao;
    private FoodAdviceDao foodAdviceDao;

    public FoodMatchingService(USDAFoodMappingDao usdaFoodMappingDao, FoodAdviceDao foodAdviceDao) {
        this.usdaFoodMappingDao = usdaFoodMappingDao;
        this.foodAdviceDao = foodAdviceDao;
    }

    public String findBestMatch(String usdaFoodName, String usdaCategory) {
        // 1. Check for existing mapping
        USDAFoodMapping existingMapping = usdaFoodMappingDao.getMapping(usdaFoodName);
        if (existingMapping != null && existingMapping.confidence > 0.7) {
            return existingMapping.adviceFoodName;
        }

        // 2. Try category-specific matching
        String categoryMatch = matchByCategory(usdaFoodName, usdaCategory);
        if (categoryMatch != null) {
            // Save this mapping for future use
            saveMapping(usdaFoodName, usdaCategory, categoryMatch, 0.8);
            return categoryMatch;
        }

        // 3. Try general pattern matching
        String patternMatch = matchByPattern(usdaFoodName);
        if (patternMatch != null) {
            saveMapping(usdaFoodName, usdaCategory, patternMatch, 0.6);
            return patternMatch;
        }

        // 4. Fallback: extract main food name
        String extractedName = extractMainFoodName(usdaFoodName);
        saveMapping(usdaFoodName, usdaCategory, extractedName, 0.4);
        return extractedName;
    }

    private String matchByCategory(String usdaFoodName, String usdaCategory) {
        if (usdaCategory == null) return null;

        Map<String, List<String>> categoryPatterns = getCategoryPatterns();
        List<String> patterns = categoryPatterns.get(usdaCategory.toLowerCase());

        if (patterns != null) {
            for (String pattern : patterns) {
                if (usdaFoodName.toLowerCase().contains(pattern)) {
                    return getAdviceFoodNameForPattern(pattern, usdaCategory);
                }
            }
        }

        return null;
    }

    private Map<String, List<String>> getCategoryPatterns() {
        Map<String, List<String>> patterns = new HashMap<>();

        // Fruits
        patterns.put("fruits", Arrays.asList("apple", "banana", "orange", "mango", "pear",
                "watermelon", "peach", "cherry", "berry"));

        // Vegetables
        patterns.put("vegetables", Arrays.asList("onion", "garlic", "broccoli", "cauliflower",
                "asparagus", "mushroom", "pea", "bean", "carrot",
                "potato", "tomato", "lettuce", "cabbage"));

        // Dairy
        patterns.put("dairy", Arrays.asList("milk", "cheese", "yogurt", "cream", "butter",
                "ice cream", "custard"));

        // Grains
        patterns.put("grains", Arrays.asList("wheat", "bread", "pasta", "rice", "oat",
                "cereal", "flour", "barley", "rye"));

        // Proteins
        patterns.put("proteins", Arrays.asList("chicken", "beef", "fish", "pork", "egg",
                "tofu", "lentil", "nut", "seed"));

        // Sweeteners
        patterns.put("sweeteners", Arrays.asList("honey", "sugar", "syrup", "molasses",
                "agave", "maple", "stevia"));

        return patterns;
    }

    private String getAdviceFoodNameForPattern(String pattern, String category) {
        Map<String, String> patternToAdvice = new HashMap<>();

        if ("vegetables".equalsIgnoreCase(category)) {
            patternToAdvice.put("onion", "Onion");
            patternToAdvice.put("garlic", "Garlic");
            patternToAdvice.put("broccoli", "Broccoli");
            patternToAdvice.put("cauliflower", "Cauliflower");
            patternToAdvice.put("carrot", "Carrot");
            patternToAdvice.put("asparagus", "Asparagus");
        }
        else if ("fruits".equalsIgnoreCase(category)) {
            patternToAdvice.put("apple", "Apple");
            patternToAdvice.put("banana", "Banana");
            patternToAdvice.put("orange", "Orange");
            patternToAdvice.put("mango", "Mango");
        }
        // Add more category mappings...

        return patternToAdvice.get(pattern);
    }

    private String matchByPattern(String usdaFoodName) {
        // Simple pattern matching without category
        Map<String, String> generalPatterns = new HashMap<>();
        generalPatterns.put("(?i).onion.", "Onion");
        generalPatterns.put("(?i).garlic.", "Garlic");
        generalPatterns.put("(?i).apple.", "Apple");
        generalPatterns.put("(?i).banana.", "Banana");
        generalPatterns.put("(?i).broccoli.", "Broccoli");
        generalPatterns.put("(?i).milk.", "Milk");
        generalPatterns.put("(?i).cheese.", "Cheese");
        generalPatterns.put("(?i).honey.", "Honey");
        generalPatterns.put("(?i).wheat.", "Wheat");

        for (Map.Entry<String, String> entry : generalPatterns.entrySet()) {
            if (usdaFoodName.matches(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private String extractMainFoodName(String usdaFoodName) {
        // Remove common brand names and descriptors
        String[] removePatterns = {
                "(?i)\\b(organic|fresh|frozen|canned|cooked|raw|dried)\\b",
                "(?i)\\b([0-9]+(oz|g|kg|lb|ml|l))\\b",
                "(?i)\\b(with|and|in|of|the)\\b",
                "\\(.*\\)",
                "\\[.*\\]"
        };

        String cleaned = usdaFoodName;
        for (String pattern : removePatterns) {
            cleaned = cleaned.replaceAll(pattern, "");
        }

        // Take the first 1-2 meaningful words
        String[] words = cleaned.trim().split("\\s+");
        if (words.length == 0) return usdaFoodName;

        if (words.length == 1) return words[0];

        // Return first two words, but skip very short words
        List<String> meaningfulWords = new ArrayList<>();
        for (String word : words) {
            if (word.length() > 2) {
                meaningfulWords.add(word);
                if (meaningfulWords.size() == 2) break;
            }
        }

        return String.join(" ", meaningfulWords);
    }

    private void saveMapping(String usdaFoodName, String usdaCategory, String adviceFoodName,double confidence) {
        USDAFoodMapping mapping = new USDAFoodMapping(usdaFoodName, usdaCategory, adviceFoodName, confidence);
        usdaFoodMappingDao.insert(mapping);
    }
}