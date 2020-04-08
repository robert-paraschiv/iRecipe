package com.rokudoz.irecipe.Utils.Adapters.EditRecipe;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.rokudoz.irecipe.Models.Instruction;
import com.rokudoz.irecipe.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class EditRecipeInstructionsAdapter extends RecyclerView.Adapter<EditRecipeInstructionsAdapter.EditRecipeInstructionViewHolder> {
    private List<Instruction> instructionList = new ArrayList<>();
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onAddImageClick(int position);

        void onRemoveStepClick(int position);
    }

    public List<Instruction> getInstructionList() {
        return instructionList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public class EditRecipeInstructionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        EditText stepEditText;
        ImageView stepImage;
        MaterialButton addPhotoBtn, removeStepBtn;

        public MyCustomEditTextListener myCustomEditTextListener;

        EditRecipeInstructionViewHolder(View itemView, MyCustomEditTextListener myCustomEditTextListener) {
            super(itemView);
            stepEditText = itemView.findViewById(R.id.rv_edit_recipe_instruction_editText);
            stepImage = itemView.findViewById(R.id.rv_edit_recipe_instruction_imageView);
            addPhotoBtn = itemView.findViewById(R.id.rv_edit_recipe_instruction_addPhoto_btn);
            removeStepBtn = itemView.findViewById(R.id.rv_edit_recipe_instruction_removeStep_btn);

            this.myCustomEditTextListener = myCustomEditTextListener;
            stepEditText.addTextChangedListener(myCustomEditTextListener);

            addPhotoBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            mListener.onAddImageClick(position);
                        }
                    }
                }
            });
            removeStepBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            mListener.onRemoveStepClick(position);
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
                    mListener.onAddImageClick(position);
                }
            }
        }
    }

    public EditRecipeInstructionsAdapter(List<Instruction> instructionList) {
        this.instructionList = instructionList;
    }

    @Override
    public EditRecipeInstructionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_edit_recipe_instruction_layout_item, parent, false);
        return new EditRecipeInstructionViewHolder(v, new MyCustomEditTextListener());
    }

    @Override
    public void onBindViewHolder(@NonNull final EditRecipeInstructionViewHolder holder, final int position) {
        final Instruction currentItem = instructionList.get(position);

        holder.myCustomEditTextListener.updatePosition(holder.getAdapterPosition());

        if (currentItem.getText() != null)
            holder.stepEditText.setText(currentItem.getText());
        if (currentItem.getImgUrl() != null && !currentItem.getImgUrl().equals("")) {
            holder.stepImage.setVisibility(View.VISIBLE);
            Glide.with(holder.stepImage).load(currentItem.getImgUrl()).centerCrop().into(holder.stepImage);
            holder.addPhotoBtn.setText("Change Photo");
            holder.addPhotoBtn.setIconResource(R.drawable.ic_add_a_photo_black_24dp);
        } else {
            holder.addPhotoBtn.setText("Photo");
            holder.addPhotoBtn.setIconResource(R.drawable.ic_fb_plus);
            holder.stepImage.setVisibility(View.GONE);
        }

    }

    // we make TextWatcher to be aware of the position it currently works with
    // this way, once a new item is attached in onBindViewHolder, it will
    // update current position MyCustomEditTextListener, reference to which is kept by ViewHolder
    private class MyCustomEditTextListener implements TextWatcher {
        private int position;

        public void updatePosition(int position) {
            this.position = position;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            // no op
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            instructionList.get(position).setText(charSequence.toString());
        }

        @Override
        public void afterTextChanged(Editable editable) {
            // no op
        }
    }

    @Override
    public int getItemCount() {
        return instructionList.size();
    }
}
