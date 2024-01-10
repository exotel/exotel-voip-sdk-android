/*
 * Copyright (c) 2019 Exotel Techcom Pvt Ltd
 * All rights reserved
 */
package com.exotel.voicesample;

public class VoiceAppStatus {


    private VoiceAppState state;

    private String message;

    public VoiceAppState getState() {
        return state;
    }

    public void setState(VoiceAppState state) {
        this.state = state;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
