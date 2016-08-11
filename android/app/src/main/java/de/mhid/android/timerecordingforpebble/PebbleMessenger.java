package de.mhid.android.timerecordingforpebble;

import android.content.Context;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.Hashtable;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class PebbleMessenger {
    protected final static UUID APP_UUID = UUID.fromString("43157217-0040-4514-a747-0042745874fd");

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
    private PebbleKit.PebbleAckReceiver ackReceiver = null;
    private PebbleKit.PebbleNackReceiver nackReceiver = null;
    private Hashtable<Integer, MessageEvent> eventTable = new Hashtable<>();
    private Context context = null;

    private boolean waitingForAck = false;
    private ReentrantLock waitingForAckLock = new ReentrantLock();

    private int seq = 0;
    private final static int SEQ_MAX = 256;

    private ReentrantLock cacheLock = new ReentrantLock();
    private Hashtable<Integer, PebbleDictionary> sendCache = new Hashtable<>();
    private Hashtable<Integer, Integer> retryCache = new Hashtable<>();
    private LinkedBlockingQueue<Integer> sendQueue = new LinkedBlockingQueue<>();

    public PebbleMessenger() {
    }

    protected void initReceiver(Context ctx) {
        Log.d(this.getClass().getName(), "initReceiver()");
        this.context = ctx;
        if(dataReceiver == null) {
            dataReceiver = new PebbleKit.PebbleDataReceiver(APP_UUID) {
                @Override
                public void receiveData(Context context, int transaction_id, PebbleDictionary dict) {
                    Log.d(this.getClass().getName(), "PKG received: " + transaction_id);
                    // new message received from pebble -> ack this message
                    PebbleKit.sendAckToPebble(context, transaction_id);

                    // process new message
                    receivedMessage(dict);
                }
            };
            PebbleKit.registerReceivedDataHandler(context, dataReceiver);
        }
        if(ackReceiver == null) {
            ackReceiver = new PebbleKit.PebbleAckReceiver(APP_UUID) {
                @Override
                public void receiveAck(Context context, int transaction_id) {
                    Log.d(this.getClass().getName(), "ACK received: " + transaction_id);
                    cacheLock.lock();
                    if(sendCache.containsKey(transaction_id)) {
                        // remove item from cache
                        sendCache.remove(transaction_id);
                    }
                    if(retryCache.containsKey(transaction_id)) {
                        // remove seq from retry cache
                        retryCache.remove(transaction_id);
                    }
                    cacheLock.unlock();

                    sendNextPacketInQueue();
                }
            };
            PebbleKit.registerReceivedAckHandler(context, ackReceiver);
        }
        if(nackReceiver == null) {
            nackReceiver = new PebbleKit.PebbleNackReceiver(APP_UUID) {
                @Override
                public void receiveNack(Context context, int transaction_id) {
                    Log.d(this.getClass().getName(), "NACK received: " + transaction_id);
                    cacheLock.lock();
                    int retry = 0;
                    if(retryCache.containsKey(transaction_id)) {
                        retry = retryCache.get(transaction_id);
                    }
                    retry++;
                    retryCache.put(transaction_id, retry);

                    if(retry >= 3) {
                        if(sendCache.containsKey(transaction_id)) {
                            // remove item from cache
                            sendCache.remove(transaction_id);
                        }
                        if(retryCache.containsKey(transaction_id)) {
                            // remove seq from retry cache
                            retryCache.remove(transaction_id);
                        }
                    } else {
                        sendQueue.add(transaction_id);
                    }
                    cacheLock.unlock();

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }

                    sendNextPacketInQueue();
                }
            };
            PebbleKit.registerReceivedNackHandler(context, nackReceiver);
        }
    }

    private void sendNextPacketInQueue() {
        cacheLock.lock();
        PebbleDictionary nextDict = null;
        Integer nextSeq = null;
        while(true) {
            // fetch next item
            nextSeq = sendQueue.poll();
            if (nextSeq != null && sendCache.containsKey(nextSeq)) {
                nextDict = sendCache.get(nextSeq);
                break;
            } else if(nextSeq == null) {
                break;
            }
        }
        cacheLock.unlock();

        if(nextDict == null) {
            // queue is empty
            waitingForAckLock.lock();
            waitingForAck = false;
            waitingForAckLock.unlock();
        } else {
            // send next packet in queue
            PebbleKit.sendDataToPebbleWithTransactionId(context, APP_UUID, nextDict, nextSeq);
        }
    }

    protected void deinitReceiver() {
        if(dataReceiver != null) {
            context.unregisterReceiver(dataReceiver);
        }
        dataReceiver = null;
        if(ackReceiver != null) {
            context.unregisterReceiver(ackReceiver);
        }
        ackReceiver = null;
        if(nackReceiver != null) {
            context.unregisterReceiver(nackReceiver);
        }
        nackReceiver = null;
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

        cacheLock.lock();
        // calculate next seq
        seq = (seq + 1) % SEQ_MAX;
        int mySeq = seq;
        // put data in send cache
        sendCache.put(seq, dict);
        cacheLock.unlock();

        waitingForAckLock.lock();
        boolean sendOut = !waitingForAck;
        waitingForAck = true;
        waitingForAckLock.unlock();

        if(sendOut) {
            // send out directly
            PebbleKit.sendDataToPebbleWithTransactionId(context, APP_UUID, dict, mySeq);
        } else {
            // enqueue
            sendQueue.add(mySeq);
        }
    }

    protected void registerMessageEvent(int cmdID, MessageEvent evt) {
        // register event for a command
        eventTable.put(cmdID, evt);
    }


    interface MessageEvent {
        public abstract void messageReceived(PebbleMessenger msgr, PebbleDictionary dict);
    }
}
