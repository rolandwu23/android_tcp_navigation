package com.grok.akm.ctrlworks;

import android.Manifest;
import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

public class SensorService extends Service implements SensorEventListener {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;


    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;


    private float[] mAccelerometerData = new float[3];
    private float[] mMagnetometerData = new float[3];
    private JSONObject json;

    private int interval;
    private int accuracy;



    @Override
    public void onSensorChanged(SensorEvent event) {

        int sensorType = event.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                mAccelerometerData = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagnetometerData = event.values.clone();
                break;
            default:
                break;
        }

        float[] gravity = new float[9];
        float[] magnetic = new float[9];
        boolean rotationOK = SensorManager.getRotationMatrix(gravity,
                magnetic, mAccelerometerData, mMagnetometerData);

        float orientationValues[] = new float[3];
        if (rotationOK) {
            SensorManager.getOrientation(gravity, orientationValues);
        }

        for (int i = 0; i < orientationValues.length; i++) {
            orientationValues[i] = (float) Math.toDegrees(orientationValues[i]);
        }

        float azimuth = orientationValues[0];
        float pitch = orientationValues[1];
        float roll = orientationValues[2];


        if (TCPCommunicator.connect) {
            try {
                json.put("Yaw", azimuth);
                json.put("Pitch", pitch);
                json.put("Roll", roll);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            final JSONObject jsonReadyForSend = json;
            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    TCPCommunicator.writeToSocket(jsonReadyForSend);
                }
            });
            thread.start();


        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        stopLocationUpdates();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        json = new JSONObject();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(interval);
        mLocationRequest.setFastestInterval(interval + 4000);

        switch (accuracy){
            case 0:
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                break;
            case 1:
                mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                break;
            case 2:
                mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
                break;
            case 3:
                mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
                break;
        }
        startLocationUpdates();
    }


    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    try {
                        json.put("Latitude", location.getLatitude());
                        json.put("Longitude", location.getLongitude());
                        json.put("Altitude", location.getAltitude());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            };
        };

        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        interval = intent.getIntExtra("interval", 1000);
        accuracy = intent.getIntExtra("accuracy", 0);


        mSensorManager.registerListener(this,mAccelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this,mMagnetometer,SensorManager.SENSOR_DELAY_NORMAL);
        return Service.START_STICKY_COMPATIBILITY;
    }
}
