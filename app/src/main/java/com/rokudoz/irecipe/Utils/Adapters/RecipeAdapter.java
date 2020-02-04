package com.rokudoz.irecipe.Utils.Adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.R;

import java.util.List;
import java.util.Map;


public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {
    private List<Recipe> mRecipeList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);

        void onFavoriteClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public class RecipeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvTitle, tvDescription, tvNrOfFaves, tvNumMissingIngredients, tvNumComments;
        ImageView mImageView, imgFavorited, imgPrivacy;

        public RecipeViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.text_view_title);
            tvDescription = itemView.findViewById(R.id.text_view_description);
            mImageView = itemView.findViewById(R.id.recipeItem_image);
            imgFavorited = itemView.findViewById(R.id.recyclerview_favorite);
            tvNrOfFaves = itemView.findViewById(R.id.recyclerview_nrOfFaves_textView);
            imgPrivacy = itemView.findViewById(R.id.recycler_view_privacy);
            tvNumMissingIngredients = itemView.findViewById(R.id.recycler_view_recipeItem_missingIngredients);
            tvNumComments = itemView.findViewById(R.id.recycler_view_recipeItem_nrOfComments_textView);

            itemView.setOnClickListener(this);

            imgFavorited.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            mListener.onFavoriteClick(position);
                        }
                    }
                }
            });
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
    }

    public RecipeAdapter(List<Recipe> recipeList) {
        mRecipeList = recipeList;
    }

    @Override
    public RecipeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_recipe_item, parent, false);
        return new RecipeViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecipeViewHolder holder, int position) {
        final Recipe currentItem = mRecipeList.get(position);

        holder.tvTitle.setText(currentItem.getTitle());
        holder.tvDescription.setText(currentItem.getDescription());

        Glide.with(holder.mImageView).load(currentItem.getImageUrls_list().get(0)).centerCrop().into(holder.mImageView);

        if (currentItem.getNrOfLikes() != null) {
            holder.tvNrOfFaves.setText("" + currentItem.getNrOfLikes());
        }
        if (currentItem.getMissingIngredients() != null) {
            if (currentItem.getNrOfMissingIngredients() == 0) {
                holder.tvNumMissingIngredients.setVisibility(View.GONE);
            } else {
                StringBuilder missingIngredients = new StringBuilder("Missing ingredients: ");
                for (int i = 0; i < currentItem.getMissingIngredients().size(); i++) {
                    if (i == currentItem.getMissingIngredients().size() - 1) {
                        missingIngredients.append(currentItem.getMissingIngredients().get(i));
                    } else
                        missingIngredients.append(currentItem.getMissingIngredients().get(i)).append(", ");
                }
                holder.tvNumMissingIngredients.setVisibility(View.VISIBLE);
                holder.tvNumMissingIngredients.setText(missingIngredients.toString());
            }
        } else
            holder.tvNumMissingIngredients.setVisibility(View.GONE);

        if (mRecipeList.get(position).getPrivacy().equals("Everyone")) {
            holder.imgPrivacy.setVisibility(View.GONE);
        }

        holder.imgFavorited.setImageResource(R.drawable.ic_favorite_border_black_24dp);
        if (currentItem.getFavorite() != null && currentItem.getFavorite()) {
            holder.imgFavorited.setImageResource(R.drawable.ic_favorite_red_24dp);
        }
        if (currentItem.getNrOfComments() != null) {
            holder.tvNumComments.setText("" + currentItem.getNrOfComments());
        }

    }

    @Override
    public int getItemCount() {
        return mRecipeList.size();
    }
}