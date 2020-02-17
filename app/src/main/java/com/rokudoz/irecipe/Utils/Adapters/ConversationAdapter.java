package com.rokudoz.irecipe.Utils.Adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.rokudoz.irecipe.Models.Conversation;
import com.rokudoz.irecipe.Models.Friend;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.TimeAgo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;


public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {
    private List<Conversation> conversationList;
    private OnItemClickListener mListener;


    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public class ConversationViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvName, tvMessage, tvTimeStamp;
        CircleImageView mImage;


        public ConversationViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.recycler_view_conversationItem_friendName);
            mImage = itemView.findViewById(R.id.recycler_view_conversationItem_friendImage);
            tvMessage = itemView.findViewById(R.id.recycler_view_conversationItem_lastMessageText);
            tvTimeStamp = itemView.findViewById(R.id.recycler_view_conversationItem_lastMessageTimeStamp);

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

    public ConversationAdapter(List<Conversation> conversationList) {
        this.conversationList = conversationList;
    }

    @Override
    public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_conversation_item, parent, false);
        return new ConversationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ConversationViewHolder holder, int position) {
        final Conversation currentItem = conversationList.get(position);

        if (currentItem.getUser_name() != null)
            holder.tvName.setText(currentItem.getUser_name());
        if (currentItem.getUser_profilePic() != null && !currentItem.getUser_profilePic().equals(""))
            Glide.with(holder.mImage).load(currentItem.getUser_profilePic()).centerCrop().into(holder.mImage);

        if (currentItem.getDate() != null) {
            Date date = currentItem.getDate();
            DateFormat dateFormat = new SimpleDateFormat("HH:mm, MMM d", Locale.getDefault());
            String creationDate = dateFormat.format(date);
            long time = date.getTime();
            if (currentItem.getDate() != null && !currentItem.getDate().equals("")) {
                TimeAgo timeAgo = new TimeAgo();
                holder.tvTimeStamp.setText(timeAgo.getTimeAgo(time));

            }
            holder.tvMessage.setText(currentItem.getText());
        }
        if (currentItem.getRead() != null && currentItem.getType() != null) {
            if (currentItem.getType().equals("message_received") && !currentItem.getRead()) {
                holder.tvMessage.setTextColor(holder.tvMessage.getResources().getColor(R.color.colorPrimary));
            } else {
                TextView textView = new TextView(holder.tvMessage.getContext());
                holder.tvMessage.setTextColor(textView.getCurrentTextColor());
            }
        }
    }


    @Override
    public int getItemCount() {
        return conversationList.size();
    }

}