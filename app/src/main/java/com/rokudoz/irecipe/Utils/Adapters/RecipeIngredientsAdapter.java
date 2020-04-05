package com.rokudoz.irecipe.Utils.Adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Instruction;
import com.rokudoz.irecipe.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class RecipeIngredientsAdapter extends RecyclerView.Adapter<RecipeIngredientsAdapter.RecipeIngredientsViewHolder> {
    private List<Ingredient> ingredientList = new ArrayList<>();

    class RecipeIngredientsViewHolder extends RecyclerView.ViewHolder {
        TextView tvIngredientName, tvIngredientQuantity, tvQuantityType;
        ImageView spacer;

        RecipeIngredientsViewHolder(View itemView) {
            super(itemView);
            tvIngredientName = itemView.findViewById(R.id.ingredient_item_Name_TextView);
            tvIngredientQuantity = itemView.findViewById(R.id.ingredient_item_quantity_TextView);
            tvQuantityType = itemView.findViewById(R.id.ingredient_item_quantity_type_TextView);
            spacer = itemView.findViewById(R.id.rv_layout_ingredient_spacer);
        }

    }

    public RecipeIngredientsAdapter(List<Ingredient> ingredientList) {
        this.ingredientList = ingredientList;
    }

    @Override
    public RecipeIngredientsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_ingredient_item, parent, false);
        return new RecipeIngredientsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecipeIngredientsViewHolder holder, int position) {
        final Ingredient currentItem = ingredientList.get(position);

        if (position == 0) {
            holder.spacer.setVisibility(View.GONE);
        } else {
            holder.spacer.setVisibility(View.VISIBLE);
        }

        if (currentItem.getName() != null)
            holder.tvIngredientName.setText(currentItem.getName());
        if (currentItem.getQuantity() != null){
            String quantity = String.format(Locale.US, "%.1f", currentItem.getQuantity());
            holder.tvIngredientQuantity.setText(quantity);
        }
        if (currentItem.getQuantity_type() != null)
            holder.tvQuantityType.setText(currentItem.getQuantity_type());
    }

    @Override
    public int getItemCount() {
        return ingredientList.size();
    }
}