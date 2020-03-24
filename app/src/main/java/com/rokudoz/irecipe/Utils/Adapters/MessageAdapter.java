package com.rokudoz.irecipe.Utils.Adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.rokudoz.irecipe.Models.Message;
import com.rokudoz.irecipe.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;


public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

    private Context context;
    private List<Message> messageList;
    private static final String TAG = "MessageAdapter";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageText, tvMessageTimeStamp;
        MaterialCardView materialCardView;
        ImageView readStatus;

        MessageViewHolder(View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.recycler_view_messageItem_Text);
            tvMessageTimeStamp = itemView.findViewById(R.id.recycler_view_messageItem_MessageTimeStamp);
            materialCardView = itemView.findViewById(R.id.recycler_view_messageItem_layout);
            readStatus = itemView.findViewById(R.id.recycler_view_messageItem_ReadStatus);
        }

    }

    public MessageAdapter(List<Message> messageList, Context context) {
        this.messageList = messageList;
        this.context = context;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_message_item, parent, false);
        return new MessageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, int position) {
        final Message currentItem = messageList.get(position);
        Log.d(TAG, "onBindViewHolder: " + messageList.get(position).toString());

        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        if (currentItem.getType() != null && currentItem.getType().equals("message_sent")) {

            holder.readStatus.setVisibility(View.VISIBLE);
            if (currentItem.getRead() != null && currentItem.getRead()) {
                Log.d(TAG, "onBindViewHolder: is read");
                holder.readStatus.setImageResource(R.drawable.ic_message_read_status);
                holder.readStatus.setColorFilter(ContextCompat.getColor(context,R.color.colorPrimary));
            } else if (currentItem.getRead() != null && !currentItem.getRead()) {
                holder.readStatus.setImageResource(R.drawable.ic_pngwave);
                holder.readStatus.setColorFilter(ContextCompat.getColor(context,R.color.black));
            } else if (currentItem.getRead() == null) {
                holder.readStatus.setImageResource(R.drawable.ic_check_black_24dp);
                holder.readStatus.setColorFilter(ContextCompat.getColor(context,R.color.black));
            }


            //Reset params
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            holder.materialCardView.setLayoutParams(params);
            holder.materialCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_message_friend_background));
            holder.tvMessageText.setTextColor(ContextCompat.getColor(context, R.color.color_message_friend_text));


        } else if (currentItem.getType() != null && currentItem.getType().equals("message_received")) {
            holder.readStatus.setVisibility(View.GONE);

            if (currentItem.getRead() != null && !currentItem.getRead()) {
                WriteBatch batch = db.batch();
                batch.update(usersReference.document(currentUserId).collection("Conversations").document(currentItem.getSender_id()).collection(currentItem.getSender_id())
                        .document(currentItem.getDocumentId()), "read", true);
                batch.update(usersReference.document(currentItem.getSender_id()).collection("Conversations").document(currentItem.getReceiver_id()).collection(currentItem.getReceiver_id())
                        .document(currentItem.getDocumentId()), "read", true);

                batch.update(usersReference.document(currentUserId).collection("Conversations").document(currentItem.getSender_id())
                        , "read", true);
                batch.update(usersReference.document(currentItem.getSender_id()).collection("Conversations").document(currentUserId)
                        , "read", true);

                batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: Updated received = true");
                    }
                });

            }
            //Set params for message sent
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
            holder.materialCardView.setLayoutParams(params);
            holder.materialCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_message_user_background));
            holder.tvMessageText.setTextColor(ContextCompat.getColor(context, R.color.color_message_friend_text));


        }

        if (currentItem.getText() != null)
            holder.tvMessageText.setText(currentItem.getText());

        if (currentItem.getTimestamp() != null) {
            Date date = currentItem.getTimestamp();
            long time = date.getTime();
            String timeAgo = getTimeAgo(time);
            if (currentItem.getTimestamp() != null && !currentItem.getTimestamp().toString().equals("")) {
                DateFormat smallDateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String timeString = smallDateFormat.format(date);
                if (timeAgo != null) {
                    if (timeAgo.equals("just now") || timeAgo.equals("a minute ago") || timeAgo.contains("hours ago") || timeAgo.equals("an hour ago")) {
                        holder.tvMessageTimeStamp.setText(getTimeAgo(time));
                    } else if (timeAgo.equals("yesterday")) {
                        holder.tvMessageTimeStamp.setText(String.format("yesterday, %s", timeString));
                    } else if (timeAgo.contains("minutes ago")) {
                        holder.tvMessageTimeStamp.setText(getTimeAgo(time));
                    } else
                        holder.tvMessageTimeStamp.setText(String.format("%s, %s", timeAgo, timeString));
                } else {
                    holder.tvMessageTimeStamp.setText("");
                }
            }
        } else {
            holder.tvMessageTimeStamp.setText("");
        }
    }

    public String getTimeAgo(long time) {
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
    public int getItemCount() {
        return messageList.size();
    }
}