package com.hsn.geofencemonitor.Service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.hsn.geofencemonitor.GeofenceManager;
import com.hsn.geofencemonitor.GeofenceMonitor;
import com.hsn.geofencemonitor.Model.Geofence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hassanshakeel on 2/27/18.
 */

public class GeofenceMonitorImpl implements GeofenceMonitor, ServiceConnection {


    private GeofenceManager.OnMonitorReadyListener monitorReadyListener;
    private Context context;
    private GeofenceEventListener geofenceEventListener;
    private Map<String, Geofence> geofenceMap = new HashMap<>();
    private GeofenceMonitor geofenceMonitor;
    boolean bounded = false;

    public GeofenceMonitorImpl(Context context, GeofenceManager.OnMonitorReadyListener listener) {
        this.context = context;
        this.monitorReadyListener = listener;
        if (!bounded) {
            Intent intent = new Intent(context, GeofenceService.class);
            context.bindService(intent, this, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void addGeofence(Geofence geofence) {
        checkReady();
        geofenceMonitor.addGeofence(geofence);
    }

    @Override
    public Geofence removeGeofence(String id) {
        checkReady();
        return geofenceMonitor.removeGeofence(id);
    }

    @Override
    public void addGeofence(List<Geofence> geofence) {
        checkReady();
        geofenceMonitor.addGeofence(geofence);
    }

    @Override
    public void startMonitor() {
        if (!bounded) {
            Intent intent = new Intent(context, GeofenceService.class);
            context.bindService(intent, this, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public List<Geofence> getMonitoredRegions() {
        checkReady();
        return geofenceMonitor.getMonitoredRegions();
    }

    @Override
    public void setGeofenceEventListener(GeofenceEventListener geofenceEventListener) {
        checkReady();
        geofenceMonitor.setGeofenceEventListener(geofenceEventListener);
    }

    @Override
    public void setOnLocationProviderChangeListener(OnLocationProviderChangeListener locationProviderChangeListener) {
        checkReady();
        geofenceMonitor.setOnLocationProviderChangeListener(locationProviderChangeListener);
    }

    @Override
    public void stopMonitoring() {
        if (bounded) {
            context.unbindService(this);
            bounded = false;
        }
    }

    @Override
    public void setMotionStrategy(boolean enabled) {
        checkReady();
        geofenceMonitor.setMotionStrategy(enabled);
    }

    @Override
    public boolean isInitialized() {
        return geofenceMonitor != null;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        GeofenceService.GeofenceServiceBinder binder = (GeofenceService.GeofenceServiceBinder) iBinder;
        geofenceMonitor = binder.getGeofenceMonitor();
        monitorReadyListener.onReady(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    private void checkReady() {
        if (geofenceMonitor == null) {
            throw new IllegalStateException("Monitor not ready yet");
        }
    }
}

