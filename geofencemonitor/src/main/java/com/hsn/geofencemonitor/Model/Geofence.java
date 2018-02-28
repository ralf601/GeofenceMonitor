package com.hsn.geofencemonitor.Model;

/**
 * Created by hassanshakeel on 2/27/18.
 */

public class Geofence {

    public static final int STATE_OUTSIDE = 0;
    public static final int STATE_INSIDE = 1;

    private String id,alias;
    private Double lat,lon;
    private Float radius;
    private int state = 0;

    public Geofence(String id, String alias, Double lat, Double lon, Float radius) {
        this.id = id;
        this.alias = alias;
        this.lat = lat;
        this.lon = lon;
        this.radius = radius;
    }

    /**
     * Unique id representing a particular geofence, can be used later in order to remove a geofence from monitoring
     * @return unique id
     */
    public String getId() {
        return id;
    }


    /**
     *
     * @return
     */
    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    /**
     * Alias for the geofence
     * @return alias
     */
    public String getAlias() {
        return alias;
    }

    public Float getRadius() {
        return radius;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
