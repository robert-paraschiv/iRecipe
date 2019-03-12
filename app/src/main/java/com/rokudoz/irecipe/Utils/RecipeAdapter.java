package com.rokudoz.irecipe.Utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.rokudoz.irecipe.Models.Recipe;
import com.rokudoz.irecipe.R;

import java.util.ArrayList;
import java.util.Map;

public class RecipeAdapter extends RecyclerView.Adapter< RecipeAdapter.ViewHolder> {

    private static final String TAG = "RecipeAdapter";

    private OnItemClickListener listener;
    private ArrayList<Recipe> mRecipes = new ArrayList<>();
    private Context mContext;
    private int mSelectedNoteIndex;

    public RecipeAdapter(Context context, ArrayList<Recipe> recipes) {
        mRecipes = recipes;
        mContext = context;
    }




    @Override
    public int getItemCount() {
        return mRecipes.size();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder;
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.recipe_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        viewHolder.tvTitle.setText(mRecipes.get(position).getTitle());
        viewHolder.tvDescription.setText(mRecipes.get(position).getDescription());
        viewHolder.tvDocumentId.setText(mRecipes.get(position).getDocumentId());
    }


    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvDocumentId;
        Map<String, Boolean> ingredientTags;

        public ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.text_view_title);
            tvDescription = itemView.findViewById(R.id.text_view_description);
            tvDocumentId = itemView.findViewById(R.id.text_view_id);

        }
    }

    public interface OnItemClickListener{
        void onItemClick(DocumentSnapshot documentSnapshot, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        this.listener = listener;
    }
}
