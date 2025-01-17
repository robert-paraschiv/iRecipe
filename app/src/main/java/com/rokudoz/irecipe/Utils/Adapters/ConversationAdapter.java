package com.rokudoz.irecipe.Utils.Adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    private static final String TAG = "ConversationAdapter";
    private List<Conversation> conversationList;
    private OnItemClickListener mListener;
    Context context;


    public interface OnItemClickListener {
        void onConversationClick(int position);

        void onDeleteClick(int position);

        void onPictureClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public class ConversationViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvName, tvMessage, tvTimeStamp;
        CircleImageView mImage;
        ImageView seenCheck, spacer;


        public ConversationViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.recycler_view_conversationItem_friendName);
            mImage = itemView.findViewById(R.id.recycler_view_conversationItem_friendImage);
            tvMessage = itemView.findViewById(R.id.recycler_view_conversationItem_lastMessageText);
            tvTimeStamp = itemView.findViewById(R.id.recycler_view_conversationItem_lastMessageTimeStamp);
            seenCheck = itemView.findViewById(R.id.recycler_view_conversationItem_lastMessageSeen);
            spacer = itemView.findViewById(R.id.rv_conversation_item_spacer);
            itemView.setOnClickListener(this);
            itemView.setOnCreateContextMenuListener(mOnCreateContextMenuListener);

            mImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            mListener.onPictureClick(position);
                        }
                    }
                }
            });
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    mListener.onConversationClick(position);
                }
            }
        }

        private final View.OnCreateContextMenuListener mOnCreateContextMenuListener = new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                if (conversationList != null) {
                    MenuItem deleteAction = menu.add("Delete conversation");
                    deleteAction.setOnMenuItemClickListener(onDeleteClickListener);
                }
            }
        };

        private final MenuItem.OnMenuItemClickListener onDeleteClickListener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (mListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Log.d(TAG, "onMenuItemClick: " + item.getOrder());
                        mListener.onDeleteClick(position);
                    }
                }
                return true;
            }
        };

    }

    public ConversationAdapter(Context context, List<Conversation> conversationList) {
        this.context = context;
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
        if (position == 0) {
            holder.spacer.setVisibility(View.GONE);
        } else {
            holder.spacer.setVisibility(View.VISIBLE);
        }

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
            if (currentItem.getType().equals("message_received") && !currentItem.getRead()) { // last message is received and not read
                holder.seenCheck.setVisibility(View.GONE);
                holder.tvMessage.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                holder.tvTimeStamp.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
            } else if (currentItem.getType().equals("message_received") && currentItem.getRead()) { // last message is received and read
                holder.seenCheck.setVisibility(View.GONE);
                holder.tvMessage.setTextColor(ContextCompat.getColor(context, R.color.calendar_textColor));
                holder.tvTimeStamp.setTextColor(ContextCompat.getColor(context, R.color.calendar_textColor));
                holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.calendar_textColor));
            } else if (currentItem.getType().equals("message_sent") && !currentItem.getRead()) { // last message is sent and not read
                holder.seenCheck.setVisibility(View.VISIBLE);
                holder.seenCheck.setImageResource(R.drawable.ic_message_read_status);
                holder.seenCheck.setColorFilter(ContextCompat.getColor(context, R.color.grey));
                holder.tvMessage.setTextColor(ContextCompat.getColor(context, R.color.calendar_textColor));
                holder.tvTimeStamp.setTextColor(ContextCompat.getColor(context, R.color.calendar_textColor));
                holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.calendar_textColor));
            } else if (currentItem.getType().equals("message_sent") && currentItem.getRead()) { // last message is sent and read
                holder.seenCheck.setVisibility(View.VISIBLE);
                holder.seenCheck.setImageResource(R.drawable.ic_message_read_status);
                holder.seenCheck.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
                holder.tvMessage.setTextColor(ContextCompat.getColor(context, R.color.calendar_textColor));
                holder.tvTimeStamp.setTextColor(ContextCompat.getColor(context, R.color.calendar_textColor));
                holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.calendar_textColor));
            }
        }
    }


    @Override
    public int getItemCount() {
        return conversationList.size();
    }

}