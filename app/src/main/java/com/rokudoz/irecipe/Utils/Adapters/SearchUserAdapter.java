package com.rokudoz.irecipe.Utils.Adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.SearchUserViewHolder> implements Filterable {
    private static final String TAG = "FriendAdapter";
    private List<User> mUserList;
    private List<User> mUserListFull;
    private OnItemClickListener mListener;
    TextView friendReqReceivedTv;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public class SearchUserViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvName;
        CircleImageView mImage;


        public SearchUserViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.searchUserItem_user_name_textView);
            mImage = itemView.findViewById(R.id.searchUserItem_image);
            friendReqReceivedTv = itemView.findViewById(R.id.searchUserItem_sentReqTv);

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

    public SearchUserAdapter(List<User> userList) {
        this.mUserList = userList;
        mUserListFull = new ArrayList<>(userList);
    }

    @Override
    public SearchUserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_search_user_item, parent, false);
        return new SearchUserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final SearchUserViewHolder holder, int position) {
        final User currentItem = mUserList.get(position);

        Log.d(TAG, "onBindViewHolder: ");

        if (currentItem.getName() != null)
            holder.tvName.setText(currentItem.getName());
        if (currentItem.getUserProfilePicUrl() != null && !currentItem.getUserProfilePicUrl().equals(""))
            Glide.with(holder.mImage).load(currentItem.getUserProfilePicUrl()).centerCrop().into(holder.mImage);

//        if (currentItem.getFriend_status().equals("friend_request_received")) {
//            friendReqReceivedTv.setVisibility(View.VISIBLE);
//        } else if (currentItem.getFriend_status().equals("friends") || currentItem.getFriend_status().equals("friend_request_accepted")) {
//            friendReqReceivedTv.setVisibility(View.GONE);
//        }
    }


    @Override
    public int getItemCount() {
        return mUserList.size();
    }


    @Override
    public Filter getFilter() {
        return postFilter;
    }

    private Filter postFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<User> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(mUserListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (User user : mUserListFull) {
                    if (user.getName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(user);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mUserList.clear();
            mUserList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };
}