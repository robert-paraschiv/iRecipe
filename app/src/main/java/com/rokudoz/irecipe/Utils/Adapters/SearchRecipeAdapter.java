package com.rokudoz.irecipe.Utils.Adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;


public class SearchRecipeAdapter extends RecyclerView.Adapter<SearchRecipeAdapter.SearchRecipeViewHolder> implements Filterable {
    private static final String TAG = "SearchRecipeAdapter";
    private List<Recipe> mRecipeList;
    private List<Recipe> mRecipeListFull;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);

        void onFavoriteClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public class SearchRecipeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvTitle, tvDescription, tvNrOfFaves, tvCreatorName;
        ImageView mImageView, imgFavorited, privacy;

        public SearchRecipeViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.text_view_title);
            tvDescription = itemView.findViewById(R.id.text_view_description);
            mImageView = itemView.findViewById(R.id.recipeItem_image);
            imgFavorited = itemView.findViewById(R.id.recyclerview_favorite);
            tvNrOfFaves = itemView.findViewById(R.id.recyclerview_nrOfFaves_textView);
            tvCreatorName = itemView.findViewById(R.id.recipeItem_creator_name_textView);
            privacy = itemView.findViewById(R.id.recycler_view_privacy);

            itemView.setOnClickListener(this);
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

    public SearchRecipeAdapter(List<Recipe> recipeList) {
        this.mRecipeList = recipeList;
        mRecipeListFull = new ArrayList<>(recipeList);
    }

    @Override
    public SearchRecipeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_recipe_item, parent, false);
        return new SearchRecipeViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final SearchRecipeViewHolder holder, int position) {
        final Recipe currentItem = mRecipeList.get(position);

        holder.privacy.setVisibility(View.GONE);
        if (currentItem.getTitle() != null)
            holder.tvTitle.setText(currentItem.getTitle());
        if (currentItem.getDescription() != null)
            holder.tvDescription.setText(currentItem.getDescription());
        if (currentItem.getCreator_name() != null)
            holder.tvCreatorName.setText(currentItem.getCreator_name());
        if (currentItem.getNumber_of_likes() != null)
            holder.tvNrOfFaves.setText(MessageFormat.format("{0}", currentItem.getNumber_of_likes()));
        if (currentItem.getImageUrls_list() != null)
            Glide.with(holder.mImageView).load(currentItem.getImageUrls_list().get(0)).centerCrop().into(holder.mImageView);

    }


    @Override
    public int getItemCount() {
        return mRecipeList.size();
    }

    @Override
    public Filter getFilter() {
        return recipeFilter;
    }

    private Filter recipeFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Recipe> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(mRecipeListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (Recipe recipe : mRecipeListFull) {
                    if (recipe.getTitle().toLowerCase().contains(filterPattern)) {
                        filteredList.add(recipe);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mRecipeList.clear();
            mRecipeList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };
}
