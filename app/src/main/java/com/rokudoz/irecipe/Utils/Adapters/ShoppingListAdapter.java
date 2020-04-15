package com.rokudoz.irecipe.Utils.Adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.R;

import java.text.MessageFormat;
import java.util.List;

public class ShoppingListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "ShoppingListAdapter";

    private static int TYPE_CATEGORY = 1;
    private static int TYPE_INGREDIENT = 2;
    private Context context;
    private List<Ingredient> ingredientList;

    public ShoppingListAdapter(Context context, List<Ingredient> ingredientList) {
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
            view = LayoutInflater.from(context).inflate(R.layout.rv_shopping_list_item, viewGroup, false);
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
            ((ShoppingListAdapter.CategoryViewHolder) viewHolder).setCategoryDetails(ingredientList.get(position));
        } else {
            ((ShoppingListAdapter.MyIngredientViewHolder) viewHolder).setIngredientDetails(ingredientList.get(position), (ShoppingListAdapter.MyIngredientViewHolder) viewHolder, position);
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
        private MaterialCheckBox checkBox;

        MyIngredientViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.shoppingList_checkbox);
        }

        void setIngredientDetails(Ingredient ingredient, MyIngredientViewHolder holder, final int position) {
            checkBox.setText(MessageFormat.format("{0} {1}  {2}", ingredient.getQuantity(), ingredient.getQuantity_type(), ingredient.getName()));
            if (ingredient.getOwned() != null)
                checkBox.setChecked(ingredient.getOwned());
            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    ingredientList.get(position).setOwned(isChecked);
                }
            });
        }
    }
}

