package com.rr.bubtbustracker.model;

public class TripLocation {
    public double lat;
    public double lng;
    public long time;

    public TripLocation(double lat, double lng, long time) {
        this.lat = lat;
        this.lng = lng;
        this.time = time;
    }
}
