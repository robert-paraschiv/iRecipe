package com.rokudoz.irecipe.Utils.Adapters;

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

public class MyIngredientAdapter extends RecyclerView.Adapter<MyIngredientAdapter.MyIngredientViewHolder> {
    private List<Ingredient> ingredientList = new ArrayList<>();

    public class MyIngredientViewHolder extends RecyclerView.ViewHolder {
        TextView tvIngredientName;

        public MyIngredientViewHolder(View itemView) {
            super(itemView);
            tvIngredientName = itemView.findViewById(R.id.my_ingredient_name);
        }

    }

    public MyIngredientAdapter(List<Ingredient> ingredientList) {
        this.ingredientList = ingredientList;
    }

    @Override
    public MyIngredientViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_myingredients_ingredient_item, parent, false);
        return new MyIngredientViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyIngredientViewHolder holder, int position) {
        final Ingredient currentItem = ingredientList.get(position);

        holder.tvIngredientName.setText(currentItem.getName());
    }

    @Override
    public int getItemCount() {
        return ingredientList.size();
    }

}
