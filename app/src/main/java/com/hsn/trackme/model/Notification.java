package com.hsn.trackme.model;

import io.realm.RealmObject;

/**
 * Created by hassanshakeel on 2/27/18.
 */

public class Notification extends RealmObject {

    public static final int NOTIFICATION_ENTER_GEOFENCE = 0;
    public static final int NOTIFICATION_EXIT_GEOFENCE = 1;

    private MyGeofence geofence;
    private String dateTime;
    private int notificationType;

    public Notification() {
    }
    public Notification(MyGeofence geofence, String dateTime, int notificationType) {
        this.geofence = geofence;
        this.dateTime = dateTime;
        this.notificationType = notificationType;
    }

    public MyGeofence getGeofence() {
        return geofence;
    }

    public String getDateTime() {
        return dateTime;
    }

    public int getNotificationType() {
        return notificationType;
    }

    public String getMessage() {
        return (notificationType == 0 ? "Entering to " : "Leaving from")
                + " " + geofence.getTag();
    }
}
