package com.hsn.trackme.model;

import com.google.android.gms.maps.model.LatLng;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Created by hassanshakeel on 2/27/18.
 */

public class MyGeofence extends RealmObject{

    @PrimaryKey
    private String id;

    private Float radius;
    private String tag;

    private Double lat,lon;

    public MyGeofence() {
    }

    public MyGeofence(LatLng location, Float radius, String tag) {
        this.lat = location.latitude;
        this.lon = location.longitude;
        this.radius = radius;
        this.tag = tag;
        this.id = location.toString();
    }

    public String getId() {
        return id;
    }

    public LatLng getLocation() {
        return new LatLng(lat,lon);
    }


    public Float getRadius() {
        return radius;
    }

    public String getTag() {
        return tag;
    }


}
