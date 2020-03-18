package com.rokudoz.irecipe.Utils.Adapters.EditRecipe;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
        Spinner quantityTypeSpinner;

        EditRecipeIngredientsViewHolder(View itemView) {
            super(itemView);
            nameEditText = itemView.findViewById(R.id.rv_edit_recipe_ingredient_name_editText);
            quantityEditText = itemView.findViewById(R.id.rv_edit_recipe_ingredient_quantity_editText);
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
    public void onBindViewHolder(@NonNull final EditRecipeIngredientsViewHolder holder, final int position) {
        final Ingredient currentItem = ingredientList.get(position);

        if (currentItem.getName() != null)
            holder.nameEditText.setText(currentItem.getName());
        if (currentItem.getQuantity() != null)
            holder.quantityEditText.setText(currentItem.getQuantity().toString());

        holder.nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ingredientList.get(position).setName(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        holder.quantityEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ingredientList.get(position).setQuantity(Float.parseFloat(s.toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        final String[] ingredientSpinnerItems = holder.quantityTypeSpinner.getResources().getStringArray(R.array.ingredient_quantity_type);
        holder.quantityTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int i, long id) {
                ingredientList.get(position).setQuantity_type(ingredientSpinnerItems[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return ingredientList.size();
    }
}