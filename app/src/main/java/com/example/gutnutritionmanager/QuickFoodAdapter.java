package com.example.gutnutritionmanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class QuickFoodAdapter extends BaseAdapter {
    private Context context;
    private List<QuickFood> foods;
    private LayoutInflater inflater;

    public QuickFoodAdapter(Context context, List<QuickFood> foods) {
        this.context = context;
        this.foods = foods;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() { return foods.size(); }

    @Override
    public Object getItem(int position) { return foods.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            // Inflate the layout if convertView is null
            convertView = inflater.inflate(R.layout.quick_food_item, parent, false);

            // Create ViewHolder and find views
            holder = new ViewHolder();
            holder.emojiView = convertView.findViewById(R.id.foodEmoji);
            holder.nameView = convertView.findViewById(R.id.foodName);
            holder.prepView = convertView.findViewById(R.id.preparationHint);

            convertView.setTag(holder);
        } else {
            // Reuse the existing view
            holder = (ViewHolder) convertView.getTag();
        }

        // Get the food item
        QuickFood food = foods.get(position);

        // Set the data
        holder.emojiView.setText(food.getEmoji());
        holder.nameView.setText(food.getName());

        // Show preparation hint if available
        if (food.getDefaultPreparation() != null && !food.getDefaultPreparation().isEmpty()) {
            holder.prepView.setText(food.getDefaultPreparation());
            holder.prepView.setVisibility(View.VISIBLE);
        } else {
            holder.prepView.setVisibility(View.GONE);
        }

        return convertView;
    }

    // ViewHolder pattern for better performance
    private static class ViewHolder {
        TextView emojiView;
        TextView nameView;
        TextView prepView;
    }

}
