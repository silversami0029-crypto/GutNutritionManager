package com.example.gutnutritionmanager;



import com.example.gutnutritionmanager.api.USDAFood;

import java.util.List;

public class USDAFoodSearchResponse {
    private List<USDAFood> foods;
    private int totalHits;
    private int currentPage;
    private int totalPages;

    // Getters and setters
    public List<USDAFood> getFoods() { return foods; }
    public void setFoods(List<USDAFood> foods) { this.foods = foods; }

    public int getTotalHits() { return totalHits; }
    public void setTotalHits(int totalHits) { this.totalHits = totalHits; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
