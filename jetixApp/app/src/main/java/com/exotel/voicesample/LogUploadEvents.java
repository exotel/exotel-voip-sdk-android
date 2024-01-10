/*
 * Copyright (c) 2019 Exotel Techcom Pvt Ltd
 * All rights reserved
 */
package com.exotel.voicesample;

import com.exotel.voice.ExotelVoiceError;

public interface LogUploadEvents {

    void onUploadLogSuccess();

    void onUploadLogFailure(ExotelVoiceError error);

}
