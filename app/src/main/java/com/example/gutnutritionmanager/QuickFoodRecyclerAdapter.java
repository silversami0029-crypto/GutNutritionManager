package com.example.gutnutritionmanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class QuickFoodRecyclerAdapter extends RecyclerView.Adapter<QuickFoodRecyclerAdapter.ViewHolder> {
    private Context context;
    private List<QuickFood> foods;
    private OnFoodClickListener listener;

    public interface OnFoodClickListener {void onFoodClick(QuickFood food);}

    public QuickFoodRecyclerAdapter(Context context, List<QuickFood> foods, OnFoodClickListener listener) {
        this.context = context;
        this.foods = foods;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.quick_food_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuickFood food = foods.get(position);

        holder.emojiView.setText(food.getEmoji());
        holder.nameView.setText(food.getName());

        if (food.getDefaultPreparation() != null && !food.getDefaultPreparation().isEmpty()) {
            holder.prepView.setText(food.getDefaultPreparation());
            holder.prepView.setVisibility(View.VISIBLE);
        } else {
            holder.prepView.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            // Handle click - you'll need to pass a callback
            if (listener != null) {
                listener.onFoodClick(food);
            }
        });
    }

    @Override
    public int getItemCount() {
        return foods.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView emojiView, nameView, prepView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            emojiView = itemView.findViewById(R.id.foodEmoji);
            nameView = itemView.findViewById(R.id.foodName);
            prepView = itemView.findViewById(R.id.preparationHint);
        }
    }


}
