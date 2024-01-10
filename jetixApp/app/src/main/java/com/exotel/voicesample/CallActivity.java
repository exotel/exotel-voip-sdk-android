/*
 * Copyright (c) 2019 Exotel Techcom Pvt Ltd
 * All rights reserved
 */
package com.exotel.voicesample;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;

import com.exotel.voice.CallAudioRoute;
import com.exotel.voice.CallDetails;
import com.exotel.voice.CallDirection;
import com.exotel.voice.CallEndReason;
import com.exotel.voice.CallState;
import com.exotel.voice.CallStatistics;
import com.exotel.voice.Call;
import com.exotel.voice.ExotelVoiceError;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class CallActivity extends AppCompatActivity implements CallEvents, SensorEventListener, CallContextEvents {

    private static String TAG = "CallActivity";

    private VoiceAppService mService;
    private boolean mBound;
    private String callId;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean mEnableMulticall = false;

    private SensorManager sensorManager;
    private Sensor proximity;
    PowerManager powerManager;
    PowerManager.WakeLock proximityWakeLock;
    private static final int SENSOR_SENSITIVITY = 4;

    ImageButton speakerButton;
    ImageButton muteButton;
    ImageButton bluetoothButton;
    private int connectedDevicesViaBluetooth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        VoiceAppLogger.setContext(getApplicationContext());
        VoiceAppLogger.debug(TAG, "onCreate for call Activity");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        Intent intent = getIntent();
        callId = intent.getStringExtra("callId");
        String destination = intent.getStringExtra("destination");
        TextView destinationTextView = findViewById(R.id.remoteIdCall);
        //destinationTextView.setText(destination);
        //destinationTextView.setVisibility(View.VISIBLE);

        VoiceAppLogger.debug(TAG, "CallID received in Call Activity is: " + callId);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        proximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "exovoip:wakelocktag");

        setClickListeners();
        connectedDevicesViaBluetooth = 0;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        VoiceAppLogger.debug(TAG, "onRestart for call activity");

    }


    @Override
    protected void onStart() {
        super.onStart();
        VoiceAppLogger.debug(TAG, "onStart for call Activity");
        ApplicationUtils utils = ApplicationUtils.getInstance(getApplicationContext());
        utils.setCallContextListener(this);
        Intent intent = new Intent(this, VoiceAppService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        VoiceAppLogger.debug(TAG, "onResume for call Activity");
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
        mEnableMulticall = sharedPreferencesHelper.getBoolean("enable_multicall");
        startUpdateDuration();
        updateMuteAndSpeakerButton();
        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        VoiceAppLogger.debug(TAG, "onPause for call Activity");
        endUpdateDuration();
        if (proximityWakeLock.isHeld()) {
            VoiceAppLogger.debug(TAG, "onPause: Releasing proximity sensor");
            proximityWakeLock.release();
        }

        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        VoiceAppLogger.debug(TAG, "onStop for call Activity, service bound is: " + mBound);
        if (mBound) {
            unbindService(connection);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VoiceAppLogger.debug(TAG, "onDestroy for call Activity");
        try {
            CallActivity.this.unregisterReceiver(broadcastReceiver);
        }
        catch(RuntimeException re){
            VoiceAppLogger.warn(TAG, re.getMessage());
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        VoiceAppLogger.debug(TAG, "Back button prssed for call Activity");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        VoiceAppLogger.debug(TAG, "Received New Intent");
        setIntent(intent);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] >= -SENSOR_SENSITIVITY && event.values[0] <= SENSOR_SENSITIVITY) {
                VoiceAppLogger.debug(TAG, "Putting screen to off");
                if (!proximityWakeLock.isHeld())
                    proximityWakeLock.acquire();
            } else {
                VoiceAppLogger.debug(TAG, "Putting screen to on");

                if (proximityWakeLock.isHeld())
                    proximityWakeLock.release();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*checking the button state while returning to call screen while app is returned from background.*/
    private void updateMuteAndSpeakerButton() {
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
        boolean isSpeakerEnabled = sharedPreferencesHelper.getBoolean("isSpeakerEnabled");
        boolean isMuteEnabled = sharedPreferencesHelper.getBoolean("isMuteEnabled");

        VoiceAppLogger.debug("speaker_value: ", isSpeakerEnabled + " isMuteEnabled: " + isMuteEnabled);
        if (!isSpeakerEnabled)
            speakerButton.setBackgroundTintList(ContextCompat.getColorStateList(CallActivity.this, R.color.grey_button_color));
        else
            speakerButton.setBackgroundTintList(ContextCompat.getColorStateList(CallActivity.this, R.color.blue_button_color));


        if (!isMuteEnabled)
            muteButton.setBackgroundTintList(ContextCompat.getColorStateList(CallActivity.this, R.color.grey_button_color));
        else
            muteButton.setBackgroundTintList(ContextCompat.getColorStateList(CallActivity.this, R.color.blue_button_color));

    }

    private void startUpdateDuration() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Call call = mService.getCallFromCallId(callId);
                if (null != mService && mBound && null != call) {
                    int duration;
                    if (call.getCallDetails().getCallState() == CallState.ESTABLISHED ||
                    call.getCallDetails().getCallState() == CallState.MEDIA_DISRUPTED) {
                        duration = mService.getCallDuration();
                    }
                    /*uncomment the below code to get ringing duration*/
                    /*else if (call.getCallDetails().getCallState() == CallState.RINGING) {
                        duration = mService.getRingingDuration();
                    } */else {
                        return;
                    }

                    if (duration >= 0) {
                        int minutes = duration / 60;
                        int seconds = duration % 60;
                        int hours = duration / 3600;
                        TextView callDurationTextView = findViewById(R.id.calling_duration);
                        String minStr;
                        String secStr;
                        String hourStr = "";
                        if (minutes < 10) {
                            minStr = "0" + String.valueOf(minutes);
                        } else {
                            minStr = String.valueOf(minutes);
                        }
                        if (seconds < 10) {
                            secStr = "0" + String.valueOf(seconds);
                        } else {
                            secStr = String.valueOf(seconds);
                        }
                        if (hours > 0) {
                            if (hours < 10) {
                                hourStr = "0" + String.valueOf(hours);
                            } else {
                                hourStr = String.valueOf(hours);
                            }
                        }
                        String callDuration;
                        if (hours > 0) {
                            callDuration = hourStr + ":" + minStr + ":" + secStr;
                        } else {
                            callDuration = minStr + ":" + secStr;
                        }

                        callDurationTextView.setVisibility(View.VISIBLE);
                        callDurationTextView.setText(callDuration);

                        if (call.getCallDetails().getCallState() == CallState.ESTABLISHED) {
                            CallStatistics callStats = mService.getStatistics();
                            VoiceAppLogger.debug(TAG, "Average Jitter Ms: " + callStats.getAverageJitterMs());
                            VoiceAppLogger.debug(TAG, "Max Jitter Ms: " + callStats.getMaxJitterMs());
                            VoiceAppLogger.debug(TAG, "Round Trip Time: " + callStats.getRttMs());
                            VoiceAppLogger.debug(TAG, "Fraction Lost: " + callStats.getFractionLost());
                            VoiceAppLogger.debug(TAG, "MOS" + callStats.getMOS());
                            VoiceAppLogger.debug(TAG, "Codec Name: " + callStats.getAudioCodec());
                            VoiceAppLogger.debug(TAG, "Audio Bitrate: " + callStats.getAudioBitrate());
                            VoiceAppLogger.debug(TAG, "Cumulative Lost: " + callStats.getCumulativeLost());
                            VoiceAppLogger.debug(TAG, "Jitter Samples: " + callStats.getJitterSamples());
                            VoiceAppLogger.debug(TAG, "Jitter Buffer(ms): " + callStats.getJitterBufferMs());
                            VoiceAppLogger.debug(TAG, "Preferred Jitter Buffer(ms): " + callStats.getPreferredJitterBufferMs());
                            VoiceAppLogger.debug(TAG, "Remote Fraction Loss: " + callStats.getRemoteFractionLoss());
                            VoiceAppLogger.debug(TAG, "Discarded Packets: " + callStats.getDiscardedPackets());
                        }

                    }


                }

                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void endUpdateDuration() {
        handler.removeCallbacksAndMessages(null);
    }

    private void setClickListeners() {

        ImageView hangupButton = findViewById(R.id.hangup_image);

        hangupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VoiceAppLogger.debug(TAG, "Hanging up current call");


                try {
                    mService.hangup();
                } catch (Exception e) {
                    VoiceAppLogger.debug(TAG, "Exception in hangup: " + e.getMessage());
                }
                updateUi();

            }
        });

        ImageView answerButton = findViewById(R.id.answer_button);
        answerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VoiceAppLogger.debug(TAG, "Answering current Call");

                try {
                    VoiceAppLogger.debug(TAG, "Answering call");
                    mService.answer();
                } catch (Exception e) {
                    Toast.makeText(CallActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                updateUi();
            }
        });

        speakerButton = findViewById(R.id.speaker_image);

        speakerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VoiceAppLogger.debug(TAG, "Speaker button pressed");
                SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
                boolean speakerState = sharedPreferencesHelper.getBoolean("isSpeakerEnabled");
                if (speakerState) {
                    disableSpeakerButton();
                } else {
                    enableSpeakerButton();
                }
            }
        });

        muteButton = findViewById(R.id.mute_image);

        muteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VoiceAppLogger.debug(TAG, "Mute button pressed");
                SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
                boolean isMuteEnabled = sharedPreferencesHelper.getBoolean("isMuteEnabled");

                if (isMuteEnabled) {
                    muteButton.setBackgroundTintList(ContextCompat.getColorStateList(CallActivity.this, R.color.grey_button_color));
                    mService.unmute();
                    sharedPreferencesHelper.putBoolean("isMuteEnabled", false);
                } else {
                    muteButton.setBackgroundTintList(ContextCompat.getColorStateList(CallActivity.this, R.color.blue_button_color));
                    mService.mute();
                    sharedPreferencesHelper.putBoolean("isMuteEnabled", true);
                }
            }
        });

        Button toggleKeyPad = findViewById(R.id.b_toggle_keypad);
        View keypadLayout = findViewById(R.id.layout_call_keypad);
        View callLayout = findViewById(R.id.layout_call);
        setDtmfClickListeners();
        toggleKeyPad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (keypadLayout.getVisibility() != View.VISIBLE) {
                    callLayout.setVisibility(View.GONE);
                    keypadLayout.setVisibility(View.VISIBLE);
                    toggleKeyPad.setText(R.string.hide_keypad);
                } else {
                    keypadLayout.setVisibility(View.GONE);
                    callLayout.setVisibility(View.VISIBLE);
                    toggleKeyPad.setText(R.string.show_keypad);
                }
            }
        });

        bluetoothButton = findViewById(R.id.bluetooth_image);
        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VoiceAppLogger.debug(TAG, "Bluetooth button pressed");
                SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
                boolean bluetoothState = sharedPreferencesHelper.getBoolean("isBluetoothEnabled");
                if (bluetoothState) {
                    disableBluetoothButton();
                } else {
                    enableBluetoothButton();
                }
            }
        });
    }
    private void disableSpeakerButton() {
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
        mService.disableSpeaker();
        sharedPreferencesHelper.putBoolean("isSpeakerEnabled", false);
        updateAudioButtonUI();
    }
    private void enableSpeakerButton() {
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
        if(sharedPreferencesHelper.getBoolean("isBluetoothEnabled")){
            disableBluetoothButton();
        }
        mService.enableSpeaker();
        sharedPreferencesHelper.putBoolean("isSpeakerEnabled", true);
        updateAudioButtonUI();
    }
    private void disableBluetoothButton() {
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
        mService.disableBluetooth();
        sharedPreferencesHelper.putBoolean("isBluetoothEnabled", false);
        updateAudioButtonUI();
    }
    private void enableBluetoothButton() {
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
        if(sharedPreferencesHelper.getBoolean("isSpeakerEnabled")){
            disableSpeakerButton();
        }
        mService.enableBluetooth();
        sharedPreferencesHelper.putBoolean("isBluetoothEnabled", true);
        updateAudioButtonUI();
    }

    private void updateAudioButtonUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
                if(sharedPreferencesHelper.getBoolean("isSpeakerEnabled")){
                    speakerButton.setBackgroundTintList(ContextCompat.getColorStateList(CallActivity.this, R.color.blue_button_color));
                } else {
                    speakerButton.setBackgroundTintList(ContextCompat.getColorStateList(CallActivity.this, R.color.grey_button_color));
                }
                if(sharedPreferencesHelper.getBoolean("isBluetoothEnabled")){
                    bluetoothButton.setBackgroundTintList(ContextCompat.getColorStateList(CallActivity.this, R.color.blue_button_color));
                } else {
                    bluetoothButton.setBackgroundTintList(ContextCompat.getColorStateList(CallActivity.this, R.color.grey_button_color));
                }
            }
        });
    }


    private void setDtmfClickListeners() {
        Button digit1 = findViewById(R.id.b_1);
        Button digit2 = findViewById(R.id.b_2);
        Button digit3 = findViewById(R.id.b_3);
        Button digit4 = findViewById(R.id.b_4);
        Button digit5 = findViewById(R.id.b_5);
        Button digit6 = findViewById(R.id.b_6);
        Button digit7 = findViewById(R.id.b_7);
        Button digit8 = findViewById(R.id.b_8);
        Button digit9 = findViewById(R.id.b_9);
        Button digit0 = findViewById(R.id.b_0);
        Button digitHash = findViewById(R.id.b_hash);
        Button digitStar = findViewById(R.id.b_star);

        digit1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendDtmf('1');
            }
        });
        digit2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendDtmf('2');
            }
        });
        digit3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendDtmf('3');
            }
        });
        digit4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendDtmf('4');
            }
        });
        digit5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendDtmf('5');
            }
        });
        digit6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendDtmf('6');
            }
        });
        digit7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendDtmf('7');
            }
        });
        digit8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendDtmf('8');
            }
        });
        digit9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendDtmf('9');
            }
        });
        digit0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendDtmf('0');
            }
        });
        digitStar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendDtmf('*');
            }
        });
        digitHash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendDtmf('#');
            }
        });


    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            VoiceAppLogger.debug(TAG, "Service connected");
            VoiceAppService.LocalBinder binder = (VoiceAppService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            if(mService.getCall() == null) {
                changeActivity("No Active Call");
                return;
            }
            mService.removeCallEventListener(CallActivity.this);
            mService.addCallEventListener(CallActivity.this);
            VoiceAppLogger.debug(TAG, "Getting call object from callId: " + callId);
            updateUi();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            VoiceAppLogger.debug(TAG, "Service disconnected");
            if (null != mService) {
                VoiceAppLogger.debug(TAG, "Removing event listeners");
                mService.removeCallEventListener(CallActivity.this);
            }
            CallActivity.this.unregisterReceiver(broadcastReceiver);
        }
    };

    private void updateUi() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                VoiceAppLogger.debug(TAG, "Update UI");
                CallDetails callDetails = mService.getLatestCallDetails();
                if (null == callDetails) {
                    VoiceAppLogger.debug(TAG, "Current Call Details are NULL");
                    return;
                }
                if (CallState.ENDED == callDetails.getCallState() || CallState.NONE == callDetails.getCallState()) {
                    return;
                }
                if (mEnableMulticall) {
                    VoiceAppLogger.debug(TAG, "Multicall enabled in updateUi");
                    if (CallState.INCOMING == callDetails.getCallState()) {
                        /* HACK for AUto Answer */
                        try {
                            mService.answer();
                        } catch (Exception e) {
                            VoiceAppLogger.debug(TAG, "Exception in answering call");
                        }

                    }
                }


                VoiceAppLogger.debug(TAG, "After get Current call details");

                TextView remoteIdtextView = findViewById(R.id.remoteIdCall);
                if (CallDirection.OUTGOING == callDetails.getCallDirection()) {
                    SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
                    /* Hack since the number actually dialled with always be the VN */
                    String destination = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.LAST_DIALLED_NO.toString());
                    remoteIdtextView.setText(destination);
                } else {
                    if (callDetails.getRemoteDisplayName().isEmpty()) {
                        remoteIdtextView.setText(callDetails.getRemoteId());
                    } else {
                        remoteIdtextView.setText(callDetails.getRemoteDisplayName());
                    }

                }

                SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
                String callContextMessage = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.CONTEXT_MESSAGE.toString());

                remoteIdtextView.setVisibility(View.VISIBLE);
                TextView callStatusTextView = findViewById(R.id.call_status);
                ImageView answerButton = findViewById(R.id.answer_button);
                TextView callMessage = findViewById(R.id.call_message);
                LinearLayout audioSettings = findViewById(R.id.audio_settings);
                ImageButton muteButton = findViewById(R.id.mute_image);
                ImageButton speakerButton = findViewById(R.id.speaker_image);

                VoiceAppLogger.debug(TAG, "Update UI 2");
                if (callDetails.getCallState() == CallState.OUTGOING_INITIATED) {
                    callStatusTextView.setText("Connecting..");
                    callStatusTextView.setVisibility(View.VISIBLE);
                    answerButton.setVisibility(View.GONE);
                } else if (callDetails.getCallState() == CallState.RINGING) {
                    callStatusTextView.setText("Ringing");
                    callStatusTextView.setVisibility(View.VISIBLE);
                    answerButton.setVisibility(View.GONE);
                } else if (callDetails.getCallState() == CallState.INCOMING) {
                    callStatusTextView.setText("Incoming");
                    callStatusTextView.setVisibility(View.VISIBLE);
                    answerButton.setVisibility(View.VISIBLE);
                } else if (callDetails.getCallState() == CallState.ANSWERING) {
                    callStatusTextView.setText("Answering..");
                    callStatusTextView.setVisibility(View.VISIBLE);
                    answerButton.setVisibility(View.GONE);
                } else if (callDetails.getCallState() == CallState.ESTABLISHED) {
                    callStatusTextView.setText("Connected");
                    callStatusTextView.setVisibility(View.VISIBLE);
                    answerButton.setVisibility(View.GONE);
                } else if (callDetails.getCallState() == CallState.MEDIA_DISRUPTED) {
                    callStatusTextView.setText("Reconnecting");
                    callStatusTextView.setVisibility(View.VISIBLE);
                    answerButton.setVisibility(View.GONE);
                } else if (callDetails.getCallState() == CallState.ENDING) {
                    callStatusTextView.setText("Ending");
                    callStatusTextView.setVisibility(View.VISIBLE);
                    answerButton.setVisibility(View.GONE);
                }
                if (callDetails.getCallState() == CallState.ESTABLISHED) {
//                    audioSettings.setVisibility(View.VISIBLE);
                    muteButton.setVisibility(View.VISIBLE);
                    speakerButton.setVisibility(View.VISIBLE);
                    callMessage.setVisibility(View.INVISIBLE);
                    if(connectedDevicesViaBluetooth > 0 ) {
                        bluetoothButton.setVisibility(View.VISIBLE);
                    }
                }

                if (callDetails.getCallState() == CallState.INCOMING || callDetails.getCallState() == CallState.ANSWERING
                        || callDetails.getCallState() == CallState.ESTABLISHED) {
                    if (!callContextMessage.isEmpty()) {
                        callMessage.setText(callContextMessage);
                        callMessage.setVisibility(View.VISIBLE);
                    }
                }
                VoiceAppLogger.debug(TAG, "Update UI 3");
            }
        });
    }

    /* CallBacks from SampleService */
    public void onCallInitiated(Call call) {
        VoiceAppLogger.debug(TAG, "Call Initiated, callId: " + call.getCallDetails().getCallId()
                + " Destination: " + call.getCallDetails().getRemoteId());
    }

    public void onCallRinging(Call call) {
        VoiceAppLogger.debug(TAG, "Call Ringing, callId: " + call.getCallDetails().getCallId()
                + " Destination: " + call.getCallDetails().getRemoteId());
        updateUi();
        startUpdateDuration();
    }

    public void onCallEstablished(Call call) {
        VoiceAppLogger.debug(TAG, "Call Established, callId: " + call.getCallDetails().getCallId()
                + " Destination: " + call.getCallDetails().getRemoteId());
        if(mService.getCallAudioState() == CallAudioRoute.BLUETOOTH) {
            connectedDevicesViaBluetooth++;
            enableBluetoothButton();
        }
        updateUi();
        startUpdateDuration();
        registerBluetoothListener();
        if (mEnableMulticall) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {

                    VoiceAppLogger.debug(TAG, "**Timer task triggered for ending call");

                    VoiceAppLogger.debug(TAG, "Call Direction: " + call.getCallDetails().getCallDirection());
                    if (CallDirection.OUTGOING == call.getCallDetails().getCallDirection()) {
                        try {
                            mService.hangup();
                        } catch (Exception e) {
                            VoiceAppLogger.debug(TAG, "Exception in hangup: " + e.getMessage());
                        }
                    }


                }
            }, 40000);
        }

    }

    public void onCallEnded(Call call) {
        VoiceAppLogger.debug(TAG, "Call Ended");
        endUpdateDuration();
        ApplicationUtils applicationUtils = ApplicationUtils.getInstance(getApplicationContext());
        String userId;
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
        userId = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.USER_NAME.toString());
        sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.CONTEXT_MESSAGE.toString(), "");
        applicationUtils.removeCallContext(userId);
        sharedPreferencesHelper.putBoolean("isSpeakerEnabled", false);
        sharedPreferencesHelper.putBoolean("isMuteEnabled", false);
        VoiceAppLogger.debug(TAG, "Call ID: " + call.getCallDetails().getCallId()
                + " Session ID: " + call.getCallDetails().getSessionId()
                + " Establish time: " + call.getCallDetails().getCallEstablishedTime());

        String message = " ";
        if (call.getCallDetails().getCallEndReason() != null) {
            if (call.getCallDetails().getCallEndReason() == CallEndReason.NONE) {
                message = "Call Ended";
            } else
                message = "Call Ended - " + call.getCallDetails().getCallEndReason();
        } else {
            message = "Call Ended";
        }
        changeActivity(message);
    }

    private void changeActivity(String toastMessage){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                /* TODO - Figure out a way to determine whether or not to launch home screen activity when call ends */
                if (true) {
                    Intent intent = new Intent(CallActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    Toast.makeText(CallActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
                    VoiceAppLogger.debug(TAG, "Calling Home Activity");
                    startActivity(intent);
                    finish();
                }


            }
        });
    }


    public void onCallHold(Call call) {
        VoiceAppLogger.debug(TAG, "call hold: callId: " + call.getCallDetails().getCallId());
    }

    public void onCallResume(Call call) {
        VoiceAppLogger.debug(TAG, "call Resumed: callId: " + call.getCallDetails().getCallId());
    }

    @Override
    public void onMissedCall(String remoteId, Date time) {
        VoiceAppLogger.debug(TAG, "Got missed call event, remoteId: " + remoteId + " time: " + time);
    }

    @Override
    public void onMediaDisrupted(Call call) {
        VoiceAppLogger.debug(TAG, "Call media disrupted, callId: " + call.getCallDetails().getCallId()
                + " Destination: " + call.getCallDetails().getRemoteId());
        updateUi();
        startUpdateDuration();
    }

    @Override
    public void onRenewingMedia(Call call) {
        VoiceAppLogger.debug(TAG, "Call media renewing, callId: " + call.getCallDetails().getCallId()
                + " Destination: " + call.getCallDetails().getRemoteId());
    }

    public void onCallFailed(Call call, ExotelVoiceError exotelVoiceError,
                             String exophoneNumber) {
        VoiceAppLogger.debug(TAG, "Call Failed: callId: " + call.getCallDetails().getCallId() +
                " error: " + exotelVoiceError.getErrorType());
        endUpdateDuration();
        if (true) {
            Intent intent = new Intent(CallActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            VoiceAppLogger.debug(TAG, "Calling start Activity1");
            startActivity(intent);
        }

        VoiceAppLogger.debug(TAG, "Calling finish");
        finish();
    }

    @Override
    public void onGetContextSuccess() {
        VoiceAppLogger.debug(TAG, "onGetContextSuccess");
        updateUi();
    }
    private void registerBluetoothListener() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        CallActivity.this.registerReceiver(broadcastReceiver, filter);
    }
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        BluetoothDevice device;
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                connectedDevicesViaBluetooth++;
                updateUi();
                Toast.makeText(getApplicationContext(), "Bluetooth Device is now Connected",    Toast.LENGTH_SHORT).show();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        enableBluetoothButton();
                    }
                },2000);
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                connectedDevicesViaBluetooth--;
                if(connectedDevicesViaBluetooth > 0) {
                    if(sharedPreferencesHelper.getBoolean("isBluetoothEnabled")){
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                enableBluetoothButton();
                            }
                        },2000);
                    }
                    return;
                }
                disableBluetoothButton();
                bluetoothButton.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "Bluetooth Device is disconnected",       Toast.LENGTH_SHORT).show();
            }
        }
    };

}
