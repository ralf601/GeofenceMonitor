package com.hsn.geofencemonitor.Service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.util.Log;

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



    private static class LocationUpdateConfig {
        public int minLocationUpdateTime, minlocationUpdateDistance, type, minAccuracy;

        @Override
        public String toString() {
            switch (type) {
                case 0:
                    return "LowPower@" + minLocationUpdateTime / 1000 + "s"+"@"+minAccuracy+"m(Accuracy)";
                case 1:
                    return "MedPower@" + minLocationUpdateTime / 1000 + "s"+"@"+minAccuracy+"m(Accuracy)";
                case 2:
                    return "HighPower@" + minLocationUpdateTime / 1000 + "s"+"@"+minAccuracy+"m(Accuracy)";
            }
            return "unknown";
        }

        public LocationUpdateConfig(int minLocationUpdateTime,
                                    int minlocationUpdateDistance,
                                    int type,
                                    int minAccuracy) {
            this.minLocationUpdateTime = minLocationUpdateTime;
            this.minlocationUpdateDistance = minlocationUpdateDistance;
            this.minAccuracy = minAccuracy;
            this.type = type;
        }

        public static LocationUpdateConfig getLowPowerConfig() {
            return new LocationUpdateConfig(5 * 1000, 3, 0, 30);
        }

        public static LocationUpdateConfig getMidPowerConfig() {
            return new LocationUpdateConfig(2 * 1000, 2, 1, 15);
        }

        public static LocationUpdateConfig getHighPowerConfig() {
            return new LocationUpdateConfig(500, 0, 2, 5);
        }
    }


    private final int MAX_LAST_KNOWN_LOCATION_AGE = 15 * 1000; //if no location found in 15 seconds turn on gps provider


    private final String GPS_PROVIDER = LocationManager.GPS_PROVIDER;
    private final String NETWORK_PROVIDER = LocationManager.NETWORK_PROVIDER;
    private final String TAG = getClass().getSimpleName();
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
    private LocationUpdateConfig currentLocationUpdateConfig = LocationUpdateConfig.getMidPowerConfig();
    private boolean binded = false;

    private final Runnable tuneLocationUpdates = new Runnable() {
        @Override
        public void run() {
            //no gps fix
            if (geofenceMap.size() == 0) {
                handler.postDelayed(this, 1 * 1000);//keep checking  at every 1 second
                return;
            }

            if (lastLocation == null) {
                enableGpsAndNetworkProvider(currentLocationUpdateConfig);
            } else {
                if (motionStrategyEnabled) {
                    if (!motionDetector.isMoving()) {
                        //no significant movement so we don't want location updates
                        disableAllProviders();
                    } else {
                        //check if location updates are on
                        boolean locationUpdatesOn = networkProviderEnabled && gpsProviderEnabled;
                        if (!locationUpdatesOn && geofenceMap.size() > 0) {
                            //we need to turn on location updates as there is a significant movement
                            //enabling only network provider as it will be automatically switched to gps if needed
                            if (System.currentTimeMillis() - lastLocation.getTime() > MAX_LAST_KNOWN_LOCATION_AGE) {
                                //last location very old need new location from gps
                                enableGpsAndNetworkProvider(currentLocationUpdateConfig);
                            } else {
                                //location is good we should continue with network provider
                                enableNetworkProvider();
                            }
                        }
                    }
                } else {
                    //not using motion strategy
                    long diff = System.currentTimeMillis() - lastLocation.getTime();
                    if (diff > MAX_LAST_KNOWN_LOCATION_AGE) {
                        //last location very old need new location from gps
                        enableGpsAndNetworkProvider(currentLocationUpdateConfig);
                        //Log.d(TAG,"Location expired diff="+diff);
                    } else {
                        //location is good we should continue with network provider
                        enableNetworkProvider();
                        //Log.d(TAG,"Location valid from network provider="+diff);

                    }
                }
            }

            handler.postDelayed(this, 1 * 1000);//keep checking  at every 1 second

        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        binded=true;
        return mBinder;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (motionStrategyEnabled) {
            try {
                motionDetector = new MotionDetector(getApplicationContext());
                motionDetector.startDetectingMotion();
                motionStrategyEnabled = true;
            } catch (Exception e) {
                e.printStackTrace();
                motionStrategyEnabled = false;
            }
        }
        handler.post(tuneLocationUpdates);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void addGeofence(Geofence geofence) {
        //location needed as a new geofence added
        if (geofenceMap.get(geofence.getId()) == null) {
            geofenceMap.put(geofence.getId(), geofence);
            enableGpsAndNetworkProvider(currentLocationUpdateConfig);
        }
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
            addGeofence(geofence);
        }

    }

    @RequiresPermission("android.permission.ACCESS_FINE_LOCATION")
    @Override
    public void startMonitor() {
        enableGpsAndNetworkProvider(currentLocationUpdateConfig);
    }

    @RequiresPermission("android.permission.ACCESS_FINE_LOCATION")
    @Override
    public void stopMonitoring() {
        locationManager.removeUpdates(this);
    }

    @Override
    public void setMotionStrategy(boolean enabled) {
        if (enabled && motionDetector == null) {
            try {
                motionDetector = new MotionDetector(getApplicationContext());
                motionDetector.startDetectingMotion();
                motionStrategyEnabled = true;
            } catch (Exception e) {
                e.printStackTrace();
                motionStrategyEnabled = false;
            }
        } else {
            motionStrategyEnabled = false;
            if (motionDetector != null) {
                motionDetector.stopDetectingMotion();
                motionDetector = null;
            }

        }
    }

    @Override
    public boolean isInitialized() {
        return binded;
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
        if (location.hasAccuracy() && location.getAccuracy() <= currentLocationUpdateConfig.minAccuracy) {
            lastLocation = location;
            handleLocationChange();
        }
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
        if (distance == null) {
            //no geofences to monitor
            disableAllProviders();
            return;
        }
        if (distance < 20) {
            //need to turn on gps
            enableGpsAndNetworkProvider(LocationUpdateConfig.getHighPowerConfig());
        } else if (distance < 50) {
            //need to turn on gps or tweak config
            enableGpsAndNetworkProvider(LocationUpdateConfig.getMidPowerConfig());
        } else if (distance < 100) {
            //need to turn on gps
            enableGpsAndNetworkProvider(LocationUpdateConfig.getLowPowerConfig());
        } else {
            //enable network provider only
            currentLocationUpdateConfig = LocationUpdateConfig.getLowPowerConfig();
            enableNetworkProvider();
        }

    }

    private void disableAllProviders() {
        networkProviderEnabled = gpsProviderEnabled = false;
        locationManager.removeUpdates(GeofenceService.this);
        if (locationProviderChangeListener != null)
            locationProviderChangeListener.currentProvider("idle");
    }

    private void enableGpsAndNetworkProvider(LocationUpdateConfig locationUpdateConfig) {
        if (!gpsProviderEnabled || locationUpdateConfig.type != currentLocationUpdateConfig.type) {
            locationManager.requestLocationUpdates(GPS_PROVIDER, locationUpdateConfig.minLocationUpdateTime, locationUpdateConfig.minlocationUpdateDistance, this);
        }
        if (!networkProviderEnabled) {
            locationManager.requestLocationUpdates(NETWORK_PROVIDER, locationUpdateConfig.minLocationUpdateTime, locationUpdateConfig.minlocationUpdateDistance, this);
        }
        currentLocationUpdateConfig = locationUpdateConfig;
        networkProviderEnabled = gpsProviderEnabled = true;
        if (locationProviderChangeListener != null) {
            locationProviderChangeListener.currentProvider(GPS_PROVIDER + " | " + NETWORK_PROVIDER + " " + currentLocationUpdateConfig.toString());
        }

    }

    private void enableNetworkProvider() {
        //turn off all providers
        disableAllProviders();
        //turn on only network provider
        if (!networkProviderEnabled)
            locationManager.requestLocationUpdates(NETWORK_PROVIDER, currentLocationUpdateConfig.minLocationUpdateTime, currentLocationUpdateConfig.minlocationUpdateDistance, this);
        if (locationProviderChangeListener != null)
            locationProviderChangeListener.currentProvider(NETWORK_PROVIDER + " " + currentLocationUpdateConfig.toString());
        networkProviderEnabled = true;
    }

    private Float getNearestGeofenceDistance() {
        Float distanceToNearestGeoFenceEvent = null;
        float[] distance = new float[1];
        for (Geofence geofence : geofenceMap.values()) {
            Location.distanceBetween(geofence.getLat(), geofence.getLon(), lastLocation.getLatitude(), lastLocation.getLongitude(), distance);

            if (distance[0] > geofence.getRadius() - 1) { //to avoid unnecessary toggles
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

            if (distance[0] > geofence.getRadius() - 1) { //to avoid unnecessary toggles) {
                //outside
                if (geofence.getState() == Geofence.STATE_INSIDE) {
                    geofence.setState(Geofence.STATE_OUTSIDE);
                    geofenceMap.put(geofence.getId(), geofence);
                    if (geofenceEventListener != null) {
                        geofenceEventListener.onEvent(GeofenceEvent.Exit, geofence);
                    }
                }
            } else {
                //inside geofence
                if (geofence.getState() == Geofence.STATE_OUTSIDE) {
                    geofence.setState(Geofence.STATE_INSIDE);
                    geofenceMap.put(geofence.getId(), geofence);
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
