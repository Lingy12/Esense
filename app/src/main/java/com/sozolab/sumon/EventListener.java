package com.sozolab.sumon;

import android.util.Log;

import com.sozolab.sumon.io.esense.esenselib.ESenseConfig;
import com.sozolab.sumon.io.esense.esenselib.ESenseEventListener;

public class EventListener implements ESenseEventListener {
    private String TAG = "ESenseEventListener";

    @Override
    public void onBatteryRead(double voltage) {

    }

    @Override
    public void onButtonEventChanged(boolean pressed) {

    }

    @Override
    public void onAdvertisementAndConnectionIntervalRead(int minAdvertisementInterval, int maxAdvertisementInterval, int minConnectionInterval, int maxConnectionInterval) {

    }

    @Override
    public void onDeviceNameRead(String deviceName) {

    }

    @Override
    public void onSensorConfigRead(ESenseConfig config) {
        Log.d(TAG, "Noted config changes");
    }

    @Override
    public void onAccelerometerOffsetRead(int offsetX, int offsetY, int offsetZ) {

    }
}
