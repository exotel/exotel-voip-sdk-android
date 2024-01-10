/*
 * Copyright (c) 2019 Exotel Techcom Pvt Ltd
 * All rights reserved
 */
package com.exotel.voicesample;

import com.exotel.voice.Call;

import java.util.Date;

public interface CallEvents {


    void onCallInitiated(Call call);

    void onCallRinging(Call call);

    void onCallEstablished(Call call);

    void onCallEnded(Call call);

    void onMissedCall(String remoteUserId, Date time);

    void onMediaDisrupted(Call call);

    void onRenewingMedia(Call call);
}
