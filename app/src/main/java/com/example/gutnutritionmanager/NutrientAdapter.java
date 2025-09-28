package com.example.gutnutritionmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gutnutritionmanager.api.USDAFood;
import java.util.List;

public class NutrientAdapter extends RecyclerView.Adapter<NutrientAdapter.ViewHolder> {

    private List<USDAFood.FoodNutrient> nutrients;

    public NutrientAdapter(List<USDAFood.FoodNutrient> nutrients) {
        this.nutrients = nutrients;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nutrient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        USDAFood.FoodNutrient nutrient = nutrients.get(position);

        if (nutrient != null) {
            // Use the getter methods from your FoodNutrient class
            holder.nutrientName.setText(nutrient.getNutrientName());
            holder.nutrientValue.setText(String.format("%.2f %s",
                    nutrient.getValue(),
                    nutrient.getUnitName()));
        }
    }

    @Override
    public int getItemCount() {
        return nutrients != null ? nutrients.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nutrientName;
        TextView nutrientValue;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nutrientName = itemView.findViewById(R.id.nutrient_name);
            nutrientValue = itemView.findViewById(R.id.nutrient_value);
        }
    }
}