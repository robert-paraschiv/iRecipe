package com.rokudoz.irecipe.Utils.Adapters.MyIngredients;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.R;

import java.util.ArrayList;
import java.util.List;

public class MyIngredientsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    private static int TYPE_CATEGORY = 1;
    private static int TYPE_INGREDIENT = 2;
    private Context context;
    private List<Ingredient> ingredientList;

    public MyIngredientsAdapter(Context context, List<Ingredient> ingredientList) {
        this.context = context;
        this.ingredientList = ingredientList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view;
        if (viewType == TYPE_CATEGORY) { // for category layout
            view = LayoutInflater.from(context).inflate(R.layout.category_title, viewGroup, false);
            return new CategoryViewHolder(view);

        } else { // for ingredient layout
            view = LayoutInflater.from(context).inflate(R.layout.ingredient_title, viewGroup, false);
            return new MyIngredientViewHolder(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (TextUtils.isEmpty(ingredientList.get(position).getCategory_name()) || ingredientList.get(position).getCategory_name() == null) {
            return TYPE_INGREDIENT;
        } else {
            return TYPE_CATEGORY;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        if (getItemViewType(position) == TYPE_CATEGORY) {
            ((CategoryViewHolder) viewHolder).setCategoryDetails(ingredientList.get(position));
        } else {
            ((MyIngredientViewHolder) viewHolder).setIngredientDetails(ingredientList.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return ingredientList.size();
    }

    public class CategoryViewHolder extends RecyclerView.ViewHolder {

        private TextView txtName;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.myingredients_category_title);
        }

        void setCategoryDetails(Ingredient ingredient) {
            txtName.setText(ingredient.getCategory_name());
        }
    }

    public class MyIngredientViewHolder extends RecyclerView.ViewHolder {
        private TextView txtName;

        MyIngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.myingredients_ingredient_title);
        }

        void setIngredientDetails(Ingredient ingredient) {
            txtName.setText(ingredient.getName());
        }
    }
}
