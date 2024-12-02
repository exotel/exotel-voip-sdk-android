/*
 * Copyright (c) 2019 Exotel Techcom Pvt Ltd
 * All rights reserved
 */
package com.exotel.voicesample;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Response;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity implements VoiceAppStatusEvents, DeviceTokenStatusEvents {

    private static String TAG = "LoginActivity";
    //    private VoiceAppService mService;
    private boolean mBound;
    private String username;
    private String password;
    private String appHostname;
    private ProgressBar progressBar;
    private String subscriberToken;
    private String accountSid;
    private String sdkHostname;
    private String displayName;
    private String contactDisplayName;
    private VoiceAppService voiceAppService;
    private ApplicationUtils mApplicationUtils;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VoiceAppLogger.setContext(getApplicationContext());
        setContentView(R.layout.activity_login);
        voiceAppService = VoiceAppService.getInstance(this.getApplication().getApplicationContext());
        Button signInButton;
        EditText usernameText;
        Button reportProblemButton;
        EditText passwordText;
        EditText hostnameText;
        EditText accountSidText;
        EditText displayNameText;
        LinearLayout advancedSettingsLayout;
        TextView toggleAdvancedSettings;
        Drawable arrowDown;
        Drawable arrowRight;


        usernameText = findViewById(R.id.username);
        passwordText = findViewById(R.id.password);
        signInButton = findViewById(R.id.b_login);
        reportProblemButton = findViewById(R.id.b_report_problem);
        hostnameText = findViewById(R.id.hostname);
        accountSidText = findViewById(R.id.account_sid);
        displayNameText = findViewById(R.id.displayName);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        advancedSettingsLayout = findViewById(R.id.advancedSettingsLayout);
        advancedSettingsLayout.setVisibility(View.GONE);
        toggleAdvancedSettings = findViewById(R.id.toggleAdvancedSettings);

        arrowDown = getBaseContext().getResources().getDrawable(R.drawable.ic_arrow_down);
        arrowRight = getBaseContext().getResources().getDrawable(R.drawable.ic_arrow_right);

        VoiceAppLogger.debug(TAG, "onCreate for LoginActivity");

        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
        mApplicationUtils = ApplicationUtils.getInstance(getApplicationContext());
        mApplicationUtils.addDeviceTokenListener(this);
        username = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.USER_NAME.toString());
        password = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.PASSWORD.toString());
        appHostname = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.APP_HOSTNAME.toString());
        accountSid = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.ACCOUNT_SID.toString());
        displayName = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.DISPLAY_NAME.toString());
        usernameText.setText(username);
        passwordText.setText(password);
        displayNameText.setText(displayName);

        if (!appHostname.isEmpty()) {
            hostnameText.setText(appHostname);
        }

        if (!accountSid.isEmpty()) {
            accountSidText.setText(accountSid);
        }
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        reportProblemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VoiceAppLogger.debug(TAG, "Report problem button is clicked");
                //String message = "Not yet implemented";
                //Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                VoiceAppLogger.getSignedUrlForLogUpload();
            }
        });

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VoiceAppLogger.debug(TAG, "Sign In button has been clicked");

                if (View.VISIBLE == progressBar.getVisibility()) {
                    VoiceAppLogger.warn(TAG, "Sign-In already in progress");
                    return;
                }
                username = usernameText.getText().toString().trim();
                password = passwordText.getText().toString().trim();
                displayName = displayNameText.getText().toString().trim();
                VoiceAppLogger.debug(TAG, "Username is: [" + username + "]");
                accountSid = accountSidText.getText().toString().trim();
                appHostname = hostnameText.getText().toString();
                if (username.equals("")) {
                    createAlertDialog("User Name");
                } else if (password.trim().isEmpty()) {
                    createAlertDialog("Password");
                } else if (accountSid.trim().isEmpty()) {
                    createAlertDialog("Account SID");

                } else if (appHostname.trim().isEmpty()) {
                    createAlertDialog("Hostname");
                } else if (!appHostname.startsWith("http://") && !appHostname.startsWith("https://")) {
                    createAlertDialog("Hostname");
                } else if (displayName.isEmpty() || !isValidDisplayName(displayName)) {
                    createAlertDialog("a valid Display Name");
                } else {

                    VoiceAppLogger.debug(TAG, "Signing In");

                    progressBar.setVisibility(View.VISIBLE);
                    if (null == voiceAppService) {
                        VoiceAppLogger.debug(TAG, "Starting the service");

                        //sendTokenAndInitialize();
                        ApplicationUtils applicationUtils = ApplicationUtils.getInstance(getApplicationContext());
                        applicationUtils.login(appHostname, accountSid, username, password, mCallback);
//                        Intent serviceIntent = new Intent(LoginActivity.this, VoiceAppService.class);
//                        startService(serviceIntent);
                        VoiceAppLogger.debug(TAG, "Calling bind Service");
//                        bindService(serviceIntent, connection, BIND_AUTO_CREATE);
                    } else {
                        VoiceAppLogger.debug(TAG, "Calling sendTokenAndInitialize fron onClick");
                        ApplicationUtils applicationUtils = ApplicationUtils.getInstance(getApplicationContext());
                        applicationUtils.login(appHostname, accountSid, username, password, mCallback);

                    }
                }
            }

        });

        toggleAdvancedSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (advancedSettingsLayout.getVisibility() == View.VISIBLE) {
                    advancedSettingsLayout.setVisibility(View.GONE);
                    toggleAdvancedSettings.setCompoundDrawablesWithIntrinsicBounds(null, null, arrowRight, null);
                } else {
                    advancedSettingsLayout.setVisibility(View.VISIBLE);
                    toggleAdvancedSettings.setCompoundDrawablesWithIntrinsicBounds(null, null, arrowDown, null);
                }
            }
        });

        //changes the display name to user ID when the User ID is being typed.
        usernameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                displayNameText.setText(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        getFirebaseToken();

    }

    /*checking if the username is alpha-numeric value.
    Added to imitate the behaviour similar to iOS.
    Also calls wont be successful if there is a space or special character in the display name*/
    private boolean isValidDisplayName(String displayName) {
        String pattern = "^[a-zA-Z0-9]*$";
        Log.d("isValidName", displayName.matches(pattern) + " " + displayName);
        return displayName.matches(pattern);
    }


    private void getFirebaseToken() {
        /** https://exotel.atlassian.net/browse/AP2AP-42
         * change : FirebaseInstanceId has been deprecated.
         * https://firebase.google.com/docs/reference/android/com/google/firebase/iid/FirebaseInstanceId
         * */
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (!task.isSuccessful()) {
                    VoiceAppLogger.warn(TAG, "getInstanceId failed: " + task.getException());
                    return;
                }

                // Get new Instance ID token
                String token = task.getResult();

                VoiceAppLogger.info(TAG, "onComplete for getFirebase Token");
                VoiceAppLogger.debug(TAG, "Token is: " + token);

                // Log and toast
                String msg = getString(R.string.msg_token_fmt, token);
                VoiceAppLogger.debug(TAG, msg);
                SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
                sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.DEVICE_TOKEN.toString(), token);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        VoiceAppLogger.debug(TAG, "In onStart for LoginActivity");
        askForPermissions();

    }


    private void sendTokenAndInitialize() {

        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());

        sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.USER_NAME.toString(), username);
        sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.PASSWORD.toString(), password);
        sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.APP_HOSTNAME.toString(), appHostname);
        sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.ACCOUNT_SID.toString(), accountSid);
        sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.DISPLAY_NAME.toString(), displayName);

        /* Login and Get Details */

        VoiceAppLogger.debug(TAG, "In sendTokenAndInitialize");
        try {
            ApplicationUtils utils = ApplicationUtils.getInstance(getApplicationContext());
            String deviceToken = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.DEVICE_TOKEN.toString());
            utils.sendDeviceToken(deviceToken, appHostname, username, accountSid);
        } catch (Exception e) {
            String message = "Exception in send Device Token";
            VoiceAppLogger.debug(TAG, message);
            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            VoiceAppLogger.debug(TAG, "Calling initialize in LoginActivity");
            voiceAppService.initialize(sdkHostname, username, accountSid, subscriberToken, displayName);
        } catch (Exception e) {
            String message = "Exception in initialization";
            if (null != e.getMessage()) {
                message = message + e.getMessage();
            }
            VoiceAppLogger.debug(TAG, message);
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();

        }
    }

    private Callback mCallback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    VoiceAppLogger.error(TAG, "login:failed " + e.getMessage());
                    progressBar.setVisibility(View.INVISIBLE);
                    String message = "Login Failed: " + e.getMessage();
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });

        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            VoiceAppLogger.debug(TAG, "login: Got response for Login: " + response.code());
            String jsonData;
            jsonData = response.body().string();
            JSONObject jObject;
            String exophone;
            VoiceAppLogger.debug(TAG, "Get regAuth Token response is: " + jsonData);
            if (200 == response.code() || 201 == response.code()) {
                try {
                    jObject = new JSONObject(jsonData);
                    subscriberToken = jObject.getString("subscriber_token");
                    sdkHostname = jObject.getString("host_name");
                    accountSid = jObject.getString("account_sid");
                    exophone = jObject.getString("exophone");
                    contactDisplayName = jObject.getString("contact_display_name");
                } catch (Exception e) {
                    response.body().close();
                    return;
                }

                SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
                sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.SUBSCRIBER_TOKEN.toString(), subscriberToken);
                sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.SDK_HOSTNAME.toString(), sdkHostname);
                sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.ACCOUNT_SID.toString(), accountSid);
                sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.EXOPHONE.toString(), exophone);
                sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.CONTACT_DISPLAY_NAME.toString(), contactDisplayName);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sendTokenAndInitialize();
                    }
                });

            } else if (403 == response.code()) {
                VoiceAppLogger.error(TAG, "Failed to login, wrong credentials");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.INVISIBLE);
                        String message = "Login Failed: wrong credentials";
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                VoiceAppLogger.error(TAG, "Failed to login");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.INVISIBLE);
                        String message = "Login Failed";
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }
    };

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
  /*  private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            VoiceAppLogger.debug(TAG, "Service connected in LoginActivity");
            VoiceAppService.LocalBinder binder = (VoiceAppService.LocalBinder) service;
            mService = binder.getService();
            mService.addStatusEventListener(LoginActivity.this);
            mBound = true;

            //sendTokenAndInitialize();
            ApplicationUtils applicationUtils = ApplicationUtils.getInstance(getApplicationContext());
            applicationUtils.login(appHostname, accountSid, username, password, mCallback);

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
           *//* VoiceAppLogger.debug(TAG, "Service disconnected in LoginActivity");
            mBound = false;*//*
        }
    };
*/
    @Override
    public void onStatusChange() {
        VoiceAppLogger.debug(TAG, "Received On Status Change in LoginActivty");


        processStatusChange();
        VoiceAppLogger.debug(TAG, "Returning From On Status Change in LoginActivty");
    }

    @Override
    public void onAuthFailure() {
        VoiceAppLogger.debug(TAG, "On Authentication failure");
    }

    @Override
    public void deviceTokenStatusChange() {
        VoiceAppLogger.debug(TAG, "deviceToken Change event");
        processStatusChange();
    }

    private void processStatusChange() {
        if (null == voiceAppService) {
            VoiceAppLogger.warn(TAG, "Service not yet connected, not processing");
            return;
        }
        DeviceTokenStatus deviceTokenStatus = mApplicationUtils.getDeviceTokenStatus();
        VoiceAppLogger.debug(TAG, " Device Token status: " + deviceTokenStatus.getDeviceTokenState());

        /*We are no longer checking for sdk initialisation before login is Successful
         * only checking for Device token state*/
        if (DeviceTokenState.DEVICE_TOKEN_SEND_SUCCESS == deviceTokenStatus.getDeviceTokenState()) {
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            VoiceAppLogger.debug(TAG, "Starting HomeActivity");
            voiceAppService.removeStatusEventListener(LoginActivity.this);
            SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
            sharedPreferencesHelper.putBoolean(ApplicationSharedPreferenceData.IS_LOGGED_IN.toString(), true);
            startActivity(intent);
            VoiceAppLogger.debug(TAG, "Finishing the current activity");
            finish();
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    VoiceAppStatus voiceAppStatus = voiceAppService.getCurrentStatus();
                    if (VoiceAppState.STATUS_INITIALIZATION_FAILURE == voiceAppStatus.getState()) {
                        String message = voiceAppStatus.getMessage();
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.INVISIBLE);
                    } else if (DeviceTokenState.DEVICE_TOKEN_SEND_FAILURE == deviceTokenStatus.getDeviceTokenState()) {
                        String message = deviceTokenStatus.getDeviceTokenStatusMessage();
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.INVISIBLE);

                    }
                }
            });
        }
    }

    private void createAlertDialog(String missingField) {
        AlertDialog alertDialog;
        VoiceAppLogger.debug(TAG, "Creating Alert dialog for missing " + missingField);
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        //builder.setTitle("Incoming Call");
        String message = "Please Enter " + missingField;
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                VoiceAppLogger.debug(TAG, "onClick of Dialog");
                dialogInterface.cancel();
            }
        });
        alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        VoiceAppLogger.debug(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        VoiceAppLogger.debug(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        VoiceAppLogger.debug(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VoiceAppLogger.debug(TAG, "Service bound is: " + mBound);
//        mApplicationUtils.removeDeviceTokenListener(this);
       /* if (mBound) {
            VoiceAppLogger.debug(TAG, "Unbinding the service");
            unbindService(connection);



        }*/
        voiceAppService.removeStatusEventListener(this);
//        voiceAppService = null;
        VoiceAppLogger.debug(TAG, "onDestroy");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        VoiceAppLogger.debug(TAG, "onRestart");
    }


    private void askForPermissions() {
        VoiceAppLogger.debug(TAG, "Asking for permissions");
        ArrayList<String> requestPermissionList = new ArrayList<String>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkAndAddToRequestPermissionList(requestPermissionList, Manifest.permission.POST_NOTIFICATIONS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkAndAddToRequestPermissionList(requestPermissionList, Manifest.permission.BLUETOOTH_CONNECT);
        }
        checkAndAddToRequestPermissionList(requestPermissionList, android.Manifest.permission.RECORD_AUDIO);
        checkAndAddToRequestPermissionList(requestPermissionList, Manifest.permission.READ_PHONE_STATE);
        /** https://exotel.atlassian.net/browse/AP2AP-42
         * change : modified logic for requesting multiple permission at once.
         **/
        if (!requestPermissionList.isEmpty()) {
            String permissionListStr[] = requestPermissionList.toArray(new String[requestPermissionList.size()]);
            ActivityCompat.requestPermissions(this, permissionListStr, 1);
        }
        requestPermissionList.clear();
    }

    private void checkAndAddToRequestPermissionList(ArrayList<String> requestPermissionList, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            VoiceAppLogger.info(TAG, "permission will be asked for " + permission);
            requestPermissionList.add(permission);
        } else {
            VoiceAppLogger.info(TAG, "permission is already granted for " + permission);
        }
    }


}
