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

import java.util.ArrayList;

public class BroadcastReceiverOnTimeRecDataChanged extends BroadcastReceiver {
    private ArrayList<TimeRecConnector.DataChangeEventHandler> eventHandlers = new ArrayList<>();

    public BroadcastReceiverOnTimeRecDataChanged() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(this.getClass().getName(), "onReceive()");

        for(TimeRecConnector.DataChangeEventHandler eventHandler : eventHandlers) {
            if(eventHandler == null) continue;
            eventHandler.onDataChanged();
        }
    }

    protected void registerDataChangedEvent(TimeRecConnector.DataChangeEventHandler eventHandler) {
        if(eventHandler == null) return;

        eventHandlers.add(eventHandler);
    }

    protected void unregisterDataChangedEvent(TimeRecConnector.DataChangeEventHandler eventHandler) {
        if(eventHandler == null) return;

        eventHandlers.remove(eventHandler);
    }
}
