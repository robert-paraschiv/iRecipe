package com.rokudoz.irecipe.Utils;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.RemoteMessage;
import com.rokudoz.irecipe.App;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.R;

import static com.rokudoz.irecipe.Fragments.Messages.MessageFragment.messagingStyle;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService implements LifecycleObserver {
    private static final String TAG = "FirebaseMessagingServic";

    //Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");

    private boolean isAppInForeground;
    private LocalBroadcastManager broadcaster;

    @Override
    public void onCreate() {
        broadcaster = LocalBroadcastManager.getInstance(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData().size() > 0) {
            final String messageTitle = remoteMessage.getData().get("title");
            final String messageBody = remoteMessage.getData().get("body");
            final String click_action = remoteMessage.getData().get("click_action");
            final String friend_id = remoteMessage.getData().get("friend_id");
            String user_id = remoteMessage.getData().get("user_id");
            String friend_status = remoteMessage.getData().get("friend_status");
            String post_id = remoteMessage.getData().get("post_id");
            String recipe_id = remoteMessage.getData().get("recipe_id");
            String name = remoteMessage.getData().get("name");
            String user_profilePic = remoteMessage.getData().get("user_profilePic");
            String timestamp = remoteMessage.getData().get("timestamp");


            Log.d(TAG, "onMessageReceived: " + messageTitle + " " + messageBody + " " + click_action);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), App.CHANNEL_COMMENTS)
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setColor(Color.BLUE)
                    .setContentTitle(messageTitle)
                    .setContentText(messageBody)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);


            Intent resultIntent = new Intent(click_action);
            if (click_action != null && click_action.equals("com.rokudoz.foodify.MessageNotification")) {
                Intent intent = new Intent("MessageNotification");
                intent.putExtra("coming_from", "FirebaseMessagingService");
                intent.putExtra("click_action", click_action);
                intent.putExtra("friend_id", friend_id);
                intent.putExtra("messageTitle", messageTitle);
                intent.putExtra("messageBody", messageBody);
                intent.putExtra("timestamp", timestamp);
                intent.putExtra("name", name);
                intent.putExtra("user_profilePic", user_profilePic);
                Log.d(TAG, "onMessageReceived: +" + friend_id);
                broadcaster.sendBroadcast(intent);
            }
            if (click_action != null && click_action.equals("com.rokudoz.foodify.FriendRequestNotification")) {
                resultIntent.putExtra("user_id", user_id);
                resultIntent.putExtra("friend_status", friend_status);
            }
            if (click_action != null && click_action.equals("com.rokudoz.foodify.RecipeLikeNotification")) {
                resultIntent.putExtra("recipe_id", recipe_id);
            }
            if (click_action != null && click_action.equals("com.rokudoz.foodify.RecipeCommentNotification")) {
                resultIntent.putExtra("recipe_id", recipe_id);
            }
            if (click_action != null && click_action.equals("com.rokudoz.foodify.PostLikeNotification")) {
                resultIntent.putExtra("post_id", post_id);
            }
            if (click_action != null && click_action.equals("com.rokudoz.foodify.PostCommentNotification")) {
                resultIntent.putExtra("post_id", post_id);
            }

            PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(resultPendingIntent);


            //User id to unique INT id for notifications
            int notificationID = 0;
            char[] chars = friend_id.toCharArray();
            for (Character c : chars) {
                notificationID += c - 'a' + 1;
            }

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
            if (click_action != null && !click_action.equals("com.rokudoz.foodify.MessageNotification")) {
                notificationManager.notify(notificationID, builder.build());
            } else {
                if (!isAppInForeground) {

                    final int finalNotificationID = notificationID;
                    usersReference.document(friend_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot != null) {
                                User user = documentSnapshot.toObject(User.class);
                                if (user != null) {
                                    Glide.with(getApplicationContext()).asBitmap().load(user.getUserProfilePicUrl()).apply(RequestOptions.circleCropTransform()).into(new CustomTarget<Bitmap>() {
                                        @Override
                                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                            androidx.core.app.RemoteInput remoteInput = new androidx.core.app.RemoteInput.Builder("key_text_reply")
                                                    .setLabel("Send message").build();
                                            Intent replyIntent = new Intent(FirebaseMessagingService.this, DirectReplyReceiver.class);
                                            replyIntent.putExtra("friend_id_messageFragment", friend_id);
                                            replyIntent.putExtra("coming_from", "MessageFragment");
                                            replyIntent.putExtra("notification_id", finalNotificationID);
                                            PendingIntent replyPendingIntent = PendingIntent.getBroadcast(FirebaseMessagingService.this, finalNotificationID, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                                            NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                                                    R.drawable.ic_send_black_24dp,
                                                    "Reply",
                                                    replyPendingIntent
                                            ).addRemoteInput(remoteInput).build();

                                            Person user = new Person.Builder().setName(messageTitle).setIcon(IconCompat.createWithBitmap(resource)).build();
                                            messagingStyle = new NotificationCompat.MessagingStyle(user);
                                            messagingStyle.setConversationTitle("Chat");

                                            NotificationCompat.MessagingStyle.Message message =
                                                    new NotificationCompat.MessagingStyle.Message(messageBody, System.currentTimeMillis(), user);
                                            messagingStyle.addMessage(message);

                                            NotificationCompat.Builder builder2 = new NotificationCompat.Builder(FirebaseMessagingService.this, App.CHANNEL_MESSAGES)
                                                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                                                    .setStyle(messagingStyle)
                                                    .addAction(replyAction)
                                                    .setColor(Color.BLUE)
                                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                                                    .setAutoCancel(true);

                                            Intent resultIntent2 = new Intent(click_action);
                                            resultIntent2.putExtra("friend_id", friend_id);
                                            resultIntent2.putExtra("coming_from", "MessageFragment");
                                            resultIntent2.putExtra("notification_id", finalNotificationID);
                                            PendingIntent resultPendingIntent2 = PendingIntent.getActivity(FirebaseMessagingService.this, finalNotificationID, resultIntent2, PendingIntent.FLAG_UPDATE_CURRENT);
                                            builder2.setContentIntent(resultPendingIntent2);

                                            NotificationManagerCompat notificationManager2 = NotificationManagerCompat.from(FirebaseMessagingService.this);
                                            notificationManager2.notify(finalNotificationID, builder2.build());
                                        }

                                        @Override
                                        public void onLoadCleared(@Nullable Drawable placeholder) {

                                        }
                                    });
                                }
                            }
                        }
                    });

                }
            }

        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onForegroundStart() {
        isAppInForeground = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onForegroundStop() {
        isAppInForeground = false;
    }
}
