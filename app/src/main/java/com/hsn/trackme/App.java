package com.hsn.trackme;

import android.app.Application;

import com.hsn.trackme.common.Utils;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by hassanshakeel on 2/27/18.
 */

public class App extends Application {

    public void onCreate() {
        super.onCreate();
        setupRealm();
    }

    private void setupRealm() {
        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .schemaVersion(1)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(config);

        //Utils.addDummyNotifications();
    }

}
