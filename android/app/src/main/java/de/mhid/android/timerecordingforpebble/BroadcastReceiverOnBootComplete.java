package de.mhid.android.timerecordingforpebble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BroadcastReceiverOnBootComplete extends BroadcastReceiver {
    public BroadcastReceiverOnBootComplete() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(this.getClass().getName(), "onReceive()");
        /* trigger pebble background service */
        Intent serviceIntent = new Intent(context, PebbleService.class);
        context.startService(serviceIntent);
    }
}
