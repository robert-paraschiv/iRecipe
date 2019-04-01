package com.rokudoz.irecipe.Utils;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
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
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.annotation.Nullable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

public class ParentCommentAdapter
        extends RecyclerView.Adapter<ParentCommentAdapter.CommentViewHolder>
        implements View.OnClickListener {

    private static final String TAG = "ParentCommentAdapter";
    private ArrayList<Comment> mCommentList;
    private ChildCommentAdapter childAdapter;
    private User mUser;
    Context ctx;

    public class CommentViewHolder extends RecyclerView.ViewHolder {
        public CircleImageView mImageView;
        public TextView mName;
        public TextView mCommentText;
        public TextView mCommentTimeStamp;
        public MaterialButton mAddReplyBtn;
        public TextInputEditText mReplyText;
        RelativeLayout llExpandArea;
        RecyclerView rv_child;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.comment_rv_profile_img);
            mName = itemView.findViewById(R.id.comment_rv_tv_name);
            mCommentText = itemView.findViewById(R.id.comment_rv_tv_comment_text);
            mCommentTimeStamp = itemView.findViewById(R.id.comment_rv_time_created);
            llExpandArea = itemView.findViewById(R.id.llExpandArea);
            rv_child = itemView.findViewById(R.id.comment_rv_childRecyclerView);
            mAddReplyBtn = itemView.findViewById(R.id.comment_rv_addReply_btn);
            mReplyText = itemView.findViewById(R.id.comment_rv_reply_editText);
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
        String commentID = mCommentList.get(holder.getAdapterPosition()).getmCommentText();

        if (holder.llExpandArea.getVisibility() == View.VISIBLE) {
            holder.llExpandArea.setVisibility(View.GONE);
        } else {
            holder.llExpandArea.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "onClick: " + commentID + holder.llExpandArea.getVisibility());
    }

    @Override
    public void onBindViewHolder(@NonNull final CommentViewHolder holder, int position) {
        final Comment currentItem = mCommentList.get(position);

        if (!currentItem.getmImageUrl().equals("")) {
            Picasso.get()
                    .load(currentItem.getmImageUrl())
                    .fit()
                    .centerCrop()
                    .into(holder.mImageView);
        }

        holder.mName.setText(currentItem.getmName());
        holder.mCommentText.setText(currentItem.getmCommentText());

        Date date = currentItem.getmCommentTimeStamp();
        if (date != null) {
            DateFormat dateFormat = new SimpleDateFormat("HH:mm, MMM d");
            String creationDate = dateFormat.format(date);
            if (currentItem.getmCommentTimeStamp() != null && !currentItem.getmCommentTimeStamp().equals("")) {
                holder.mCommentTimeStamp.setText(creationDate);
            }
        }


        holder.mAddReplyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String replyText = holder.mReplyText.getText().toString();
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                if (!replyText.trim().equals("")) {
                    Comment comment = new Comment(currentItem.getmRecipeDocumentId(), mUser.getUser_id(), mUser.getUserProfilePicUrl()
                            , mUser.getName(), replyText, null);
                    db.collection("Recipes").document(currentItem.getmRecipeDocumentId()).collection("Comments")
                            .document(currentItem.getDocumentId()).collection("ChildComments").add(comment)
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
                }

            }
        });

        //Initialize Child Comment RecyclerView
        initChildLayout(holder.rv_child, getChildComments(currentItem));
    }

    private void getCurrentUserDetails() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").whereEqualTo("user_id", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "onEvent: ", e);
                            return;
                        }
                        for (DocumentChange documentSnapshot : queryDocumentSnapshots.getDocumentChanges()) {
                            mUser = documentSnapshot.getDocument().toObject(User.class);
                        }
                    }
                });
    }

    private ArrayList<Comment> getChildComments(Comment comment) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final ArrayList<Comment> childComments = new ArrayList<>();
        final ArrayList<String> childCommentID = new ArrayList<>();
        db.collection("Recipes").document(comment.getmRecipeDocumentId()).collection("Comments")
                .document(comment.getDocumentId()).collection("ChildComments").orderBy("mCommentTimeStamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    Comment commentToAdd = document.toObject(Comment.class);
                    if (!childCommentID.contains(document.getId())) {
                        childCommentID.add(document.getId());
                        childComments.add(0, commentToAdd);
                    }

                }
                childAdapter.notifyDataSetChanged();
            }
        });
//        childAdapter.notifyDataSetChanged();
        return childComments;

    }


    private void initChildLayout(RecyclerView rv_child, ArrayList<Comment> childData) {
        rv_child.setLayoutManager(new LinearLayoutManager(ctx));
        childAdapter = new ChildCommentAdapter(childData);
        rv_child.setAdapter(childAdapter);
        rv_child.setHasFixedSize(true);
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
