/*
 * Copyright (c) 2019 Exotel Techcom Pvt Ltd
 * All rights reserved
 */
package com.exotel.voicesample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Base64;

import androidx.core.content.ContextCompat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;


public class VoiceAppFirebaseService extends FirebaseMessagingService {

    private String TAG = "VoiceAppFirebaseService";

    @Override
    public void onCreate() {
        VoiceAppLogger.info(TAG,"in onCreate for FirebaseMessaging Service");
    }

    @Override
    public void onNewToken(String token) {
        VoiceAppLogger.debug(TAG,"onNewToken: "+token);
        ApplicationUtils utils = ApplicationUtils.getInstance(getApplicationContext());
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
        String appHostname = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.APP_HOSTNAME.toString());
        String username = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.USER_NAME.toString());
        String accountSid = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.ACCOUNT_SID.toString());
        try {
            utils.sendDeviceToken(token,appHostname,username,accountSid);
        } catch (Exception e ) {
            VoiceAppLogger.error(TAG,"Exception in sending device token: "+e.getMessage());
        }

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        VoiceAppLogger.debug(TAG, "From: " + remoteMessage.getFrom());
        // Check if message contains a data payload.
        VoiceAppLogger.debug(TAG,"Checking if data contains a payload");
        if (remoteMessage.getData().size() > 0) {
            VoiceAppLogger.debug(TAG, "Message data payload: " + remoteMessage.getData());

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                VoiceAppLogger.debug(TAG,"Audio permissions are allowed");
                VoiceAppLogger.debug(TAG,"Calling process push notification data");
                processPushNotification(remoteMessage.getData());
            }
            else {
                VoiceAppLogger.debug(TAG,"Audio permissions missing, cannot proceed to send notifications");
                logRemoteMessages(remoteMessage);
                return;
            }
        }
        logRemoteMessages(remoteMessage);

        Date sendtTime = new Date(remoteMessage.getSentTime());
        Date curTime = new Date();

        VoiceAppLogger.info(TAG,"Push notification was sent at: "+sendtTime);
        VoiceAppLogger.info(TAG,"Current time is: "+curTime);
        VoiceAppLogger.info(TAG,"Current time in MS is: "+curTime.getTime());
        long diff = (curTime.getTime() - sendtTime.getTime()) /1000;

        VoiceAppLogger.info(TAG,"Difference in seconds is: "+diff);

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            VoiceAppLogger.debug(TAG, "Message Notification Body: "
                    + remoteMessage.getNotification().getBody());
        } else {
            VoiceAppLogger.debug(TAG,"Notification is NULL");
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    private static String getJson(String strEncoded) throws UnsupportedEncodingException {
        byte[] decodedBytes = Base64.decode(strEncoded, Base64.URL_SAFE);
        return new String(decodedBytes, "UTF-8");
    }

    private void logRemoteMessages(RemoteMessage remoteMessage){
        VoiceAppLogger.debug(TAG,"Message priority: "+remoteMessage.getPriority());
        VoiceAppLogger.debug(TAG,"Message Original Prioirty is: "+remoteMessage.getOriginalPriority());
        VoiceAppLogger.debug(TAG,"Remote Data From: "+remoteMessage.getFrom());
        VoiceAppLogger.debug(TAG,"Remote Data To: "+remoteMessage.getTo());
        VoiceAppLogger.debug(TAG,"Remote Data TTL: "+remoteMessage.getTtl());
        VoiceAppLogger.debug(TAG,"Remote Data Sent Time: "+remoteMessage.getSentTime());
        VoiceAppLogger.debug(TAG,"Message Type is: "+remoteMessage.getMessageType());
    }

    private void processPushNotification(Map<String, String> remoteData) {

        VoiceAppLogger.debug(TAG,"Got a push notification!!");
        VoiceAppLogger.debug(TAG,"Printing all the key value pairs in the hashmap");
        for (String entry : remoteData.keySet()) {
            String value = remoteData.get(entry);
            VoiceAppLogger.debug(TAG,"Now printing values");
            VoiceAppLogger.debug(TAG,"Key is: "+entry);
            VoiceAppLogger.debug(TAG,"Value is: "+remoteData.get(entry));


        }
        VoiceAppLogger.debug(TAG,"Trying to decode the JSON");


        final ObjectMapper mapper = new ObjectMapper();
        final PushNotificationData pushNotificationData = mapper.convertValue(remoteData,
                PushNotificationData.class);
        VoiceAppLogger.debug(TAG,"Version is: "+pushNotificationData.getPayloadVersion());
        VoiceAppLogger.debug(TAG,"Payload is: "+pushNotificationData.getPayload());
        VoiceAppLogger.debug(TAG,"User ID is: "+pushNotificationData.getSubscriberName());

        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
        String subscriberName = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.USER_NAME.toString());
        if(!subscriberName.equals(pushNotificationData.getSubscriberName())) {
            VoiceAppLogger.error(TAG,"User ID in push notification: "
                    + pushNotificationData.getSubscriberName() + " Current User Id: "+subscriberName);
            return;
        }

        /* Check for Logged In Status */
        boolean isLoggedIn = sharedPreferencesHelper.getBoolean(ApplicationSharedPreferenceData.IS_LOGGED_IN.toString());
        if(!isLoggedIn) {
            VoiceAppLogger.error(TAG,"Not logged in, blocking the call: ");
            return;
        }
        VoiceAppLogger.debug(TAG,"Creating intent for service");
        Intent intent = new Intent(this,VoiceAppService.class);
        intent.putExtra("pushNotificationPayloadVersion",pushNotificationData.getPayloadVersion());
        intent.putExtra("pushNotificationPayload",pushNotificationData.getPayload());

        intent.putExtra("subscriberName",pushNotificationData.getSubscriberName());
        intent.putExtra("relayPushNotification",true);


        String hostname = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.SDK_HOSTNAME.toString());
        String subscriberToken = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.SUBSCRIBER_TOKEN.toString());
        String accountSid = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.ACCOUNT_SID.toString());

        intent.putExtra("hostname",hostname);
        intent.putExtra("regAuthToken",subscriberToken);
        intent.putExtra("accountSid",accountSid);


        VoiceAppLogger.debug(TAG,"Service is NULL");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra("foreground",true);
            startForegroundService(intent);

        } else {
            startService(intent);
        }
        VoiceAppLogger.debug(TAG,"Calling bind Service");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        VoiceAppLogger.debug(TAG,"In onDestroy of Firebase Service");
    }

}
