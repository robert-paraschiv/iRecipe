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

    public class RecipeInstructionViewHolder extends RecyclerView.ViewHolder {
        TextView tvStepNumber, tvStepText;
        ImageView stepImage;

        public RecipeInstructionViewHolder(View itemView) {
            super(itemView);
            tvStepNumber = itemView.findViewById(R.id.step_item_StepNumber_TextView);
            tvStepText = itemView.findViewById(R.id.step_item_StepText_TextView);
            stepImage = itemView.findViewById(R.id.stepItem_image);
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

        holder.tvStepNumber.setText("Step " + currentItem.getStepNumber());
        holder.tvStepText.setText(currentItem.getText());
        if (!currentItem.getImgUrl().equals("")) {
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