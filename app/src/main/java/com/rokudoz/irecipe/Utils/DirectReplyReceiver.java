package com.rokudoz.irecipe.Utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.IconCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.WriteBatch;
import com.rokudoz.irecipe.App;
import com.rokudoz.irecipe.Fragments.Messages.MessageFragment;
import com.rokudoz.irecipe.MainActivity;
import com.rokudoz.irecipe.Models.Conversation;
import com.rokudoz.irecipe.Models.Message;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;

import java.util.Objects;

import static com.rokudoz.irecipe.Fragments.Messages.MessageFragment.messagingStyle;

public class DirectReplyReceiver extends BroadcastReceiver {
    private static final String TAG = "DirectReplyReceiver";

    private User userFriend = new User();
    private User mUser = new User();
    String friend_id = "";

    //Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");

    @Override
    public void onReceive(final Context context, Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);

        if (remoteInput != null) {
            final CharSequence replyText = remoteInput.getCharSequence("key_text_reply");
            final String text = Objects.requireNonNull(replyText).toString();


            if (intent.hasExtra("coming_from")) {
                String comingFrom = intent.getStringExtra("coming_from");

                if (comingFrom.equals("MessageFragment")) {
                    friend_id = intent.getStringExtra("friend_id_messageFragment");
                } else if (comingFrom.equals("MainActivity")) {
                    friend_id = intent.getStringExtra("friend_id_mainActivity");
                }
                final int notificationID = intent.getIntExtra("notification_id", 0);

                Log.d(TAG, "onReceive: friend id " + friend_id + "coming from " + comingFrom + " notification id " + notificationID);

                if (!text.trim().equals(""))
                    usersReference.document(FirebaseAuth.getInstance().getCurrentUser().getUid()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot != null) {
                                mUser = documentSnapshot.toObject(User.class);
                                final String currentUserId = mUser.getUser_id();

                                usersReference.document(friend_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                        if (documentSnapshot != null) {
                                            userFriend = documentSnapshot.toObject(User.class);

                                            final com.rokudoz.irecipe.Models.Message messageForCurrentUser = new com.rokudoz.irecipe.Models.Message(currentUserId, friend_id, text
                                                    , "message_sent", null, false);
                                            final com.rokudoz.irecipe.Models.Message messageForFriendUser = new Message(currentUserId, friend_id, text, "message_received"
                                                    , null, false);

                                            final Conversation conversationForCurrentUser = new Conversation(friend_id, userFriend.getName(), userFriend.getUserProfilePicUrl(), text
                                                    , "message_sent", null, false);
                                            final Conversation conversationForFriendUser = new Conversation(currentUserId, mUser.getName(), mUser.getUserProfilePicUrl(), text
                                                    , "message_received", null, false);

                                            //Send message to db in batch
                                            WriteBatch batch = db.batch();
                                            String messageID = usersReference.document(currentUserId).collection("Conversations").document(friend_id).collection(friend_id)
                                                    .document().getId();
                                            Log.d(TAG, "sendMessage: " + messageID);
                                            batch.set(usersReference.document(currentUserId).collection("Conversations").document(friend_id), conversationForCurrentUser);
                                            batch.set(usersReference.document(friend_id).collection("Conversations").document(currentUserId), conversationForFriendUser);
                                            batch.set(usersReference.document(currentUserId).collection("Conversations").document(friend_id).collection(friend_id)
                                                            .document(messageID)
                                                    , messageForCurrentUser);
                                            batch.set(usersReference.document(friend_id).collection("Conversations").document(currentUserId).collection(currentUserId)
                                                            .document(messageID)
                                                    , messageForFriendUser);

                                            batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.d(TAG, "onSuccess: added message");

                                                    //Get user profile pic and send notification
                                                    Glide.with(context).asBitmap().load(mUser.getUserProfilePicUrl()).apply(RequestOptions.circleCropTransform())
                                                            .into(new CustomTarget<Bitmap>() {
                                                                @Override
                                                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                                                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                                                    if (notificationManager != null) {
                                                                        Person person = new Person.Builder().setName("Me").setIcon(IconCompat.createWithBitmap(resource)).build();
                                                                        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(person);
                                                                        messagingStyle.setConversationTitle("Chat");
                                                                        NotificationCompat.MessagingStyle.Message message =
                                                                                new NotificationCompat.MessagingStyle.Message(text, System.currentTimeMillis(), person);
                                                                        messagingStyle.addMessage(message);
                                                                        // Build a new notification, which informs the user that the system
                                                                        // handled their interaction with the previous notification.
                                                                        Notification repliedNotification = new NotificationCompat.Builder(context, App.CHANNEL_MESSAGES)
                                                                                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                                                                                .setStyle(messagingStyle)
                                                                                .setContentText(text)
                                                                                .setColor(Color.BLUE)
                                                                                .setAutoCancel(true)
                                                                                .setTimeoutAfter(100)
                                                                                .build();

                                                                        // Issue the new notification.
                                                                        notificationManager.notify(notificationID, repliedNotification);
                                                                    }
                                                                }

                                                                @Override
                                                                public void onLoadCleared(@Nullable Drawable placeholder) {

                                                                }
                                                            });
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        }
                    });

            } else {
                Log.d(TAG, "onReceive: NO COMING FROM  " + intent.getExtras().toString());
            }
        }
    }
}
