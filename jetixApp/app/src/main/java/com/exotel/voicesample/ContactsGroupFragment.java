package com.exotel.voicesample;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.exotel.voice.Call;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ContactsGroupFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private final String TAG = ContactsGroupFragment.this.getClass().getSimpleName();
    private VoiceAppService mService;
    private boolean mBound;
    private DatabaseHelper databaseHelper;
    private Context context;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView listEmptyText;

    private ExpandableListViewAdapter adapter;
    private ExpandableListView expandableListView;
    private List<GroupDetails> groupDetailsList = new ArrayList<>();
    private SearchView searchView;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        VoiceAppLogger.debug(TAG, "onAttach for ContactsFragment");
        databaseHelper = DatabaseHelper.getInstance(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VoiceAppLogger.debug(TAG, "onCreate for ContactsFragment");
    }

    /* Hits the contact list API*/
    private void fetchContactList() {

        OkHttpClient okHttpClient = new OkHttpClient();
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.getInstance(context);
        String url = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.APP_HOSTNAME.toString());
        String accountSid = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.ACCOUNT_SID.toString());
        String username = sharedPreferencesHelper.getString(ApplicationSharedPreferenceData.USER_NAME.toString());

        url = url + "/accounts/" + accountSid + "/subscribers/" + username + "/contacts";
        VoiceAppLogger.debug(TAG, "contactApiUrl :" + url);
        groupDetailsList.clear();
        adapter.notifyDataSetChanged();
        swipeRefreshLayout.setRefreshing(true);
        Request request = new Request.Builder()
                .url(url)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                VoiceAppLogger.error(TAG, "getContactList: Failed to get response"
                        + e.getMessage());
                if (getActivity() != null)
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                        listEmptyText.setVisibility(View.VISIBLE);
                    });
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull Response response) throws IOException {
                VoiceAppLogger.debug(TAG, "response code :" + response.code());
                String jsonData;
                jsonData = response.body().string();
                JSONObject jsonObject;
                VoiceAppLogger.debug(TAG, "Response body: " + jsonData);

                try {
                    jsonObject = new JSONObject(jsonData);
                    JSONArray jsonArray = jsonObject.optJSONArray("response");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject innerObject = jsonArray.getJSONObject(i);

                        if (innerObject.optString("code").equalsIgnoreCase("200")) {
                            JSONObject data = innerObject.optJSONObject("data");

                            GroupDetails groupDetails = new GroupDetails();
                            List<ContactDetails> contactDetailsList = new ArrayList<>();

                            groupDetails.setGroupName(data.optString("group"));
                            groupDetails.setSpecial(data.optBoolean("is_special"));
                            JSONArray contactArray = data.optJSONArray("contacts");
                            for (int j = 0; j < contactArray.length(); j++) {
                                JSONObject contactObject = contactArray.optJSONObject(j);
                                ContactDetails contactDetails = new ContactDetails();
                                contactDetails.setContactName(contactObject.optString("contact_name"));
                                contactDetails.setContactNumber(contactObject.optString("contact_number"));
                                contactDetailsList.add(contactDetails);
                            }
                            if (getActivity() != null)
                                getActivity().runOnUiThread(() -> {
                                    groupDetails.setChildItemList(contactDetailsList);
                                    groupDetailsList.add(groupDetails);
                                });

                        } else {
                            VoiceAppLogger.error(TAG, "contacts group error:" + innerObject);
                            if (getActivity() != null)
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "No Contacts found", Toast.LENGTH_SHORT).show();
                                    swipeRefreshLayout.setRefreshing(false);
                                });
                        }

                    }
                    if (getActivity() != null)
                        getActivity().runOnUiThread(() -> {
                            swipeRefreshLayout.setRefreshing(false);
                            if (groupDetailsList.isEmpty())
                                listEmptyText.setVisibility(View.VISIBLE);
                            else listEmptyText.setVisibility(View.GONE);

                            /*For some reason the ui for expandable list isn't displaying, so this is kind of a hack,
                             * will check on this soon*/
                            for (int i = 0; i < adapter.getGroupCount(); i++) {
                                expandableListView.expandGroup(i);
                                expandableListView.collapseGroup(i);
                            }
                        });
                } catch (Exception e) {
                    VoiceAppLogger.error(TAG, "contact exception" + e.getMessage());
                    if (getActivity() != null)
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            swipeRefreshLayout.setRefreshing(false);
                            listEmptyText.setVisibility(View.VISIBLE);
                        });
                }
                response.body().close();
            }

        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        VoiceAppLogger.debug(TAG, "onCreateView for ContactsFragment");
        View view = inflater.inflate(R.layout.fragment_contact_groups, container, false);
        adapter = new ExpandableListViewAdapter(context, groupDetailsList);
        expandableListView = view.findViewById(R.id.expandableList);
        searchView = view.findViewById(R.id.contactSearchView);
        searchView.clearFocus();
        listEmptyText = view.findViewById(R.id.listEmptyText);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        expandableListView.setAdapter(adapter);
        fetchContactList();
        adapter.notifyDataSetChanged();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                VoiceAppLogger.debug(TAG,"search text = "+newText);
                if(newText.trim().length() == 0){
                    adapter.setFilterList(groupDetailsList);
                    return false;
                }
                List<GroupDetails> filterGroupDetailsList = new ArrayList<GroupDetails>();
                    for(GroupDetails groupDetails:groupDetailsList) {
                        GroupDetails filterGroupDetails = new GroupDetails();
                        filterGroupDetails.setGroupName(groupDetails.getGroupName());
                        filterGroupDetails.setSpecial(groupDetails.isSpecial());
                        List<ContactDetails> filteredContactDetails = new ArrayList<ContactDetails>();
                        for(ContactDetails contactDetails:groupDetails.getChildItemList()) {
                            if(contactDetails.getContactName().toLowerCase().startsWith(newText.toLowerCase()) ||
                                    contactDetails.getContactNumber().toLowerCase().startsWith(newText.toLowerCase())) {
                                filteredContactDetails.add(contactDetails);
                            }
                        }
                        filterGroupDetails.setChildItemList(filteredContactDetails);
                        if(filteredContactDetails.size() > 0 ) {
                            filterGroupDetailsList.add(filterGroupDetails);
                        }
                    }

                adapter.setFilterList(filterGroupDetailsList);
                return false;
            }
        });

        return view;
    }


    @Override
    public void onStart() {
        super.onStart();
        VoiceAppLogger.debug(TAG, "onStart for ContactsFragment");
        Intent intent = new Intent(getActivity(), VoiceAppService.class);
        if (getActivity() != null){
            getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
        VoiceAppLogger.debug(TAG, "onStop for ContactsFragment");
        if (mBound) {
            if (getActivity() != null)
                getActivity().unbindService(connection);
        }
    }

    @Override
    public void onRefresh() {
        fetchContactList();
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            VoiceAppLogger.debug(TAG, "Service connected in ContactsFragment");
            VoiceAppService.LocalBinder binder = (VoiceAppService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            VoiceAppLogger.debug(TAG, "Return from onServiceConnected in ContactsFragment");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            VoiceAppLogger.debug(TAG, "Service disconnected in HomeActivity");
            mBound = false;
        }
    };

    class ExpandableListViewAdapter extends BaseExpandableListAdapter {

        private final String TAG = this.getClass().getSimpleName();


        private final class ViewHolder {
            TextView contactName;
            TextView contactNumber;
            ImageView callButton;

            ImageView whatsappButton;
            TextView groupName;
            Drawable arrowDown;
            Drawable arrowUp;
        }

        private List<GroupDetails> itemList;
        private final LayoutInflater inflater;
        private Context context;

        public ExpandableListViewAdapter(Context context, List<GroupDetails> itemList) {
            this.inflater = LayoutInflater.from(context);
            this.itemList = itemList;
            this.context = context;
        }

        @Override
        public ContactDetails getChild(int groupPosition, int childPosition) {

            return itemList.get(groupPosition).getChildItemList().get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return itemList.get(groupPosition).getChildItemList().size();
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
                                 final ViewGroup parent) {
            View resultView = convertView;
            ExpandableListViewAdapter.ViewHolder holder;


            if (resultView == null) {

                resultView = inflater.inflate(R.layout.list_contact, null,false);
                holder = new ExpandableListViewAdapter.ViewHolder();
                holder.contactName = resultView.findViewById(R.id.contact_name);
                holder.contactNumber = resultView.findViewById(R.id.contact_number);
                holder.callButton = resultView.findViewById(R.id.call_button);
                holder.whatsappButton = resultView.findViewById(R.id.whatsapp_button);

                resultView.setTag(holder);
            } else {
                holder = (ExpandableListViewAdapter.ViewHolder) resultView.getTag();
            }

            final ContactDetails item = getChild(groupPosition, childPosition);
            GroupDetails groupDetails = getGroup(groupPosition);

            holder.contactName.setText(item.getContactName());
            holder.contactNumber.setText(item.getContactNumber());
            if(groupDetails.getGroupName().contains("Exotel")) {
                holder.whatsappButton.setVisibility(View.VISIBLE); // show whatspp button for exotel group
            } else {
                holder.whatsappButton.setVisibility(View.GONE);
            }
            holder.callButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    VoiceAppLogger.debug(TAG, "Call button tapped");
                    String contactNumber = item.getContactNumber();
                    String number;
                    ApplicationUtils utils = ApplicationUtils.getInstance(context.getApplicationContext());
                    boolean isSpecialGroup = groupDetails.isSpecial();
                    if (isSpecialGroup) {
                        number = utils.getNumberToDialForContact(contactNumber, false);
                    } else
                        number = utils.getNumberToDialForContact(contactNumber, true);
                    VoiceAppLogger.debug(TAG, "Dialing number from contacts: " + contactNumber);
                    utils.makeIPCall(mService,number,contactNumber);
                }
            });
            holder.whatsappButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    VoiceAppLogger.debug(TAG, "Whatsapp button tapped");
                    String contactNumber = item.getContactNumber();
                    String number;
                    ApplicationUtils utils = ApplicationUtils.getInstance(context.getApplicationContext());
                    VoiceAppLogger.debug(TAG, "Dialing whatsapp number from contacts: " + contactNumber);
                    utils.makeWhatsAppCall(mService,contactNumber);
                }
            });

            return resultView;
        }

        @Override
        public GroupDetails getGroup(int groupPosition) {
            return itemList.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return itemList.size();
        }

        @Override
        public long getGroupId(final int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View theConvertView, ViewGroup parent) {
            View resultView = theConvertView;
            ExpandableListViewAdapter.ViewHolder holder;

            if (resultView == null) {
                resultView = inflater.inflate(R.layout.group_item, null, false);
                holder = new ViewHolder();
                holder.groupName = resultView.findViewById(R.id.parentText);
                holder.arrowDown = context.getDrawable(R.drawable.ic_arrow_down);
                holder.arrowUp = context.getDrawable(R.drawable.ic_arrow_up);
                resultView.setTag(holder);
            } else {
                holder = (ViewHolder) resultView.getTag();
            }

            final GroupDetails item = getGroup(groupPosition);

            holder.groupName.setText(item.getGroupName());

            if (isExpanded)
                holder.groupName.setCompoundDrawablesWithIntrinsicBounds(
                        null, null, holder.arrowUp, null);
            else
                holder.groupName.setCompoundDrawablesWithIntrinsicBounds(
                        null, null, holder.arrowDown, null);

            return resultView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

       public  void setFilterList( List<GroupDetails> itemList) {
           this.itemList = itemList;
           notifyDataSetChanged();
       }

    }
}