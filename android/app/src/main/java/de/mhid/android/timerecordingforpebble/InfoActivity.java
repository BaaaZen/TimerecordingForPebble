/*
  Copyright 2016 Mirko Hansen

  This file is part of Timerecording for Pebble
  http://www.github.com/BaaaZen/TimerecordingForPebble

  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.

*/

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
