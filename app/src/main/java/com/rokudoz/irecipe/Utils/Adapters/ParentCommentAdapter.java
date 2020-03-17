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
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.Comment;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.annotation.Nullable;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import de.hdodenhof.circleimageview.CircleImageView;

public class ParentCommentAdapter extends RecyclerView.Adapter<ParentCommentAdapter.CommentViewHolder> implements View.OnClickListener {

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;


    private static final String TAG = "RecipeParentCommentAdapter";
    private Integer expandedPosition = -1;
    private ArrayList<Comment> mCommentList;
    private ChildCommentAdapter childAdapter;
    private User mUser;
    private Context ctx;

    class CommentViewHolder extends RecyclerView.ViewHolder {
        CircleImageView mImageView;
        TextView mName;
        TextView mCommentText;
        TextView mCommentTimeStamp;
        MaterialButton mAddReplyBtn, editCommentBtn, saveCommentBtn, cancelEditBtn, deleteCommentBtn;
        TextInputEditText mReplyText, editCommentInput;
        RelativeLayout llExpandArea, editCommentLayout;
        RecyclerView rv_child;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.comment_rv_profile_img);
            mName = itemView.findViewById(R.id.comment_rv_tv_name);
            mCommentText = itemView.findViewById(R.id.comment_rv_tv_comment_text);
            mCommentTimeStamp = itemView.findViewById(R.id.comment_rv_time_created);
            llExpandArea = itemView.findViewById(R.id.llExpandArea);
            rv_child = itemView.findViewById(R.id.comment_rv_childRecyclerView);
            mAddReplyBtn = itemView.findViewById(R.id.comment_rv_addReply_btn);
            editCommentBtn = itemView.findViewById(R.id.comment_rv_editBtn);
            mReplyText = itemView.findViewById(R.id.comment_rv_reply_editText);

            editCommentLayout = itemView.findViewById(R.id.comment_rv_editCommentLayout);
            editCommentInput = itemView.findViewById(R.id.comment_rv_comment_editText);
            saveCommentBtn = itemView.findViewById(R.id.comment_rv_comment_save);
            deleteCommentBtn = itemView.findViewById(R.id.comment_rv_comment_delete);
            cancelEditBtn = itemView.findViewById(R.id.comment_rv_comment_cancel);

        }
    }

    public ParentCommentAdapter(Context ctx, ArrayList<Comment> commentArrayList) {
        this.ctx = ctx;
        mCommentList = commentArrayList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_parent_comment, parent, false);
        CommentViewHolder cvh = new CommentViewHolder(v);
        mUser = new User();
        getCurrentUserDetails();
        cvh.itemView.setOnClickListener(ParentCommentAdapter.this);
        cvh.itemView.setTag(cvh);
        return cvh;
    }

    @Override
    public void onClick(View v) {
        CommentViewHolder holder = (CommentViewHolder) v.getTag();
        String commentID = mCommentList.get(holder.getAdapterPosition()).getComment_text();

        if (holder.llExpandArea.getVisibility() == View.VISIBLE) {
            holder.llExpandArea.setVisibility(View.GONE);

//            notifyItemChanged(holder.getAdapterPosition());
        } else {
            holder.llExpandArea.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "onClick: " + commentID + holder.llExpandArea.getVisibility());
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
                holder.mCommentTimeStamp.setText(getTimeAgo(time));
            }
        }

        holder.mAddReplyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String replyText = holder.mReplyText.getText().toString();
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                if (!replyText.trim().equals("")) {
                    Comment comment = new Comment(currentItem.getRecipe_documentID(), mUser.getUser_id(), mUser.getName(), mUser.getUserProfilePicUrl(), replyText
                            , currentItem.getComment_for_type(), currentItem.getDocumentID(), null);
                    db.collection("Posts").document(currentItem.getRecipe_documentID()).collection("Comments")
                            .document(currentItem.getDocumentID()).collection("ChildComments").add(comment)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Toast.makeText(ctx, "Successfully Added Reply", Toast.LENGTH_SHORT).show();
                                    holder.mReplyText.setText("");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(ctx, "Failed to add Reply", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else
                    Toast.makeText(ctx, "Comment can't be empty", Toast.LENGTH_SHORT).show();

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
                                .collection("Comments").document(currentItem.getDocumentID());
                    } else if (currentItem.getComment_for_type().equals("Post")) {
                        commentRef = db.collection("Posts").document(currentItem.getRecipe_documentID())
                                .collection("Comments").document(currentItem.getDocumentID());
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

        //Initialize Child Comment RecyclerView
        initchildLayout(holder.rv_child, getChildComments(currentItem));
    }

    private void getCurrentUserDetails() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                mUser = documentSnapshot.toObject(User.class);
            }
        });

    }

    private ArrayList<Comment> getChildComments(final Comment comment) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final ArrayList<Comment> childComments = new ArrayList<>();
        db.collection("Posts").document(comment.getRecipe_documentID()).collection("Comments")
                .document(comment.getDocumentID()).collection("ChildComments").orderBy("comment_timeStamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e == null && queryDocumentSnapshots != null) {
                            for (DocumentSnapshot document : queryDocumentSnapshots) {
                                Comment commentToAdd = document.toObject(Comment.class);
                                if (commentToAdd != null) {
                                    commentToAdd.setDocumentID(document.getId());
                                    commentToAdd.setParent_comment_ID(comment.getDocumentID());

                                    if (!childComments.contains(commentToAdd)) {
                                        childComments.add(0, commentToAdd);
                                        childAdapter.notifyItemInserted(0);
                                    } else {
                                        childComments.set(childComments.indexOf(commentToAdd), commentToAdd);
                                        childAdapter.notifyItemChanged(childComments.indexOf(commentToAdd));
                                    }
                                }
                            }
//                        childAdapter.notifyDataSetChanged();
                        }
                    }
                });
//        childAdapter.notifyDataSetChanged();
        return childComments;

    }


    private void initchildLayout(RecyclerView rv_child, ArrayList<Comment> childData) {
        rv_child.setLayoutManager(new LinearLayoutManager(ctx));
        childAdapter = new ChildCommentAdapter(ctx, childData);
        rv_child.setAdapter(childAdapter);
        rv_child.setHasFixedSize(true);
    }


    public static String getTimeAgo(long time) {
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = System.currentTimeMillis();
        if (time > now || time <= 0) {
            return null;
        }

        // TODO: localize
        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return "just now";
        } else if (diff < 2 * MINUTE_MILLIS) {
            return "a minute ago";
        } else if (diff < 50 * MINUTE_MILLIS) {
            return diff / MINUTE_MILLIS + " minutes ago";
        } else if (diff < 90 * MINUTE_MILLIS) {
            return "an hour ago";
        } else if (diff < 24 * HOUR_MILLIS) {
            return diff / HOUR_MILLIS + " hours ago";
        } else if (diff < 48 * HOUR_MILLIS) {
            return "yesterday";
        } else {
            return diff / DAY_MILLIS + " days ago";
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
