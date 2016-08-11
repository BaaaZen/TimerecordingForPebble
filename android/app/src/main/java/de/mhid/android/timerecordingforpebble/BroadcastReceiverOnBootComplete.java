/*
  Copyright 2016 Mirko Hansen

  This file is part of Timerecording for Pebble
  http://www.github.com/BaaaZen/TimerecordingForPebble

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

*/

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
