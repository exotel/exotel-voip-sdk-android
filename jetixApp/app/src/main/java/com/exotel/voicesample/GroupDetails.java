package com.exotel.voicesample;

import java.util.ArrayList;
import java.util.List;

public class GroupDetails {

    private List<ContactDetails> contactDetailsList = new ArrayList<>();
    private String groupName;
    private boolean isSpecial;

    public GroupDetails() {
        contactDetailsList = new ArrayList<ContactDetails>();
    }

    public List<ContactDetails> getChildItemList() {
        return contactDetailsList;
    }

    public void setChildItemList(List<ContactDetails> contactDetailsList) {
        this.contactDetailsList = contactDetailsList;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public boolean isSpecial() {
        return isSpecial;
    }

    public void setSpecial(boolean special) {
        isSpecial = special;
    }
}

class ContactDetails {

    private String contactName;
    private String contactNumber;

    public ContactDetails() {
    }

    public ContactDetails(String contactName, String contactNumber, String contactType) {
        this.contactName = contactName;
        this.contactNumber = contactNumber;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

}