package com.rokudoz.irecipe.Utils.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rokudoz.irecipe.Models.ScheduledMeal;
import com.rokudoz.irecipe.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ScheduledMealAdapter extends RecyclerView.Adapter<ScheduledMealAdapter.ScheduledMealViewHolder> {
    private static final String TAG = "ScheduledMealAdapter";
    private List<ScheduledMeal> scheduledMealList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public class ScheduledMealViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView recipe_image;
        TextView recipe_category, meal_time, recipe_title;

        public ScheduledMealViewHolder(@NonNull View itemView) {
            super(itemView);

            recipe_image = itemView.findViewById(R.id.recycler_view_scheduled_meal_image);
            recipe_category = itemView.findViewById(R.id.recycler_view_scheduled_meal_category);
            recipe_title = itemView.findViewById(R.id.recycler_view_scheduled_meal_title);
            meal_time = itemView.findViewById(R.id.recycler_view_scheduled_meal_time);

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

    public ScheduledMealAdapter(List<ScheduledMeal> mealList) {
        scheduledMealList = mealList;
    }

    @NonNull
    @Override
    public ScheduledMealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_scheduled_meal_item, parent, false);
        return new ScheduledMealViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduledMealViewHolder holder, int position) {
        final ScheduledMeal currentItem = scheduledMealList.get(position);
        if (currentItem != null) {
            if (currentItem.getMealPicture() != null) {
                Glide.with(holder.recipe_image).load(currentItem.getMealPicture()).centerCrop().into(holder.recipe_image);
            }
            if (currentItem.getMealTitle() != null) {
                holder.recipe_title.setText(currentItem.getMealTitle());
            }
            if (currentItem.getMealType() != null) {
                holder.recipe_category.setText("Planned for " + currentItem.getMealType());
            }
            if (currentItem.getDate() != null) {
                final DateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String dateString = dateFormat.format(currentItem.getDate());
                holder.meal_time.setText("At " + dateString);
            }
        }
    }

    @Override
    public int getItemCount() {
        return scheduledMealList.size();
    }

}
