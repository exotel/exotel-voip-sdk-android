/*
 * Copyright (c) 2019 Exotel Techcom Pvt Ltd
 * All rights reserved
 */
package com.exotel.voicesample;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.ServiceCompat;

import com.exotel.voice.Call;
import com.exotel.voice.CallAudioRoute;
import com.exotel.voice.CallDetails;
import com.exotel.voice.CallDirection;
import com.exotel.voice.CallEndReason;
import com.exotel.voice.CallIssue;
import com.exotel.voice.CallState;
import com.exotel.voice.CallStatistics;
import com.exotel.voice.ExotelVoiceClientSDK;
import com.exotel.voice.CallController;
import com.exotel.voice.CallListener;
import com.exotel.voice.ExotelVoiceClient;
import com.exotel.voice.ExotelVoiceClientEventListener;
import com.exotel.voice.ExotelVoiceError;
import com.exotel.voice.LogLevel;

import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Callback;
import okhttp3.Response;

public class VoiceAppService implements ExotelVoiceClientEventListener, CallListener {

    private static String TAG = "VoiceAppService";
    private final IBinder binder = new LocalBinder();
    private ExotelVoiceClient exotelVoiceClient;
    private CallController callController;
    private List<CallEvents> callEventListenerList = new ArrayList<>();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Call mCall;
    private Call mPreviousCall;
    private List<VoiceAppStatusEvents> voiceAppStatusListenerList = new ArrayList<>();
    private LogUploadEvents logUploadEventListener;
    private long ringingStartTime = 0;
    private DatabaseHelper databaseHelper;

    private boolean initializationInProgress = false;
    private String initializationErrorMessage;
    private RingTonePlayback tonePlayback;
    private ApplicationUtils utils;
    private static final int NOTIFICATION_ID = 7;
    private final Object statusListenerListMutex = new Object();

    private VoiceAppStatus voiceAppStatus = new VoiceAppStatus();


    public VoiceAppService() {
        VoiceAppLogger.debug(TAG, "Constructor for sample app service");
    }

    //    @Override
    public IBinder onBind(Intent intent) {
        VoiceAppLogger.debug(TAG, "onBind for sample service");
        return binder;
    }


    private static volatile VoiceAppService instance;
    private Context context;

    private VoiceAppService(Context context) {
        // Store the application context to avoid memory leaks
        this.context = context;
        onCreate();
        onStartCommand();
    }


    public static VoiceAppService getInstance(Context context) {
        if (instance == null) {
            synchronized (VoiceAppService.class) {
                if (instance == null) {
                    instance = new VoiceAppService(context);
                    return instance;
                }
            }
        }
        return instance;
    }

    private Context getApplicationContext() {
        return context;
    }

    //    @Override
    public void onCreate() {
        VoiceAppLogger.setContext(getApplicationContext());
        VoiceAppLogger.debug(TAG, "Entry: onCreate VoiceAppService");
//        super.onCreate();


        utils = ApplicationUtils.getInstance(getApplicationContext());
        utils.createNotificationChannel();

        databaseHelper = DatabaseHelper.getInstance(getApplicationContext());
        tonePlayback = new RingTonePlayback(getApplicationContext());
        tonePlayback.initializeTonePlayback();
        VoiceAppLogger.debug(TAG, "Exit: onCreate VoiceAppService");

    }


    public void makeServiceForeground(Notification notification) {
        VoiceAppLogger.debug(TAG, "Making the service as foreground");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
//            startForeground(NOTIFICATION_ID, notification);
        } else {
//            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST);
//            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL);
        }


    }

    public void makeServiceBackground() {

        if (null == mCall) {
            VoiceAppLogger.debug(TAG, "Removing the service from foreground");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
//                stopForeground(true);
            }
        }

    }

    public void sendPushNotificationData(String payload, String payloadVersion, String userId) {
        VoiceAppLogger.debug(TAG, "Sending push notification data function");
        Map<String, String> pushNotificationData = new HashMap<>();
        pushNotificationData.put("payload", payload);
        pushNotificationData.put("payloadVersion", payloadVersion);
        if (null != exotelVoiceClient) {
            try {
                if (!exotelVoiceClient.relaySessionData(pushNotificationData)) {
                    makeServiceBackground();
                }
            } catch (Exception e) {
                VoiceAppLogger.error(TAG, "Exception in relaySessionData: " + e.getMessage());
                makeServiceBackground();
            }

        } else {
            VoiceAppLogger.error(TAG, "Initialize has not been called before relaySessionData");

        }
    }


    public CallDetails getLatestCallDetails() {
        VoiceAppLogger.debug(TAG, "getCurrentCallDetails");
        if (null == callController) {
            return null;
        }
        return callController.getLatestCallDetails();
    }

    public Call getCallFromCallId(String callId) {
        VoiceAppLogger.debug(TAG, "Getting call object for callId: " + callId);
        if (null == callController) {
            return null;
        }
        return callController.getCallFromCallId(callId);
    }


    public void deinitialize() throws Exception {

        VoiceAppLogger.debug(TAG, "De-Initialize Voice App Service");
        exotelVoiceClient.stop();
        VoiceAppLogger.debug(TAG, "After De-Initialize Voice App Service");
        synchronized (statusListenerListMutex) {
            for (VoiceAppStatusEvents statusEvents : voiceAppStatusListenerList) {
                statusEvents.onStatusChange();
            }
        }

    }

    public void initialize(String hostname, String subscriberName, String accountSid, String subscriberToken, String displayName) throws Exception {
        VoiceAppLogger.info(TAG, "Initialize Sample App Service");
        initializationErrorMessage = null;
        /* Initialize the SDK */
        exotelVoiceClient = ExotelVoiceClientSDK.getExotelVoiceClient();
        exotelVoiceClient.setEventListener(this);

        VoiceAppLogger.debug(TAG, "Set is Logged in to True");

        VoiceAppLogger.debug(TAG, "SDK initialized is: " + exotelVoiceClient.isInitialized() + "Init in Progress is: " + initializationInProgress);

        VoiceAppLogger.debug(TAG, "Hostname: " + hostname + " SubscriberName: " + subscriberName + " AccountSID: " + accountSid + " SubscriberToken: " + subscriberToken);
        if (null == displayName || displayName.trim().isEmpty()) {
            displayName = subscriberName;
        } else {
            try {
                initializationInProgress = true;
                exotelVoiceClient.initialize(this.getApplicationContext(), hostname, subscriberName, displayName, accountSid, subscriberToken);
            } catch (Exception e) {
                initializationInProgress = false;
                VoiceAppLogger.error(TAG, "Exception in SDK initialization: " + e.getMessage());
                initializationErrorMessage = e.getMessage();
                throw new Exception(e.getMessage());
            }
        }
        callController = exotelVoiceClient.getCallController();
        callController.setCallListener(this);
        /* Temp - Added for Testing */
        CallDetails callDetails = callController.getLatestCallDetails();
        if (null != callDetails) {
            VoiceAppLogger.debug(TAG, "callId: " + callDetails.getCallId() + " remoteId: " + callDetails.getRemoteId() + "duration: " + callDetails.getCallDuration() + " callState: " + callDetails.getCallState());
        }

        /* End */
        VoiceAppLogger.debug(TAG, "Returning from initialize with params in sample service");
    }

    void reset() {
        VoiceAppLogger.info(TAG, "Reset sample application Service");

        if (null == exotelVoiceClient || !exotelVoiceClient.isInitialized()) {
            VoiceAppLogger.error(TAG, "SDK is not yet initialized");
        } else {
            //exotelVoipClient.reset();
            exotelVoiceClient.stop();
        }
        VoiceAppLogger.debug(TAG, "End: Reset in sample App Service");
    }

    public String getVersionDetails() {
        VoiceAppLogger.debug(TAG, "Getting version details in sample app service");

        String message;
        message = ExotelVoiceClientSDK.getVersion();
        VoiceAppLogger.debug(TAG, "External Storage Dir is: " + getApplicationContext().getExternalFilesDir(null));
        return message;
    }

    public class LocalBinder extends Binder {
        VoiceAppService getService() {
            return VoiceAppService.this;
        }
    }

    //    @Override
    public void onDestroy() {
//        super.onDestroy();
        tonePlayback.resetTonePlayback();
        VoiceAppLogger.debug(TAG, "onDestroy");
    }

    public void onStartCommand() {
        VoiceAppLogger.debug(TAG, "Entry: onStart command for service, startId: ");


        boolean startForeground = /*intent.getBooleanExtra("foreground", false);*/ false;
        VoiceAppLogger.debug(TAG, "Start forground is: " + startForeground);
        if (startForeground) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                VoiceAppLogger.debug(TAG, "Making the service as foreground12");
                if (getLatestCallDetails() != null) {
                    if (getLatestCallDetails().getCallState() != CallState.ESTABLISHED) {
                        VoiceAppLogger.debug(TAG, "creating notification");
                        Notification notification = utils.createNotification(CallState.NONE, null, null, CallDirection.INCOMING);
                        makeServiceForeground(notification);
                    }
                }


            }
        }

        VoiceAppLogger.debug(TAG, "Exit: onStartCommand for VoiceAppService");
//        return START_NOT_STICKY;
    }


    public void parsingPushNotification(String pushNotificationPayloadVersion, String pushNotificationPayload, boolean relayPushNotification, String subscriberName, String hostname, String subscriberToken, String accountSid) {
        /*String pushNotificationPayloadVersion;
        String pushNotificationPayload;
        boolean relayPushNotification;
        String subscriberName;
        String hostname;
        String subscriberToken;
        String accountSid;
        String displayName;*/
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());

        relayPushNotification = false; /*intent.getBooleanExtra("relayPushNotification", false);*/

        VoiceAppLogger.debug(TAG, "Relay push notification is: " + relayPushNotification);

        /* If startService() was called for passing push notification */

           /* pushNotificationPayloadVersion = intent.getStringExtra("pushNotificationPayloadVersion");
            pushNotificationPayload = intent.getStringExtra("pushNotificationPayload");
            subscriberName = intent.getStringExtra("subscriberName");
            hostname = intent.getStringExtra("hostname");

            subscriberToken = intent.getStringExtra("regAuthToken");
            accountSid = intent.getStringExtra("accountSid");
            VoiceAppLogger.debug(TAG, "Payload Version: " + pushNotificationPayloadVersion + "payload: " + pushNotificationPayload + " userId: " + subscriberName);
*/
        try {
            String displayName = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.DISPLAY_NAME.toString());
            initialize(hostname, subscriberName, accountSid, subscriberToken, displayName);
            VoiceAppLogger.debug(TAG, "Before sendPushNotifcationData");
            makeServiceBackground();
            sendPushNotificationData(pushNotificationPayload, pushNotificationPayloadVersion, subscriberName);
        } catch (Exception e) {
            VoiceAppLogger.error(TAG, "Exception in initialization for push notification");
//                if (startForeground) {
            VoiceAppLogger.debug(TAG, "Stopping foreground service");
//                }
        }
    }

    public Call dial(String destination, String message) throws Exception {

        return dialSDK(destination, message);
    }

    public void sendDtmf(char digit) throws InvalidParameterException {
        VoiceAppLogger.debug(TAG, "Sending DTMF digit: " + digit);
        mCall.sendDtmf(digit);
    }

    private Call dialSDK(String destination, String message) throws Exception {
        Call call;

        VoiceAppLogger.debug(TAG, "In dial API in Sample Service, SDK initialized is: " + exotelVoiceClient.isInitialized());

        //destination = "sip." + destination;
        VoiceAppLogger.debug(TAG, "Destination is: " + destination);
        try {
            /*if(null == message || message.trim().isEmpty()) {
                call = callController.dial(destination);
            } else {
                call = callController.dialWithMessage(destination,message);
            }*/
            call = callController.dial(destination, message);

        } catch (Exception e) {
            VoiceAppLogger.error(TAG, "Exception in dial");
            String lastDialledNo;

            SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
            lastDialledNo = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.LAST_DIALLED_NO.toString());
            Date date = new Date();
            if (!e.getMessage().contains("Invalid number")) {
                databaseHelper.insertData(lastDialledNo, date, CallType.OUTGOING);
            }
            throw new Exception(e.getMessage());
        }

        Notification notification = utils.createNotification(CallState.OUTGOING_INITIATED, destination, call.getCallDetails().getCallId(), call.getCallDetails().getCallDirection());
       /* NotificationManager manager;
        manager = context.getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, notification);
        h*/
        handler.post(new Runnable() {
            @Override
            public void run() {
                NotificationManager manager = context.getSystemService(NotificationManager.class);
                manager.notify(NOTIFICATION_ID, notification);
            }
        });
        mCall = call;
        return call;
    }

    /*
    public Call dialWithMessage (String destination, String message) throws Exception{

        return dialSDK(destination,message);
    }*/

    public void addCallEventListener(CallEvents callEvents) {
        VoiceAppLogger.debug(TAG, "Adding call event listener: " + callEvents);
        callEventListenerList.add(callEvents);
        for (CallEvents events : callEventListenerList) {
            VoiceAppLogger.debug(TAG, "Listener is: " + events);
        }
    }

    public void removeCallEventListener(CallEvents callEvents) {
        VoiceAppLogger.debug(TAG, "Remvoing call event listener: " + callEvents + " Class name: " + callEvents.getClass().getName());
        List<CallEvents> removeList = new ArrayList<>();
        for (CallEvents events : callEventListenerList) {
            VoiceAppLogger.debug(TAG, "Listener is: " + events + " Class is : " + events.getClass().getName());
            if (callEvents.getClass().getName().equals(events.getClass().getName())) {
                removeList.add(events);
            }
        }
        callEventListenerList.removeAll(removeList);
    }

    public void addStatusEventListener(VoiceAppStatusEvents statusEvents) {
        synchronized (statusListenerListMutex) {
            voiceAppStatusListenerList.add(statusEvents);
        }

    }

    public void removeStatusEventListener(VoiceAppStatusEvents statusEvents) {
        synchronized (statusListenerListMutex) {
            voiceAppStatusListenerList.remove(statusEvents);
        }

    }

    public void setLogUploadEventListener(LogUploadEvents logUploadEvents) {
        logUploadEventListener = logUploadEvents;
    }

    public void hangup() throws Exception {
        if (null == mCall) {
            String message = "Call object is NULL";
            throw new Exception(message);
        }
        VoiceAppLogger.debug(TAG, "hangup with callId: " + mCall.getCallDetails().getCallId());
        try {
            mCall.hangup();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        VoiceAppLogger.debug(TAG, "Return from hangup in Sample App Service");
    }

    public void enableSpeaker() {
        if (null != mCall) {
            mCall.enableSpeaker();
        }
    }

    public void disableSpeaker() {
        if (null != mCall) {
            mCall.disableSpeaker();
        }
    }

    public void enableBluetooth() {
        if (null != mCall) {
            mCall.enableBluetooth();
        }
    }

    public void disableBluetooth() {
        if (null != mCall) {
            mCall.disableBluetooth();
        }
    }

    public CallAudioRoute getCallAudioState() {
        if (mCall != null) {
            return mCall.getAudioRoute();
        }
        return CallAudioRoute.EARPIECE;
    }

    public void mute() {
        if (null != mCall) {
            mCall.mute();
        }
    }

    public void unmute() {
        if (null != mCall) {
            mCall.unmute();
        }
    }


    public int getCallDuration() {

        if (null == mCall) {
            return -1;
        }
        int duration = mCall.getCallDetails().getCallDuration();
        //VoiceAppLogger.debug(TAG,"Get Call Duration is VoiceApp Service, duration: "+duration);
        return duration;
    }

    public CallStatistics getStatistics() {
        if (null == mCall) {
            return null;
        }
        return mCall.getStatistics();
    }

    /*uncomment the below code to get ringing duration.*/
    /*public int getRingingDuration() {
        long curTime = System.currentTimeMillis() / 1000L;
        int diff = (int) (curTime - ringingStartTime);

        return diff;
    }*/

    public VoiceAppStatus getCurrentStatus() {
        if (exotelVoiceClient == null) {
            VoiceAppLogger.debug(TAG, "VoIP Client not initialized");
            voiceAppStatus.setState(VoiceAppState.STATUS_NOT_INITIALIZED);
            voiceAppStatus.setMessage("In Progress");
        } else if (initializationInProgress) {
            VoiceAppLogger.debug(TAG, "Initialization In Progress");
            voiceAppStatus.setState(VoiceAppState.STATUS_INITIALIZATION_IN_PROGRESS);
            voiceAppStatus.setMessage("In Progress");
        } else {
            boolean isSDKInitialized = exotelVoiceClient.isInitialized();
            if (isSDKInitialized) {
                VoiceAppLogger.debug(TAG, "SDK initialized : READY");
                voiceAppStatus.setState(VoiceAppState.STATUS_READY);
                voiceAppStatus.setMessage("Ready");
            }
        }
        return voiceAppStatus;
    }

    /*public VoiceAppStatus getCurrentStatus() {

        VoiceAppStatus status = new VoiceAppStatus();
        VoiceAppLogger.debug(TAG, "Start: getCurrentStatus");

        if (null == exotelVoiceClient) {
            VoiceAppLogger.debug(TAG, "VoIP Client not initialized");
            status.setMessage("Not Initialized");
            status.setState(VoiceAppState.STATUS_NOT_INITIALIZED);
            return status;
        }

        if (!exotelVoiceClient.isInitialized()) {
            if (initializationInProgress) {
                VoiceAppLogger.debug(TAG, "Initialization In Progress");
                status.setMessage("Init in Progress - Please wait");
                status.setState(VoiceAppState.STATUS_INITIALIZATION_IN_PROGRESS);
            } else {
                String message = "Not Initialized: ";
                if (null != initializationErrorMessage) {
                    message = message + initializationErrorMessage;
                }
                VoiceAppLogger.error(TAG, message);
                status.setMessage(message);
                status.setState(VoiceAppState.STATUS_INITIALIZATION_FAILURE);
            }

            return status;
        }

        status.setMessage("Ready");
        status.setState(VoiceAppState.STATUS_READY);

        VoiceAppLogger.debug(TAG, "End: getCurrentStatus");
        return status;
    }*/

    /* Implementation of ExotelVoipClientEventListener events */
    @Override
    public void onInitializationSuccess() {

        VoiceAppLogger.debug(TAG, "Start: onStatusChange");
        initializationInProgress = false;
        initializationErrorMessage = null;
        VoiceAppLogger.debug(TAG, "Initialization of SDK success");

        voiceAppStatus.setState(VoiceAppState.STATUS_READY);
        voiceAppStatus.setMessage("Ready");

        synchronized (statusListenerListMutex) {
            for (VoiceAppStatusEvents statusEvents : voiceAppStatusListenerList) {
                statusEvents.onStatusChange();
            }
        }

        VoiceAppLogger.debug(TAG, "End: onStatusChange");

    }

    @Override
    public void onDestroyMediaSession() {
        VoiceAppLogger.debug(TAG, "in onDestroyMediaSession");
    }

    @Override
    public void onDeInitialized() {
        VoiceAppLogger.debug(TAG, "Start: onDeInitialized");
        initializationInProgress = false;
        synchronized (statusListenerListMutex) {
            for (VoiceAppStatusEvents statusEvents : voiceAppStatusListenerList) {
                statusEvents.onStatusChange();
            }
        }
        VoiceAppLogger.debug(TAG, "End: onDeInitialized");
    }

    @Override
    public void onInitializationFailure(ExotelVoiceError error) {

        VoiceAppLogger.debug(TAG, "Start: onInitializationFailure");
        initializationInProgress = false;
        initializationErrorMessage = error.getErrorMessage();
        VoiceAppLogger.error(TAG, "Failed to initialize voip SDK, error is: " + error.getErrorMessage());

        voiceAppStatus.setState(VoiceAppState.STATUS_INITIALIZATION_FAILURE);
        voiceAppStatus.setMessage(error.getErrorMessage());

        synchronized (statusListenerListMutex) {
            for (VoiceAppStatusEvents statusEvents : voiceAppStatusListenerList) {
                statusEvents.onStatusChange();
            }
        }

        VoiceAppLogger.debug(TAG, "End: onInitializationFailure");
    }

    @Override
    public void onLog(LogLevel logLevel, String tag, String message) {
        if (LogLevel.DEBUG == logLevel) {
            VoiceAppLogger.debug(tag, message);
        } else if (LogLevel.INFO == logLevel) {
            VoiceAppLogger.info(tag, message);
        } else if (LogLevel.WARNING == logLevel) {
            VoiceAppLogger.warn(tag, message);
        } else if (LogLevel.ERROR == logLevel) {
            VoiceAppLogger.error(tag, message);
        }
    }

    @Override
    public void onUploadLogSuccess() {
        logUploadEventListener.onUploadLogSuccess();
    }

    @Override
    public void onUploadLogFailure(ExotelVoiceError error) {
        logUploadEventListener.onUploadLogFailure(error);
    }

    @Override
    public void onAuthenticationFailure(ExotelVoiceError exotelVoiceError) {
        VoiceAppLogger.error(TAG, "Authentication Failure");
        synchronized (statusListenerListMutex) {

            ApplicationUtils applicationUtils = ApplicationUtils.getInstance(getApplicationContext());

            SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
            String regAuthToken = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.SUBSCRIBER_TOKEN.toString());

            if (!applicationUtils.isRefreshTokenValid(regAuthToken)) {

                String username = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.USER_NAME.toString());
                String sdkHostname = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.APP_HOSTNAME.toString());
                String accountSid = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.ACCOUNT_SID.toString());
                String password = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.PASSWORD.toString());

                applicationUtils.login(sdkHostname, accountSid, username, password, mCallback);
            }
            synchronized (statusListenerListMutex) {
                for (VoiceAppStatusEvents statusEvents : voiceAppStatusListenerList) {
                    statusEvents.onAuthFailure();
                }
            }

        }
    }

    /* Implementation of ExotelVoipCallListemer events */
    @Override
    public void onIncomingCall(Call call) {
        VoiceAppLogger.debug(TAG, "Incoming call Received, callId: " + call.getCallDetails().getCallId());


        tonePlayback.startTone();
        handler.post(new Runnable() {
            @Override
            public void run() {
                Notification notification = utils.createNotification(CallState.INCOMING, call.getCallDetails().getRemoteId(), call.getCallDetails().getCallId(), call.getCallDetails().getCallDirection());
                makeServiceForeground(notification);
            }
        });
        mCall = call;
        ApplicationUtils utils = ApplicationUtils.getInstance(getApplicationContext());
        utils.launchCallActivity(call);
    }

    @Override
    public void onCallInitiated(Call call) {
        VoiceAppLogger.debug(TAG, "on Call initiated");
        for (CallEvents callEvents : callEventListenerList) {
            callEvents.onCallInitiated(call);
        }
        mCall = call;
        updateForegroundServiceType(call, CallState.OUTGOING_INITIATED);
        VoiceAppLogger.debug(TAG, "End: onCallInitiated");
    }

    @Override
    public void onCallRinging(Call call) {
        VoiceAppLogger.debug(TAG, "on call ringing event is Sample Application Service");

        ringingStartTime = System.currentTimeMillis() / 1000L;
        mCall = call;
        for (CallEvents callEvents : callEventListenerList) {
            callEvents.onCallRinging(call);
        }
        Notification notification = utils.createNotification(CallState.RINGING, call.getCallDetails().getRemoteId(), call.getCallDetails().getCallId(), call.getCallDetails().getCallDirection());
        handler.post(new Runnable() {
            @Override
            public void run() {
                NotificationManager manager;
                manager = context.getSystemService(NotificationManager.class);
                manager.notify(NOTIFICATION_ID, notification);
            }
        });
        VoiceAppLogger.debug(TAG, "End: onCallRinging");
    }

    public void answer() throws Exception {
        VoiceAppLogger.debug(TAG, "Answering call");
        if (null == mCall) {
            String message = "Call object is NULL";
            throw new Exception(message);
        }
        try {
            updateForegroundServiceType(mCall, CallState.ANSWERING);
            tonePlayback.stopTone();
            mCall.answer();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        VoiceAppLogger.debug(TAG, "After Answering call");
    }

    private void updateForegroundServiceType(Call call, CallState outgoingInitiated) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Notification notification = utils.createNotification(outgoingInitiated, call.getCallDetails().getRemoteId(), call.getCallDetails().getCallId(), call.getCallDetails().getCallDirection());
                /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    startForeground(NOTIFICATION_ID, notification);
                } else {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST);
//                        ServiceCompat.startForeground(VoiceAppService.this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL);
                }*/

                NotificationManager manager;
                manager = context.getSystemService(NotificationManager.class);
                manager.notify(NOTIFICATION_ID, notification);
            }
        });
    }


    @Override
    public void onCallEstablished(Call call) {
        VoiceAppLogger.debug(TAG, "Call Estabslished");
        ringingStartTime = 0;
        mCall = call;
        for (CallEvents callEvents : callEventListenerList) {
            callEvents.onCallEstablished(call);
        }
        Notification notification = utils.createNotification(CallState.ESTABLISHED, call.getCallDetails().getRemoteId(), call.getCallDetails().getCallId(), call.getCallDetails().getCallDirection());
        handler.post(new Runnable() {
            @Override
            public void run() {
                NotificationManager manager;
//                manager = (NotificationManager) context.getSystemService(NotificationManager.class);
               /* if (null != manager) {
                    manager.notify(NOTIFICATION_ID, notification);
                } else {
                    VoiceAppLogger.error(TAG, "Notification manager is NULL");
                }*/

                manager = context.getSystemService(NotificationManager.class);
                manager.notify(NOTIFICATION_ID, notification);


            }
        });
    }

    @Override
    public void onCallEnded(Call call) {

        Notification notification = utils.createNotification(CallState.ESTABLISHED, call.getCallDetails().getRemoteId(), call.getCallDetails().getCallId(), call.getCallDetails().getCallDirection());


        NotificationManager manager;
//                manager = (NotificationManager) context.getSystemService(NotificationManager.class);
               /* if (null != manager) {
                    manager.notify(NOTIFICATION_ID, notification);
                } else {
                    VoiceAppLogger.error(TAG, "Notification manager is NULL");
                }*/

        manager = context.getSystemService(NotificationManager.class);
        manager.cancel(NOTIFICATION_ID);

        VoiceAppLogger.debug(TAG, "Call Ended, call ID: " + call.getCallDetails().getCallId() + " Session ID: " + call.getCallDetails().getSessionId() + "Call end reason: " + call.getCallDetails().getCallEndReason());
        ringingStartTime = 0;


        tonePlayback.stopTone();

        mCall = null;
        mPreviousCall = call;
        for (CallEvents callEvents : callEventListenerList) {
            VoiceAppLogger.debug(TAG, "Sending call ended event to: " + callEvents);
            callEvents.onCallEnded(call);
        }

        if (CallEndReason.BUSY == call.getCallDetails().getCallEndReason()) {
            VoiceAppLogger.debug(TAG, "Playing busy tone");
            tonePlayback.playBusyTone();
        } else if (CallEndReason.TIMEOUT == call.getCallDetails().getCallEndReason()) {
            VoiceAppLogger.debug(TAG, "Playing reorder tone");
            tonePlayback.playReorderTone();
        }

        /* Insert into SqlLite DB for recent call Tabs */
        CallType callType = CallType.INCOMING;
        String destination;
        destination = call.getCallDetails().getRemoteId();

        if (CallDirection.INCOMING == call.getCallDetails().getCallDirection() && CallEndReason.TIMEOUT == call.getCallDetails().getCallEndReason()) {
            callType = CallType.MISSED;
        } else if (CallDirection.INCOMING == call.getCallDetails().getCallDirection()) {
            callType = CallType.INCOMING;
        } else if (CallDirection.OUTGOING == call.getCallDetails().getCallDirection()) {
            callType = CallType.OUTGOING;
            SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
            destination = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.LAST_DIALLED_NO.toString());
        }
        Date date = new Date(call.getCallDetails().getCallStartedTime() * 1000);

        databaseHelper.insertData(destination, date, callType);
        /**/

        makeServiceBackground();
    }

    @Override
    public void onMissedCall(String remoteId, Date time) {
        VoiceAppLogger.debug(TAG, "Missed call, remoteId: " + remoteId + " Time: " + time);

        VoiceAppLogger.debug(TAG, "Size of call event listener is: " + callEventListenerList.size());
        for (CallEvents callEvents : callEventListenerList) {
            callEvents.onMissedCall(remoteId, time);
        }

        VoiceAppLogger.debug(TAG, "Playing waiting tone");
        tonePlayback.playWaitingTone();

        /* Add to SqlLite DB for Recent Call Fragment */
        databaseHelper.insertData(remoteId, time, CallType.MISSED);
        makeServiceBackground();
    }

    @Override
    public void onMediaDisrupted(Call call) {
        VoiceAppLogger.debug(TAG, "Call media disrupted, Call Id: " + call.getCallDetails().getCallId());
        for (CallEvents callEvents : callEventListenerList) {
            callEvents.onMediaDisrupted(call);
        }
        mCall = call;
        Notification notification = utils.createNotification(CallState.MEDIA_DISRUPTED, call.getCallDetails().getRemoteId(), call.getCallDetails().getCallId(), call.getCallDetails().getCallDirection());

        handler.post(new Runnable() {
            @Override
            public void run() {
                NotificationManager manager;
                manager = context.getSystemService(NotificationManager.class);
                manager.notify(NOTIFICATION_ID, notification);
            }
        });
    }

    @Override
    public void onRenewingMedia(Call call) {
        VoiceAppLogger.debug(TAG, "Call media renewing, Call Id: " + call.getCallDetails().getCallId());
        for (CallEvents callEvents : callEventListenerList) {
            callEvents.onRenewingMedia(call);
        }
        mCall = call;
    }

    public void uploadLogs(Date startDate, Date endDate, String description) throws Exception {
        VoiceAppLogger.debug(TAG, "uploadLogs: startDate: " + startDate + " EndDate: " + endDate);
        exotelVoiceClient.uploadLogs(startDate, endDate, description);
    }

    void postFeedback(int rating, CallIssue issue) throws InvalidParameterException {
        if (null != mPreviousCall) {
            mPreviousCall.postFeedback(rating, issue);
        } else {
            VoiceAppLogger.error(TAG, "Call handle is NULL, cannot post feedback");
        }
    }

    private Callback mCallback = new Callback() {
        @Override
        public void onFailure(okhttp3.Call call, IOException e) {
            VoiceAppLogger.error(TAG, "Failed to get response for login");
            /* TODO: Exception on UI thread */
        }

        @Override
        public void onResponse(okhttp3.Call call, Response response) throws IOException {
            VoiceAppLogger.debug(TAG, "Got response for login: " + response.code());
            if (200 == response.code()) {
                String jsonData;
                jsonData = response.body().string();
                JSONObject jObject;
                VoiceAppLogger.debug(TAG, "Get regAuth Token response is: " + jsonData);
                try {
                    jObject = new JSONObject(jsonData);
                    String regAuthToken = jObject.getString("subscriber_token");
                    String sdkHostname = jObject.getString("host_name");
                    String accountSid = jObject.getString("account_sid");
                    String exophone = jObject.getString("exophone");
                    String contactDisplayName = jObject.getString("contact_display_name");

                    SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
                    sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.SUBSCRIBER_TOKEN.toString(), regAuthToken);
                    sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.SDK_HOSTNAME.toString(), sdkHostname);
                    sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.EXOPHONE.toString(), exophone);
                    sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.CONTACT_DISPLAY_NAME.toString(), contactDisplayName);

                    String username = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.USER_NAME.toString());
                    String displayName = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.DISPLAY_NAME.toString());
                    initialize(sdkHostname, username, accountSid, regAuthToken, displayName);


                } catch (Exception exp) {
                    VoiceAppLogger.error(TAG, "Exception in service initialization: " + exp.getMessage());
                    /* TODO: Exception on UI thread */
                }
            }
        }
    };

    public Call getCall() {
        return mCall;
    }
}
