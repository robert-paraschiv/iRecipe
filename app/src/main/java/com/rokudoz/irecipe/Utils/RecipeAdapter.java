package com.rokudoz.irecipe.Utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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

        void onFavoriteCick(int position);

        void onDeleteClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public class RecipeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
        TextView tvTitle, tvDescription;
        ImageView mImageView,imgFavorited;
        Map<String, Boolean> ingredientTags;

        public RecipeViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.text_view_title);
            tvDescription = itemView.findViewById(R.id.text_view_description);
            mImageView = itemView.findViewById(R.id.recipeItem_image);
            imgFavorited = itemView.findViewById(R.id.recyclerview_favorite);

            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    mListener.onItemClick(position);
                }
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.setHeaderTitle("Select Action");
            MenuItem setAsFavorite = menu.add(Menu.NONE, 1, 1, "Favorite");
            MenuItem delete = menu.add(Menu.NONE, 2, 2, "Delete");

            setAsFavorite.setOnMenuItemClickListener(this);
            delete.setOnMenuItemClickListener(this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            if (mListener != null) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    switch (item.getItemId()) {
                        case 1:
                            mListener.onFavoriteCick(position);
                            break;
                        case 2:
                            mListener.onDeleteClick(position);
                            break;
                    }
                }
            }
            return false;
        }
    }

    public RecipeAdapter(ArrayList<Recipe> recipeList) {
        mRecipeList = recipeList;
    }

    @Override
    public RecipeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_recipe_item, parent, false);
        return new RecipeViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe currentItem = mRecipeList.get(position);

        holder.tvTitle.setText(currentItem.getTitle());
        holder.tvDescription.setText(currentItem.getDescription());
        Picasso.get()
                .load(currentItem.getImageUrl())
                .fit()
                .centerCrop()
                .into(holder.mImageView);
        holder.imgFavorited.setImageResource(R.drawable.ic_star_border_black_24dp);
        if (currentItem.getFavorite() != null && currentItem.getFavorite()) {
            holder.imgFavorited.setImageResource(R.drawable.ic_star_gold_24dp);
        }

    }

    @Override
    public int getItemCount() {
        return mRecipeList.size();
    }
}