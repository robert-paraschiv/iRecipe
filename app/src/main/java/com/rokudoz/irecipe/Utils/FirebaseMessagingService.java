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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.Timestamp;
import com.google.firebase.messaging.RemoteMessage;
import com.rokudoz.irecipe.App;
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

        String messageTitle = remoteMessage.getNotification().getTitle();
        String messageBody = remoteMessage.getNotification().getBody();
        String click_action = remoteMessage.getNotification().getClickAction();
        String friend_id = remoteMessage.getData().get("friend_id");
        String user_id = remoteMessage.getData().get("user_id");
        String friend_status = remoteMessage.getData().get("friend_status");
        String post_id = remoteMessage.getData().get("post_id");
        String recipe_id = remoteMessage.getData().get("recipe_id");
        String name = remoteMessage.getData().get("name");
        String user_profilePic = remoteMessage.getData().get("user_profilePic");
        String timestamp = remoteMessage.getData().get("timestamp");



        Log.d(TAG, "onMessageReceived: " + messageTitle + " " + messageBody +" "+ click_action);

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
            intent.putExtra("click_action", click_action);
            intent.putExtra("friend_id", friend_id);
            intent.putExtra("messageTitle", messageTitle);
            intent.putExtra("messageBody", messageBody);
            intent.putExtra("timestamp", timestamp);
            intent.putExtra("name", name);
            intent.putExtra("user_profilePic", user_profilePic);
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
}
