package com.example.gutnutritionmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FoodLogAdapter extends RecyclerView.Adapter<FoodLogAdapter.ViewHolder> {
    private List<FoodLog> foodLogs;
    private OnFoodLogClickListener listener;

    public interface OnFoodLogClickListener {
        void onFoodLogClick(FoodLog foodLog);
        void onFoodLogDelete(FoodLog foodLog);
    }

    public FoodLogAdapter(List<FoodLog> foodLogs, OnFoodLogClickListener listener) {
        this.foodLogs = foodLogs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.food_log_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodLog foodLog = foodLogs.get(position);

        holder.foodName.setText(foodLog.getFoodName());
        holder.foodDetails.setText(foodLog.getPreparation() + " • " + foodLog.getMealType());
        holder.fodmapBadge.setText(foodLog.getFodmapStatus());
        holder.moodText.setText(foodLog.getMood());
        holder.stressLevelText.setText("Stress: " + foodLog.getStressLevel() + "/5");

        // Set emoji based on food name
        holder.foodEmoji.setText(getFoodEmoji(foodLog.getFoodName()));

        // Set FODMAP badge color
        if ("LOW".equals(foodLog.getFodmapStatus())) {
            holder.fodmapBadge.setBackgroundResource(R.drawable.fodmap_low_badge);
        } else {
            holder.fodmapBadge.setBackgroundResource(R.drawable.fodmap_high_badge);
        }

        // Click listener for edit
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFoodLogClick(foodLog);
            }
        });

        // Delete button click
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFoodLogDelete(foodLog);
            }
        });
    }

    @Override
    public int getItemCount() {
        return foodLogs != null ? foodLogs.size() : 0;
    }

    public void updateData(List<FoodLog> newFoodLogs) {
        this.foodLogs = newFoodLogs;
        notifyDataSetChanged();
    }
/*
    public void deleteItem(int position) {
        if (position >= 0 && position < foodLogs.size()) {
            FoodLog deletedFoodLog = foodLogs.remove(position);
            notifyItemRemoved(position);

            if (listener != null) {
                listener.onFoodLogDelete(deletedFoodLog);
            }
        }
    }*/

    private String getFoodEmoji(String foodName) {
        if (foodName == null) return "🍽️";

        String lowerName = foodName.toLowerCase();
        switch (lowerName) {
            case "chicken": return "🍗";
            case "fish": return "🐟";
            case "eggs": return "🥚";
            case "rice": return "🍚";
            case "oats": return "🌾";
            case "carrots": return "🥕";
            case "eggplant": return "🍆";
            case "tomatoes": return "🍅";
            case "grapes": return "🍇";
            case "oranges": return "🍊";
            case "cheese": return "🧀";
            case "berries": return "🍓";
            default: return "🍽️";
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView foodEmoji, foodName, foodDetails, fodmapBadge;
        View deleteButton;
        TextView moodText, stressLevelText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            foodEmoji = itemView.findViewById(R.id.foodEmoji);
            foodName = itemView.findViewById(R.id.foodName);
            foodDetails = itemView.findViewById(R.id.foodDetails);
            fodmapBadge = itemView.findViewById(R.id.fodmapBadge);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            moodText = itemView.findViewById(R.id.moodText);
            stressLevelText = itemView.findViewById(R.id.stressLevelText);
        }
    }

    public void deleteItem(int position) {
        if (position >= 0 && position < foodLogs.size()) {
            FoodLog deletedFoodLog = foodLogs.remove(position);
            notifyItemRemoved(position);

            if (listener != null) {
                listener.onFoodLogDelete(deletedFoodLog);
            }
        }
    }
}