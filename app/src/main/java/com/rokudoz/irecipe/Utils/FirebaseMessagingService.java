package com.rokudoz.irecipe.Utils;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.RemoteMessage;
import com.rokudoz.irecipe.R;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    private static final String TAG = "FirebaseMessagingServic";

    private LocalBroadcastManager broadcaster;

    @Override
    public void onCreate() {
        broadcaster = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        createNotificationChannel();

        String messageTitle = remoteMessage.getNotification().getTitle();
        String messageBody = remoteMessage.getNotification().getBody();
        String click_action = remoteMessage.getNotification().getClickAction();
        String friend_id = remoteMessage.getData().get("friend_id");
        String user_id = remoteMessage.getData().get("user_id");
        String friend_status = remoteMessage.getData().get("friend_status");
        String post_id = remoteMessage.getData().get("post_id");
        String recipe_id = remoteMessage.getData().get("recipe_id");


        Log.d(TAG, "onMessageReceived: " + messageTitle + " " + messageBody + click_action);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), getString(R.string.default_notification_channel_id))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);


        Intent resultIntent = new Intent(click_action);
        if (click_action != null && click_action.equals("com.rokudoz.foodify.MessageNotification")) {
            Intent intent = new Intent("MessageNotification");
            intent.putExtra("click_action", click_action);
            intent.putExtra("friend_id", friend_id);
            intent.putExtra("messageTitle", messageTitle);
            intent.putExtra("messageBody", messageBody);
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


        int mNotificationId = (int) System.currentTimeMillis();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        if (!click_action.equals("com.rokudoz.foodify.MessageNotification"))
            notificationManager.notify(mNotificationId, builder.build());

    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Foodify";
            String description = "For friend requests";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(getString(R.string.default_notification_channel_id), name,
                    importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviours after this
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
