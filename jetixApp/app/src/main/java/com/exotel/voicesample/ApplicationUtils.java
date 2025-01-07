/*
 * Copyright (c) 2019 Exotel Techcom Pvt Ltd
 * All rights reserved
 */
package com.exotel.voicesample;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.exotel.voice.Call;
import com.exotel.voice.CallDirection;
import com.exotel.voice.CallState;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApplicationUtils {
    private static String TAG = "ApplicationUtils";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static String deviceTokenMessage;
    private static DeviceTokenState deviceTokenState = DeviceTokenState.DEVICE_TOKEN_SEND_SUCCESS;
    private List<DeviceTokenStatusEvents> deviceTokenListenerList = new ArrayList<>();
    private CallContextEvents callContextListener;
    private boolean debugDiallingEnabled = false;

    private static final String CHANNEL_ID = "exotelVoiceSample";

    private static ApplicationUtils mApplicationUtils;

    private Context context;

    private ApplicationUtils(Context context) {
        this.context = context;
    }

    public static ApplicationUtils getInstance(Context context) {
        if (null == mApplicationUtils) {
            mApplicationUtils = new ApplicationUtils(context);
        }
        return mApplicationUtils;
    }

    public void sendDeviceToken(String deviceToken, String hostname, String userId, String accountSid) throws Exception {

        if (hostname.equals("") || userId.equals("")) {
            VoiceAppLogger.debug(TAG, "Returning from sendDeviceToken since userId or hostname are not set");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        OkHttpClient client = new OkHttpClient();

        deviceTokenMessage = "Device Token not yet sent";
        deviceTokenState = DeviceTokenState.DEVICE_TOKEN_NOT_SENT;
        VoiceAppLogger.debug(TAG, "sendDeviceToken, token: " + deviceToken + " hostname: " + hostname + " userId: " + userId);
        String url = hostname + "/accounts/" + accountSid + "/subscribers/" + userId + "/devicetoken";
        VoiceAppLogger.debug(TAG, "Device token request is: " + url);

        try {
            jsonObject.put("deviceToken", deviceToken);
        } catch (JSONException e) {
            VoiceAppLogger.error(TAG, "Error in creating device token body");
            deviceTokenState = DeviceTokenState.DEVICE_TOKEN_SEND_FAILURE;
            deviceTokenMessage = "Failed to create request Body";
            throw new Exception(e.getMessage());
        }

        RequestBody body = RequestBody.create(JSON, jsonObject.toString());

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                VoiceAppLogger.error(TAG, "sendDeviceTokenAysnc: Failed to get response for send device token: "
                        + e.getMessage());
                deviceTokenMessage = "Device Token send failed";
                deviceTokenState = DeviceTokenState.DEVICE_TOKEN_SEND_FAILURE;
                for (DeviceTokenStatusEvents deviceTokenStatusEvents : deviceTokenListenerList) {
                    deviceTokenStatusEvents.deviceTokenStatusChange();
                }
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                VoiceAppLogger.debug(TAG, "sendDeviceTokenAysnc: Got response for send device token: " + response.code());
                if (200 != response.code() && 201 != response.code()) {
                    deviceTokenMessage = "Device Token send received: " + String.valueOf(response.code());
                    deviceTokenState = DeviceTokenState.DEVICE_TOKEN_SEND_FAILURE;

                    SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(context);
                    sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.DEVICE_TOKEN.toString(), deviceToken);
                } else {
                    deviceTokenMessage = null;
                    deviceTokenState = DeviceTokenState.DEVICE_TOKEN_SEND_SUCCESS;
                }
                for (DeviceTokenStatusEvents deviceTokenStatusEvents : deviceTokenListenerList) {
                    deviceTokenStatusEvents.deviceTokenStatusChange();
                }
            }
        });
    }

    public DeviceTokenStatus getDeviceTokenStatus() {
        VoiceAppLogger.debug(TAG, "getDeviceTokenStatus: state: " + deviceTokenState + " Message: " + deviceTokenMessage);
        DeviceTokenStatus deviceTokenStatus = new DeviceTokenStatus();
        deviceTokenStatus.setDeviceTokenState(deviceTokenState);
        deviceTokenStatus.setDeviceTokenStatusMessage(deviceTokenMessage);
        return deviceTokenStatus;
    }

    public void addDeviceTokenListener(DeviceTokenStatusEvents deviceTokenStatusEvents) {
        deviceTokenListenerList.add(deviceTokenStatusEvents);
    }

    public void removeDeviceTokenListener(DeviceTokenStatusEvents deviceTokenStatusEvents) {
        List<DeviceTokenStatusEvents> removeList = new ArrayList<>();
        for (DeviceTokenStatusEvents events : deviceTokenListenerList) {
            VoiceAppLogger.debug(TAG, "Listener is: " + events + " Class is : " + events.getClass().getName());
            if (deviceTokenStatusEvents.getClass().getName().equals(events.getClass().getName())) {
                removeList.add(events);
            }
        }
        deviceTokenListenerList.removeAll(removeList);
    }

    public void setCallContextListener(CallContextEvents callContextListener) {
        this.callContextListener = callContextListener;
    }

    void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID,
                    "Exotel Voice Sample", NotificationManager.IMPORTANCE_HIGH);
            serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = context.getSystemService(NotificationManager.class);

            manager.createNotificationChannel(serviceChannel);
        }
    }

    Notification
    createNotification(CallState state, String destination, String callId, CallDirection callDirection) {

        Intent notificationIntent;
        String text;
        //callState = state;
        VoiceAppLogger.info(TAG, "Creating notification, callState: " + state +
                " destination: " + destination + " callId: " + callId);

        if (CallDirection.OUTGOING == callDirection) {
            SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(context);
            destination = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.LAST_DIALLED_NO.toString());
        }

        if (CallState.OUTGOING_INITIATED == state) {
            notificationIntent = new Intent(context, CallActivity.class);
            text = "Connecting ..." + destination;
        } else if (CallState.RINGING == state) {
            notificationIntent = new Intent(context, CallActivity.class);
            text = "Ringing ..." + destination;
        } else if (CallState.INCOMING == state) {
            notificationIntent = new Intent(context, CallActivity.class);
            text = "Incoming Call ..." + destination;
            notificationIntent.putExtra("callState", CallState.RINGING);
        } else if (CallState.ANSWERING == state) {
            notificationIntent = new Intent(context, CallActivity.class);
            text = "Answering.." + destination;
        } else if (CallState.ENDING == state) {
            notificationIntent = new Intent(context, CallActivity.class);
            text = "Ending.." + destination;
        } else if (CallState.MEDIA_DISRUPTED == state) {
            notificationIntent = new Intent(context, CallActivity.class);
            text = "Reconnecting ..." + destination;
        } else if (CallState.NONE == state) {
            notificationIntent = new Intent(context, HomeActivity.class);
            text = "Received a Notification";
        } else {
            notificationIntent = new Intent(context, CallActivity.class);
            text = "In Call ..." + destination;
        }

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (null != callId) {
            notificationIntent.putExtra("callId", callId);
            VoiceAppLogger.debug(TAG, "Setting call ID in the intent to: " + callId);
        }
        if (null != destination) {
            notificationIntent.putExtra("destination", destination);
            VoiceAppLogger.debug(TAG, "Setting desitnation in the intent to: " + destination);
        }

        /* https://stackoverflow.com/questions/7370324/notification-passes-old-intent-extras?noredirect=1&lq=1 */
        int iUniqueId = (int) (System.currentTimeMillis() & 0xfffffff);
        PendingIntent pendingIntent = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            pendingIntent = PendingIntent.getActivity(context,iUniqueId,notificationIntent,PendingIntent.FLAG_MUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(context, iUniqueId, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        }

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Exotel Voice Application")
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_call_24dp)
                .build();

        return notification;
    }

    public void launchCallActivity(Call call) {
        VoiceAppLogger.debug(TAG, "Launching incoming call Activity");
        Intent intent = new Intent(context, CallActivity.class);
        intent.putExtra("callId", call.getCallDetails().getCallId());
        intent.putExtra("destination", call.getCallDetails().getRemoteId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        VoiceAppLogger.debug(TAG, "Launching incoming call activity");

        context.startActivity(intent);
        /* Get the message sent as part of call */
        getCallContext(call.getCallDetails().getRemoteId());
    }

    public void setCallContext(String userId, String destination, String message) {
        JSONObject jsonObject = new JSONObject();
        OkHttpClient client = new OkHttpClient();

        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(context);
        String url = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.APP_HOSTNAME.toString());
        String accountSid = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.ACCOUNT_SID.toString());
        url = url + "/accounts/" + accountSid + "/subscribers/" + userId + "/context";

        VoiceAppLogger.debug(TAG, "setCallContext URL is: " + url);
        VoiceAppLogger.debug(TAG, "setCallContext Destination is: " + destination);
        VoiceAppLogger.debug(TAG, "setCallContext userID is: " + userId);
        VoiceAppLogger.debug(TAG, "setCallContext message is: " + message);
        try {
            jsonObject.put("dialToNumber", destination);
            jsonObject.put("message", message);
        } catch (JSONException e) {
            VoiceAppLogger.error(TAG, "Error in creating device token body");
            return;
        }

        RequestBody body = RequestBody.create(JSON, jsonObject.toString());

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                VoiceAppLogger.error(TAG, "setCallContext: Failed to get response"
                        + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                VoiceAppLogger.debug(TAG, "setCallContext: Got response for setCallContext: " + response.code());


            }
        });
    }

    public void getCallContext(String remoteId) {
        OkHttpClient client = new OkHttpClient();

        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(context);
        String url = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.APP_HOSTNAME.toString());
        String accountSid = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.ACCOUNT_SID.toString());
        url = url + "/accounts/" + accountSid + "/subscribers/" + remoteId + "/context";
        VoiceAppLogger.debug(TAG, "getCallContext: URL is: " + url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                VoiceAppLogger.error(TAG, "getCallContext: Failed to get response"
                        + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                VoiceAppLogger.debug(TAG, "getCallContext: Got response for getCallContext: " + response.code());
                String jsonData;
                jsonData = response.body().string();
                JSONObject jObject;
                VoiceAppLogger.debug(TAG, "getCallContext: Response body is: " + jsonData);
                String contextMessage;
                SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(context);
                sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.CONTEXT_MESSAGE.toString(), "");
                try {
                    jObject = new JSONObject(jsonData);

                    contextMessage = jObject.getString("message");


                    if (contextMessage.isEmpty() || contextMessage.equals("null")) {
                        response.body().close();
                        return;
                    }


                    VoiceAppLogger.debug(TAG, "getCallContext: Context Message is: " + contextMessage);

                } catch (JSONException e) {
                    response.body().close();
                    return;
                }

                VoiceAppLogger.debug(TAG, "getCallContext: Setting shared Preferences");
                sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.CONTEXT_MESSAGE.toString(), contextMessage);
                if (null != callContextListener) {
                    VoiceAppLogger.debug(TAG, "getCallContext: sending callback");
                    callContextListener.onGetContextSuccess();
                }

                response.body().close();


            }
        });
    }

    public void removeCallContext(String userId) {
        OkHttpClient client = new OkHttpClient();
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(context);
        String url = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.APP_HOSTNAME.toString());
        String accountSid = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.ACCOUNT_SID.toString());
        url = url + "/accounts/" + accountSid + "/subscribers/" + userId + "/context";
        VoiceAppLogger.debug(TAG, "Remove call context URL is: " + url);

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                VoiceAppLogger.error(TAG, "removeCallContext: Failed to get response"
                        + e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                VoiceAppLogger.debug(TAG, "removeCallContext: Got response for removeCallContext: " + response.code());

            }
        });

    }


    public String getUpdatedNumberToDial(String destination) {
        if (null == destination) {
            VoiceAppLogger.error(TAG, "getUpdatedNumberToDial: Invalid number passed");
            return null;
        }
        if (debugDiallingEnabled) {
            return destination;
        }
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(context);
        return sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.EXOPHONE.toString());
    }

    /*This method is only used for contact list calling, as this overrides
        debug dialing is enabled or not.*/
    public String getNumberToDialForContact(String destination, boolean isRegularNumber) {
        if (destination == null) {
            VoiceAppLogger.error(TAG, "getUpdatedNumberToDial: Invalid number passed");
            return null;
        }
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(context);
        String destinationExophone = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.EXOPHONE.toString());
        if (isRegularNumber) {
            return destinationExophone;
        }
        return destination;
    }

    void login(String hostname, String accountSid, String username, String password, Callback callback) {
        JSONObject jsonObject = new JSONObject();

        VoiceAppLogger.debug(TAG, "Calling login API");
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        VoiceAppLogger.debug(TAG, "Android ID is: " + androidId);
        String url = hostname + "/login";
        try {
            jsonObject.put("user_name", username);
            jsonObject.put("password", password);
            jsonObject.put("account_sid", accountSid);
            jsonObject.put("device_id", androidId);
        } catch (JSONException e) {
            VoiceAppLogger.error(TAG, "Error in create login request body");
            return;

        }

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, jsonObject.toString());

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    void setDebugDiallingMode(boolean enabled) {
        this.debugDiallingEnabled = enabled;
    }

    Boolean isRefreshTokenValid(String regAuthToken) {
        VoiceAppLogger.debug(TAG, "isRefreshTokenValid: " + regAuthToken);

        try {
            JSONObject jsonObject = new JSONObject(regAuthToken);
            String refreshToken = jsonObject.getString("refresh_token");
            VoiceAppLogger.debug(TAG, "isRefreshTokenValid: " + refreshToken);

            String tokenParts[] = refreshToken.split("\\.");
            VoiceAppLogger.debug(TAG, "isRefreshTokenValid: " + tokenParts);

            String tokenPayload = tokenParts[1];
            VoiceAppLogger.debug(TAG, "isRefreshTokenValid: Token Payload " + tokenPayload);

            String tokenString = new String(Base64.decode(tokenPayload, Base64.URL_SAFE));
            VoiceAppLogger.debug(TAG, "isRefreshTokenValid: Token String " + tokenString);
            JSONObject regAuthTokenJson = new JSONObject(tokenString);
            VoiceAppLogger.debug(TAG, "isRefreshTokenValid: JSON " + regAuthTokenJson.toString());
            long expTime = regAuthTokenJson.getInt("exp");
            VoiceAppLogger.debug(TAG, "isRefreshTokenValid: Token Expiry " + expTime);

            long curTime = System.currentTimeMillis();
            if (curTime > expTime) {
                VoiceAppLogger.debug(TAG, "Refresh Token Expired");
                return false;
            }
        } catch (JSONException | NullPointerException e) {
            VoiceAppLogger.error(TAG, "Unable to decode refresh token " + e.getMessage());
        }

        return true;
    }

    public void  makeIPCall(VoiceAppService mService, String number, String destination) {
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(context);
        sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.LAST_DIALLED_NO.toString(),destination);
        destination = "sip:"+destination;
        makeCall(mService,number,destination);
    }
    public void  makeWhatsAppCall(VoiceAppService mService, String destination) {
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(context);
        String exophone = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.EXOPHONE.toString());
        sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.LAST_DIALLED_NO.toString(),destination);
        destination = "wa:"+destination;
        makeCall(mService,exophone,destination);
    }

    public void makeCall(VoiceAppService mService, String phone, String destination) {
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(context);
        String subscriberName = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.USER_NAME.toString());
        String contextMessage = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.CONTACT_DISPLAY_NAME.toString());

        VoiceAppLogger.debug(TAG,"Initiating outgoing call");
        Call call = null;
        try {
            VoiceAppLogger.debug(TAG,"Making dial API call to sample service");
            call = mService.dial(phone, contextMessage);
        } catch (Exception e) {
            String errorMessage = "Outgoing call Failed:";
            errorMessage = errorMessage + e.getMessage();
            VoiceAppLogger.debug(TAG,"Exception is: "+e.getMessage());
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
            return;
        }

        if(null != call){
            setCallContext(subscriberName,destination,"");
            Intent intent = new Intent(context,
                    CallActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("callId",call.getCallDetails().getCallId());
            intent.putExtra("destination",call.getCallDetails().getRemoteId());
            context.startActivity(intent);
            //finish();
        }
    }
}
