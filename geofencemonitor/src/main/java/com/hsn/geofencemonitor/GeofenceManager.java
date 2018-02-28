package com.hsn.geofencemonitor;

import android.content.Context;

import com.hsn.geofencemonitor.Model.Geofence;
import com.hsn.geofencemonitor.Service.GeofenceMonitorImpl;

/**
 * Created by hassanshakeel on 2/27/18.
 */

final public class GeofenceManager {

    /**
     *
     */
    public interface OnMonitorReadyListener {
        /**
         *
         */
        void onReady(GeofenceMonitor geofenceMonitor);
    }


    private static GeofenceMonitor geofenceMonitor;

    public static void getGeofenceMonitor(Context context, OnMonitorReadyListener listener) {
        if (geofenceMonitor == null) {
            synchronized (GeofenceManager.class) {
                geofenceMonitor = new GeofenceMonitorImpl(context, listener);
            }
        } else {
            listener.onReady(geofenceMonitor);
        }
    }


}
