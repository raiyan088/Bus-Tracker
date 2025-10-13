package com.rr.bubtbustracker.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

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
            final  String title = getTitle(extras);
            final  String body = getMessage(extras);
            Log.d("BusTrackerLog", "onHandleWork: "+title);
            Log.d("BusTrackerLog", "onHandleWork: "+body);
        } catch (Exception ignored) {}
    }

    private String getTitle(Bundle bundle) {
        try {
            String msg = bundle.getString("gcm.notification.title");
            if (msg == null) {
                for (String key : bundle.keySet()) {
                    if (key.contains("title")) {
                        return bundle.getString(key);
                    }
                }
            }
            return msg;
        } catch (Exception ignored) {}

        return null;
    }

    private String getMessage(Bundle bundle) {
        try {
            String msg = bundle.getString("gcm.notification.body");
            if (msg == null) {
                for (String key : bundle.keySet()) {
                    if (key.contains("body")) {
                        return bundle.getString(key);
                    }
                }
            }
            return msg;
        } catch (Exception ignored) {}

        return null;
    }
}

