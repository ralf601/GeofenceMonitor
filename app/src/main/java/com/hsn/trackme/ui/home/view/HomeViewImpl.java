package com.hsn.trackme.ui.home.view;

import android.location.Location;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.hsn.trackme.R;
import com.hsn.trackme.model.MyGeofence;
import com.hsn.trackme.model.Notification;
import com.hsn.trackme.ui.home.adapter.NotificationHistoryAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hassanshakeel on 2/27/18.
 */

public class HomeViewImpl implements HomeView, NotificationHistoryAdapter.OnItemClickListener, View.OnClickListener {

    private GoogleMap mMap;
    private View mRootView;
    private RecyclerView notificationHistoryListView;
    private NotificationHistoryAdapter notificationHistoryAdapter;
    private HomeInteractor interactor;
    private Map<String, Circle> geofenceMap = new HashMap<>();
    private boolean cameraMoved = false;
    private TextView locationProvider;
    private Marker currLocationMarker;
    private View gpsEnableMessageView;
    private Switch motionStrategy;

    public HomeViewImpl(LayoutInflater inflater, ViewGroup container) {
        mRootView = inflater.inflate(R.layout.fragment_home, container, false);
        initView(mRootView);
        initListeners();
        setupNotificationList();
    }


    @Override
    public View getRootView() {
        return mRootView;
    }

    @Override
    public void initView(View view) {
        notificationHistoryListView = view.findViewById(R.id.list);
        locationProvider = view.findViewById(R.id.locationProvider);
        gpsEnableMessageView = view.findViewById(R.id.enableGpsWarning);
        motionStrategy = view.findViewById(R.id.swtch);

    }

    @Override
    public void initListeners() {
        gpsEnableMessageView.setOnClickListener(this);
        motionStrategy.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    interactor.enableSignificantMotionStrategy(b);
            }
        });
    }

    @Override
    public void setupMap(GoogleMap googleMap) {
        this.mMap = googleMap;

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                Log.i("Clicked",latLng.latitude+","+latLng.longitude);
                if (interactor != null)
                    interactor.addGeofence(latLng);
            }
        });

    }

    @Override
    public void bindGeofence(List<MyGeofence> geofences) {
        //delete geofence if needed
        for (String key : geofenceMap.keySet()) {
            boolean toRemove = true;
            for (MyGeofence geofence : geofences) {
                if (geofence.getId().equals(key)) {
                    toRemove = false;
                    continue;
                }
            }
            if (toRemove)
                geofenceMap.remove(key).remove();

        }
        //add new geofence if needed
        for (MyGeofence geofence : geofences) {
            if (geofenceMap.get(geofence.getLocation().toString()) == null) {
                //need to add geofence
                CircleOptions circleOptions = new CircleOptions()
                        .center(geofence.getLocation())
                        .fillColor(R.color.colorAccent)
                        .radius(geofence.getRadius());

                Circle circle = mMap.addCircle(circleOptions);

                geofenceMap.put(geofence.getLocation().toString(), circle);

            }
        }

//        if (!cameraMoved) {
//            moveCameraFirstTime();
//            cameraMoved = true;
//        }
    }

    @Override
    public void bindNotificationHistory(List<Notification> notification) {
        notificationHistoryAdapter.update(notification);
    }

    @Override
    public void updateLocationProvider(String provider) {
        locationProvider.setText(provider);
    }

    @Override
    public void setInteractor(HomeInteractor interactor) {
        this.interactor = interactor;
    }

    @Override
    public void updateCurrentLocation(Location location) {

        if (currLocationMarker != null) {
            currLocationMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
            return;
        }
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(location.getLatitude(), location.getLongitude()));
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        currLocationMarker = mMap.addMarker(markerOptions);

        if (!cameraMoved) {
            cameraMoved = true;
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 10);
            mMap.animateCamera(cameraUpdate);
        }
    }

    @Override
    public void gpsEnabled(boolean enabled) {
        if (enabled) {
            gpsEnableMessageView.setVisibility(View.GONE);
            locationProvider.setVisibility(View.VISIBLE);
            motionStrategy.setVisibility(View.VISIBLE);
        } else {
            gpsEnableMessageView.setVisibility(View.VISIBLE);
            locationProvider.setVisibility(View.GONE);
            motionStrategy.setVisibility(View.GONE);

        }
    }

    @Override
    public void onClick(Notification notification) {
        if (interactor != null)
            interactor.onNotificationClicked(notification);

    }

    private void moveCameraFirstTime() {
        if (geofenceMap.size() == 0)
            return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Circle circle : geofenceMap.values()) {
            builder.include(circle.getCenter());
        }
        LatLngBounds bounds = builder.build();

        int width = getRootView().getContext().getResources().getDisplayMetrics().widthPixels;
        int height = getRootView().getContext().getResources().getDisplayMetrics().heightPixels;
        int padding = (int) (width * 0.10);
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
        mMap.animateCamera(cu);
    }

    private void setupNotificationList() {
        notificationHistoryListView.setLayoutManager(new LinearLayoutManager(mRootView.getContext()));
        notificationHistoryAdapter = new NotificationHistoryAdapter();
        notificationHistoryAdapter.setOnItemClickListener(this);
        notificationHistoryListView.setAdapter(notificationHistoryAdapter);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.enableGpsWarning:
                interactor.enableGps();
                break;
            default:
                break;
        }
    }
}
