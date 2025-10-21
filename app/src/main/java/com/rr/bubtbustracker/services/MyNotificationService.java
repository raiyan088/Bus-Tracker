package com.rr.bubtbustracker.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.rr.bubtbustracker.R;

public class MyNotificationService extends Service {

    private static final int NOTIFICATION_ID = 3122;
    private static final String CHANNEL_ID = "BusTrackerChannel";
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            String title = intent.getStringExtra("title");
            String body = intent.getStringExtra("body");
            double busLat = intent.getDoubleExtra("latitude", 0);
            double busLng = intent.getDoubleExtra("longitude", 0);

            if (busLat > 10 && busLng > 10 && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        double userLat = location.getLatitude();
                        double userLng = location.getLongitude();

                        float[] results = new float[1];
                        Location.distanceBetween(userLat, userLng, busLat, busLng, results);
                        float distanceMeters = results[0];

                        String distanceText = String.format("Bus is %.0f meters away", distanceMeters);

                        showNotification(title, distanceText);
                    } else {
                        showNotification(title, body);
                    }
                });
            } else {
                showNotification(title, body);
            }
        } catch (Exception e) {}

        return START_STICKY;
    }

    public void showNotification(String title, String body) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title != null ? title : "Bus Tracker")
                .setContentText(body != null ? body : "Bus is near your stop")
                .setSmallIcon(R.mipmap.app_logo)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(new long[]{0, 250, 250, 250});

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification.build());
        }
    }

    private void createNotificationChannel() {
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
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
