package com.hsn.trackme.common;

import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.maps.model.LatLng;
import com.hsn.trackme.R;
import com.hsn.trackme.model.MyGeofence;
import com.hsn.trackme.model.Notification;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import io.realm.Realm;

/**
 * Created by hassanshakeel on 2/27/18.
 */

public class Utils {

    private static final SimpleDateFormat UiDateTimeFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:a"); // second example

    public static String getDateTimeForUi() {
        return UiDateTimeFormat.format(System.currentTimeMillis());

    }


    public static void addDummyNotifications() {
        Realm.getDefaultInstance().executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                MyGeofence geofence = new MyGeofence(new LatLng(21.417811, 39.854048), 2.2f, "TestFence");
                Notification notification = new Notification(geofence, getDateTimeForUi(), Notification.NOTIFICATION_ENTER_GEOFENCE);
                realm.insert(notification);
                Notification notification2 = new Notification(geofence, getDateTimeForUi(), Notification.NOTIFICATION_EXIT_GEOFENCE);
                realm.insert(notification2);
            }
        });
    }

    public static void sendNotification(Context context, String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "1");
        builder
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);


    }
}
