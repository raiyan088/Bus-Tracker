package com.rr.bubtbustracker.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.rr.bubtbustracker.R;
import com.rr.bubtbustracker.activity.DashboardActivity;

public class MyIntentService extends JobIntentService {

    public final static int JOB_ID = 3122;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, MyIntentService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        try {
            final Bundle extras = intent.getExtras();
            if (extras != null) {
                final String title = extras.getString("title");
                final String body = extras.getString("body");
                Log.d("MyBusTracker", "onHandleWork: "+title+" "+body);
                if (title != null && body != null && !body.isEmpty() && !title.isEmpty()) {
                    showNotification(this, title, body);
                }
            }
        } catch (Exception ignored) {}
    }

    private void showNotification(@NonNull Context context, @NonNull String title, @NonNull String body) {
        try {
            String CHANNEL_ID = "BusTrackerChannel";
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Bus Tracker Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setSound(soundUri, new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                channel.setDescription("Shows bus status updates");
                manager.createNotificationChannel(channel);
            }

            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


            NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setSmallIcon(R.mipmap.app_logo)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setVibrate(new long[]{0, 250, 250, 250});

            int notificationId = (int) System.currentTimeMillis();
            manager.notify(notificationId, notification.build());
        } catch (Exception ignored) {}
    }

}
