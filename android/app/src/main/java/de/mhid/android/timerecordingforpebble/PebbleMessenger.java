package de.mhid.android.timerecordingforpebble;

import android.content.Context;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.Hashtable;
import java.util.UUID;

public class PebbleMessenger {
    private final UUID APP_UUID = UUID.fromString("43157217-0040-4514-a747-0042745874fd");

    protected final static byte MESSAGE_COLOR_BLACK = 0;
    protected final static byte MESSAGE_COLOR_WHITE = 1;
    protected final static byte MESSAGE_COLOR_RED = 2;
    protected final static byte MESSAGE_COLOR_GREEN = 3;
    protected final static byte MESSAGE_COLOR_BLUE = 4;
    protected final static byte MESSAGE_COLOR_YELLOW = 5;
    protected final static byte MESSAGE_COLOR_ORANGE = 6;
    protected final static byte MESSAGE_COLOR_DARKGRAY = 7;
    protected final static byte MESSAGE_COLOR_LIGHTGRAY = 8;

    protected final static int MESSAGE_CMD_STATUS_REQUEST = 1;
    protected final static int MESSAGE_CMD_STATUS_RESPONSE = 2;
    protected final static int MESSAGE_CMD_ACTION_PUNCH = 3;

    protected final static int MESSAGE_KEY_CMD = 1;

    protected final static int MESSAGE_KEY_STATUS_RESPONSE_STATUS_CHECKED_IN = 2;
    protected final static int MESSAGE_KEY_STATUS_RESPONSE_STATUS_CONTENT_TEXT = 3;
    protected final static int MESSAGE_KEY_STATUS_RESPONSE_STATUS_CONTENT_COLOR = 4;

    protected final static int MESSAGE_KEY_STATUS_RESPONSE_FACE_ID = 5;
    protected final static int MESSAGE_KEY_STATUS_RESPONSE_FACE_TEXT = 6;
    protected final static int MESSAGE_KEY_STATUS_RESPONSE_FACE_COLOR = 7;
    protected final static int MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME1_TEXT = 8;
    protected final static int MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME1_COLOR = 9;
    protected final static int MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME2_TEXT = 10;
    protected final static int MESSAGE_KEY_STATUS_RESPONSE_FACE_TIME2_COLOR = 11;

    private PebbleKit.PebbleDataReceiver dataReceiver = null;
    private Hashtable<Integer, MessageEvent> eventTable = new Hashtable<>();
    private Context context = null;

    public PebbleMessenger() {
    }

    protected void initReceiver(Context ctx) {
        Log.d(this.getClass().getName(), "initReceiver()");
        if(dataReceiver == null) {
            this.context = ctx;
            dataReceiver = new PebbleKit.PebbleDataReceiver(APP_UUID) {
                @Override
                public void receiveData(Context context, int transaction_id, PebbleDictionary dict) {
                    // new message received from pebble -> ack this message
                    PebbleKit.sendAckToPebble(context, transaction_id);

                    // process new message
                    receivedMessage(dict);
                }
            };

            PebbleKit.registerReceivedDataHandler(context, dataReceiver);
        }
    }

    protected void deinitReceiver() {
        if(dataReceiver != null) {
            context.unregisterReceiver(dataReceiver);
        }
        dataReceiver = null;
    }

    private void receivedMessage(PebbleDictionary dict) {
        // received message -> parse it
        Long cmdValue = dict.getUnsignedIntegerAsLong(MESSAGE_KEY_CMD);
        if(cmdValue != null) {
            int cmd = cmdValue.intValue();
            // check if command is registered?
            if(eventTable.containsKey(cmd)) {
                // trigger message event
                MessageEvent event = eventTable.get(cmd);
                if(event != null) eventTable.get(cmd).messageReceived(this, dict);
            } else {
                Log.e(this.getClass().getName(), "Message-ID/Command " + cmd + " not found.");
            }
        }
    }

    protected void sendMessage(int cmd, PebbleDictionary dict) {
        // create a message for pebble and send it
        if(dict == null) dict = new PebbleDictionary();
        dict.addUint8(MESSAGE_KEY_CMD, (byte)cmd);

        PebbleKit.sendDataToPebble(context, APP_UUID, dict);
    }

    protected void registerMessageEvent(int cmdID, MessageEvent evt) {
        // register event for a command
        eventTable.put(cmdID, evt);
    }


    interface MessageEvent {
        public abstract void messageReceived(PebbleMessenger msgr, PebbleDictionary dict);
    }
}
