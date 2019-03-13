package com.rokudoz.irecipe.Utils;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.R;

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
        Map<String, Boolean> ingredientTags;

        public RecipeViewHolder(View itemView, final OnItemClickListener listener) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.text_view_title);
            tvDescription = itemView.findViewById(R.id.text_view_description);
            tvDocumentId = itemView.findViewById(R.id.text_view_id);

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

    public RecipeAdapter(ArrayList<Recipe> exampleList) {
        mRecipeList = exampleList;
    }

    @Override
    public RecipeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recipe_item, parent, false);
        RecipeViewHolder evh = new RecipeViewHolder(v, mListener);
        return evh;
    }

    @Override
    public void onBindViewHolder(RecipeViewHolder holder, int position) {
        Recipe currentItem = mRecipeList.get(position);

        holder.tvTitle.setText(currentItem.getTitle());
        holder.tvDescription.setText(currentItem.getDescription());
        holder.tvDocumentId.setText(currentItem.getDocumentId());
    }

    @Override
    public int getItemCount() {
        return mRecipeList.size();
    }
}