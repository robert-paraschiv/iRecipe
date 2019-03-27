package com.rokudoz.irecipe.Utils;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.rokudoz.irecipe.Models.Comment;
import com.rokudoz.irecipe.R;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChildCommentAdapter extends RecyclerView.Adapter<ChildCommentAdapter.CommentViewHolder> {

    private ArrayList<Comment> mCommentList;

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
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment currentItem = mCommentList.get(position);

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
            Log.d("TAG", creationDate);
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