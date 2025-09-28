package com.example.gutnutritionmanager.api;

import java.util.List;

public class USDAFoodSearchResponse {
    public List<USDAFood> foods;
    public int totalHits;
    public int currentPage;
    public int totalPages;

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