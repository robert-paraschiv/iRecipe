package com.rokudoz.irecipe;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

public class App extends Application {
    public static final String CHANNEL_MESSAGES = "Channel_Messages";
    public static final String CHANNEL_LIKES = "Channel_Likes";
    public static final String CHANNEL_FRIEND_REQUEST = "Channel_Friend_Request";
    public static final String CHANNEL_COMMENTS = "Channel_Comments";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            List<NotificationChannel> notificationChannelList = new ArrayList<>();


            //Messages Channel
            NotificationChannel channel_messages = new NotificationChannel(
                    CHANNEL_MESSAGES,
                    "Messages channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel_messages.setDescription("This is the channel used for messages");
            channel_messages.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationChannelList.add(channel_messages);

            //Likes Channel
            NotificationChannel channel_likes = new NotificationChannel(
                    CHANNEL_LIKES,
                    "Messages channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel_likes.setDescription("This is the channel used for likes");
            channel_likes.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationChannelList.add(channel_likes);

            //Comments Channel
            NotificationChannel channel_comments = new NotificationChannel(
                    CHANNEL_COMMENTS,
                    "Comments channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel_comments.setDescription("This is the channel used for comments");
            channel_comments.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationChannelList.add(channel_comments);

            //Friend request Channel
            NotificationChannel channel_friend_req = new NotificationChannel(
                    CHANNEL_FRIEND_REQUEST,
                    "Friend Request channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel_friend_req.setDescription("This is the channel used for friend requests");
            channel_friend_req.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationChannelList.add(channel_friend_req);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannels(notificationChannelList);
        }
    }
}
