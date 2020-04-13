package com.rokudoz.irecipe;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rokudoz.irecipe.Fragments.FeedFragmentDirections;
import com.rokudoz.irecipe.Fragments.Messages.MessageFragment;
import com.rokudoz.irecipe.Models.User;
import com.rokudoz.irecipe.Models.UserLastSeen;
import com.rokudoz.irecipe.Utils.DirectReplyReceiver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;
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
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;

import static com.rokudoz.irecipe.App.SETTINGS_PREFS_NAME;
import static com.rokudoz.irecipe.Fragments.Messages.MessageFragment.messagingStyle;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private AdView mBannerAd;

    //Firebase RealTime db
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference usersRef;

    //Firebase
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference usersReference = db.collection("Users");

    NavController navController;

    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, getResources().getString(R.string.admob_app_id));

        mBannerAd = findViewById(R.id.banner_adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("2F1C484BD502BA7D51AC78D75751AFE0") // Mi 9T Pro
                .addTestDevice("B141CB779F883EF84EA9A32A7D068B76") // RedMi 5 Plus
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();

        mBannerAd.loadAd(adRequest);

        setUpNavigation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver),
                new IntentFilter("MessageNotification")
        );
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            usersRef = database.getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            UserLastSeen userLastSeen = new UserLastSeen(true, ServerValue.TIMESTAMP);
            usersRef.setValue(userLastSeen).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "onSuccess: SET user online TRUE");
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            usersRef = database.getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            UserLastSeen userLastSeen = new UserLastSeen(false, ServerValue.TIMESTAMP);
            usersRef.setValue(userLastSeen).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "onSuccess: SET user online FALSE");
                }
            });
        }
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

    private void sendNotification(final Intent intents, final String friend_id) {
        int notificationID = 0;
        char[] chars = friend_id.toCharArray();
        for (Character c : chars) {
            notificationID += c - 'a' + 1;
        }

        final int finalNotificationID = notificationID;
        usersReference.document(friend_id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot != null) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        Glide.with(MainActivity.this).asBitmap().load(user.getUserProfilePicUrl()).apply(RequestOptions.circleCropTransform()).into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                String click_action = intents.getStringExtra("click_action");
                                String messageBody = intents.getStringExtra("messageBody");
                                String messageTitle = intents.getStringExtra("messageTitle");
                                Log.d(TAG, "onReceive: " + friend_id);
                                androidx.core.app.RemoteInput remoteInput = new androidx.core.app.RemoteInput.Builder("key_text_reply")
                                        .setLabel("Send message").build();

                                Intent replyIntent = new Intent(MainActivity.this, DirectReplyReceiver.class);
                                replyIntent.putExtra("coming_from", "MainActivity");
                                replyIntent.putExtra("friend_id_mainActivity", friend_id);
                                replyIntent.putExtra("notification_id", finalNotificationID);
                                PendingIntent replyPendingIntent = PendingIntent.getBroadcast(MainActivity.this, finalNotificationID, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

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


                                NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, App.CHANNEL_MESSAGES)
                                        .setSmallIcon(R.mipmap.ic_launcher_foreground)
                                        .setStyle(messagingStyle)
                                        .addAction(replyAction)
                                        .setColor(getResources().getColor(R.color.notification_color, getTheme()))
                                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                                        .setAutoCancel(true);

                                Intent resultIntent = new Intent(click_action);
                                resultIntent.putExtra("friend_id", friend_id);
                                resultIntent.putExtra("coming_from", "MainActivity");
                                resultIntent.putExtra("notification_id", finalNotificationID);
                                PendingIntent resultPendingIntent = PendingIntent.getActivity(MainActivity.this, finalNotificationID, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                                builder.setContentIntent(resultPendingIntent);


                                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);
                                notificationManager.notify(finalNotificationID, builder.build());
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