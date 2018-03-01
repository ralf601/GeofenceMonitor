package com.hsn.trackme.ui.home.controller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.hsn.geofencemonitor.GeofenceManager;
import com.hsn.geofencemonitor.GeofenceMonitor;
import com.hsn.geofencemonitor.Model.Geofence;
import com.hsn.trackme.R;
import com.hsn.trackme.common.Utils;
import com.hsn.trackme.model.MyGeofence;
import com.hsn.trackme.model.Notification;
import com.hsn.trackme.ui.common.controller.BaseFragment;
import com.hsn.trackme.ui.home.view.HomeView;
import com.hsn.trackme.ui.home.view.HomeViewImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by hassanshakeel on 2/27/18.
 */

public class HomeFragment extends BaseFragment
        implements OnMapReadyCallback, HomeView.HomeInteractor,
        GeofenceMonitor.GeofenceEventListener, GeofenceMonitor.OnLocationProviderChangeListener {


    private HomeView homeView;
    private GeofenceMonitor geofenceMonitor;
    private List<MyGeofence> geofences = new ArrayList<>();
    private boolean permissionsGaranted = false;
    private LocationManager locationManager;

    final BroadcastReceiver gpsStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            homeView.gpsEnabled(enabled);
            if (enabled && ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (last == null)
                    last = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (last == null)
                    last = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                if (last != null) {
                    onLocationUpdated(last);
                } else {
                    fetchLocationOnce();
                }

            }

        }
    };


    private void registerGpsStateChangeReceiver() {
        getActivity().registerReceiver(gpsStateChangeReceiver, new IntentFilter("android.location.PROVIDERS_CHANGED"));
    }

    private void unRegisterGpsStateChangeReceiver() {
        if (gpsStateChangeReceiver != null)
            getActivity().unregisterReceiver(gpsStateChangeReceiver);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        homeView = new HomeViewImpl(inflater, container);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        homeView.setInteractor(this);
        return homeView.getRootView();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Realm.getDefaultInstance().where(Notification.class).findAllAsync().addChangeListener(new RealmChangeListener<RealmResults<Notification>>() {
            @Override
            public void onChange(RealmResults<Notification> notifications) {
                homeView.bindNotificationHistory(notifications);
            }
        });
        checkPermissions();


    }

    private void setupGeofenceEvents() {
        GeofenceManager.getGeofenceMonitor(getActivity().getApplicationContext(), new GeofenceManager.OnMonitorReadyListener() {
            @Override
            public void onReady(GeofenceMonitor geofenceMonitor) {
                HomeFragment.this.geofenceMonitor = geofenceMonitor;
                HomeFragment.this.geofenceMonitor.setMotionStrategy(false);
                HomeFragment.this.geofenceMonitor.setGeofenceEventListener(HomeFragment.this);
                HomeFragment.this.geofenceMonitor.setOnLocationProviderChangeListener(HomeFragment.this);
                monitorGeofences(geofences);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        homeView.setupMap(googleMap);
        setupGps();

        Realm.getDefaultInstance()
                .where(MyGeofence.class)
                .findAllAsync()
                .addChangeListener(new RealmChangeListener<RealmResults<MyGeofence>>() {
                    @Override
                    public void onChange(RealmResults<MyGeofence> geofences) {
                        HomeFragment.this.geofences = geofences;
                        homeView.bindGeofence(geofences);
                        monitorGeofences(geofences);
                    }
                });
    }

    @Override
    public void onNotificationClicked(Notification notification) {
        //handle click action
    }

    @Override
    public void addGeofence(final LatLng latLng) {
        final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.geofence_dialog, null);
        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(dialogView)
                .show();


        final EditText nameField = dialogView.findViewById(R.id.tag);
        final EditText radiusField = dialogView.findViewById(R.id.radius);

        dialogView.findViewById(R.id.add_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(nameField.getText())) {
                    nameField.setError(getString(R.string.empty_error));
                } else if (TextUtils.isEmpty(nameField.getText())) {
                    radiusField.setError(getString(R.string.empty_error));
                } else {
                    MyGeofence geofence;
                    try {
                        geofence =
                                new MyGeofence(latLng, Float.parseFloat(radiusField.getText().toString()), nameField.getText().toString());
                    } catch (NumberFormatException e) {
                        radiusField.setError(getString(R.string.invalid_value_error));
                        return;

                    }
                    addGeofenceToDb(geofence);
                    dialog.dismiss();
                }
            }
        });

        dialogView.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });


    }

    @Override
    public void enableGps() {
        if (permissionsGaranted)
            Utils.showEnableGpsDialog(getActivity());
        else
            checkPermissions();
    }

    @Override
    public void enableSignificantMotionStrategy(boolean enable) {
        if (geofenceMonitor != null) {
            geofenceMonitor.setMotionStrategy(enable);
        }
    }


    private void monitorGeofences(List<MyGeofence> geofences) {
        if (geofenceMonitor == null || !permissionsGaranted)
            return;
        for (MyGeofence geofence : geofences) {
            geofenceMonitor.addGeofence(new Geofence(geofence.getId(), geofence.getTag(),
                    geofence.getLocation().latitude, geofence.getLocation().longitude, geofence.getRadius()));
        }

    }

    private void setupGps() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        registerGpsStateChangeReceiver();
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        homeView.gpsEnabled(enabled);
        if (enabled) {
            Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last == null)
                last = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (last == null)
                last = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (last != null)
                onLocationUpdated(last);
            else fetchLocationOnce();
        }

    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?


            // No explanation needed; request the permission
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    0);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.

        } else {
            // Permission has already been granted
            permissionsGaranted = true;
            setupGeofenceEvents();

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionsGaranted = true;
                    setupGps();
                    setupGeofenceEvents();
                } else {


                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private void fetchLocationOnce() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                onLocationUpdated(location);
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
        };
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);
    }

    private void addGeofenceToDb(final MyGeofence geofence) {

        Realm.getDefaultInstance().executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.insertOrUpdate(geofence);
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                Realm.getDefaultInstance()
                        .where(MyGeofence.class)
                        .findAllAsync()
                        .addChangeListener(new RealmChangeListener<RealmResults<MyGeofence>>() {
                            @Override
                            public void onChange(RealmResults<MyGeofence> geofences) {
                                HomeFragment.this.geofences = geofences;
                                homeView.bindGeofence(geofences);
                                monitorGeofences(geofences);
                            }
                        });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unRegisterGpsStateChangeReceiver();
    }

    @Override
    public void onEvent(final GeofenceMonitor.GeofenceEvent event, final Geofence geofence) {
        String message = (event == GeofenceMonitor.GeofenceEvent.Exit ? "Leaving from" : "Entering to") + " " + geofence.getAlias();
        Utils.sendNotification(getActivity(), geofence.getAlias(), message);
        Realm.getDefaultInstance().executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                MyGeofence geofence1 = new MyGeofence(new LatLng(geofence.getLat(), geofence.getLon()), geofence.getRadius(), geofence.getAlias());
                int type = event == GeofenceMonitor.GeofenceEvent.Exit ? Notification.NOTIFICATION_EXIT_GEOFENCE : Notification.NOTIFICATION_ENTER_GEOFENCE;
                Notification notification = new Notification(geofence1, Utils.getDateTimeForUi(), type);
                realm.insertOrUpdate(notification);
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                Realm.getDefaultInstance().where(Notification.class).findAllSortedAsync("dateTime", Sort.DESCENDING)
                        .addChangeListener(new RealmChangeListener<RealmResults<Notification>>() {
                            @Override
                            public void onChange(RealmResults<Notification> notifications) {
                                try {
                                    homeView.bindNotificationHistory(notifications);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
            }
        });
    }

    @Override
    public void onLocationUpdated(Location location) {
        homeView.updateCurrentLocation(location);
    }

    @Override
    public void currentProvider(String provider) {
        homeView.updateLocationProvider(provider);
    }
}
