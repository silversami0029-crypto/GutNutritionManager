package com.example.gutnutritionmanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> implements Filterable {
    private List<Recipe> recipeList; // Current displayed list
    private List<Recipe> recipeListFull; // Full list for filtering
    private OnRecipeClickListener listener;

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
        void onRecipeFavorite(Recipe recipe, boolean isFavorite);
    }

    public RecipeAdapter(List<Recipe> recipes, OnRecipeClickListener listener) {
        this.recipeList = new ArrayList<>(recipes);
        this.recipeListFull = new ArrayList<>(recipes);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);

        holder.recipeTitle.setText(recipe.getTitle());
        holder.recipeTime.setText(recipe.getCookingTime());
        holder.recipeDifficulty.setText(recipe.getDifficulty());

        // Set FODMAP badge
        if ("LOW".equals(recipe.getFodmapLevel())) {
            holder.fodmapBadge.setText("LOW FODMAP");
            holder.fodmapBadge.setBackgroundResource(R.drawable.fodmap_low_badge);
        } else if ("MODERATE".equals(recipe.getFodmapLevel())) {
            holder.fodmapBadge.setText("MODERATE");
            holder.fodmapBadge.setBackgroundResource(R.drawable.fodmap_moderate_badge);
        } else {
            holder.fodmapBadge.setText("HIGH");
            holder.fodmapBadge.setBackgroundResource(R.drawable.fodmap_high_badge);
        }

        // Set favorite state
        holder.favoriteButton.setImageResource(
                recipe.isFavorite() ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border
        );

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClick(recipe);
            }
        });

        holder.favoriteButton.setOnClickListener(v -> {
            boolean newFavoriteState = !recipe.isFavorite();
            recipe.setFavorite(newFavoriteState);
            holder.favoriteButton.setImageResource(
                    newFavoriteState ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border
            );

            if (listener != null) {
                listener.onRecipeFavorite(recipe, newFavoriteState);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipeList != null ? recipeList.size() : 0;
    }

    public void updateData(List<Recipe> newRecipes) {
        this.recipeList = new ArrayList<>(newRecipes);
        this.recipeListFull = new ArrayList<>(newRecipes);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView recipeTitle, recipeTime, recipeDifficulty, fodmapBadge;
        ImageButton favoriteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeTitle = itemView.findViewById(R.id.recipeTitle);
            recipeTime = itemView.findViewById(R.id.recipeTime);
            recipeDifficulty = itemView.findViewById(R.id.recipeDifficulty);
            fodmapBadge = itemView.findViewById(R.id.fodmapBadge);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
        }
    }

    @Override
    public Filter getFilter() {
        return recipeFilter;
    }

    private Filter recipeFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Recipe> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(recipeListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (Recipe recipe : recipeListFull) {
                    if (recipe.getTitle().toLowerCase().contains(filterPattern)) {
                        filteredList.add(recipe);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            recipeList.clear();
            recipeList.addAll((List<Recipe>) results.values);
            notifyDataSetChanged();
        }
    };
}