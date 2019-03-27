package com.rokudoz.irecipe.Utils;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rokudoz.irecipe.Models.Comment;
import com.rokudoz.irecipe.R;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

public class ParentCommentAdapter
        extends RecyclerView.Adapter<ParentCommentAdapter.CommentViewHolder>
        implements View.OnClickListener {

    private static final String TAG = "ParentCommentAdapter";
    private Integer expandedPosition = -1;
    private ArrayList<Comment> mCommentList;
    Context ctx;

    public class CommentViewHolder extends RecyclerView.ViewHolder {
        public CircleImageView mImageView;
        public TextView mName;
        public TextView mCommentText;
        public TextView mCommentTimeStamp;
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

        cvh.itemView.setOnClickListener(ParentCommentAdapter.this);
        cvh.itemView.setTag(cvh);
        return cvh;
    }

    @Override
    public void onClick(View v) {
        CommentViewHolder holder = (CommentViewHolder) v.getTag();
        String commentID = mCommentList.get(holder.getPosition()).getDocumentId();

        // Check for an expanded view, collapse if you find one
        if (expandedPosition >= 0) {
            int prev = expandedPosition;
            notifyItemChanged(prev);
        }
        // Set the current position to "expanded"
        expandedPosition = holder.getPosition();
        notifyItemChanged(expandedPosition);

        Log.d(TAG, "onClick: expanded " + commentID);
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

        if (position == expandedPosition) {
            holder.llExpandArea.setVisibility(View.VISIBLE);
        } else {
            holder.llExpandArea.setVisibility(View.GONE);
        }

        CommentViewHolder cvh = (CommentViewHolder) holder;

        initchildLayout(cvh.rv_child, mCommentList);
    }

    private void initchildLayout(RecyclerView rv_child, ArrayList<Comment> childData) {
        rv_child.setLayoutManager(new NestedRecyclerLinearLayoutManager(ctx));
        ChildCommentAdapter childAdapter = new ChildCommentAdapter(childData);
        rv_child.setAdapter(childAdapter);
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
