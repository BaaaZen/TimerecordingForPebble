package de.mhid.android.timerecordingforpebble;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.getpebble.android.kit.PebbleKit;

public class InfoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* trigger pebble background service */
        Intent serviceIntent = new Intent(this, PebbleService.class);
        this.startService(serviceIntent);

        /* show activity */
        setContentView(R.layout.activity_info);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    public void onClickButtonStartPebbleApp(View view) {
        PebbleKit.startAppOnPebble(this, PebbleMessenger.APP_UUID);
    }
}
