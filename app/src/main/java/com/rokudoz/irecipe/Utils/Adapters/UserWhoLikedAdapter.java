package com.rokudoz.irecipe.Utils.Adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserWhoFaved;
import com.rokudoz.irecipe.R;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class UserWhoLikedAdapter extends RecyclerView.Adapter<UserWhoLikedAdapter.UserWhoLikedViewHolder> {
    private static final String TAG = "UsersWhoLikedAdapter";
    private List<UserWhoFaved> mUserList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public class UserWhoLikedViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvName;
        CircleImageView mImage;


        public UserWhoLikedViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.userWhoLikedItem_user_name_textView);
            mImage = itemView.findViewById(R.id.userWhoLikedItem_image);

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

    public UserWhoLikedAdapter(List<UserWhoFaved> userList) {
        this.mUserList = userList;
    }

    @Override
    public UserWhoLikedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_userwholiked_item, parent, false);
        return new UserWhoLikedViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final UserWhoLikedViewHolder holder, int position) {
        final UserWhoFaved currentItem = mUserList.get(position);

        Log.d(TAG, "onBindViewHolder: ");

        if (currentItem.getUser_name() != null)
            holder.tvName.setText(currentItem.getUser_name());
        if (currentItem.getUser_imageUrl() != null && !currentItem.getUser_imageUrl().equals(""))
            Glide.with(holder.mImage).load(currentItem.getUser_imageUrl()).centerCrop().into(holder.mImage);
    }

    @Override
    public int getItemCount() {
        return mUserList.size();
    }

}