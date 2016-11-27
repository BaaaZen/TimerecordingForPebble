/*
  Copyright 2016 Mirko Hansen

  This file is part of Timerecording for Pebble
  http://www.github.com/BaaaZen/TimerecordingForPebble

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

*/

package de.mhid.android.timerecordingforpebble;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.io.IOException;
import java.util.Date;
import java.util.Random;

import nl.palolem.timeline.Timeline;
import nl.palolem.timeline.api.pin.Icon;
import nl.palolem.timeline.api.pin.Pin;
import nl.palolem.timeline.api.pin.layout.GenericPin;
import nl.palolem.timeline.util.PebbleException;

public class PebbleService extends Service {
    private final static String SPREF_KEY_CURRENT_ID = "CURRENT_ID";
    private final static String SPREF_KEY_CURRENT_CTR = "CURRENT_CTR";
    private final static String SPREF_KEY_TIMELINE_PIN_TIMESTAMP = "TIMELINE_PIN_TIMESTAMP";
    private final static String SPREF_KEY_TIMELINE_TOKEN = "TIMELINE_TOKEN";
    private final static String SPREF_KEY_UNIQUE_ID = "UNIQUE_ID";

    private PebbleMessenger messenger = new PebbleMessenger();
    private TimeRecConnector timeRec = null;
    private boolean firstTimelineUpdate = true;
    private String pebbleTimelineToken = null;

    private static long lastStatusUpdateTimestamp = 0;

    public PebbleService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        /* a little hack to make http requests in main thread available */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        /* init time rec connector */
        timeRec = new TimeRecConnector(this);

        /* register pebble message events */
        initEvents();

        /* init pebble messenger */
        messenger.initReceiver(this);

        Log.d(this.getClass().getName(), "onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean isConnected = PebbleKit.isWatchConnected(this);
        Log.d(this.getClass().getName(), "onStartCommand() - Pebble connected? " + isConnected);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(this.getClass().getName(), "onDestroy()");

        /* destroy pebble messenger */
        messenger.deinitReceiver();

        /* destroy connector for time recording */
        if(timeRec != null) {
            timeRec.destroy();
            timeRec = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean isRedundantStatusUpdate() {
        long currentTS = System.currentTimeMillis();
        try {
            /* invalid last timestamp? */
            if(lastStatusUpdateTimestamp <= 0 || lastStatusUpdateTimestamp > currentTS) return false;
            /* last time called was less than 100ms ago? */
            if(currentTS - lastStatusUpdateTimestamp < 100) return true;

            return false;
        } finally {
            lastStatusUpdateTimestamp = currentTS;
        }

    }

    private void initEvents() {
        Log.d(this.getClass().getName(), "initEvents()");
        /* request status */
        messenger.registerMessageEvent(PebbleMessenger.MESSAGE_CMD_STATUS_REQUEST, new PebbleMessenger.MessageEvent() {
            @Override
            public void messageReceived(PebbleMessenger msgr, PebbleDictionary dict) {
                // send current status to pebble
                evtRequestStatus(true, firstTimelineUpdate, false);
                firstTimelineUpdate = false;
            }
        });
        /* trigger punch */
        messenger.registerMessageEvent(PebbleMessenger.MESSAGE_CMD_ACTION_PUNCH, new PebbleMessenger.MessageEvent() {
            @Override
            public void messageReceived(PebbleMessenger msgr, PebbleDictionary dict) {
                // punch
                evtActionPunch();
                // after that send current status to pebble
                evtRequestStatus(true, true, true);
            }
        });
        /* received timeline token */
        messenger.registerMessageEvent(PebbleMessenger.MESSAGE_CMD_RESPONSE_TL_TOKEN, new PebbleMessenger.MessageEvent() {
            @Override
            public void messageReceived(PebbleMessenger msgr, PebbleDictionary dict) {
                if(dict.contains(PebbleMessenger.MESSAGE_KEY_TL_TOKEN)) {
                    pebbleTimelineToken = dict.getString(PebbleMessenger.MESSAGE_KEY_TL_TOKEN);
                }
            }
        });

        timeRec.registerOnDataChangeEvent(new TimeRecConnector.DataChangeEventHandler() {
            @Override
            public void onDataChanged() {
                /* force update timeline after data changed */
                evtRequestStatus(true, true, true);
            }
        });
    }

    private void evtActionPunch() {
        /* event from pebble: punch */
        Log.d(this.getClass().getName(), "evtActionPunch()");
        timeRec.timeRecActionPunch(null);
    }

    private void evtRequestStatus(final boolean sendStatusMessage, final boolean updateTimeline, final boolean forceUpdateTimeline) {
        /* event from pebble: request status */
        Log.d(this.getClass().getName(), "evtRequestStatus()");

        if(isRedundantStatusUpdate()) {
            Log.d(this.getClass().getName(), "evtRequestStatus(): !!! WARNING: redundant status update -> ignore request !!!");
            return;
        }

        TimeRecConnector.MessageEvent recvHandler = new TimeRecConnector.MessageEvent() {
            @Override
            public void messageReceived(Bundle bundle) {
                Bundle result = bundle.getBundle("com.dynamicg.timerecording.RESULT");

                if(sendStatusMessage) genResponseStatus(result);
                if(updateTimeline) genResponseTimeline(result, forceUpdateTimeline);
            }
        };
        timeRec.timeRecGetInfo(recvHandler);
    }

    private byte getColorForTimevalue(String tv) {
        if(tv == null) return PebbleMessenger.MESSAGE_COLOR_BLACK;
        if(tv.length() < 1) return PebbleMessenger.MESSAGE_COLOR_BLACK;

        if(tv.charAt(0) == '-')
            return PebbleMessenger.MESSAGE_COLOR_RED;
        else if(tv.charAt(0) == '+')
            return PebbleMessenger.MESSAGE_COLOR_GREEN;
        else
            return PebbleMessenger.MESSAGE_COLOR_BLACK;
    }

    private String getStringFromBundle(Bundle bundle, String key) {
        String str = bundle.getString(key);
        return str != null ? str : "";
    }

    private long getLongFromBundle(Bundle bundle, String key) {
        return bundle.getLong(key, 0);
    }

    private synchronized long getNextTimelineCtr() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        long currentCtr = sharedPrefs.getLong(SPREF_KEY_CURRENT_CTR, 0);

        currentCtr++;

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putLong(SPREF_KEY_CURRENT_CTR, currentCtr);
        editor.commit();

        return currentCtr;
    }

    private void genResponseTimeline(Bundle bundle, boolean forceUpdateTimeline) {
        Log.d(this.getClass().getName(), "genResponseTimeline()");

        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        long currentId = sharedPrefs.getLong(SPREF_KEY_CURRENT_ID, 1);
        String uniqueID = sharedPrefs.getString(SPREF_KEY_UNIQUE_ID, "");
        long timelinePinTimestamp = sharedPrefs.getLong(SPREF_KEY_TIMELINE_PIN_TIMESTAMP, 0);
        String timelineToken = sharedPrefs.getString(SPREF_KEY_TIMELINE_TOKEN, null);

        if(pebbleTimelineToken != null && !pebbleTimelineToken.isEmpty()) {
            /* use freshly fetched timeline token */
            timelineToken = pebbleTimelineToken;
        } else if(timelineToken == null || timelineToken.isEmpty()) {
            /* we don't have a timeline token, so we can't continue */
            return;
        }

        if(uniqueID == null || uniqueID.isEmpty()) {
            /* generate a unique id for pebble timeline api */
            Random r = new Random();
            uniqueID = Integer.toString(r.nextInt(Integer.MAX_VALUE));
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(SPREF_KEY_UNIQUE_ID, uniqueID);
            editor.commit();
        }

        boolean updateTimelinePin = false;
        boolean deleteTimelinePin = false;

        if(bundle == null) {
            /* no data available -> delete timeline pin */
            deleteTimelinePin = true;
        } else {
            /* fetch target reached timestamp from bundle */
            long timestamp = getLongFromBundle(bundle, TimeRecConnector.DAY_TARGET_REACHED_MILLIS);
            if(timestamp > 0) {
                /* we have a valid timestamp! */
                if(timestamp != timelinePinTimestamp) {
                    /* we need to update timeline pin */
                    updateTimelinePin = true;
                    timelinePinTimestamp = timestamp;
                }
            } else {
                /* no timestamp set -> delete timeline pin */
                deleteTimelinePin = true;
            }
        }

        final boolean fDeleteTimelinePin = deleteTimelinePin;
        final boolean fUpdateTimelinePin = updateTimelinePin;
        final long fCurrentId = currentId;
        final String fUniqueID = uniqueID;
        final String fTimelineToken = timelineToken;
        final long fTimelinePinTimestamp = timelinePinTimestamp;

        Runnable r = new Runnable() {
            @Override
            public void run() {
                long currentId = fCurrentId;
                String uniqueID = fUniqueID;
                String timelineToken = fTimelineToken;
                long timelinePinTimestamp = fTimelinePinTimestamp;

                /* boolean updateSharedPrefs = false; */
                if(fDeleteTimelinePin && currentId > 0) {
                    String timelineId = "TR4P-" + uniqueID + "-" + Long.toString(currentId);

                    /* delete timeline pin */
                    Pin pin = new Pin.Builder().id(timelineId).build();
                    try {
                        Timeline.deletePin(timelineToken, pin);

                        timelinePinTimestamp = 0;
                        currentId = 0;

                        Log.d(this.getClass().getName(), "pin " + timelineId + " deleted successfully");
                    } catch (IOException e) {
                        /* failed contacting server -> do nothing and retry later */
                        Log.e(this.getClass().getName(), "error deleting pin", e);
                    } catch (PebbleException e) {
                        /* other error -> maybe pin already deleted? */

                        timelinePinTimestamp = 0;
                        currentId = 0;

                        Log.e(this.getClass().getName(), "error deleting pin", e);
                    }
                } else if(fUpdateTimelinePin && timelinePinTimestamp > 0) {
                    if(currentId == 0) {
                        currentId = getNextTimelineCtr();
                    }

                    String timelineId = "TR4P-" + uniqueID + "-" + Long.toString(currentId);

                    /* create/update timeline pin */
                    Pin pin = new Pin.Builder().id(timelineId)
                            .time(new Date(timelinePinTimestamp))
                            .layout(new GenericPin.Builder()
                                .title(getString(R.string.tr4p_pebble_timeline_pin_target_reached))
                                .tinyIcon(Icon.RESULT_SENT)
                                .build())
                            .build();
                    try {
                        Timeline.sendPin(timelineToken, pin);

                        Log.d(this.getClass().getName(), "pin " + timelineId + " created/updated successfully");
                    } catch (IOException e) {
                        /* failed contacting server -> retry creating/updating later */
                        timelinePinTimestamp = 0;

                        Log.e(this.getClass().getName(), "error creating/updating pin", e);
                    } catch (PebbleException e) {
                        /* other error -> retry later */
                        timelinePinTimestamp = 0;

                        Log.e(this.getClass().getName(), "error creating/updating pin", e);
                    }
                }

                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putLong(SPREF_KEY_CURRENT_ID, currentId);
                editor.putLong(SPREF_KEY_TIMELINE_PIN_TIMESTAMP, timelinePinTimestamp);
                editor.putString(SPREF_KEY_TIMELINE_TOKEN, timelineToken);
                editor.commit();
            }
        };

        /* asynchronous update timeline */
        new AsyncExecTask().doInBackground(r);
    }

    private void genResponseStatus(Bundle bundle) {
        if(bundle == null) {
            // we didn't receive any data from time recording app
            // -> clear all data on pebble
            PebbleDictionary noDataDict = new PebbleDictionary();
            noDataDict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_CLEARALL, (byte)1);

            // -> send warning state
            String status = "(" + getString(R.string.tr4p_pebble_no_data) + ")";
            noDataDict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_STATUS_CHECKED_IN, (byte)0);
            noDataDict.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_STATUS_CONTENT_TEXT, status);
            noDataDict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_STATUS_CONTENT_COLOR, PebbleMessenger.MESSAGE_COLOR_RED);

            messenger.sendMessageToPebble(PebbleMessenger.MESSAGE_CMD_STATUS_RESPONSE, noDataDict);

            return;
        }

        /* create first package: checked in and main description */
        PebbleDictionary dict1 = new PebbleDictionary();

        boolean commonCheckedIn = bundle.getBoolean(TimeRecConnector.CHECKED_IN);
        String commonStatus = "";
        byte commonColor = PebbleMessenger.MESSAGE_COLOR_BLACK;
        if(commonCheckedIn) {
            /* user is checked in */
            String taskName = bundle.getString(TimeRecConnector.TASK);
            if(taskName == null || taskName.isEmpty())
                taskName = "(" + getString(R.string.tr4p_pebble_no_task) + ")";
            commonStatus = getString(R.string.tr4p_pebble_task) + ": " + taskName;
            commonColor = PebbleMessenger.MESSAGE_COLOR_BLACK;
        } else {
            /* not checked in */
            commonStatus = "(" + getString(R.string.tr4p_pebble_checked_out) + ")";
            commonColor = PebbleMessenger.MESSAGE_COLOR_LIGHTGRAY;
        }
        dict1.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_STATUS_CHECKED_IN, commonCheckedIn ? (byte)1 : 0);
        dict1.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_STATUS_CONTENT_TEXT, commonStatus);
        dict1.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_STATUS_CONTENT_COLOR, commonColor);
        /* send first package */
        messenger.sendMessageToPebble(PebbleMessenger.MESSAGE_CMD_STATUS_RESPONSE, dict1);

        String time;
        /* create face 1: work time day/week */
        PebbleDictionary dict2 = new PebbleDictionary();
        dict2.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_ID, (byte)0);

        dict2.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TEXT, getString(R.string.tr4p_pebble_face_title_workingtime_day_week));
        dict2.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_COLOR, PebbleMessenger.MESSAGE_COLOR_BLACK);

        time = getStringFromBundle(bundle, TimeRecConnector.TIME_TOTAL_FORMATTED);
        dict2.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME1_TEXT, time);
        dict2.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME1_COLOR, PebbleMessenger.MESSAGE_COLOR_BLACK);

        time = getStringFromBundle(bundle, TimeRecConnector.TIME_TOTAL_WEEK_FORMATTED);
        dict2.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME2_TEXT, time);
        dict2.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME2_COLOR, PebbleMessenger.MESSAGE_COLOR_BLACK);
        /* send face 1 */
        messenger.sendMessageToPebble(PebbleMessenger.MESSAGE_CMD_STATUS_RESPONSE, dict2);

        /* create face 2: daily time left */
        PebbleDictionary dict3 = new PebbleDictionary();
        dict3.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_ID, (byte)1);

        dict3.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TEXT, getString(R.string.tr4p_pebble_face_title_delta_day));
        dict3.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_COLOR, PebbleMessenger.MESSAGE_COLOR_BLACK);

        time = getStringFromBundle(bundle, TimeRecConnector.DELTA_DAY_FORMATTED);
        dict3.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME1_TEXT, time);
        dict3.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME1_COLOR, getColorForTimevalue(time));

        time = getStringFromBundle(bundle, TimeRecConnector.DAY_TARGET_REACHED_FORMATTED_SHORT);
        dict3.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME2_TEXT, time);
        dict3.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME2_COLOR, PebbleMessenger.MESSAGE_COLOR_BLACK);
        /* send face 2 */
        messenger.sendMessageToPebble(PebbleMessenger.MESSAGE_CMD_STATUS_RESPONSE, dict3);

        /* create face 3: weekly time left */
        PebbleDictionary dict4 = new PebbleDictionary();
        dict4.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_ID, (byte)2);

        dict4.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TEXT, getString(R.string.tr4p_pebble_face_title_delta_week));
        dict4.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_COLOR, PebbleMessenger.MESSAGE_COLOR_BLACK);

        time = getStringFromBundle(bundle, TimeRecConnector.WTD_DELTA_DAY_FORMATTED);
        dict4.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME1_TEXT, time);
        dict4.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME1_COLOR, getColorForTimevalue(time));

        time = getStringFromBundle(bundle, TimeRecConnector.DELTA_WEEK_FORMATTED);
        dict4.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME2_TEXT, time);
        dict4.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME2_COLOR, getColorForTimevalue(time));
        /* send face 3 */
        messenger.sendMessageToPebble(PebbleMessenger.MESSAGE_CMD_STATUS_RESPONSE, dict4);
    }


    private class AsyncExecTask extends AsyncTask<Runnable, Void, Void> {
        @Override
        protected Void doInBackground(Runnable... runnables) {
            for(Runnable r : runnables) {
                r.run();
            }

            return null;
        }
    }
}
