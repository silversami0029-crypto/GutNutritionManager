package com.example.gutnutritionmanager;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FoodAdapter extends ArrayAdapter<Food> implements Filterable {
    private List<Food> foodList;
    private List<Food> filteredFoodList;
    private NutritionViewModel viewModel;
    private List<Food> localFoods = new ArrayList<>();
    private List<Food> usdaFoods = new ArrayList<>();
    private boolean showingUSDAResults = false;

    // KEEP ALL YOUR EXISTING METHODS - just fix the filter logic
    public void setLocalFoodList(List<Food> foods) {
        Log.d("FoodAdapter", "Setting local food list: " + foods.size() + " items");
        this.localFoods.clear();
        this.localFoods.addAll(foods);
        this.filteredFoodList.clear();

        // Remove duplicates when setting the list
        Set<String> seenNames = new HashSet<>();
        for (Food food : foods) {
            String normalizedName = food.name.toLowerCase().trim();
            if (seenNames.add(normalizedName)) {
                this.filteredFoodList.add(food);
            }
        }

        this.showingUSDAResults = false;
        for (Food food : foods) {
            Log.d("FoodAdapter", "Local Food: " + food.name + ", FODMAP: " + food.fodmapStatus);
        }
        notifyDataSetChanged();
    }

    public void setUSDAFoodList(List<Food> foods) {
        this.usdaFoods.clear();
        this.usdaFoods.addAll(foods);
        this.filteredFoodList.clear();

        // Remove duplicates when setting the list
        Set<String> seenNames = new HashSet<>();
        for (Food food : foods) {
            String normalizedName = food.name.toLowerCase().trim();
            if (seenNames.add(normalizedName)) {
                this.filteredFoodList.add(food);
            }
        }

        this.showingUSDAResults = true;
        notifyDataSetChanged();
    }

    public boolean isShowingUSDAResults() {
        return showingUSDAResults;
    }

    public FoodAdapter(@NonNull Context context, int resource, NutritionViewModel viewModel) {
        super(context, resource, new ArrayList<>());
        this.foodList = new ArrayList<>();
        this.filteredFoodList = new ArrayList<>();
        this.viewModel = viewModel;
    }

    public void setFoodList(List<Food> foodList) {
        Log.d("FoodAdapter", "Setting food list: " + foodList.size() + " items");
        this.foodList.clear();
        this.filteredFoodList.clear();
        if (foodList != null) {
            // Remove duplicates when setting the list
            Set<String> seenNames = new HashSet<>();
            for (Food food : foodList) {
                String normalizedName = food.name.toLowerCase().trim();
                if (seenNames.add(normalizedName)) {
                    this.foodList.add(food);
                    this.filteredFoodList.add(food);
                }
            }
            for (Food food : foodList) {
                Log.d("FoodAdapter", "Food: " + food.name + ", FODMAP: " + food.fodmapStatus);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return filteredFoodList.size();
    }

    @Override
    public Food getItem(int position) {
        return filteredFoodList.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_food, parent, false);
        }

        convertView.setOnClickListener(v -> {
            Food clickedFood = getItem(position);
            if (clickedFood != null) {
                Log.d("FoodAdapter", "Clicked on: " + clickedFood.name);
                Intent intent = new Intent(getContext(), NutritionDetailActivity.class);
                intent.putExtra("food", clickedFood);
                getContext().startActivity(intent);
            } else {
                Log.e("FoodAdapter", "No food object for position: " + position);
            }
        });

        TextView textView = convertView.findViewById(R.id.foodName);
        TextView sourceTextView = convertView.findViewById(R.id.foodSource);

        Food food = getItem(position);
        Log.d("FoodAdapter", "Position: " + position + ", Food: " + (food != null ? food.name : "null"));

        if (food != null) {
            String displayName = formatFoodName(food.name);
            textView.setText(displayName); // Use formatted name

            Log.d("FoodAdapter", "Food: " + food.name +
                    ", USDA ID: " + (food.usdaId != null ? food.usdaId : "null") +
                    ", FODMAP Status: " + (food.fodmapStatus != null ? food.fodmapStatus : "null"));

            if (food.usdaId != null && !food.usdaId.isEmpty()) {
                Log.d("FoodAdapter", "Showing USDA indicator for: " + food.name + ", USDA ID: " + food.usdaId);
                sourceTextView.setVisibility(View.VISIBLE);
                sourceTextView.setText("USDA");
            } else {
                Log.d("FoodAdapter", "No USDA ID for: " + food.name);
                sourceTextView.setVisibility(View.GONE);
            }

            String fodmapStatus = food.fodmapStatus;
            if (fodmapStatus != null && !fodmapStatus.equals("UNKNOWN")) {
                int color = FODMAPUtils.getColorForStatus(fodmapStatus);
                String description = FODMAPUtils.getDescriptionForStatus(fodmapStatus);
                textView.setTextColor(color);
            }
        }

        return convertView;
    }

    private String formatFoodName(String foodName) {
        if (foodName == null || foodName.isEmpty()) {
            return foodName;
        }

        String[] words = foodName.split("\\s+");
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i > 0) formatted.append(" ");
            String word = words[i];
            if (word.isEmpty()) continue;
            formatted.append(word.substring(0, 1).toUpperCase())
                    .append(word.substring(1).toLowerCase());
        }
        return formatted.toString();
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new FoodFilter();
    }

    private class FoodFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            List<Food> filteredList = new ArrayList<>();
            Set<String> seenFoodNames = new HashSet<>();

            if (constraint == null || constraint.length() == 0) {
                for (Food food : foodList) {
                    String normalizedName = food.name.toLowerCase().trim();
                    if (seenFoodNames.add(normalizedName)) {
                        filteredList.add(food);
                    }
                }
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Food food : foodList) {
                    if (food.name.toLowerCase().contains(filterPattern)) {
                        String normalizedName = food.name.toLowerCase().trim();
                        if (seenFoodNames.add(normalizedName)) {
                            filteredList.add(food);
                        }
                    }
                }
            }

            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredFoodList.clear();
            if (results.values != null) {
                filteredFoodList.addAll((List<Food>) results.values);
            }
            notifyDataSetChanged();
        }
    }
}