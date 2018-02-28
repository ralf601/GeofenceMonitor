package com.hsn.geofencemonitor;

import android.location.Location;

import com.hsn.geofencemonitor.Model.Geofence;

import java.util.List;

/**
 * Created by hassanshakeel on 2/27/18.
 */

public interface GeofenceMonitor {

    /*
    Geofence event indicating entering or leaving a geofence
     */
    enum GeofenceEvent {
        Enter,
        Exit
    }

    /**
     * Listener for geofence updates, like when user enter or leaves a geofence
     */
    interface GeofenceEventListener {
        /**
         * Invoked when a geofence event occur
         *
         * @param event    event type, if user is leaving or entering a geofence
         * @param geofence event related to the geofence
         */
        void onEvent(GeofenceEvent event, Geofence geofence);


        void onLocationUpdated(Location location);
    }

    /**
     * Listener for geofence updates, like when user enter or leaves a geofence
     */
    interface OnLocationProviderChangeListener {
        void currentProvider(String provider);
    }



    /**
     * Add a geofence to monitor
     *
     * @param geofence
     */
    void addGeofence(Geofence geofence);

    /**
     * @param id geofence id to remove
     * @return geofence object if its currently being monitored
     */
    Geofence removeGeofence(String id);

    /**
     * Add list of geofence to monitor
     *
     * @param geofence
     */
    void addGeofence(List<Geofence> geofence);

    /**
     * Start monitoring geofences
     */
    void startMonitor();


    /**
     * @return list of all geofences that are being monitored
     */
    List<Geofence> getMonitoredRegions();


    /**
     * Listner for geofence events
     * @param geofenceEventListener
     */
    void setGeofenceEventListener(GeofenceEventListener geofenceEventListener);

    /**
     * Listener for current location provider
     * @param locationProviderChangeListener
     */
    void setOnLocationProviderChangeListener(OnLocationProviderChangeListener locationProviderChangeListener);

    /**
     * Stop monitoring geofences
     */
    void stopMonitoring();


    /**
     * Use siginificant motion strategy to monitor geofence
     * @param enabled enable or disable strategy
     */
    void setMotionStrategy(boolean enabled);

}
