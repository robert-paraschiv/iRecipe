package com.rokudoz.irecipe.Utils.Adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rokudoz.irecipe.Models.Comment;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.TimeAgo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChildCommentAdapter extends RecyclerView.Adapter<ChildCommentAdapter.CommentViewHolder> {

    private Context ctx;
    private ArrayList<Comment> mCommentList;
    private static final String TAG = "ChildCommentAdapter";

    class CommentViewHolder extends RecyclerView.ViewHolder {
        CircleImageView mImageView;
        TextView mName;
        TextView mCommentText;
        TextView mCommentTimeStamp;
        MaterialButton saveCommentBtn, cancelEditBtn, deleteCommentBtn, editCommentBtn;
        TextInputEditText editCommentInput;
        RelativeLayout editCommentLayout;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.comment_rv_profile_img);
            mName = itemView.findViewById(R.id.comment_rv_tv_name);
            mCommentText = itemView.findViewById(R.id.comment_rv_tv_comment_text);
            mCommentTimeStamp = itemView.findViewById(R.id.comment_rv_time_created);

            editCommentBtn = itemView.findViewById(R.id.comment_rv_editBtn);
            editCommentLayout = itemView.findViewById(R.id.comment_rv_editCommentLayout);
            editCommentInput = itemView.findViewById(R.id.comment_rv_comment_editText);
            saveCommentBtn = itemView.findViewById(R.id.comment_rv_comment_save);
            deleteCommentBtn = itemView.findViewById(R.id.comment_rv_comment_delete);
            cancelEditBtn = itemView.findViewById(R.id.comment_rv_comment_cancel);
        }
    }

    public ChildCommentAdapter(Context context, ArrayList<Comment> commentArrayList) {
        ctx = context;
        mCommentList = commentArrayList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_child_comment, parent, false);
        CommentViewHolder cvh = new CommentViewHolder(v);
        return cvh;
    }

    @Override
    public void onBindViewHolder(@NonNull final CommentViewHolder holder, int position) {
        final Comment currentItem = mCommentList.get(position);
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (currentItem.getUser_name() != null)
            holder.mName.setText(currentItem.getUser_name());
        if (currentItem.getUser_profilePic() != null && !currentItem.getUser_profilePic().equals(""))
            Glide.with(holder.mImageView).load(currentItem.getUser_profilePic()).centerCrop().into(holder.mImageView);

        holder.mCommentText.setText(currentItem.getComment_text());

        Date date = currentItem.getComment_timeStamp();
        if (date != null) {
            DateFormat dateFormat = new SimpleDateFormat("HH:mm, MMM d", Locale.getDefault());
            String creationDate = dateFormat.format(date);
            long time = date.getTime();
            if (currentItem.getComment_timeStamp() != null && !currentItem.getComment_timeStamp().equals("")) {
                TimeAgo timeAgo = new TimeAgo();
                holder.mCommentTimeStamp.setText(timeAgo.getTimeAgo(time));
            }
        }
        holder.mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putString("documentID", currentItem.getUser_id());
                Navigation.findNavController(v).navigate(R.id.userProfileFragment2, args);
            }
        });


        // EDIT COMMENT
        if (currentItem.getComment_for_type() == null || !currentItem.getUser_id().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            holder.editCommentBtn.setVisibility(View.GONE);
        } else {
            holder.editCommentBtn.setVisibility(View.VISIBLE);
            holder.editCommentBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.mCommentText.setVisibility(View.INVISIBLE);
                    holder.mCommentTimeStamp.setVisibility(View.INVISIBLE);
                    holder.editCommentLayout.setVisibility(View.VISIBLE);
                    holder.editCommentBtn.setVisibility(View.GONE);
                    holder.editCommentInput.setText(currentItem.getComment_text());

                    DocumentReference commentRef = null;
                    if (currentItem.getComment_for_type().equals("Recipe")) {
                        commentRef = db.collection("Recipes").document(currentItem.getRecipe_documentID())
                                .collection("Comments").document(currentItem.getParent_comment_ID())
                                .collection("ChildComments").document(currentItem.getDocumentID());
                    } else if (currentItem.getComment_for_type().equals("Post")) {
                        commentRef = db.collection("Posts").document(currentItem.getRecipe_documentID())
                                .collection("Comments").document(currentItem.getParent_comment_ID())
                                .collection("ChildComments").document(currentItem.getDocumentID());
                    }

                    final DocumentReference finalCommentRef = commentRef;
                    holder.saveCommentBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            currentItem.setComment_text(holder.editCommentInput.getText().toString());

                            if (finalCommentRef != null) {
                                finalCommentRef.set(currentItem).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "onSuccess: SAVED COMMENT");
                                        holder.mCommentText.setText(currentItem.getComment_text());
                                        holder.mCommentText.setVisibility(View.VISIBLE);
                                        holder.mCommentTimeStamp.setVisibility(View.VISIBLE);
                                        holder.editCommentBtn.setVisibility(View.VISIBLE);
                                        holder.editCommentLayout.setVisibility(View.GONE);

                                    }
                                });
                            }
                        }
                    });
                    holder.cancelEditBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d(TAG, "onClick: CANCELLED Editting");
                            holder.mCommentText.setVisibility(View.VISIBLE);
                            holder.mCommentTimeStamp.setVisibility(View.VISIBLE);
                            holder.editCommentBtn.setVisibility(View.VISIBLE);
                            holder.editCommentLayout.setVisibility(View.GONE);
                        }
                    });
                    holder.deleteCommentBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(ctx
                                    , R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered);
                            materialAlertDialogBuilder.setMessage("Are you sure you want to delete this comment?");
                            materialAlertDialogBuilder.setCancelable(true);
                            materialAlertDialogBuilder.setPositiveButton(
                                    "Yes",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            //Delete comment
                                            mCommentList.remove(currentItem);
                                            holder.mCommentText.setVisibility(View.VISIBLE);
                                            holder.mCommentTimeStamp.setVisibility(View.VISIBLE);
                                            holder.editCommentLayout.setVisibility(View.GONE);
                                            if (finalCommentRef != null)
                                                finalCommentRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Log.d(TAG, "onSuccess: DELETED COMM");
                                                    }
                                                });
                                            dialog.cancel();
                                        }
                                    });

                            materialAlertDialogBuilder.setNegativeButton(
                                    "No",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });

                            materialAlertDialogBuilder.show();
                        }
                    });
                }
            });
        }

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mCommentList.size();
    }
}