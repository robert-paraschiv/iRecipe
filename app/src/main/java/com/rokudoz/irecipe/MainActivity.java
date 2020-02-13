package com.rokudoz.irecipe;

import com.google.android.gms.ads.MobileAds;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.rokudoz.irecipe.Fragments.FeedFragmentDirections;
import com.rokudoz.irecipe.Fragments.Messages.MessageFragment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;


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

            if (intent.hasExtra("friend_id") && !navController.getCurrentDestination().getLabel().equals("fragment_message")) {

                Log.d(TAG, "onReceive: current destinationLabel" + navController.getCurrentDestination().getLabel());
                createNotificationChannel();

                Log.d(TAG, "onReceive: messageLabel " + navController.getCurrentDestination().getLabel());
                String click_action = intent.getStringExtra("click_action");
                String friend_id = intent.getStringExtra("friend_id");
                String messageBody = intent.getStringExtra("messageBody");
                String messageTitle = intent.getStringExtra("messageTitle");

                Intent resultIntent = new Intent(click_action);
                resultIntent.putExtra("friend_id", friend_id);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), getString(R.string.default_notification_channel_id))
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);

                PendingIntent resultPendingIntent = PendingIntent.getActivity(MainActivity.this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(resultPendingIntent);


                int mNotificationId = (int) System.currentTimeMillis();

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                notificationManager.notify(mNotificationId, builder.build());
            }
        }
    };

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

    private void setUpNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();
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