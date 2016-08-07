package de.mhid.android.timerecordingforpebble;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

public class TimeRecConnector {
    private static final String EXTRA_FLAG = "com.dynamicg.timerecording.FLAGS";

    private static final int FLAG_WEEKLY_DELTA = 1;
    private static final int FLAG_MONTHLY_DELTA = 2;
    private static final int FLAG_FOR_WEAR_NOTIFICATION = 4;

    protected static final String TIME_TOTAL_SECS = "TIME_TOTAL_SECS";
    protected static final String TIME_TOTAL_FORMATTED = "TIME_TOTAL_FORMATTED";
    protected static final String AMOUNT_TOTAL = "AMOUNT_TOTAL";
    protected static final String AMOUNT_TOTAL_FORMATTED = "AMOUNT_TOTAL_FORMATTED";
    protected static final String VALUE1_TOTAL = "VALUE1_TOTAL"; //double
    protected static final String VALUE2_TOTAL = "VALUE2_TOTAL"; //double
    protected static final String DAY_COMMENT = "DAY_COMMENT";
    protected static final String DELTA_DAY_SECS = "DELTA_DAY_SECS";
    protected static final String DELTA_DAY_FORMATTED = "DELTA_DAY_FORMATTED";
    protected static final String DELTA_WEEK_SECS = "DELTA_WEEK_SECS";
    protected static final String DELTA_WEEK_FORMATTED = "DELTA_WEEK_FORMATTED";
    protected static final String DAY_TARGET_REACHED = "DAY_TARGET_REACHED";
    protected static final String DAY_MAX_TIME_THRESHOLD = "DAY_MAX_TIME_THRESHOLD";
    protected static final String WEEK_TARGET_REACHED = "WEEK_TARGET_REACHED";
    protected static final String WEEK_MAX_TIME_THRESHOLD = "WEEK_MAX_TIME_THRESHOLD";
    protected static final String CHECKED_IN = "CHECKED_IN"; // boolean
    protected static final String NUM_WORK_UNITS = "NUM_WORK_UNITS";
    protected static final String TASK_ID = "TASK_ID";
    protected static final String TASK = "TASK";
    protected static final String CUSTOMER = "CUSTOMER";
    protected static final String CHECK_IN_TIME = "CHECK_IN_TIME";
    protected static final String CHECK_OUT_TIME = "CHECK_OUT_TIME";
    protected static final String WORK_UNIT_COMMENT = "WORK_UNIT_COMMENT";
    protected static final String TASK_EXTRA1 = "TASK_EXTRA1";
    protected static final String TASK_EXTRA2 = "TASK_EXTRA2";
    protected static final String TARGET_DAILY_ENABLED = "TARGET_DAILY_ENABLED";
    protected static final String TARGET_WEEKLY_ENABLED = "TARGET_WEEKLY_ENABLED";
    protected static final String TARGET_MONTHLY_ENABLED = "TARGET_MONTHLY_ENABLED";
    protected static final String WTD_DELTA_DAY_SECS = "WTD_DELTA_DAY_SECS";
    protected static final String WTD_DELTA_DAY_FORMATTED = "WTD_DELTA_DAY_FORMATTED";
    protected static final String CURRENT_WORK_UNIT_TOTAL_SECS = "CURRENT_WORK_UNIT_TOTAL_SECS";
    protected static final String CURRENT_WORK_UNIT_TOTAL_FORMATTED = "CURRENT_WORK_UNIT_TOTAL_FORMATTED";
    protected static final String WEAR_NOTIFICATION_FIELDS = "WEAR_NOTIFICATION_FIELDS";
    protected static final String DELTA_MONTH_SECS = "DELTA_MONTH_SECS";
    protected static final String DELTA_MONTH_FORMATTED = "DELTA_MONTH_FORMATTED";
    protected static final String MTD_DELTA_DAY_SECS = "MTD_DELTA_DAY_SECS";
    protected static final String MTD_DELTA_DAY_FORMATTED = "MTD_DELTA_DAY_FORMATTED";

    private final Context context;
    public TimeRecConnector(Context context) {
        this.context = context;
    }

    private String getMatchingTimeRecPackage(String... packages) {
        for (String pkg:packages) {
            try {
                if(context.getPackageManager().getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)!=null) {
                    return pkg;
                }
            } catch (PackageManager.NameNotFoundException e) {

            }
        }
        return null;
    }

    private void sendIntent(Intent intent, final MessageEvent evt) {
        intent.setPackage(getMatchingTimeRecPackage("com.dynamicg.timerecording.pro", "com.dynamicg.timerecording"));
        BroadcastReceiver resultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent resultIntent) {
            if(evt != null) evt.messageReceived(this.getResultExtras(true));
            }
        };
        context.sendOrderedBroadcast(intent, null, resultReceiver, null, Activity.RESULT_OK, null, null);
    }

    protected void timeRecGetInfo(MessageEvent evt) {
        Intent intent = new Intent("com.dynamicg.timerecording.GET_INFO");
        intent.putExtra(EXTRA_FLAG, FLAG_WEEKLY_DELTA + FLAG_MONTHLY_DELTA);

        sendIntent(intent, evt);
    }

    protected void timeRecActionPunch(MessageEvent evt) {
        Intent intent = new Intent("com.dynamicg.timerecording.PUNCH");

        sendIntent(intent, evt);
    }



    interface MessageEvent {
        public abstract void messageReceived(Bundle bundle);
    }
}
