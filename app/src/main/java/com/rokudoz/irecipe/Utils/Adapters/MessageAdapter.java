package com.rokudoz.irecipe.Utils.Adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.rokudoz.irecipe.Models.Ingredient;
import com.rokudoz.irecipe.Models.Instruction;
import com.rokudoz.irecipe.Models.Message;
import com.rokudoz.irecipe.R;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> messageList = new ArrayList<>();

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageText, tvMessageTimeStamp;
        MaterialCardView materialCardView;

        public MessageViewHolder(View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.recycler_view_messageItem_Text);
            tvMessageTimeStamp = itemView.findViewById(R.id.recycler_view_messageItem_MessageTimeStamp);
            materialCardView = itemView.findViewById(R.id.recycler_view_messageItem_layout);
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

        if (currentItem.getSender_id().equals(currentUserId)) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.materialCardView.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            holder.materialCardView.setLayoutParams(params);
            holder.materialCardView.setBackgroundResource(R.color.colorPrimary);
        } else {

        }

        holder.tvMessageText.setText(currentItem.getText());

        if (currentItem.getTimestamp() != null) {
            Date date = currentItem.getTimestamp();
            DateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String creationDate = dateFormat.format(date);
            holder.tvMessageTimeStamp.setText(creationDate);
        }


    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }
}