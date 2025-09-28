package com.example.gutnutritionmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FoodAdviceAdapter extends RecyclerView.Adapter<FoodAdviceAdapter.AdviceViewHolder> {

    private List<FoodAdvice> adviceList;

    public FoodAdviceAdapter(List<FoodAdvice> adviceList) {
        this.adviceList = adviceList;
    }

    public void updateData(List<FoodAdvice> newAdviceList) {
        this.adviceList = newAdviceList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_advice, parent, false);
        return new AdviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdviceViewHolder holder, int position) {
        FoodAdvice advice = adviceList.get(position);
        holder.bind(advice);
    }

    @Override
    public int getItemCount() {
        return adviceList.size();
    }

    static class AdviceViewHolder extends RecyclerView.ViewHolder {
        private TextView descriptionTextView;
        private TextView explanationTextView;
        private TextView impactTextView;

        public AdviceViewHolder(@NonNull View itemView) {
            super(itemView);
            descriptionTextView = itemView.findViewById(R.id.advice_description);
            explanationTextView = itemView.findViewById(R.id.advice_explanation);
            impactTextView = itemView.findViewById(R.id.advice_impact);
        }

        public void bind(FoodAdvice advice) {
            descriptionTextView.setText(advice.description);
            explanationTextView.setText(advice.explanation);

            // Set impact text and color
            if (advice.fodmapImpact != null) {
                impactTextView.setText(advice.fodmapImpact);

                // Set background color based on impact
                switch (advice.fodmapImpact.toUpperCase()) {
                    case "BETTER":
                        impactTextView.setBackgroundColor(0xFF4CAF50); // Green
                        break;
                    case "WORSE":
                        impactTextView.setBackgroundColor(0xFFF44336); // Red
                        break;
                    case "SAME":
                        impactTextView.setBackgroundColor(0xFF2196F3); // Blue
                        break;
                    default:
                        impactTextView.setBackgroundColor(0xFF9E9E9E); // Gray
                }
            } else {
                impactTextView.setVisibility(View.GONE);
            }
        }
    }
}