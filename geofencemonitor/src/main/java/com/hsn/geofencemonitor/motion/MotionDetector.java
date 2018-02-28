package com.hsn.geofencemonitor.motion;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.Handler;


/**
 * Created by hassanshakeel on 2/27/18.
 */

public class MotionDetector extends TriggerEventListener {

    private Context context;
    private final Handler handler = new Handler();
    private SensorManager sensorManager;
    private Sensor sigMotion;
    private long lastMovement = -1;
    private final long MAX_TIME_FOR_VALID_MOTION = 15 * 1000;

    public MotionDetector(Context context) throws Exception {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sigMotion = sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
        if (sigMotion == null) {
            throw new Exception("Significant motion Sensor not found");
        }
    }

    public void startDetectingMotion() {
        if (sigMotion != null) {
            sensorManager.requestTriggerSensor(this, sigMotion);
        }
    }

    public void stopDetectingMotion() {
        sensorManager.cancelTriggerSensor(this, sigMotion);
    }

    public boolean isMoving() {
        long diff = System.currentTimeMillis() - lastMovement;
        return diff < MAX_TIME_FOR_VALID_MOTION;
    }

    @Override
    public void onTrigger(TriggerEvent triggerEvent) {
        lastMovement = System.currentTimeMillis();
        sensorManager.requestTriggerSensor(this, sigMotion);

    }
}
