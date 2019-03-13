package com.rokudoz.irecipe.Utils;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Map;


public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {
    private ArrayList<Recipe> mRecipeList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvDocumentId;
        ImageView mImageView;
        Map<String, Boolean> ingredientTags;

        public RecipeViewHolder(View itemView, final OnItemClickListener listener) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.text_view_title);
            tvDescription = itemView.findViewById(R.id.text_view_description);
            tvDocumentId = itemView.findViewById(R.id.text_view_id);
            mImageView = itemView.findViewById(R.id.recipeItem_image);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onItemClick(position);
                        }
                    }
                }
            });
        }
    }

    public RecipeAdapter(ArrayList<Recipe> recipeList) {
        mRecipeList = recipeList;
    }

    @Override
    public RecipeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recipe_item, parent, false);
        RecipeViewHolder recipeViewHolder = new RecipeViewHolder(v, mListener);
        return recipeViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe currentItem = mRecipeList.get(position);

        holder.tvTitle.setText(currentItem.getTitle());
        holder.tvDescription.setText(currentItem.getDescription());
        holder.tvDocumentId.setText(currentItem.getDocumentId());
        Picasso.get()
                .load(currentItem.getImageUrl())
                .fit()
                .centerCrop()
                .into(holder.mImageView);

    }

    @Override
    public int getItemCount() {
        return mRecipeList.size();
    }
}