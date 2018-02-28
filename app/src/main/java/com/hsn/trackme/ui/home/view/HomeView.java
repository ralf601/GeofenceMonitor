package com.hsn.trackme.ui.home.view;

import android.location.Location;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.hsn.trackme.model.MyGeofence;
import com.hsn.trackme.model.Notification;
import com.hsn.trackme.ui.common.view.ViewMvc;

import java.util.List;

/**
 * Created by hassanshakeel on 2/27/18.
 */

public interface HomeView extends ViewMvc {

    interface HomeInteractor {
        void onNotificationClicked(Notification notification);
        void addGeofence(LatLng latLng);

    }

    void setupMap(GoogleMap googleMap);

    void bindGeofence(List<MyGeofence> geofences);


    void bindNotificationHistory(List<Notification> notification);


    void updateLocationProvider(String provider);


    void setInteractor(HomeInteractor interactor);

    void updateCurrentLocation(Location location);

}
