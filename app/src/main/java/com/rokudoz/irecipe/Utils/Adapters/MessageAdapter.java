package com.rokudoz.irecipe.Utils.Adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
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

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.rokudoz.irecipe.Models.Message;
import com.rokudoz.irecipe.Models.Post;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.TimeAgo;

import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;


public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "MessageAdapter";

    private OnItemClickListener mListener;
    private Context context;
    private List<Message> messageList;

    private final int MESSAGE_VIEW_TYPE = 0;
    private final int POST_RECIPE_VIEW_TYPE = 1;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");

    public interface OnItemClickListener {
        void onPostRecipeMessageClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

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
            tvMessageText.setMaxWidth((int) (Resources.getSystem().getDisplayMetrics().widthPixels * 0.7));
        }

    }

    public class PostRecipeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvTitle, tvMessageTimeStamp;
        MaterialCardView materialCardView;
        ImageView readStatus, image;

        PostRecipeViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.rv_message_title);
            tvMessageTimeStamp = itemView.findViewById(R.id.rv_message_timestamp);
            materialCardView = itemView.findViewById(R.id.rv_message_card);
            readStatus = itemView.findViewById(R.id.rv_message_ReadStatus);
            image = itemView.findViewById(R.id.rv_message_image);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    mListener.onPostRecipeMessageClick(position);
                }
            }
        }
    }

    public MessageAdapter(List<Message> messageList, Context context) {
        this.messageList = messageList;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case MESSAGE_VIEW_TYPE:
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_message_item, parent, false);
                return new MessageViewHolder(v);
            case POST_RECIPE_VIEW_TYPE:

            default:
                View postRecipeView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_message_post_recipe, parent, false);
                return new PostRecipeViewHolder(postRecipeView);
        }


    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        switch (viewType) {
            case MESSAGE_VIEW_TYPE:
                MessageViewHolder messageViewHolder = (MessageViewHolder) holder;
                populateMessageViewHolder(messageViewHolder, position);
                break;
            case POST_RECIPE_VIEW_TYPE:
                PostRecipeViewHolder postRecipeViewHolder = (PostRecipeViewHolder) holder;
                populatePostRecipeViewHolder(postRecipeViewHolder, position);
                break;
        }
    }

    private void populatePostRecipeViewHolder(PostRecipeViewHolder holder, int position) {
        final Message currentItem = messageList.get(position);
        String imageUrl = "";
        String title = "";
        if (currentItem.getPost() == null && currentItem.getRecipe() != null) {
            imageUrl = currentItem.getRecipe().getImageUrls_list().get(0);
            title = currentItem.getRecipe().getTitle();
        } else if (currentItem.getPost() != null && currentItem.getRecipe() == null) {
            imageUrl = currentItem.getPost().getImageUrl();
            title = currentItem.getPost().getText();
        }
        if (!imageUrl.equals("")) {
            Glide.with(holder.image).load(imageUrl).centerCrop().into(holder.image);
        }

        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        if (currentItem.getType() != null && currentItem.getType().equals("message_sent")) {
            holder.readStatus.setVisibility(View.VISIBLE);
            if (currentItem.getRead() != null && currentItem.getRead()) {
                Log.d(TAG, "onBindViewHolder: is read");
                holder.readStatus.setImageResource(R.drawable.ic_message_read_status);
                holder.readStatus.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
            } else if (currentItem.getRead() != null && !currentItem.getRead()) {
                holder.readStatus.setImageResource(R.drawable.ic_pngwave);
                holder.readStatus.setColorFilter(ContextCompat.getColor(context, R.color.black));
            } else if (currentItem.getRead() == null) {
                holder.readStatus.setImageResource(R.drawable.ic_check_black_24dp);
                holder.readStatus.setColorFilter(ContextCompat.getColor(context, R.color.black));
            }


            //Reset params
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            holder.materialCardView.setLayoutParams(params);
            holder.materialCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.color_message_friend_background));
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.color_message_friend_text));


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
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.color_message_friend_text));


        }
        holder.tvTitle.setText(title);

        if (currentItem.getTimestamp() != null) {
            Date date = currentItem.getTimestamp();
            long time = date.getTime();
            TimeAgo timeAgo = new TimeAgo();
            String timeAgoString = timeAgo.getTimeAgo(time);
            if (currentItem.getTimestamp() != null && !currentItem.getTimestamp().toString().equals("")) {
                DateFormat smallDateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String timeString = smallDateFormat.format(date);
                holder.tvMessageTimeStamp.setText(timeString);
//                if (timeAgoString != null) {
//                    if (timeAgoString.equals("just now") || timeAgoString.equals("a minute ago") || timeAgoString.contains("hours ago") || timeAgoString.equals("an hour ago")) {
//                        holder.tvMessageTimeStamp.setText(timeAgo.getTimeAgo(time));
//                    } else if (timeAgoString.equals("yesterday")) {
//                        holder.tvMessageTimeStamp.setText(String.format("yesterday, %s", timeString));
//                    } else if (timeAgoString.contains("minutes ago")) {
//                        holder.tvMessageTimeStamp.setText(timeAgo.getTimeAgo(time));
//                    } else
//                        holder.tvMessageTimeStamp.setText(String.format("%s, %s", timeAgo.getTimeAgo(time), timeString));
//                } else {
//                    holder.tvMessageTimeStamp.setText("");
//                }
            }
        } else {
            holder.tvMessageTimeStamp.setText("");
        }
    }

    private void populateMessageViewHolder(@NonNull MessageViewHolder holder, int position) {
        final Message currentItem = messageList.get(position);
        Log.d(TAG, "onBindViewHolder: " + messageList.get(position).toString());

        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        if (currentItem.getType() != null && currentItem.getType().equals("message_sent")) {

            holder.readStatus.setVisibility(View.VISIBLE);
            if (currentItem.getRead() != null && currentItem.getRead()) {
                Log.d(TAG, "onBindViewHolder: is read");
                holder.readStatus.setImageResource(R.drawable.ic_message_read_status);
                holder.readStatus.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
            } else if (currentItem.getRead() != null && !currentItem.getRead()) {
                holder.readStatus.setImageResource(R.drawable.ic_pngwave);
                holder.readStatus.setColorFilter(ContextCompat.getColor(context, R.color.black));
            } else if (currentItem.getRead() == null) {
                holder.readStatus.setImageResource(R.drawable.ic_check_black_24dp);
                holder.readStatus.setColorFilter(ContextCompat.getColor(context, R.color.black));
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
            TimeAgo timeAgo = new TimeAgo();
            String timeAgoString = timeAgo.getTimeAgo(time);
            if (currentItem.getTimestamp() != null && !currentItem.getTimestamp().toString().equals("")) {
                DateFormat smallDateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String timeString = smallDateFormat.format(date);
                holder.tvMessageTimeStamp.setText(timeString);
//                if (timeAgoString != null) {
//                    if (timeAgoString.equals("just now") || timeAgoString.equals("a minute ago") || timeAgoString.contains("hours ago") || timeAgoString.equals("an hour ago")) {
//                        holder.tvMessageTimeStamp.setText(timeAgo.getTimeAgo(time));
//                    } else if (timeAgoString.equals("yesterday")) {
//                        holder.tvMessageTimeStamp.setText(String.format("yesterday, %s", timeString));
//                    } else if (timeAgoString.contains("minutes ago")) {
//                        holder.tvMessageTimeStamp.setText(timeAgo.getTimeAgo(time));
//                    } else
//                        holder.tvMessageTimeStamp.setText(String.format("%s, %s", timeAgo.getTimeAgo(time), timeString));
//                } else {
//                    holder.tvMessageTimeStamp.setText("");
//                }
            }
        } else {
            holder.tvMessageTimeStamp.setText("");
        }
    }


    @Override
    public int getItemViewType(int position) {
        if (messageList.get(position).getRecipe() == null && messageList.get(position).getPost() == null) {
            return MESSAGE_VIEW_TYPE;
        } else {
            return POST_RECIPE_VIEW_TYPE;
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }
}