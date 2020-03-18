package com.rokudoz.irecipe.Utils.Adapters.EditRecipe;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Instruction;
import com.rokudoz.irecipe.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class EditRecipeIngredientsAdapter extends RecyclerView.Adapter<EditRecipeIngredientsAdapter.EditRecipeIngredientsViewHolder> {
    private List<Ingredient> ingredientList = new ArrayList<>();

    class EditRecipeIngredientsViewHolder extends RecyclerView.ViewHolder {
        EditText nameEditText, quantityEditText;
        Spinner categorySpinner, quantityTypeSpinner;

        EditRecipeIngredientsViewHolder(View itemView) {
            super(itemView);
            nameEditText = itemView.findViewById(R.id.rv_edit_recipe_ingredient_name_editText);
            quantityEditText = itemView.findViewById(R.id.rv_edit_recipe_ingredient_quantity_editText);
            categorySpinner = itemView.findViewById(R.id.rv_edit_recipe_ingredient_category_spinner);
            quantityTypeSpinner = itemView.findViewById(R.id.rv_edit_recipe_ingredient_quantity_spinner);
        }

    }

    public EditRecipeIngredientsAdapter(List<Ingredient> ingredientList) {
        this.ingredientList = ingredientList;
    }

    @Override
    public EditRecipeIngredientsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_edit_recipe_ingredient_layout_item, parent, false);
        return new EditRecipeIngredientsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final EditRecipeIngredientsViewHolder holder, int position) {
        final Ingredient currentItem = ingredientList.get(position);

        if (currentItem.getName() != null)
            holder.nameEditText.setText(currentItem.getName());
        if (currentItem.getQuantity() != null)
            holder.quantityEditText.setText(currentItem.getQuantity().toString());
    }

    @Override
    public int getItemCount() {
        return ingredientList.size();
    }
}