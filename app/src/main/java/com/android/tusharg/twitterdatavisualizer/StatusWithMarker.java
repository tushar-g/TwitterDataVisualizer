package com.android.tusharg.twitterdatavisualizer;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import twitter4j.Status;

/**
 * Created by tushar on 29/04/16.
 */
public class StatusWithMarker {
    public twitter4j.Status mStatus;
    public Marker mMarker;

    public StatusWithMarker(Status s, Marker m) {
        mStatus = s;
        mMarker = m;
    }
}
