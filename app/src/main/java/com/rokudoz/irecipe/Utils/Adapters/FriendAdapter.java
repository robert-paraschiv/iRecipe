package com.rokudoz.irecipe.Utils.Adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rokudoz.irecipe.Models.Friend;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {
    private static final String TAG = "FriendAdapter";
    private List<Friend> mFriendList;
    private OnItemClickListener mListener;
    TextView friendReqReceivedTv;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public class FriendViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvName;
        CircleImageView mImage;


        public FriendViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.user_name_textView);
            mImage = itemView.findViewById(R.id.userItem_image);
            friendReqReceivedTv = itemView.findViewById(R.id.sentReqTv);

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

    public FriendAdapter(List<Friend> friendList) {
        mFriendList = friendList;
    }

    @Override
    public FriendViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_friend_item, parent, false);
        return new FriendViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final FriendViewHolder holder, int position) {
        final Friend currentItem = mFriendList.get(position);

        Log.d(TAG, "onBindViewHolder: ");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").document(currentItem.getFriend_user_id()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot != null) {
                    User user = documentSnapshot.toObject(User.class);
                    holder.tvName.setText(user.getName());

                    if (user.getUserProfilePicUrl() != null && !user.getUserProfilePicUrl().equals(""))
                        Glide.with(holder.mImage).load(user.getUserProfilePicUrl()).centerCrop().into(holder.mImage);
                }
            }
        });
        if (currentItem.getFriend_status().equals("friend_request_received")) {
            friendReqReceivedTv.setVisibility(View.VISIBLE);
        } else if (currentItem.getFriend_status().equals("friends") || currentItem.getFriend_status().equals("friend_request_accepted")) {
            friendReqReceivedTv.setVisibility(View.GONE);
        }
    }


    @Override
    public int getItemCount() {
        return mFriendList.size();
    }
}