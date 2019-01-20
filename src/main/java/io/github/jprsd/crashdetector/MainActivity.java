package io.github.jprsd.crashdetector;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.*;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;


public class MainActivity extends AppCompatActivity
        implements SensorEventListener, LocationListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private LocationManager locationManager;
    private Geocoder geocoder;
    private String longitude, latitude, city, street, state, country;

    private SensorManager sensorManager;
    private Sensor linearAccelerationSensor;

    private TextView emergencyInfoText, sensorDataText, currentLocation;
    private String crashMessage, emergencyContactName, emergencyContactNumber;

    private static final int REQUEST_CODE_SENSOR = 1;
    private static boolean textSent = false, seeContacts = false, track = false, sendSMS = false;
    private static final int MY_PERMISSION_REQUEST_READ_CONTACTS = 2;
    private static final int MY_PERMISSION_REQUEST_LOCATION = 3;
    private static final int MY_PERMISSION_REQUEST_SEND_SMS = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_CONTACTS}, MY_PERMISSION_REQUEST_READ_CONTACTS);
        }

        emergencyInfoText = findViewById(R.id.EmergencyInfoText);
        emergencyInfoText.setText(getResources().getString(R.string.emergency_info_text_default));

        sensorDataText = findViewById(R.id.SensorDataText);
        sensorDataText.setText(getResources().getString(R.string.sensor_data_text_default));

        Intent chooseContact = new Intent(Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(chooseContact,REQUEST_CODE_SENSOR);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(MainActivity.this, linearAccelerationSensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        currentLocation = findViewById(R.id.CurrentLocation);
        currentLocation.setText(getResources().getString(R.string.current_location_text_default));
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    10000, 100, MainActivity.this);
        }
        catch (SecurityException e){
            Log.e("MainActivity onCreate", Log.getStackTraceString(e));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(resultCode == RESULT_OK){
            if(requestCode == REQUEST_CODE_SENSOR){
                Cursor cursor;
                try{
                    emergencyContactNumber = null;
                    emergencyContactName = null;
                    Uri contactInfo = data.getData();
                    cursor = getContentResolver().query(contactInfo,
                            null, null, null, null);
                    cursor.moveToFirst();
                    int phoneInfo =
                            cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    emergencyContactNumber = cursor.getString(phoneInfo);
                    phoneInfo =
                            cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    emergencyContactName = cursor.getString(phoneInfo);
                    emergencyInfoText.setText("Emergency Contact Info: \n"
                            + emergencyContactName + "\n" + emergencyContactNumber);
                    cursor.close();
                }
                catch (Exception e){
                    Log.e("OnActivityResult", Log.getStackTraceString(e));
                }
            }
        }
        else{
            Log.e("MainActivity", "Error: Unable to select contact.");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        sensorDataText.setText("Real Time Acceleration Data (m/s^2)\n" +
                "X: " + event.values[0] + "\nY: " + event.values[1] + "\nZ: "
                + event.values[2]);

        for(float acceleration : event.values){
            if(acceleration > 10){
                findViewById(R.id.scroll_view).setBackgroundResource(R.drawable.crash_bkg);
                Log.d("DANGER", "CRASH DETECTED");

                if(!textSent) {
                    crashMessage = "The owner of this device has been detected to be in a car crash." +
                            " Estimated coordinates of crash:\n" + currentLocation.getText() +
                            "\nThis is an automated message sent by the Zeroth Responder safety app.";
                    ArrayList<String> substrings = SmsManager.getDefault().divideMessage(crashMessage);
                    SmsManager.getDefault().sendMultipartTextMessage(emergencyContactNumber, null,
                            substrings, null, null);
                    textSent = true;
                    Log.d("MainActivity",crashMessage);
                }
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = "" + location.getLatitude();
        longitude = "" + location.getLongitude();
        geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
        Address address;
        try{
            address = geocoder.getFromLocation(location.getLatitude(),
                    location.getLongitude(), 1).get(0);
            city = address.getLocality();
            street = address.getSubThoroughfare() + " " + address.getThoroughfare();
            state = address.getAdminArea();
            country = address.getCountryName();
            currentLocation.setText("Latitude: " + latitude + "\nLongitude: " + longitude +
            "\nApproximate Location: " + street + ", " + city + ", " + state + ", " + country);
        }
        catch (Exception e){
            Log.e("OnLocationChange", Log.getStackTraceString(e));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //DO NOTHING
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //DO NOTHING
    }

    @Override
    public void onProviderEnabled(String provider) {
        //DO NOTHING
    }

    @Override
    public void onProviderDisabled(String provider) {
        //DO NOTHING
    }

}
