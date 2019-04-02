package com.grok.akm.ctrlworks;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import static com.grok.akm.ctrlworks.MainActivity.CHANNEL_ID;

public class SensorService extends Service implements SensorEventListener {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */

    private static final int NOTI_ID = 9999;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    private LocationManager mLocationManager;
    private GnssStatus.Callback callback;
    private GpsStatus.Listener listener;


    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;


    private float[] mAccelerometerData = new float[3];
    private float[] mMagnetometerData = new float[3];
    private JSONObject sensorJson;
    private JSONObject locationJson;

//    private int locationUpdateInterval;
    private int accuracy;

    private int locationInterval;
    private int sensorInterval;

    private Thread locationThread;
    private Thread sensorThread;

    boolean exit;


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


//        if (TCPCommunicator.connect) {
            try {
                sensorJson.put("Yaw", azimuth);
                sensorJson.put("Pitch", pitch);
                sensorJson.put("Roll", roll);
            } catch (JSONException e) {
                e.printStackTrace();
            }

//        }

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
        if(mLocationManager != null)  {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mLocationManager.unregisterGnssStatusCallback(callback);
            }else{
                mLocationManager.removeGpsStatusListener(listener);
            }
        }
        stopThread();
        stopForeground(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sensorJson = new JSONObject();
        locationJson = new JSONObject();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationStatus();

        createLocationRequest();

    }

    protected void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
//        mLocationRequest.setInterval(locationUpdateInterval);
//        mLocationRequest.setFastestInterval(locationUpdateInterval + 4000);
        mLocationRequest.setInterval(locationInterval);
        mLocationRequest.setFastestInterval(locationInterval + 4000);

        switch (accuracy) {
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


    private void locationStatus() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            callback = new GnssStatus.Callback() {
                @Override
                public void onStarted() {
                    super.onStarted();
                    try {
                        locationJson.put("Status","Started");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onStopped() {
                    super.onStopped();
                    try {
                        locationJson.put("Status","Stopped");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFirstFix(int ttffMillis) {
                    super.onFirstFix(ttffMillis);
                }

                @Override
                public void onSatelliteStatusChanged(GnssStatus status) {
                    super.onSatelliteStatusChanged(status);

                    int satellite = status.getSatelliteCount();
                    int count = 0;
                    for (int i = 0; i < satellite; i++) {
                        if (status.usedInFix(i)) {
                            count++;
                        }
                    }
                    try {
                        if (count == 0) {
                            locationJson.put("Status","No Fix");
                        } else if (count <= 3) {
                            locationJson.put("Status","2D");
                        } else {
                            locationJson.put("Status","3D");
                        }
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            };

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            mLocationManager.registerGnssStatusCallback(callback);
        } else {
            listener = new GpsStatus.Listener() {
                @Override
                public void onGpsStatusChanged(int event) {
                    if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {

                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);
                        Iterable<GpsSatellite> satellite = gpsStatus.getSatellites();
                        int count = 0;

                        for (GpsSatellite sat : satellite) {
                            if (sat.usedInFix()) {
                                count++;
                            }
                        }
                        try {
                            if (count == 0) {
                                locationJson.put("Status","No Fix");
                            } else if (count <= 3) {
                                locationJson.put("Status","2D");
                            } else {
                                locationJson.put("Status","3D");
                            }
                        }catch (JSONException e){
                            e.printStackTrace();
                        }

                    }
                }
            };

            mLocationManager.addGpsStatusListener(listener);

        }
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
                        locationJson.put("Latitude", location.getLatitude());
                        locationJson.put("Longitude", location.getLongitude());
                        locationJson.put("Altitude", location.getAltitude());

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

//        locationUpdateInterval = intent.getIntExtra("interval", 1000);
        accuracy = intent.getIntExtra("accuracy", 0);
        sensorInterval = intent.getIntExtra("SensorInterval",1000);
        locationInterval = intent.getIntExtra("LocationInterval",1000);

        startForeground();

        exit = false;

        final JSONObject jsonReadyForSend = sensorJson;
        sensorThread = new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                while(!exit) {
                    try {

                        TCPCommunicator.writeToSocket(jsonReadyForSend);
                        Thread.sleep(sensorInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        sensorThread.start();

        final JSONObject json = locationJson;
        locationThread = new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                while(!exit) {
                    try {
                        TCPCommunicator.writeToSocket(json);
                        Thread.sleep(locationInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        locationThread.start();

        mSensorManager.registerListener(this,mAccelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this,mMagnetometer,SensorManager.SENSOR_DELAY_NORMAL);
        return Service.START_STICKY_COMPATIBILITY;
    }

    private void stopThread(){
        exit = true;
    }

    private void startForeground() {
        Intent notificationIntent = new Intent();

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTI_ID, new NotificationCompat.Builder(this, CHANNEL_ID) // don't forget create a notification channel first
                    .setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Service is running background")
                    .setContentIntent(pendingIntent)
                    .build());
        }else{
            Notification notification = new NotificationCompat.Builder(this)
                    .setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher_foreground)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Service is running background")
                    .setContentIntent(pendingIntent)
                    .setChannelId(CHANNEL_ID)
                    .build();

            startForeground(NOTI_ID, notification);
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTI_ID,notification);
        }
    }
}
