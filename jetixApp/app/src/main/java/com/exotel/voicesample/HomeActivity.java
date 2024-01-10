/*
 * Copyright (c) 2019 Exotel Techcom Pvt Ltd
 * All rights reserved
 */
package com.exotel.voicesample;

import android.content.ComponentName;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import com.exotel.voice.CallIssue;
import com.exotel.voice.ExotelVoiceError;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.Observer;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;



import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Date;


public class HomeActivity extends AppCompatActivity implements VoiceAppStatusEvents, DeviceTokenStatusEvents, LogUploadEvents {

    private static String TAG = "HomeActivity";
    private VoiceAppService mService;
    private boolean mBound;

    private TextView textBox;
    private ApplicationUtils mApplicationUtils;
    private long DAY_IN_MS = 1000 * 60 * 60 * 24;
    private int UPLOAD_LOG_NUM_DAYS = 7;

    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VoiceAppLogger.setContext(getApplicationContext());
        VoiceAppLogger.info(TAG, "In onCreate for Home Activity");


        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_activity_toolbar);
        setSupportActionBar(toolbar);
        VoiceAppLogger.debug(TAG, "Setting context for logger");

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.addTab(tabLayout.newTab().setText("Dial"));
        tabLayout.addTab(tabLayout.newTab().setText("Contacts"));
        tabLayout.addTab(tabLayout.newTab().setText("Recent Calls"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        VoiceAppLogger.debug(TAG, "Setting Tabs");
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.d(TAG, "Tab selected with position: " + tab.getPosition());
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                Log.d(TAG, "Tab unselected with position: " + tab.getPosition());
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Log.d(TAG, "Tab reselected with position: " + tab.getPosition());
            }
        });
        VoiceAppLogger.debug(TAG, "Getting application utils");
        mApplicationUtils = ApplicationUtils.getInstance(getApplicationContext());
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
        TextView curUserId = findViewById(R.id.toolbarTextUsername);
        String userId = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.USER_NAME.toString());
        curUserId.setText(userId);
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.getInstance().setUserId(userId);
        crashlytics.setCustomKey(ApplicationSharedPreferenceData.USER_NAME.toString(), userId);
        VoiceAppLogger.debug(TAG, "Exit from onCreate");

    }

    @Override
    protected void onStart() {
        super.onStart();
        VoiceAppLogger.debug(TAG, "in onStart Method for Activity HACK");
        Intent intent = new Intent(this, VoiceAppService.class);
        try {
            startService(intent);
            bindService(intent, connection, BIND_AUTO_CREATE);
            mApplicationUtils.addDeviceTokenListener(this);
        } catch (IllegalStateException e) {
            VoiceAppLogger.error(TAG, "Exception in starting service: " + e.getMessage());
        }
        VoiceAppLogger.debug(TAG, "Exit: onStart HomeActivity");
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        VoiceAppLogger.debug(TAG, "Received New Intent");
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        VoiceAppLogger.debug(TAG, "on Resume HomeActivity");
        if(!mBound) {
            VoiceAppLogger.debug(TAG, "onResume HomeActivity : service not bound hence re-binding.");
            Intent intent = new Intent(this, VoiceAppService.class);
            try {
                startService(intent);
                bindService(intent, connection, BIND_AUTO_CREATE);
                mApplicationUtils.addDeviceTokenListener(this);
            } catch (IllegalStateException e) {
                VoiceAppLogger.error(TAG, "Exception in starting service: " + e.getMessage());
            }
        }
        VoiceAppLogger.debug(TAG, "Exit: onResume HomeActivity");
    }

    @Override
    protected void onPause() {
        super.onPause();

        VoiceAppLogger.debug(TAG, "onPause callback, removing event Listener");
    }

    @Override
    protected void onStop() {
        super.onStop();
        VoiceAppLogger.debug(TAG, "onStop Method HomeActivity");
        VoiceAppLogger.info(TAG, "isFinishing is: " + isFinishing() + " Service bound: " + mBound);
        if (mBound) {
            unbindService(connection);
        }
        mApplicationUtils.removeDeviceTokenListener(this);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        VoiceAppLogger.info(TAG, "Back button has been pressed");
        VoiceAppLogger.info(TAG, "isFinishing is: " + isFinishing());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VoiceAppLogger.info(TAG, "ondestroy callback");
        VoiceAppLogger.info(TAG, "isFinishing is: " + isFinishing());

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        VoiceAppLogger.debug(TAG, "onRestart");
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        /**
         * [AP2AP-175] start
         * hide multi call based on Build Config variable
         */
        if(BuildConfig.HIDE_MULTI_CALL) {
            MenuItem menuItem = menu.findItem(R.id.enableMulticall);
            menuItem.setVisible(false);
            menuItem = menu.findItem(R.id.disableMulticall);
            menuItem.setVisible(false);
        }
        // [AP2AP-175] end
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        VoiceAppLogger.debug(TAG, "onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        VoiceAppLogger.debug(TAG, "onOptionsItemSelected");
        ApplicationUtils applicationUtils = ApplicationUtils.getInstance(getApplicationContext());
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
        switch (item.getItemId()) {
            case R.id.logout:
                logout();
                return true;

            case R.id.about:
                VoiceAppLogger.debug(TAG, "About button is clicked");
                AlertDialog alertDialog;
                AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
                builder.setTitle("SDK details");
                String message = mService.getVersionDetails();
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
                return true;

            case R.id.uploadLogs:
                VoiceAppLogger.debug(TAG, "Upload Logs is clicked");
                // get prompts.xml view
                LayoutInflater li = LayoutInflater.from(this);
                View promptsView = li.inflate(R.layout.report_problem_prompt, null, false);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        this);

                // set prompts.xml to alertdialog builder
                /*If the view already has a view, remove it. Can cause issues if not removed*/
                if (promptsView.getParent()!=null){
                    ((ViewGroup) promptsView.getParent()).removeView(promptsView);
                }
                alertDialogBuilder.setView(promptsView);

                final EditText userInput = (EditText) promptsView
                        .findViewById(R.id.editTextDialogUserInput);

                // set dialog message
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // get user input and set it to result
                                        // edit text
                                        //result.setText(userInput.getText());
                                        String description = "";
                                        VoiceAppLogger.debug(TAG, "User Input text is: " + userInput.getText());
                                        Date endDate = new Date();
                                        Date startDate = new Date(endDate.getTime() - (UPLOAD_LOG_NUM_DAYS * DAY_IN_MS));
                                        //getSignedUrlForLogUpload();
                                        if (null == userInput.getText()) {
                                            description = "";
                                        } else {
                                            description = userInput.getText().toString();
                                        }
                                        mService.setLogUploadEventListener(HomeActivity.this);
                                        try {
                                            mService.uploadLogs(startDate, endDate, description);
                                        } catch (Exception e) {
                                            VoiceAppLogger.debug(TAG, "Exception while uploadLogs: " + e.getMessage());
                                        }
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                        VoiceAppLogger.debug(TAG, "Cancelling dialog");
                                    }
                                });

                // create alert dialog
                AlertDialog alertDialogReportProblem = alertDialogBuilder.create();
                // show it
                alertDialogReportProblem.show();
                return true;

            case R.id.accountDetails:
                VoiceAppLogger.debug(TAG, "Account Details is clicked");
                AlertDialog alertDialogAccountDetails;
                AlertDialog.Builder accountDetailsbuilder = new AlertDialog.Builder(HomeActivity.this);
                accountDetailsbuilder.setTitle("Account Details");

                String username = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.USER_NAME.toString());
                String accountSid = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.ACCOUNT_SID.toString());
                String hostname = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.APP_HOSTNAME.toString());

                String accountDetailsMsg;
                accountDetailsMsg = "Subscriber Name: " + username + "\n" + "\n";
                accountDetailsMsg = accountDetailsMsg + "Account SID: " + accountSid + "\n" + "\n";
                accountDetailsMsg = accountDetailsMsg + "Base URL: " + hostname;

                VoiceAppLogger.debug(TAG, accountDetailsMsg);
                accountDetailsbuilder.setMessage(accountDetailsMsg);
                accountDetailsbuilder.setCancelable(true);
                accountDetailsbuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        VoiceAppLogger.debug(TAG, "onClick of Dialog");
                        dialogInterface.cancel();
                    }
                });
                alertDialogAccountDetails = accountDetailsbuilder.create();
                alertDialogAccountDetails.show();
                return true;

            case R.id.postFeedback:
                VoiceAppLogger.debug(TAG, "Provide Feedback for last call");
                askForFeedback();
                return true;

                /*Removing debug dialing. Can enable this if required in future.*/
         /*   case R.id.enableDebugDialling:
                VoiceAppLogger.debug(TAG, "Enabling debug dialling");
                applicationUtils.setDebugDiallingMode(true);
                return true;

            case R.id.disableDebugDialling:
                VoiceAppLogger.debug(TAG, "Disabling debug dialling");
                applicationUtils.setDebugDiallingMode(false);
                return true;*/

            case R.id.enableMulticall:
                VoiceAppLogger.debug(TAG, "Enable MultiCall");
                sharedPreferencesHelper.putBoolean("enable_multicall", true);
                return true;

            case R.id.disableMulticall:
                VoiceAppLogger.debug(TAG, "Disable MultiCall");
                sharedPreferencesHelper.putBoolean("enable_multicall", false);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void askForFeedback() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.call_rating_prompt, null, false);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);


        // set prompts.xml to alertdialog builder
        if (promptsView.getParent()!=null){
            ((ViewGroup) promptsView.getParent()).removeView(promptsView);
        }
        alertDialogBuilder.setView(promptsView);
        Spinner spinner1 = promptsView.findViewById(R.id.spinner1);
        String[] ratingArray = new String[]{"1", "2", "3", "4", "5"};
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ratingArray);
        spinner1.setAdapter(adapter1);

        Spinner spinner2 = promptsView.findViewById(R.id.spinner2);
        String[] issueArray = new String[]{"NO_ISSUE", "ECHO", "NO_AUDIO", "HIGH_LATENCY", "CHOPPY_AUDIO", "BACKGROUND_NOISE"};
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, issueArray);
        spinner2.setAdapter(adapter2);

        int spinnerPos = adapter1.getPosition("3");
        spinner1.setSelection(spinnerPos);

        spinnerPos = adapter2.getPosition("NO_ISSUE");
        spinner2.setSelection(spinnerPos);
        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                // edit text
                                //result.setText(userInput.getText());
                                int rating = Integer.parseInt(spinner1.getSelectedItem().toString());
                                CallIssue issue = CallIssue.valueOf(spinner2.getSelectedItem().toString());
                                VoiceAppLogger.debug(TAG, "Rating: " + rating + " Issue: " + issue);
                                try {
                                    mService.postFeedback(rating, issue);
                                } catch (InvalidParameterException e) {
                                    VoiceAppLogger.error(TAG, "Error is posting feedback: " + e.getMessage());
                                }


                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                VoiceAppLogger.debug(TAG, "Cancelling dialog");
                            }
                        });

        // create alert dialog
        AlertDialog alertDialogReportProblem = alertDialogBuilder.create();
        // show it
        alertDialogReportProblem.show();
    }

    private void logout() {
        VoiceAppLogger.debug(TAG, "In logout");
        if (null != mService) {
            VoiceAppLogger.debug(TAG, "Calling reset of service");
            mService.reset();
        }
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
        sharedPreferencesHelper.putBoolean(ApplicationSharedPreferenceData.IS_LOGGED_IN.toString(), false);
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        VoiceAppLogger.debug(TAG, "Return from logout in HomeActivity");
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            VoiceAppLogger.debug(TAG, "Service connected in HomeActivity");
            VoiceAppService.LocalBinder binder = (VoiceAppService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.addStatusEventListener(HomeActivity.this);

            updateStatus();

            if (mService != null) {
                VoiceAppStatus voiceAppStatus = mService.getCurrentStatus();
                if (VoiceAppState.STATUS_READY != voiceAppStatus.getState()) {
                    VoiceAppLogger.debug(TAG, "Initializing the service");
                    try {
                        ApplicationUtils applicationUtils = ApplicationUtils.getInstance(getApplicationContext());
                        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
                        String regAuthToken = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.SUBSCRIBER_TOKEN.toString());

                        if (!applicationUtils.isRefreshTokenValid(regAuthToken)) {
                            String username = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.USER_NAME.toString());
                            String accountSid = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.ACCOUNT_SID.toString());
                            String appHostname = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.APP_HOSTNAME.toString());
                            String password = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.PASSWORD.toString());
                            applicationUtils.login(appHostname, accountSid, username, password, mCallback);

                            //String sdkHostname = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.SDK_HOSTNAME.toString());
                            //mService.initialize(sdkHostname, username, accountSid, regAuthToken);
                        }
                    } catch (Exception e) {
                        VoiceAppLogger.error(TAG, "Exception in service initialization: " + e.getMessage());
                        String exceptionMsg = "Exception in service initialization: " + e.getMessage();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView initStatus = findViewById(R.id.toolbarTextStatus);
                                initStatus.setText(exceptionMsg);

                                initStatus.setTextColor(Color.RED);
                            }
                        });
                    }
                }
            }
            VoiceAppLogger.zipOlderLogs();

            VoiceAppLogger.debug(TAG, "Return from onServiceConnected in Home Activity");

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            VoiceAppLogger.debug(TAG, "Service disconnected in HomeActivity");
            mBound = false;
        }
    };


    @Override
    public void onStatusChange() {
        VoiceAppLogger.debug(TAG, "Received On Status Change Event");
        updateStatus();
        VoiceAppLogger.debug(TAG, "Returning from On Satus Change Event in HomeActivity");
    }

    @Override
    public void onAuthFailure() {
        VoiceAppLogger.warn(TAG, "On Authentication Failure");
    }

    @Override
    public void deviceTokenStatusChange() {
        VoiceAppLogger.debug(TAG, "Received on Device Token Status change event");
        updateStatus();
    }


    private Callback mCallback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            VoiceAppLogger.error(TAG, "Failed to get response for login");
            SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(getApplicationContext());
            String username = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.USER_NAME.toString());
            String regAuthToken = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.SUBSCRIBER_TOKEN.toString());
            String sdkHostname = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.SDK_HOSTNAME.toString());
            String accountSid = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.ACCOUNT_SID.toString());
            String displayName = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.DISPLAY_NAME.toString());
            try {
                mService.initialize(sdkHostname, username, accountSid, regAuthToken, displayName);
            } catch (Exception exp) {
                VoiceAppLogger.error(TAG, "Exception in service initialization: " + e.getMessage());
                String exceptionMsg = "Exception in service initialization: " + e.getMessage();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView initStatus = findViewById(R.id.toolbarTextStatus);
                        initStatus.setText(exceptionMsg);

                        initStatus.setTextColor(Color.RED);
                    }
                });
            }

        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
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
                    String username = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.USER_NAME.toString());
                    String displayName = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.DISPLAY_NAME.toString());

                    sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.SUBSCRIBER_TOKEN.toString(), regAuthToken);
                    sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.SDK_HOSTNAME.toString(), sdkHostname);
                    sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.EXOPHONE.toString(), exophone);
                    sharedPreferencesHelper.putString(ApplicationSharedPreferenceData.CONTACT_DISPLAY_NAME.toString(), contactDisplayName);

                    mService.initialize(sdkHostname, username, accountSid, regAuthToken, displayName);
                } catch (Exception exp) {
                    VoiceAppLogger.error(TAG, "Exception in service initialization: " + exp.getMessage());
                    String exceptionMsg = "Exception in service initialization: " + exp.getMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView initStatus = findViewById(R.id.toolbarTextStatus);
                            initStatus.setText(exceptionMsg);

                            initStatus.setTextColor(Color.RED);
                        }
                    });
                }

            }
        }
    };

    private void updateStatus() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                ApplicationUtils utils = ApplicationUtils.getInstance(getApplicationContext());
                DeviceTokenStatus deviceTokenStatus = utils.getDeviceTokenStatus();
                TextView initStatus = findViewById(R.id.toolbarTextStatus);

                if (mService != null) {
                    VoiceAppStatus voiceAppStatus = mService.getCurrentStatus();
                    VoiceAppLogger.debug(TAG, "updateStatus: VoiceAppState: " + voiceAppStatus.getState()
                            + " deviceTokenState: " + deviceTokenStatus.getDeviceTokenState());
                    if (VoiceAppState.STATUS_READY == voiceAppStatus.getState()
                            && DeviceTokenState.DEVICE_TOKEN_SEND_SUCCESS == deviceTokenStatus.getDeviceTokenState()) {
                        initStatus.setText(voiceAppStatus.getMessage());
                        initStatus.setTextColor(Color.GREEN);
                    } else if (VoiceAppState.STATUS_READY != voiceAppStatus.getState()) {
                        initStatus.setText(voiceAppStatus.getMessage());
                        initStatus.setTextColor(Color.RED);
                    } else if (DeviceTokenState.DEVICE_TOKEN_SEND_SUCCESS != deviceTokenStatus.getDeviceTokenState()) {
                        initStatus.setText(deviceTokenStatus.getDeviceTokenStatusMessage());
                        initStatus.setTextColor(Color.RED);
                    }
                }
            }
        });
    }

    @Override
    public void onUploadLogSuccess() {
        String message = "Successfully reported";
        VoiceAppLogger.getSignedUrlForLogUpload();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public void onUploadLogFailure(ExotelVoiceError error) {
        String message = "Failed to report: " + error.getErrorMessage();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
        int mTabCount;

        public SectionsPagerAdapter(FragmentManager fm, int tabCount) {
            super(fm);
            mTabCount = tabCount;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            Log.d(TAG, "Calling getItem, position: " + position);
            switch (position) {
                case 0:
                    DialPadFragment tab1 = new DialPadFragment();
                    return tab1;
                case 1:
                    ContactsGroupFragment tab2 = new ContactsGroupFragment();
                    return tab2;
                case 2:
                    RecentCallsFragment tab3 = new RecentCallsFragment();
                    return tab3;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            Log.d(TAG, "Getting count, tabCount is: " + mTabCount);
            return mTabCount;
        }
    }
}

