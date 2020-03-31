package com.rokudoz.irecipe;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.ServerTimestamp;
import com.rokudoz.irecipe.Models.UserLastSeen;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class App extends Application {
    private static final String TAG = "App";

    //Firebase RealTime DB

    FirebaseDatabase database;
    DatabaseReference userRef;

    public static final String SETTINGS_PREFS_NAME = "SettingsPrefs";

    public static final String CHANNEL_MESSAGES = "Channel_Messages";
    public static final String CHANNEL_LIKES = "Channel_Likes";
    public static final String CHANNEL_FRIEND_REQUEST = "Channel_Friend_Request";
    public static final String CHANNEL_COMMENTS = "Channel_Comments";
    public static final String CHANNEL_MEAL_PLANS = "Meal_Plans";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        applyNightModeFromPrefs();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            FirebaseApp.initializeApp(this);
            database = FirebaseDatabase.getInstance();
            database.setPersistenceEnabled(true);
            userRef = database.getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());

            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot != null) {
                        UserLastSeen userLastSeen = new UserLastSeen(false, ServerValue.TIMESTAMP);
                        userRef.onDisconnect().setValue(userLastSeen).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "onSuccess: SET user online FALSE");
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private void applyNightModeFromPrefs() {
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

            //Meal Plans Channel
            NotificationChannel channel_meal_plans = new NotificationChannel(
                    CHANNEL_MEAL_PLANS,
                    "Meal Plans channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel_meal_plans.setDescription("This is the channel used for planning meals");
            channel_meal_plans.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationChannelList.add(channel_meal_plans);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannels(notificationChannelList);
            }
        }
    }
}
