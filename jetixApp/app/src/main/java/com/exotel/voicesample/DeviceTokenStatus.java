/*
 * Copyright (c) 2019 Exotel Techcom Pvt Ltd
 * All rights reserved
 */
package com.exotel.voicesample;

public class DeviceTokenStatus {


    private DeviceTokenState deviceTokenState;

    private String deviceTokenStatusMessage;

    public DeviceTokenState getDeviceTokenState() {
        return deviceTokenState;
    }

    public void setDeviceTokenState(DeviceTokenState deviceTokenState) {
        this.deviceTokenState = deviceTokenState;
    }

    public String getDeviceTokenStatusMessage() {
        return deviceTokenStatusMessage;
    }

    public void setDeviceTokenStatusMessage(String deviceTokenStatusMessage) {
        this.deviceTokenStatusMessage = deviceTokenStatusMessage;
    }
}
