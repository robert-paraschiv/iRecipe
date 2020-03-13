package com.rokudoz.irecipe.Utils;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.rokudoz.irecipe.App;
import com.rokudoz.irecipe.MainActivity;
import com.rokudoz.irecipe.R;

public class ScheduleNotifReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.putExtra("recipe_id", intent.getStringExtra("recipe_id"));
        String recipe_title = intent.getStringExtra("recipe_title");
        String recipe_category = intent.getStringExtra("recipe_category");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, App.CHANNEL_COMMENTS)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setColor(Color.BLUE)
                .setContentTitle("Time to cook")
                .setContentText("You've planned to cook " + recipe_title + " for " + recipe_category)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(0, builder.build());
    }
}
