package com.grok.akm.ctrlworks;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    static String pot;
//    static String inveral;
    static int accu;
    static String senseInterval;
    static String locateInterval;

    static Context context;

    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;

    private static Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            if(preference instanceof ListPreference){
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(newValue.toString());

                accu = index;
                editor.putInt("accuracy",index);
                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            }else {

                if (preference.getKey().equals("preference_port_et")) {
                    String port = newValue.toString();
                    preference.setSummary(port);
                    pot = port;
                    editor.putString("port", port);
                }

//                } else if(preference.getKey().equals("preference_interval_et")) {
//                    String interval = newValue.toString();
//
////                    if(Integer.parseInt(interval) < 1000){
//                    if (interval.length() == 0 ) {
//                        Toast.makeText(context, "Must Enter Something", Toast.LENGTH_SHORT).show();
//                        return false;
//                    } else {
//                        inveral = interval;
//                        editor.putString("interval",interval);
//                        preference.setSummary(interval);
//
//                    }
//                }
                else if(preference.getKey().equals("preference_sensor_interval_et")) {
                    String sensorInterval = newValue.toString();

                    if (sensorInterval.length() == 0) {
                        Toast.makeText(context, "Must Enter Something", Toast.LENGTH_SHORT).show();
                        return false;
                    } else {
                        senseInterval = sensorInterval;
                        editor.putString("SensorInterval",sensorInterval);
                        preference.setSummary(sensorInterval);

                    }
                }

                else if(preference.getKey().equals("preference_location_interval_et")) {
                    String locationInterval = newValue.toString();

                    if (locationInterval.length() == 0) {
                        Toast.makeText(context, "Must Enter Something", Toast.LENGTH_SHORT).show();
                        return false;
                    } else {
                        locateInterval = locationInterval;
                        editor.putString("LocationInterval",locationInterval);
                        preference.setSummary(locationInterval);
                    }
                }
            }


            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(listener);

        listener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fragmentManager = getFragmentManager();

        if (fragmentManager.findFragmentById(android.R.id.content) == null) {
            fragmentManager.beginTransaction()
                    .add(android.R.id.content, new SettingsFragment()).commit();
        }

        context = this;

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home:{
                onBackPressed();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_settings);

            preferences = getActivity().getSharedPreferences(MainActivity.SHARED_PREF_SERVICE,MODE_PRIVATE);
            editor = preferences.edit();

            bindPreferenceSummaryToValue(findPreference("preference_port_et"));
//            bindPreferenceSummaryToValue(findPreference("preference_interval_et"));
            bindPreferenceSummaryToValue(findPreference("preference_accuracy_lp"));
            bindPreferenceSummaryToValue(findPreference("preference_sensor_interval_et"));
            bindPreferenceSummaryToValue(findPreference("preference_location_interval_et"));
        }
    }

    @Override
    public void onBackPressed() {

        editor.apply();
        editor.commit();

        Intent intent = new Intent();
        intent.putExtra("accuracy", accu);
        intent.putExtra("port", pot);
//        intent.putExtra("interval", inveral);
        intent.putExtra("SensorInterval",senseInterval);
        intent.putExtra("LocationInterval",locateInterval);
        setResult(RESULT_OK,intent);
        finish();
    }
}
