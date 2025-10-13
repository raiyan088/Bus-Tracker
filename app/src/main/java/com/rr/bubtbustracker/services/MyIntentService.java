package com.rr.bubtbustracker.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

public class MyIntentService extends JobIntentService {

    public final static int JOB_ID = 9099;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, MyIntentService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        try {
            final Bundle extras = intent.getExtras();
            if (extras != null) {
                final  String title = extras.getString("gcm.notification.title");
                final  String body = extras.getString("gcm.notification.body");
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), title + ": " + body, Toast.LENGTH_LONG).show());
                Log.d("BusTrackerLog", "onHandleWork: "+title);
                Log.d("BusTrackerLog", "onHandleWork: "+body);
            }
        } catch (Exception ignored) {}
    }
}

