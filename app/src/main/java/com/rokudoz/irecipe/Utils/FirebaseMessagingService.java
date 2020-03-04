package com.rokudoz.irecipe.Utils;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.Timestamp;
import com.google.firebase.messaging.RemoteMessage;
import com.rokudoz.irecipe.App;
import com.rokudoz.irecipe.R;

import static com.rokudoz.irecipe.Fragments.Messages.MessageFragment.messagingStyle;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService implements LifecycleObserver {
    private static final String TAG = "FirebaseMessagingServic";

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
            String messageTitle = remoteMessage.getData().get("title");
            String messageBody = remoteMessage.getData().get("body");
            String click_action = remoteMessage.getData().get("click_action");
            String friend_id = remoteMessage.getData().get("friend_id");
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
            int  notificationID = 0;
            char[] chars = friend_id.toCharArray();
            for (Character c : chars) {
                notificationID += c - 'a' + 1;
            }

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
            if (click_action != null && !click_action.equals("com.rokudoz.foodify.MessageNotification")) {
                notificationManager.notify(notificationID, builder.build());
            } else {
                if (!isAppInForeground) {
                    androidx.core.app.RemoteInput remoteInput = new androidx.core.app.RemoteInput.Builder("key_text_reply")
                            .setLabel("Send message").build();
                    Intent replyIntent = new Intent(this, DirectReplyReceiver.class);
                    replyIntent.putExtra("friend_id_messageFragment", friend_id);
                    replyIntent.putExtra("coming_from", "MessageFragment");
                    replyIntent.putExtra("notification_id", notificationID);
                    PendingIntent replyPendingIntent = PendingIntent.getBroadcast(this, notificationID, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                            R.drawable.ic_send_black_24dp,
                            "Reply",
                            replyPendingIntent
                    ).addRemoteInput(remoteInput).build();

                    Person user = new Person.Builder().setName(messageTitle).build();
                    messagingStyle = new NotificationCompat.MessagingStyle(user);
                    messagingStyle.setConversationTitle("Chat");

                    NotificationCompat.MessagingStyle.Message message =
                            new NotificationCompat.MessagingStyle.Message(messageBody, System.currentTimeMillis(), user);
                    messagingStyle.addMessage(message);

                    NotificationCompat.Builder builder2 = new NotificationCompat.Builder(this, App.CHANNEL_MESSAGES)
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
                    resultIntent2.putExtra("notification_id", notificationID);
                    PendingIntent resultPendingIntent2 = PendingIntent.getActivity(this, notificationID, resultIntent2, PendingIntent.FLAG_UPDATE_CURRENT);
                    builder2.setContentIntent(resultPendingIntent2);

                    NotificationManagerCompat notificationManager2 = NotificationManagerCompat.from(this);
                    notificationManager2.notify(notificationID, builder2.build());
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
