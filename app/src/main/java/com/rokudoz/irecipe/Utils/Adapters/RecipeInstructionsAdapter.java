package com.rokudoz.irecipe.Utils.Adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.rokudoz.irecipe.Models.Instruction;
import com.rokudoz.irecipe.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class RecipeInstructionsAdapter extends RecyclerView.Adapter<RecipeInstructionsAdapter.RecipeInstructionViewHolder> {
    private List<Instruction> instructionList = new ArrayList<>();
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemImageClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public class RecipeInstructionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvStepText;
        ImageView stepImage, spacer;

        public RecipeInstructionViewHolder(View itemView) {
            super(itemView);
            tvStepText = itemView.findViewById(R.id.step_item_StepText_TextView);
            stepImage = itemView.findViewById(R.id.stepItem_image);
            spacer = itemView.findViewById(R.id.rv_layout_instruction_spacer);

            stepImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            mListener.onItemImageClick(position);
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
                    mListener.onItemImageClick(position);
                }
            }
        }
    }

    public RecipeInstructionsAdapter(List<Instruction> instructionList) {
        this.instructionList = instructionList;
    }

    @Override
    public RecipeInstructionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_instruction_item, parent, false);
        return new RecipeInstructionViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final RecipeInstructionViewHolder holder, int position) {
        final Instruction currentItem = instructionList.get(position);

        if (position == 0) {
            holder.spacer.setVisibility(View.GONE);
        } else {
            holder.spacer.setVisibility(View.VISIBLE);
        }

        if (currentItem.getText() != null)
            holder.tvStepText.setText(currentItem.getText());

        if (currentItem.getImgUrl() != null && !currentItem.getImgUrl().equals("")) {
            Glide.with(holder.stepImage).load(currentItem.getImgUrl()).centerCrop().into(holder.stepImage);
        } else {
            holder.stepImage.setVisibility(View.GONE);
        }

    }

    @Override
    public int getItemCount() {
        return instructionList.size();
    }
}