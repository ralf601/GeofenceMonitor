package com.hsn.trackme.common;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;

import com.google.android.gms.maps.model.LatLng;
import com.hsn.trackme.R;
import com.hsn.trackme.model.MyGeofence;
import com.hsn.trackme.model.Notification;
import com.hsn.trackme.ui.common.controller.MainActivity;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import io.realm.Realm;

import static android.content.Context.NOTIFICATION_SERVICE;

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
        int notifyID = 1;
        String CHANNEL_ID = "my_channel_01";
        android.app.Notification notification =
                new NotificationCompat.Builder(context)
                        .setContentTitle(title)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentText(message)
                        .setSound(RingtoneManager.getActualDefaultRingtoneUri(context,RingtoneManager.TYPE_NOTIFICATION))
                        .setChannelId(CHANNEL_ID).build();


        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);
        }

// Issue the notification.
        mNotificationManager.notify((int) (System.currentTimeMillis()/1000), notification);
    }


    public static void showEnableGpsDialog(final Activity activity) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                activity);
        alertDialogBuilder
                .setMessage("Please enable gps")
                .setCancelable(false)
                .setPositiveButton("enable",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                activity.startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }
}
