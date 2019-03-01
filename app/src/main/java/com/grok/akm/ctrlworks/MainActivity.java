package com.grok.akm.ctrlworks;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements SensorEventListener, OnTCPMessageRecievedListener {

    private final int REQUEST_LOCATION = 1000;
    private final int REQUEST_CHECK_SETTINGS = 2000;
    private final int REQUEST_SETTINGS_ACTIVITY = 3000;
    private final int DIFF_FASTEST_INTERVAL = 4000;

    public static final String SHARED_PREF_SERVICE = "com.grok.akm.ctrlworks.Service.sharedPref";


    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;

    private float[] mAccelerometerData = new float[3];
    private float[] mMagnetometerData = new float[3];

    private TextView mDirection_tv;
    private TextView mPitch_tv;
    private TextView mRoll_tv;
    private TextView mLongitude_tv;
    private TextView mLatitude_tv;
    private TextView mAltitude_tv;

    private TextView mServerStatus_tv;
    private TextView mIPAddress_tv;
    private TextView mPort_tv;
    private TextView mLocationAccuracy_tv;
    private TextView mStatus_tv;

    private TCPCommunicator writer;

    private JSONObject json;

    private String interval;
    private String port;
    private int accuracy;
    private Intent SensorService;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    private LocationManager mLocationManager;
    private GnssStatus.Callback callback;
    private GpsStatus.Listener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        mDirection_tv = (TextView) findViewById(R.id.yaw_tv);
        mPitch_tv = (TextView) findViewById(R.id.pitch_tv);
        mRoll_tv = (TextView) findViewById(R.id.roll_tv);
        mLongitude_tv = (TextView) findViewById(R.id.longitude_tv);
        mLatitude_tv = (TextView) findViewById(R.id.latitude_tv);
        mAltitude_tv = (TextView) findViewById(R.id.altitude_tv);

        mServerStatus_tv = (TextView) findViewById(R.id.server_status_tv);
        mIPAddress_tv = (TextView) findViewById(R.id.ip_address_tv);
        mPort_tv = (TextView) findViewById(R.id.port_tv);
        mLocationAccuracy_tv = (TextView) findViewById(R.id.location_accuracy_tv);
        mStatus_tv = (TextView) findViewById(R.id.status_tv);

        SharedPreferences preferences = getApplicationContext().getSharedPreferences(SHARED_PREF_SERVICE, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        port = preferences.getString("port", "8080");
        interval = preferences.getString("interval", "1000");
        accuracy = preferences.getInt("accuracy", 0);

        switch (accuracy) {
            case 0:
                mLocationAccuracy_tv.setText("High");
                break;
            case 1:
                mLocationAccuracy_tv.setText("Balanced Power");
                break;
            case 2:
                mLocationAccuracy_tv.setText("Low Power");
                break;
            case 3:
                mLocationAccuracy_tv.setText("No Power");
                break;
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        } else {
            // permission has been granted, continue as usual

            turnLocationOn();

            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        mLatitude_tv.setText("" + location.getLatitude());
                        mLongitude_tv.setText("" + location.getLongitude());
                        mAltitude_tv.setText("" + location.getAltitude());

                        try {
                            json.put("Latitude", location.getLatitude());
                            json.put("Longitude", location.getLongitude());
                            json.put("Altitude", location.getAltitude());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }
            });

            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {

                        mLatitude_tv.setText("" + location.getLatitude());
                        mLongitude_tv.setText("" + location.getLongitude());
                        mAltitude_tv.setText("" + location.getAltitude());

                        try {
                            json.put("Latitude", location.getLatitude());
                            json.put("Longitude", location.getLongitude());
                            json.put("Altitude", location.getAltitude());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

                ;
            };

            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

                callback = new GnssStatus.Callback() {
                    @Override
                    public void onStarted() {
                        super.onStarted();
                        mStatus_tv.setText("Started");

                    }

                    @Override
                    public void onStopped() {
                        super.onStopped();
                        mStatus_tv.setText("Stopped");

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
                        if (count == 0) {
                            mStatus_tv.setText("No Fix");
                        } else if (count <= 3) {
                            mStatus_tv.setText("2D");
                        } else {
                            mStatus_tv.setText("3D");
                        }
                    }
                };

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

                            for(GpsSatellite sat : satellite){
                                if(sat.usedInFix()){
                                    count ++;
                                }
                            }
                            if (count == 0) {
                                mStatus_tv.setText("No Fix");
                            } else if (count <= 3) {
                                mStatus_tv.setText("2D");
                            } else {
                                mStatus_tv.setText("3D");
                            }

                        }
                    }
                };

                mLocationManager.addGpsStatusListener(listener);
            }

            startLocationUpdates();


        }


        writer = TCPCommunicator.getInstance();
        TCPCommunicator.addListener(this);

        json = new JSONObject();

        writer.init(Integer.parseInt(port));

        StringBuilder sb = new StringBuilder();
        sb.append(writer.getIpAddress());


        mIPAddress_tv.setText(sb.toString());
        mPort_tv.setText(port);


        starService(interval, accuracy);

    }

    private void starService(String interval, int accuracy) {
        SensorService = new Intent(this, SensorService.class);
        SensorService.putExtra("interval", Integer.parseInt(interval));
        SensorService.putExtra("accuracy", accuracy);
        startService(SensorService);
    }

    private void SettingsActivityPath() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, REQUEST_SETTINGS_ACTIVITY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                turnLocationOn();

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            mLatitude_tv.setText("" + location.getLatitude());
                            mLongitude_tv.setText("" + location.getLongitude());
                            mAltitude_tv.setText("" + location.getAltitude());

                            try {
                                json.put("Latitude", location.getLatitude());
                                json.put("Longitude", location.getLongitude());
                                json.put("Altitude", location.getAltitude());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                });

                mLocationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        if (locationResult == null) {
                            return;
                        }
                        for (Location location : locationResult.getLocations()) {

                            mLatitude_tv.setText("" + location.getLatitude());
                            mLongitude_tv.setText("" + location.getLongitude());
                            mAltitude_tv.setText("" + location.getAltitude());

                            try {
                                json.put("Latitude", location.getLatitude());
                                json.put("Longitude", location.getLongitude());
                                json.put("Altitude", location.getAltitude());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    ;
                };

                mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    callback = new GnssStatus.Callback() {
                        @Override
                        public void onStarted() {
                            super.onStarted();
                            mStatus_tv.setText("Started");

                        }

                        @Override
                        public void onStopped() {
                            super.onStopped();
                            mStatus_tv.setText("Stopped");

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
                            if (count == 0) {
                                mStatus_tv.setText("No Fix");
                            } else if (count <= 3) {
                                mStatus_tv.setText("2D");
                            } else {
                                mStatus_tv.setText("3D");
                            }
                        }
                    };

                    mLocationManager.registerGnssStatusCallback(callback);
                }else{
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

                                for(GpsSatellite sat : satellite){
                                    if(sat.usedInFix()){
                                        count ++;
                                    }
                                }
                                if (count == 0) {
                                    mStatus_tv.setText("No Fix");
                                } else if (count <= 3) {
                                    mStatus_tv.setText("2D");
                                } else {
                                    mStatus_tv.setText("3D");
                                }

                            }
                        }
                    };

                    mLocationManager.addGpsStatusListener(listener);
                }
                startLocationUpdates();


            } else {
                // Permission was denied or request was cancelled
                this.finish();
            }
        }
    }


    private void turnLocationOn() {

        createLocationRequest();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // All location preference_settings are satisfied. The client can initialize location
                    // requests here.

                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location preference_settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                        MainActivity.this,
                                        REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location preference_settings are not satisfied. However, we have no way to fix the
                            // preference_settings so we won't show the dialog.
                            break;


                    }
                }
            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS: {
                final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        Toast.makeText(MainActivity.this, states.isLocationPresent() + "", Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change preference_settings, but chose not to
                        Toast.makeText(MainActivity.this, "Canceled", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
                break;
            }
            case REQUEST_SETTINGS_ACTIVITY: {
                String pot = data.getStringExtra("port");

                if (pot != null) {

                    port = pot;
                    mIPAddress_tv.setText(writer.getIpAddress());
                    mPort_tv.setText(port);

                    TCPCommunicator.closeStreams();
                    writer.init(Integer.parseInt(port));
                }

                String inteval = data.getStringExtra("interval");
                if (inteval != null) {

                    interval = inteval;
                    mLocationRequest.setInterval(Integer.parseInt(interval));
                    mLocationRequest.setFastestInterval(Integer.parseInt(interval) + DIFF_FASTEST_INTERVAL);
                }

                int accu = data.getIntExtra("accuracy", -1);
                if (accu != -1) {
                    accuracy = accu;
                    switch (accuracy) {
                        case 0:
                            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                            mLocationAccuracy_tv.setText("High");
                            break;
                        case 1:
                            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                            mLocationAccuracy_tv.setText("Balanced Power");
                            break;
                        case 2:
                            mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
                            mLocationAccuracy_tv.setText("Low Power");
                            break;
                        case 3:
                            mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
                            mLocationAccuracy_tv.setText("No Power");
                            break;
                    }
                }

                stopService(SensorService);
                starService(interval, accuracy);


                break;
            }
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(Integer.parseInt(interval));
        mLocationRequest.setFastestInterval(Integer.parseInt(interval) + DIFF_FASTEST_INTERVAL);

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

    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }


    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mSensorManager.registerListener(this,mAccelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this,mMagnetometer,SensorManager.SENSOR_DELAY_NORMAL);
    }



    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this);

        if(mLocationManager != null)  {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mLocationManager.unregisterGnssStatusCallback(callback);
            }else{
                mLocationManager.removeGpsStatusListener(listener);
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        TCPCommunicator.closeStreams();
        stopService(SensorService);
    }

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

        for (int i = 0; i < orientationValues.length; i++){
            orientationValues[i] = (float) Math.toDegrees(orientationValues[i]);
        }

        float azimuth = orientationValues[0];
        float pitch = orientationValues[1];
        float roll = orientationValues[2];
        mDirection_tv.setText(getResources().getString(
                R.string.value_format, azimuth));
        mPitch_tv.setText(getResources().getString(
                R.string.value_format, pitch));
        mRoll_tv.setText(getResources().getString(
                R.string.value_format, roll));

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onTCPMessageRecieved(String message) {

        Toast.makeText(MainActivity.this,message,Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onConnect(boolean connect) {
            if(connect){
                mServerStatus_tv.setText("ON");
            }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.settings:
            {
                SettingsActivityPath();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

}