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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.exotel.voice.Call;

import java.util.ArrayList;
import java.util.List;

public class RecentCallsFragment extends Fragment {


    private static String TAG = "RecentCallsFragment";
    private DatabaseHelper databaseHelper;
    private MyListAdaper myListAdaper;
    private List<RecentCallDetails> callList = new ArrayList<>();
    private VoiceAppService mService;
    private boolean mBound;
    private Context context;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        VoiceAppLogger.debug(TAG, "onAttach for RecentCallsFragment");
        databaseHelper = DatabaseHelper.getInstance(context);
        this.context = context;
        updateCallList();
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        VoiceAppLogger.debug(TAG, "onCreate for RecentCallsFragment");
    }

    private void updateCallList() {
        VoiceAppLogger.debug(TAG,"In function to update Call List");
        List<RecentCallDetails> tempCallList;
        tempCallList = databaseHelper.getAllData();
        callList.clear();
        callList.addAll(tempCallList);
        VoiceAppLogger.debug(TAG,"After update, size of call List is: "+callList.size());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_recent_calls, container, false);
        VoiceAppLogger.debug(TAG, "onCreateView for RecentCallsFragment");
        VoiceAppLogger.debug(TAG,"Call List size in onCreateView is: "+callList.size());
        myListAdaper = new MyListAdaper(getActivity(), R.layout.list_view, callList);

        ListView lv = (ListView) view.findViewById(R.id.mobile_list);
        lv.setAdapter(myListAdaper);

        myListAdaper.notifyDataSetChanged();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        VoiceAppLogger.debug(TAG, "onStart for RecentCallsFragment");
        Intent intent = new Intent(getActivity(), VoiceAppService.class);
        getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
        updateCallList();
        myListAdaper.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        VoiceAppLogger.debug(TAG, "onResume for RecentCallsFragment");
    }

    @Override
    public void onPause() {
        super.onPause();
        VoiceAppLogger.debug(TAG, "onPause for RecentCallsFragment");
    }

    @Override
    public void onStop() {
        super.onStop();
        VoiceAppLogger.debug(TAG, "onStop for RecentCallsFragment");
        if(mBound) {
            getActivity().unbindService(connection);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        VoiceAppLogger.debug(TAG, "onDestroyView for RecentCallsFragment");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        VoiceAppLogger.debug(TAG, "onDestroy for RecentCallsFragment");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        VoiceAppLogger.debug(TAG, "onDetach for RecentCallsFragment");
    }




    private class MyListAdaper extends ArrayAdapter<RecentCallDetails> {
        private int layout;
        private List<RecentCallDetails> mObjects;

        private MyListAdaper(Context context, int resource, List<RecentCallDetails> objects) {
            super(context, resource, objects);
            mObjects = objects;
            layout = resource;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder mainViewholder = null;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(layout, parent, false);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.callerId = (TextView) convertView.findViewById(R.id.list_item_callerid);
                viewHolder.direction = (TextView) convertView.findViewById(R.id.list_item_call_direction);
                viewHolder.time = (TextView) convertView.findViewById(R.id.list_item_time);
                viewHolder.callButton = (ImageView) convertView.findViewById(R.id.list_item_button);
                viewHolder.whatsappButton = (ImageView) convertView.findViewById(R.id.list_item_whatsapp_button);
                viewHolder.whatsappButton.setVisibility(View.GONE); //hide whatsapp button
                convertView.setTag(viewHolder);
            }
            mainViewholder = (ViewHolder) convertView.getTag();
            mainViewholder.callButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Toast.makeText(getContext(), "Button was clicked for list item " + position, Toast.LENGTH_SHORT).show();
                    String destination = getItem(position).getRemoteId();
                    ApplicationUtils utils = ApplicationUtils.getInstance(getActivity().getApplicationContext());
                    String number = utils.getUpdatedNumberToDial(destination);

                    utils.makeIPCall(mService,number,destination);
                }
            });
            mainViewholder.whatsappButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String destination = getItem(position).getRemoteId();
                    ApplicationUtils utils = ApplicationUtils.getInstance(getActivity().getApplicationContext());
                    utils.makeWhatsAppCall(mService,destination);
                }
            });
            mainViewholder.direction.setText(getItem(position).getCallType().toString());
            mainViewholder.callerId.setText(getItem(position).getRemoteId());
            mainViewholder.time.setText(getItem(position).getTime().toString());

            return convertView;
        }
    }

    public class ViewHolder {

        TextView callerId;
        TextView direction;
        TextView time;
        ImageView callButton;

        ImageView whatsappButton;
    }


    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            VoiceAppLogger.debug(TAG, "Service connected in RecentCallFragment");
            VoiceAppService.LocalBinder binder = (VoiceAppService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            VoiceAppLogger.debug(TAG, "Return from onServiceConnected in RecentCallFragment");

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            VoiceAppLogger.debug(TAG, "Service disconnected in HomeActivity");
            mBound = false;
        }


    };

}
