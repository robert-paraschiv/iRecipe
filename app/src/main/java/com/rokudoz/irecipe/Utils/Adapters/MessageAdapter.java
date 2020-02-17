package com.rokudoz.irecipe.Utils.Adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Instruction;
import com.rokudoz.irecipe.Models.Message;
import com.rokudoz.irecipe.R;
import com.rokudoz.irecipe.Utils.TimeAgo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> messageList = new ArrayList<>();
    private static final String TAG = "MessageAdapter";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference postsRef = db.collection("Posts");
    private CollectionReference usersReference = db.collection("Users");

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageText, tvMessageTimeStamp;
        MaterialCardView materialCardView;
        ImageView readStatus;

        public MessageViewHolder(View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.recycler_view_messageItem_Text);
            tvMessageTimeStamp = itemView.findViewById(R.id.recycler_view_messageItem_MessageTimeStamp);
            materialCardView = itemView.findViewById(R.id.recycler_view_messageItem_layout);
            readStatus = itemView.findViewById(R.id.recycler_view_messageItem_ReadStatus);
        }

    }

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_layout_message_item, parent, false);
        return new MessageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, int position) {
        final Message currentItem = messageList.get(position);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (currentItem.getType() != null && currentItem.getType().equals("message_sent")) {

            holder.readStatus.setVisibility(View.VISIBLE);
            if (currentItem.getRead() != null && currentItem.getRead()) {
                holder.readStatus.setImageResource(R.drawable.ic_pngwave);
                holder.readStatus.setColorFilter(holder.readStatus.getResources().getColor(R.color.grey));
            } else if (currentItem.getRead() != null && !currentItem.getRead()) {
                ImageView imageView = new ImageView(holder.readStatus.getContext());
                holder.readStatus.setColorFilter(imageView.getColorFilter());
            }

            //Set params for message sent
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            holder.materialCardView.setLayoutParams(params);
            holder.materialCardView.setCardBackgroundColor(holder.materialCardView.getResources().getColor(R.color.colorPrimary));


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

            //Reset params
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_START);
            holder.materialCardView.setLayoutParams(params);
            MaterialCardView materialCardView = new MaterialCardView(holder.materialCardView.getContext());
            holder.materialCardView.setCardBackgroundColor(materialCardView.getCardBackgroundColor());
        }

        if (currentItem.getText() != null)
            holder.tvMessageText.setText(currentItem.getText());

        if (currentItem.getTimestamp() != null) {
            Date date = currentItem.getTimestamp();
            DateFormat dateFormat = new SimpleDateFormat("HH:mm, MMM d", Locale.getDefault());
            long time = date.getTime();
            if (currentItem.getTimestamp() != null && !currentItem.getTimestamp().equals("")) {
                TimeAgo timeAgo = new TimeAgo();
                holder.tvMessageTimeStamp.setText(timeAgo.getTimeAgo(time));

            }
        }
    }



    @Override
    public int getItemCount() {
        return messageList.size();
    }
}