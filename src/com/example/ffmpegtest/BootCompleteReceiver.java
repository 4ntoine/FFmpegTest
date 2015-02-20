package com.example.ffmpegtest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receiver to
 */
public class BootCompleteReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // don't run in 1 camera mode (Motorola)
//        if (HWRecorderActivity.VIDEO_STREAMS_COUNT == 1)
//            return;

        if (true)
            return;

        Log.d(getClass().getSimpleName(), "intent received");
        Intent myIntent = new Intent(context, HWRecorderActivity.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(myIntent);
    }

}
