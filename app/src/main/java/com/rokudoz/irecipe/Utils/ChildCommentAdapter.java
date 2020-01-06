package com.rokudoz.irecipe.Utils;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.rokudoz.irecipe.Models.Comment;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChildCommentAdapter extends RecyclerView.Adapter<ChildCommentAdapter.CommentViewHolder> {

    private ArrayList<Comment> mCommentList;
    private static final String TAG = "ChildCommentAdapter";

    public class CommentViewHolder extends RecyclerView.ViewHolder {
        public CircleImageView mImageView;
        public TextView mName;
        public TextView mCommentText;
        public TextView mCommentTimeStamp;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.comment_rv_profile_img);
            mName = itemView.findViewById(R.id.comment_rv_tv_name);
            mCommentText = itemView.findViewById(R.id.comment_rv_tv_comment_text);
            mCommentTimeStamp = itemView.findViewById(R.id.comment_rv_time_created);
        }
    }

    public ChildCommentAdapter(ArrayList<Comment> commentArrayList) {
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
        Comment currentItem = mCommentList.get(position);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").whereEqualTo("user_id", currentItem.getmUserId()).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@androidx.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @androidx.annotation.Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "onEvent: ", e);
                    return;
                }
                User user = Objects.requireNonNull(queryDocumentSnapshots).getDocuments().get(0).toObject(User.class);
                if (user.getUserProfilePicUrl() != null) {
                    Picasso.get()
                            .load(user.getUserProfilePicUrl())
                            .fit()
                            .centerCrop()
                            .into(holder.mImageView);
                    holder.mName.setText(user.getName());
                }

            }
        });
        holder.mCommentText.setText(currentItem.getmCommentText());

        Date date = currentItem.getmCommentTimeStamp();
        if (date != null) {
            DateFormat dateFormat = new SimpleDateFormat("HH:mm, MMM d", Locale.getDefault());
            String creationDate = dateFormat.format(date);
            if (currentItem.getmCommentTimeStamp() != null && !currentItem.getmCommentTimeStamp().equals("")) {
                holder.mCommentTimeStamp.setText(creationDate);
            }
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