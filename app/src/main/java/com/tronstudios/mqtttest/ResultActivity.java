package com.tronstudios.mqtttest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class ResultActivity extends AppCompatActivity {
    String TAG="mqtt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        Log.d(TAG,"en ResultActivity");


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String value = extras.getString("NotificationMessage");
            Log.d(TAG,"NotificationMessage en extra: "+value);

            //The key argument here must match that used in the other activity
        }



    }
}
