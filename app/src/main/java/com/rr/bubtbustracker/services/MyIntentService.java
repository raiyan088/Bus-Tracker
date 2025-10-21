package com.rr.bubtbustracker.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.content.ContextCompat;

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
                final String title = extras.getString("gcm.notification.title");
                final String body = extras.getString("gcm.notification.body");
                if (title != null && body != null && !body.isEmpty() && !title.isEmpty()) {
                    Intent intentSend = new Intent(this, MyNotificationService.class);
                    intentSend.putExtra("title", title);
                    intentSend.putExtra("body", body);
                    intentSend.putExtra("latitude", intent.getDoubleExtra("latitude", 0));
                    intentSend.putExtra("longitude", intent.getDoubleExtra("longitude", 0));
                    ContextCompat.startForegroundService(this, intentSend);
                }
            }
        } catch (Exception ignored) {}
    }
}
