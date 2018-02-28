package com.hsn.geofencemonitor.Service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;

import com.hsn.geofencemonitor.GeofenceMonitor;
import com.hsn.geofencemonitor.Model.Geofence;
import com.hsn.geofencemonitor.motion.MotionDetector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hassanshakeel on 2/27/18.
 */
@SuppressLint("MissingPermission")
public class GeofenceService extends Service implements GeofenceMonitor, LocationListener {

    public static final String ACTION_GEOFENCE_NOTIFICATION = "ACTION_GEOFENCE_NOTIFICATION";


    private final int DEFAULT_MIN_LOCATION_UPDATE_TIME = 1 * 1000; //seconds
    private final int DEFAULT_MIN_LOCATION_UPDATE_DISTANCE = 1;//meters
    private final String GPS_PROVIDER = LocationManager.GPS_PROVIDER;
    private final String NETWORK_PROVIDER = LocationManager.NETWORK_PROVIDER;

    private final IBinder mBinder = new GeofenceServiceBinder();
    private LocationManager locationManager;
    private MotionDetector motionDetector;
    private Map<String, Geofence> geofenceMap = new HashMap<>();
    private GeofenceEventListener geofenceEventListener;
    private OnLocationProviderChangeListener locationProviderChangeListener;
    private Location lastLocation;
    private final Handler handler = new Handler();
    private boolean motionStrategyEnabled = true;
    private boolean networkProviderEnabled = false;
    private boolean gpsProviderEnabled = false;


    private final Runnable toggleLocationUpdatesBasedOnMotion = new Runnable() {
        @Override
        public void run() {
            if (!motionDetector.isMoving() && lastLocation != null) {
                //no significant movement so we dont want location updates
                disableAllProviders();
            } else {
                //check if location updates are on
                boolean locationUpdatesOn = networkProviderEnabled && gpsProviderEnabled;
                if (!locationUpdatesOn && geofenceMap.size() > 0) {
                    //we need to turn on location updates as there is a significant movement
                    //enabling only network provider as it will be automatically switched to gps if needed
                    enableNetworkProvider();
                }
            }
            handler.postDelayed(this, 1 * 1000);//keep checking motion at every 1 second
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        try {
            motionDetector = new MotionDetector(getApplicationContext());
            motionDetector.startDetectingMotion();
            if (motionStrategyEnabled) {
                handler.post(toggleLocationUpdatesBasedOnMotion);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void addGeofence(Geofence geofence) {
        //location needed as a new geofence added
        geofenceMap.put(geofence.getId(), geofence);
        enableGpsAndNetworkProvider();
    }

    @Override
    public Geofence removeGeofence(String id) {
        return geofenceMap.remove(id);
    }

    @Override
    public void addGeofence(List<Geofence> geofences) {
        if (geofences.size() == 0)
            return;
        //location needed as a new geofence added
        for (Geofence geofence : geofences) {
            geofenceMap.put(geofence.getId(), geofence);
        }
        enableGpsAndNetworkProvider();

    }

    @RequiresPermission("android.permission.ACCESS_FINE_LOCATION")
    @Override
    public void startMonitor() {
        enableGpsAndNetworkProvider();
    }

    @RequiresPermission("android.permission.ACCESS_FINE_LOCATION")
    @Override
    public void stopMonitoring() {
        locationManager.removeUpdates(this);
    }

    @Override
    public List<Geofence> getMonitoredRegions() {
        return new ArrayList<>(geofenceMap.values());
    }

    @Override
    public void setGeofenceEventListener(GeofenceEventListener geofenceEventListener) {
        this.geofenceEventListener = geofenceEventListener;
    }

    @Override
    public void setOnLocationProviderChangeListener(OnLocationProviderChangeListener locationProviderChangeListener) {
        this.locationProviderChangeListener = locationProviderChangeListener;
    }


    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
        handleLocationChange();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @RequiresPermission("android.permission.ACCESS_FINE_LOCATION")
    private void handleLocationChange() {
        //update location
        if (geofenceEventListener != null)
            geofenceEventListener.onLocationUpdated(lastLocation);
        //send entry / exit events
        checkAndNotifyGeofenceEvent();
        Float distance = getNearestGeofenceDistance();
        if (distance < 100) {
            //need to turn on gps
            if (!gpsProviderEnabled)
                enableGpsAndNetworkProvider();
        } else {
            //enable network provider only
            enableNetworkProvider();
        }

    }

    private void disableAllProviders() {
        networkProviderEnabled = gpsProviderEnabled = false;
        locationManager.removeUpdates(GeofenceService.this);
        if (locationProviderChangeListener != null)
            locationProviderChangeListener.currentProvider("idle");
    }

    private void enableGpsAndNetworkProvider() {
        networkProviderEnabled = gpsProviderEnabled = true;
        locationManager.requestLocationUpdates(GPS_PROVIDER, DEFAULT_MIN_LOCATION_UPDATE_TIME, DEFAULT_MIN_LOCATION_UPDATE_DISTANCE, this);
        locationManager.requestLocationUpdates(NETWORK_PROVIDER, DEFAULT_MIN_LOCATION_UPDATE_TIME, DEFAULT_MIN_LOCATION_UPDATE_DISTANCE, this);
        if (locationProviderChangeListener != null)
            locationProviderChangeListener.currentProvider(GPS_PROVIDER + " | " + NETWORK_PROVIDER);

    }

    private void enableNetworkProvider() {
        //turn off all providers
        disableAllProviders();
        //turn on only network provider
        locationManager.requestLocationUpdates(NETWORK_PROVIDER, DEFAULT_MIN_LOCATION_UPDATE_TIME, DEFAULT_MIN_LOCATION_UPDATE_DISTANCE, this);
        if (locationProviderChangeListener != null)
            locationProviderChangeListener.currentProvider(NETWORK_PROVIDER);
        networkProviderEnabled = true;
    }

    private Float getNearestGeofenceDistance() {
        Float distanceToNearestGeoFenceEvent = null;
        float[] distance = new float[1];
        for (Geofence geofence : geofenceMap.values()) {
            Location.distanceBetween(geofence.getLat(), geofence.getLon(), lastLocation.getLatitude(), lastLocation.getLongitude(), distance);

            if (distance[0] > geofence.getRadius()) {
                //outside this geofence
                if (distanceToNearestGeoFenceEvent == null || (distance[0] - geofence.getRadius()) < distanceToNearestGeoFenceEvent) {
                    //getting least distance to a geofence event
                    distanceToNearestGeoFenceEvent = distance[0] - geofence.getRadius();
                }
            } else {
                //inside
                if (distanceToNearestGeoFenceEvent == null || geofence.getRadius() - distance[0] < distanceToNearestGeoFenceEvent) {
                    //inside geofence
                    distanceToNearestGeoFenceEvent = geofence.getRadius() - distance[0];
                }
            }
        }

        return distanceToNearestGeoFenceEvent;

    }

    private void checkAndNotifyGeofenceEvent() {
        float[] distance = new float[1];
        for (Geofence geofence : geofenceMap.values()) {
            Location.distanceBetween(geofence.getLat(), geofence.getLon(), lastLocation.getLatitude(), lastLocation.getLongitude(), distance);

            if (distance[0] > geofence.getRadius()) {
                //outside
                if (geofence.getState() == Geofence.STATE_INSIDE) {
                    geofence.setState(Geofence.STATE_OUTSIDE);
                    if (geofenceEventListener != null) {
                        geofenceEventListener.onEvent(GeofenceEvent.Exit, geofence);
                    }
                }
            } else {
                //inside geofence
                if (geofence.getState() == Geofence.STATE_OUTSIDE) {
                    geofence.setState(Geofence.STATE_INSIDE);
                    if (geofenceEventListener != null) {
                        geofenceEventListener.onEvent(GeofenceEvent.Enter, geofence);
                    }
                }
            }
        }
    }


    public class GeofenceServiceBinder extends Binder {

        public GeofenceMonitor getGeofenceMonitor() {
            return GeofenceService.this;
        }

    }

}
