/*
 * Copyright (c) 2019 Exotel Techcom Pvt Ltd
 * All rights reserved
 */
package com.exotel.voicesample;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.exotel.voice.Call;
import com.exotel.voice.CallDirection;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class DialPadFragment extends Fragment implements VoiceAppStatusEvents, CallEvents {

    private static String TAG = "DialPadFragment";
    private TextView textBox;
    //    private VoiceAppService mService;
    private boolean mBound;
    private View view;
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        VoiceAppLogger.debug(TAG, "onAttach for DialPadFragment");
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        VoiceAppLogger.debug(TAG, "onCreate for DialPadFragment");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        VoiceAppLogger.debug(TAG, "onCreateView for DialPadFragment");
        view = inflater.inflate(R.layout.fragment_dialler, container, false);

        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getActivity().getApplicationContext());

        //textBox.setText(sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.LAST_DIALLED_NO.toString()));


        ImageView callButton = view.findViewById(R.id.b_makecall);
        ImageView whatsappCallButton = view.findViewById(R.id.b_whatsapp_call);
        whatsappCallButton.setVisibility(View.GONE); //hide whatsapp button
        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText destinationNumber = view.findViewById(R.id.destinationNumber);
                EditText contextMessageText = view.findViewById(R.id.contextMessage); //not used
                VoiceAppLogger.debug(TAG, "Call button is clicked");

                String destination = destinationNumber.getText().toString().trim();
                VoiceAppLogger.debug(TAG, "destination :" + destination);

                String message = contextMessageText.getText().toString(); //not used
                if (!validateNumber(destination)) {
                    return;
                }
                ApplicationUtils utils = ApplicationUtils.getInstance(getActivity().getApplicationContext());
                String number = utils.getUpdatedNumberToDial(destination);
                utils.makeIPCall(voiceAppService, number, destination);
            }
        });

        whatsappCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VoiceAppLogger.debug(TAG, "Whatsapp Call button is clicked");
                EditText destinationNumber = view.findViewById(R.id.destinationNumber);
                String destination = destinationNumber.getText().toString().trim();
                VoiceAppLogger.debug(TAG, "destination :" + destination);
                if (!validateNumber(destination)) {
                    return;
                }
                ApplicationUtils utils = ApplicationUtils.getInstance(getActivity().getApplicationContext());
                utils.makeWhatsAppCall(voiceAppService, destination);
            }
        });
        return view;
    }

    private boolean validateNumber(String destination) {
        if (0 == destination.trim().length()) {
            VoiceAppLogger.warn(TAG, "No number input");
            String errorMessage = "No number entered";
            Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
            return false;
        }
        VoiceAppLogger.debug(TAG, "isValidContact: " + isValidContact(destination));
        if (!isValidContact(destination)) {

            String errorMessage = "Invalid number, no special characters allowed(except + at the beginning)";
            Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
            return false;
        }
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getActivity().getApplicationContext());
        String subscriberName = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.USER_NAME.toString());
        if (destination.equals(subscriberName)) {
            VoiceAppLogger.error(TAG, "Cannot dial out to yourself");
            String errorMessage = "Cannot dial out to yourself";
            Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
            return false;

        }
        return true;
    }


    private VoiceAppService voiceAppService;

    public void onServiceConnected() {
        // We've bound to LocalService, cast the IBinder and get LocalService instance
        voiceAppService = VoiceAppService.getInstance(requireActivity().getApplicationContext());
        VoiceAppLogger.debug(TAG, "Service connected in HomeActivity");
//        VoiceAppService.LocalBinder binder = (VoiceAppService.LocalBinder) service;
        /* voiceAppService = binder.getService();*/
//        mBound = true;
        voiceAppService.removeStatusEventListener(DialPadFragment.this);
        voiceAppService.addStatusEventListener(DialPadFragment.this);
        voiceAppService.removeCallEventListener(DialPadFragment.this);
        voiceAppService.addCallEventListener(DialPadFragment.this);
        //Status status = mService.getCurrentStatus();
        VoiceAppLogger.debug(TAG, "Return from onServiceConnected in Home Activity");
    }


    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
          /*  VoiceAppLogger.debug(TAG,"Service connected in HomeActivity");
            VoiceAppService.LocalBinder binder = (VoiceAppService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.removeStatusEventListener(DialPadFragment.this);
            mService.addStatusEventListener(DialPadFragment.this);
            mService.removeCallEventListener(DialPadFragment.this);
            mService.addCallEventListener(DialPadFragment.this);
            //Status status = mService.getCurrentStatus();

*/
            VoiceAppLogger.debug(TAG, "Return from onServiceConnected in Home Activity");

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            VoiceAppLogger.debug(TAG, "Service disconnected in HomeActivity");
            mBound = false;
        }
    };

    private boolean isValidContact(String contactInfo) {
        String genericPattern = "^[a-zA-Z0-9]*$";
        String numericPattern = "\\+[0-9]+$";
        return contactInfo.matches(genericPattern) || contactInfo.matches(numericPattern);
    }

    private void updateNumber(String digit) {
        String number = textBox.getText().toString();
        number = number + digit;
        textBox.setText(number);
    }

    private void removeLastDigit() {
        String number = textBox.getText().toString();

        if (number.length() > 0) {
            int len = number.length();
            number = number.substring(0, len - 1);
            textBox.setText(number);
        }
    }

    private void clearNumber() {
        textBox.setText("");
    }

    @Override
    public void onStart() {
        super.onStart();
        VoiceAppLogger.debug(TAG, "onStart for DialPadFragment");
        Intent intent = new Intent(getActivity(), VoiceAppService.class);
//        getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
        onServiceConnected();

    }

    @Override
    public void onResume() {
        super.onResume();
        VoiceAppLogger.debug(TAG, "onResume for DialPadFragment");
    }

    @Override
    public void onPause() {
        super.onPause();
        VoiceAppLogger.debug(TAG, "onPause for DialPadFragment");
    }

    @Override
    public void onStop() {
        super.onStop();
        VoiceAppLogger.debug(TAG, "onStop for DialPadFragment");
      /*  if(mBound) {
            getActivity().unbindService(connection);
        }*/
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        VoiceAppLogger.debug(TAG, "onDestroyView for DialPadFragment");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        VoiceAppLogger.debug(TAG, "onDestroy for DialPadFragment");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        VoiceAppLogger.debug(TAG, "onDetach for DialPadFragment");
    }

    @Override
    public void onStatusChange() {
        VoiceAppLogger.debug(TAG, "Received On Status Change Event in HomeActivity");
        /*if(null != getActivity()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Status status = mService.getCurrentStatus();
                    TextView initStatus = view.findViewById(R.id.userStatus);
                    initStatus.setText(status.getMessage());
                    if(State.STATUS_READY == status.getState()) {
                        initStatus.setTextColor(Color.GREEN);
                    } else {
                        initStatus.setTextColor(Color.RED);
                    }
                }
            });
        }
        */
        VoiceAppLogger.debug(TAG, "Returning from On Satus Change Event in HomeActivity");
    }

    @Override
    public void onAuthFailure() {
        VoiceAppLogger.debug(TAG, "On Authentication failure");
    }

    @Override
    public void onCallInitiated(Call call) {

    }

    @Override
    public void onCallRinging(Call call) {

    }

    @Override
    public void onCallEstablished(Call call) {

    }

    @Override
    public void onCallEnded(Call call) {

        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(mContext);
        boolean enableMulticall = sharedPreferencesHelper.getBoolean("enable_multicall");
        sharedPreferencesHelper.putBoolean("isSpeakerEnabled", false);
        sharedPreferencesHelper.putBoolean("isMuteEnabled", false);
        if (enableMulticall) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    VoiceAppLogger.debug(TAG, "**Timer task triggered for making call");
                    VoiceAppLogger.debug(TAG, "Call Direction: " + call.getCallDetails().getCallDirection());
                    if (CallDirection.OUTGOING == call.getCallDetails().getCallDirection()) {

                        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(mContext);
                        String exophone = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.EXOPHONE.toString());
                        String lastDialledNumber = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.LAST_DIALLED_NO.toString());
                        ApplicationUtils utils = ApplicationUtils.getInstance(getActivity().getApplicationContext());
                        utils.makeIPCall(voiceAppService, exophone, lastDialledNumber);
                    }
                }
            }, 20000);
        }

    }

    @Override
    public void onMissedCall(String remoteUserId, Date time) {

    }

    @Override
    public void onMediaDisrupted(Call call) {

    }

    @Override
    public void onRenewingMedia(Call call) {

    }
}
