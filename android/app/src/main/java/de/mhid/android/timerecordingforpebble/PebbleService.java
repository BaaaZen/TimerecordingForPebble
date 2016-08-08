package de.mhid.android.timerecordingforpebble;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

public class PebbleService extends Service {
    private PebbleMessenger messenger = new PebbleMessenger();
    private TimeRecConnector timeRec = null;

    public PebbleService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initEvents();
        timeRec = new TimeRecConnector(this);
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
        messenger.deinitReceiver();
        timeRec = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initEvents() {
        Log.d(this.getClass().getName(), "initEvents()");
        /* request status */
        messenger.registerMessageEvent(PebbleMessenger.MESSAGE_CMD_STATUS_REQUEST, new PebbleMessenger.MessageEvent() {
            @Override
            public void messageReceived(PebbleMessenger msgr, PebbleDictionary dict) {
                // send current status to pebble
                evtRequestStatus(msgr, dict);
            }
        });
        /* trigger punch */
        messenger.registerMessageEvent(PebbleMessenger.MESSAGE_CMD_ACTION_PUNCH, new PebbleMessenger.MessageEvent() {
            @Override
            public void messageReceived(PebbleMessenger msgr, PebbleDictionary dict) {
                // punch
                evtActionPunch(msgr, dict);
                // after that send current status to pebble
                evtRequestStatus(msgr, dict);
            }
        });
    }

    private void evtActionPunch(PebbleMessenger msgr, PebbleDictionary dict) {
        Log.d(this.getClass().getName(), "evtActionPunch()");
        timeRec.timeRecActionPunch(null);
    }

    private void evtRequestStatus(PebbleMessenger msgr, PebbleDictionary dict) {
        Log.d(this.getClass().getName(), "evtRequestStatus()");
        TimeRecConnector.MessageEvent recvHandler = new TimeRecConnector.MessageEvent() {
            @Override
            public void messageReceived(Bundle bundle) {
                Bundle result = bundle.getBundle("com.dynamicg.timerecording.RESULT");

                genResponseStatus(result);
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

    private void genResponseStatus(Bundle bundle) {
        if(bundle == null) return;

        /* create first package: checked in and main description */
        PebbleDictionary dict = new PebbleDictionary();

        boolean commonCheckedIn = bundle.getBoolean(TimeRecConnector.CHECKED_IN);
        String commonStatus = "";
        byte commonColor = PebbleMessenger.MESSAGE_COLOR_BLACK;
        if(commonCheckedIn) {
            /* user is checked in */
            String taskName = bundle.getString(TimeRecConnector.TASK);
            if(taskName == null || taskName.isEmpty())
                taskName = "(unbekannt)";
            commonStatus = "Task: " + taskName;
            commonColor = PebbleMessenger.MESSAGE_COLOR_BLACK;
        } else {
            /* not checked in */
            commonStatus = "(ausgestempelt)";
            commonColor = PebbleMessenger.MESSAGE_COLOR_LIGHTGRAY;
        }
        dict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_STATUS_CHECKED_IN, commonCheckedIn ? (byte)1 : 0);
        dict.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_STATUS_CONTENT_TEXT, commonStatus);
        dict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_STATUS_CONTENT_COLOR, commonColor);
        /* send first package */
        messenger.sendMessage(PebbleMessenger.MESSAGE_CMD_STATUS_RESPONSE, dict);

        String time;
        /* create face 1: work time day/week */
        dict = new PebbleDictionary();
        dict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_ID, (byte)0);

        dict.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TEXT, "Arbeitsz. Tag/Woche");
        dict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_COLOR, PebbleMessenger.MESSAGE_COLOR_BLACK);

        dict.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME1_TEXT, bundle.getString(TimeRecConnector.TIME_TOTAL_FORMATTED));
        dict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME1_COLOR, PebbleMessenger.MESSAGE_COLOR_BLACK);

        dict.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME2_TEXT, bundle.getString(TimeRecConnector.TIME_TOTAL_WEEK_FORMATTED));
        dict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME2_COLOR, PebbleMessenger.MESSAGE_COLOR_BLACK);
        /* send face 1 */
        messenger.sendMessage(PebbleMessenger.MESSAGE_CMD_STATUS_RESPONSE, dict);

        /* create face 2: daily time left */
        dict = new PebbleDictionary();
        dict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_ID, (byte)1);

        dict.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TEXT, "Delta Tag");
        dict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_COLOR, PebbleMessenger.MESSAGE_COLOR_BLACK);

        time = bundle.getString(TimeRecConnector.DELTA_DAY_FORMATTED);
        dict.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME1_TEXT, time);
        dict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME1_COLOR, getColorForTimevalue(time));

        time = bundle.getString(TimeRecConnector.DAY_TARGET_REACHED_FORMATTED_SHORT);
        if(time == null) time = "";
        dict.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME2_TEXT, time);
        dict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME2_COLOR, PebbleMessenger.MESSAGE_COLOR_BLACK);
        /* send face 2 */
        messenger.sendMessage(PebbleMessenger.MESSAGE_CMD_STATUS_RESPONSE, dict);

        /* create face 3: weekly time left */
        dict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_ID, (byte)2);

        dict.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TEXT, "Delta Woche");
        dict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_COLOR, PebbleMessenger.MESSAGE_COLOR_BLACK);

        time = bundle.getString(TimeRecConnector.WTD_DELTA_DAY_FORMATTED);
        dict.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME1_TEXT, time);
        dict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME1_COLOR, getColorForTimevalue(time));

        time = bundle.getString(TimeRecConnector.DELTA_WEEK_FORMATTED);
        dict.addString(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME2_TEXT, time);
        dict.addUint8(PebbleMessenger.MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME2_COLOR, getColorForTimevalue(time));
        /* send face 3 */
        messenger.sendMessage(PebbleMessenger.MESSAGE_CMD_STATUS_RESPONSE, dict);
    }

}
