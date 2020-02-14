package com.rokudoz.irecipe;

import com.google.android.gms.ads.MobileAds;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.rokudoz.irecipe.Fragments.FeedFragmentDirections;
import com.rokudoz.irecipe.Fragments.Messages.MessageFragment;
import com.rokudoz.irecipe.Utils.DirectReplyReceiver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import static com.rokudoz.irecipe.Fragments.Messages.MessageFragment.messagingStyle;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    NavController navController;

    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(this, getResources().getString(R.string.admob_app_id));

        setUpNavigation();

    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver),
                new IntentFilter("MessageNotification")
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getLabel() != null)
                if (intent.hasExtra("friend_id") && !navController.getCurrentDestination().getLabel().equals("fragment_message")) {
                    String click_action = intent.getStringExtra("click_action");
                    String friend_id = intent.getStringExtra("friend_id");


                    sendNotification(intent, friend_id);
                }
        }
    };

    private void sendNotification(Intent intents, String friend_id) {
        String click_action = intents.getStringExtra("click_action");
        String messageBody = intents.getStringExtra("messageBody");
        String messageTitle = intents.getStringExtra("messageTitle");
        Log.d(TAG, "onReceive: " + friend_id);
        androidx.core.app.RemoteInput remoteInput = new androidx.core.app.RemoteInput.Builder("key_text_reply")
                .setLabel("Send message").build();

        Intent replyIntent = new Intent(this, DirectReplyReceiver.class);
        replyIntent.putExtra("coming_from","MainActivity");
        replyIntent.putExtra("friend_id_mainActivity", friend_id);
        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(this, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

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


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, App.CHANNEL_MESSAGES)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setStyle(messagingStyle)
                .addAction(replyAction)
                .setColor(Color.BLUE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true);

        Intent resultIntent = new Intent(click_action);
        resultIntent.putExtra("friend_id", friend_id);
        resultIntent.putExtra("coming_from","MainActivity");
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);


        int mNotificationId = (int) System.currentTimeMillis();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build());
    }


    private void setUpNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        if (getIntent() != null && getIntent().getStringExtra("friend_id") != null) {
            String friend_id = getIntent().getStringExtra("friend_id");
            Bundle args = new Bundle();
            args.putString("user_id", friend_id);
            navController.navigate(R.id.messageFragment, args);


        }
        if (getIntent() != null && getIntent().getStringExtra("recipe_id") != null) {
            String recipe_id = getIntent().getStringExtra("recipe_id");
            Bundle args = new Bundle();
            args.putString("documentID", recipe_id);
            navController.navigate(R.id.recipeDetailedFragment, args);
        }
        if (getIntent() != null && getIntent().getStringExtra("post_id") != null) {
            String post_id = getIntent().getStringExtra("post_id");
            Bundle args = new Bundle();
            args.putString("documentID", post_id);
            navController.navigate(R.id.postDetailed, args);
        }
        if (getIntent() != null && getIntent().getStringExtra("user_id") != null) {
            String user_id = getIntent().getStringExtra("user_id");
            Bundle args = new Bundle();
            args.putString("documentID", user_id);
            navController.navigate(R.id.userProfileFragment2, args);
        }
    }
}