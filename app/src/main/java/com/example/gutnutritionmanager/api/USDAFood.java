package com.example.gutnutritionmanager.api;

import java.util.List;

public class USDAFood {
    public int fdcId;
    public String description;
    public String dataType;
    public String foodCategory;
    public String publicationDate;
    public String foodCode;
    public List<FoodNutrient> foodNutrients;

    // Getters and setters
    public int getFdcId() { return fdcId; }
    public void setFdcId(int fdcId) { this.fdcId = fdcId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getPublicationDate() { return publicationDate; }
    public void setPublicationDate(String publicationDate) { this.publicationDate = publicationDate; }

    public String getFoodCode() { return foodCode; }
    public void setFoodCode(String foodCode) { this.foodCode = foodCode; }

    public List<FoodNutrient> getFoodNutrients() { return foodNutrients; }
    public void setFoodNutrients(List<FoodNutrient> foodNutrients) { this.foodNutrients = foodNutrients; }

    public String getFoodCategory() {return foodCategory; }
    public void setFoodCategory(String foodCategory){ this.foodCategory = foodCategory; }

    // FoodNutrient inner class
    public static class FoodNutrient {
        public int nutrientId;
        public String nutrientName;
        public String nutrientNumber;
        public String unitName;
        public double value;
        public int rank;

        // Getters and setters
        public int getNutrientId() { return nutrientId; }
        public void setNutrientId(int nutrientId) { this.nutrientId = nutrientId; }

        public String getNutrientName() { return nutrientName; }
        public void setNutrientName(String nutrientName) { this.nutrientName = nutrientName; }

        public String getNutrientNumber() { return nutrientNumber; }
        public void setNutrientNumber(String nutrientNumber) { this.nutrientNumber = nutrientNumber; }

        public String getUnitName() { return unitName; }
        public void setUnitName(String unitName) { this.unitName = unitName; }

        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }

        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
    }

}