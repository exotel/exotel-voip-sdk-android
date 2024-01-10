/*
 * Copyright (c) 2019 Exotel Techcom Pvt Ltd
 * All rights reserved
 */
package com.exotel.voicesample;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.exotel.voice.CallDetails;
import com.exotel.voice.CallState;

public class LaunchActvity extends AppCompatActivity {

    private static String TAG = "LaunchActivity";
    private VoiceAppService mService;
    private boolean mBound;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_launch);

        VoiceAppLogger.debug(TAG,"onCreate");


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VoiceAppLogger.debug(TAG,"onDestroy, service bound is: "+mBound);



    }

    @Override
    protected void onStart() {
        super.onStart();
        VoiceAppLogger.debug(TAG,"onStart");
        Intent intent = new Intent(LaunchActvity.this,VoiceAppService.class);
        try {
            startService(intent);
            bindService(intent,connection,BIND_AUTO_CREATE);
        } catch (IllegalStateException e) {
            VoiceAppLogger.error(TAG,"Illegal State exception in starting service: "+e.getMessage());
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startService(intent);
                    bindService(intent,connection,BIND_AUTO_CREATE);
                }
            },400);
        }

        bindService(intent,connection,BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        VoiceAppLogger.debug(TAG,"onStop");
        if(mBound) {
            unbindService(connection);
        }
    }


    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            VoiceAppLogger.debug(TAG, "Service connected");
            VoiceAppService.LocalBinder binder = (VoiceAppService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            ActivityName activityName;

            /* Check if user is Logged in */
            SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
            boolean isLoggedIn = sharedPreferencesHelper.getBoolean(ApplicationSharedPreferenceData.IS_LOGGED_IN.toString());
            VoiceAppLogger.debug(TAG,"isLoggedIn returns: "+isLoggedIn);
            CallDetails callDetails = null;
            VoiceAppLogger.debug(TAG,"Last Dialled number: "+sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.LAST_DIALLED_NO.toString()));
            VoiceAppLogger.debug(TAG,"User Name: "+sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.USER_NAME.toString()));
            if(!isLoggedIn) {
                activityName = ActivityName.LOGIN_ACTIVITY;
            } else {
                callDetails = mService.getLatestCallDetails();
                if(null == callDetails || CallState.ENDED == callDetails.getCallState() || CallState.NONE == callDetails.getCallState()) {
                    activityName = ActivityName.HOME_ACTIVITY;
                } else {
                    activityName = ActivityName.CALL_ACTIVITY;

                }
            }
            Intent intent;
            VoiceAppLogger.debug(TAG,"Activity name returned is: "+activityName);

            switch (activityName) {
                case CALL_ACTIVITY:
                    intent = new Intent(LaunchActvity.this,CallActivity.class);
                    //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("callId",callDetails.getCallId());
                    startActivity(intent);
                    finish();
                    break;

                case HOME_ACTIVITY:
                    intent = new Intent(LaunchActvity.this,HomeActivity.class);
                    //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    break;

                case LOGIN_ACTIVITY:
                    intent = new Intent(LaunchActvity.this,LoginActivity.class);
                    //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    break;

            }

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            VoiceAppLogger.debug(TAG, "service disconnected");
            mBound = false;
        }
    };
}
