package com.example.gutnutritionmanager;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

@Entity(tableName = "foods", indices ={@Index(value = {"name"}, unique = true)})
public class Food implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    public int id;
   // private boolean fromUSDA = false;


    public String name;
    public String usdaId;
    public String category; // e.g., "Fruits", "Vegetables", "Dairy", etc.
    @ColumnInfo(name = "common")
    public boolean isCommon;

    @ColumnInfo(name = "fodmap_status")
    public String fodmapStatus;

    // Add nutritional information fields
    public double calories;
    public double protein;
    public double carbs;
    public double fat;
    public double fiber;
    public double sugar;

    public String nutrientData;


    // Parcelable constructor
    protected Food(Parcel in) {
        id = in.readInt();
        name = in.readString();
        category = in.readString();
        fodmapStatus = in.readString();
        usdaId = in.readString();
        calories = in.readDouble();
        protein = in.readDouble();
        carbs = in.readDouble();
        fat = in.readDouble();
        fiber = in.readDouble();
        sugar = in.readDouble();
        nutrientData = in.readString();
    }


    public static final Creator<Food> CREATOR = new Creator<Food>() {
        @Override
        public Food createFromParcel(Parcel in) {
            return new Food(in);
        }

        @Override
        public Food[] newArray(int size) {
            return new Food[size];
        }
    };

    @Override



    public int describeContents() {
        return 0;
    }


    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(category);
        dest.writeString(fodmapStatus);
        dest.writeString(usdaId);
        dest.writeDouble(calories);
        dest.writeDouble(protein);
        dest.writeDouble(carbs);
        dest.writeDouble(fat);
        dest.writeDouble(fiber);
        dest.writeDouble(sugar);
        dest.writeString(nutrientData);
    }

    // Constructor
    @Ignore
    public Food(int id, String name, String category, boolean isCommon,
                String fodmapStatus, String usdaId, double calories,
                double protein, double carbs, double fat, double fiber,
                double sugar, String nutrientData) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.isCommon = isCommon;
        this.fodmapStatus = fodmapStatus; // This should be set properly
        this.usdaId = usdaId;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.fiber = fiber;
        this.sugar = sugar;
        this.nutrientData = nutrientData;
    }



    // Getters and setters for all fields
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

   // public void setfromUSDA(boolean fromUSDA) { this.fromUSDA = fromUSDA; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isCommon() { return isCommon; }
    public void setCommon(boolean isCommon) { this.isCommon = isCommon; }

    public String getFodmapStatus() { return fodmapStatus; }
    public void setFodmapStatus(String fodmapStatus) { this.fodmapStatus = fodmapStatus; }

    public String getUsdaId() { return usdaId; }
    public void setUsdaId(String usdaId) { this.usdaId = usdaId; }

    public Food() {
        // Default constructor
    }
    // Add this toString method
    @Override
    public String toString() {
        return name;
    }

    public void setCalories(double calories) { this.calories = calories; }
    public void setProtein(double protein) { this.protein = protein; }
    public void setCarbs(double carbs) { this.carbs = carbs; }
    public void setFat(double fat) { this.fat = fat; }
    public void setFiber(double fiber) { this.fiber = fiber; }
    public void setSugar(double sugar) { this.sugar = sugar; }
    public void setNutrientData(String nutrientData) { this.nutrientData = nutrientData;}

    }