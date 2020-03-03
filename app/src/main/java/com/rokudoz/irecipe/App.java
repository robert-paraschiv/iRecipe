package com.rokudoz.irecipe;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.ArrayList;
import java.util.List;

public class App extends Application {
    private static final String TAG = "App";

    public static final String SETTINGS_PREFS_NAME = "SettingsPrefs";

    public static final String CHANNEL_MESSAGES = "Channel_Messages";
    public static final String CHANNEL_LIKES = "Channel_Likes";
    public static final String CHANNEL_FRIEND_REQUEST = "Channel_Friend_Request";
    public static final String CHANNEL_COMMENTS = "Channel_Comments";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        applyNightModeFromPrefs();
    }

    private void applyNightModeFromPrefs(){
        SharedPreferences sharedPreferences = getSharedPreferences(SETTINGS_PREFS_NAME, MODE_PRIVATE);
        int mode = sharedPreferences.getInt("NightMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        if (mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
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
            channel_messages.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
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
