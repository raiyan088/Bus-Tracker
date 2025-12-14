package com.rr.bubtbustracker.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.rr.bubtbustracker.services.MyIntentService;

public class RealTimeNotification extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        MyIntentService.enqueueWork(context, intent);
    }
}
