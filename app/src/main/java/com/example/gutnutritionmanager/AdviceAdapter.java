package com.example.gutnutritionmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdviceAdapter extends RecyclerView.Adapter<AdviceAdapter.ViewHolder> {

    private List<FoodAdvice> adviceList;

    public AdviceAdapter(List<FoodAdvice> adviceList) {
        this.adviceList = adviceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_advice, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodAdvice advice = adviceList.get(position);

        holder.description.setText(advice.description);
        holder.explanation.setText(advice.explanation);
        holder.impact.setText(advice.fodmapImpact);

        // Set different colors based on impact
        int color;
        if ("BETTER".equals(advice.fodmapImpact)) {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.green);
        } else if ("WORSE".equals(advice.fodmapImpact)) {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.red);
        } else {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.gray);
        }

        holder.impact.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return adviceList != null ? adviceList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView description;
        TextView explanation;
        TextView impact;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            description = itemView.findViewById(R.id.advice_description);
            explanation = itemView.findViewById(R.id.advice_explanation);
            impact = itemView.findViewById(R.id.advice_impact);
        }
    }
}
