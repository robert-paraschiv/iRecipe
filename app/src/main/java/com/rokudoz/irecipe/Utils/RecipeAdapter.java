package com.rokudoz.irecipe.Utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;


public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {
    private ArrayList<Recipe> mRecipeList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);

        void onFavoriteClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public class RecipeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView tvTitle, tvDescription, tvNumberofFaved;
        ImageView mImageView, imgFavorited;
        Map<String, Boolean> ingredientTags;

        public RecipeViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.text_view_title);
            tvDescription = itemView.findViewById(R.id.text_view_description);
            mImageView = itemView.findViewById(R.id.recipeItem_image);
            imgFavorited = itemView.findViewById(R.id.recyclerview_favorite);
            tvNumberofFaved = itemView.findViewById(R.id.recyclerview_numberOfFaved);

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

    public RecipeAdapter(ArrayList<Recipe> recipeList) {
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
        Picasso.get()
                .load(currentItem.getImageUrl().get(0))
                .fit()
                .centerCrop()
                .into(holder.mImageView);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference currentRecipeSubCollection = db.collection("Recipes").document(currentItem.getDocumentId()).collection("UsersWhoFaved");

        currentRecipeSubCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    return;
                }
                currentItem.setNumberOfFaves(queryDocumentSnapshots.size());
                if (currentItem.getNumberOfFaves() != null) {
                    holder.tvNumberofFaved.setText(Integer.toString(currentItem.getNumberOfFaves()));
                }
            }
        });


        holder.imgFavorited.setImageResource(R.drawable.ic_favorite_border_black_24dp);
        if (currentItem.getFavorite() != null && currentItem.getFavorite()) {
            holder.imgFavorited.setImageResource(R.drawable.ic_favorite_red_24dp);
        }

    }


    @Override
    public int getItemCount() {
        return mRecipeList.size();
    }
}