package com.rr.bubtbustracker.interfaces;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public interface TripStatusListener {
    void tripStatus(long id, boolean start, String from, String to);

    void readLocationRoute(ArrayList<LatLng> list);
}
