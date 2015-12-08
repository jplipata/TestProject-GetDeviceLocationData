package com.lipata.testlocationdatafromdevice;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;

/**
 *  Quick and dirty test project that gets device location and other related data.  This data can
 *  be used to feed remote APIs, etc that provide data based on a user's location
 */

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    GoogleApiClient mGoogleApiClient;
    Geocoder mGeocoder;

    /**
     * Represents a geographical location.
     */
    protected Location mLastLocation;
    protected String mLatitudeLabel;
    protected String mLongitudeLabel;

    protected TextView mLatitudeText;
    protected TextView mLongitudeText;
    protected TextView mOtherLocationData;

    static final String LOG_TAG = MainActivity.class.getSimpleName();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mLatitudeLabel = getResources().getString(R.string.latitude_label);
        mLongitudeLabel = getResources().getString(R.string.longitude_label);
        mLatitudeText = (TextView) findViewById((R.id.latitude_text));
        mLongitudeText = (TextView) findViewById((R.id.longitude_text));
        mOtherLocationData = (TextView) findViewById((R.id.otherlocationdata_text));

        buildGoogleApiClient();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Getting latest location data...", Toast.LENGTH_SHORT).show();
                updateLocationData();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.d(LOG_TAG, "buildGoogleApiClient()");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    // Override methods for Google Play Services
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(LOG_TAG, "onConnected()");
        updateLocationData();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(LOG_TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(LOG_TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    // Once connection with Google Play Services has been established, call this method to get location data
    private void updateLocationData(){
        Log.d(LOG_TAG, "updateLocationData()");

        Address address;

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {

            // Clear OtherLocationData textview
            mOtherLocationData.setText("");

            // Update textviews with data calls
            mLatitudeText.setText(String.format("%s: %f", mLatitudeLabel,
                    mLastLocation.getLatitude()));
            mLongitudeText.setText(String.format("%s: %f", mLongitudeLabel,
                    mLastLocation.getLongitude()));
            mOtherLocationData.append("\nAccuracy: " + mLastLocation.getAccuracy() + " meters");


            double altitude = mLastLocation.getAltitude();
            if(altitude!=0){
                mOtherLocationData.append("\nAltitude: " + mLastLocation.getAltitude() + " meters above the WGS 84 reference ellipsoid.");
            } else mOtherLocationData.append("\nAltitude: Not available");

            float speed = mLastLocation.getSpeed();
            if(speed!=0){
                mOtherLocationData.append("\nSpeed: " + mLastLocation.getAltitude() + " meters/second");
            } else mOtherLocationData.append("\nSpeed: Not available");

            // Once we have the location, let's get an address
            // Breaking the rules out of curiosity and running this on the main thread instead of an IntentService
            // Result: In this simple application, there's no impact on the UI
            mOtherLocationData.append("\n\nGeocoder Address Lookup");
            mGeocoder = new Geocoder(this);
            try {
                address = mGeocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1).get(0);
                try {
                    for (int i = 0; i < 3; i++) {
                        mOtherLocationData.append("\n" + address.getAddressLine(i));
                    }
                } catch (IllegalArgumentException e){
                    Log.e(LOG_TAG, "address.getAddresLine error");}

            } catch (IllegalArgumentException e) {
                Log.e(LOG_TAG, "Geocoder error.  Illegal arguments.");
            } catch (IOException e) {
                Log.e(LOG_TAG, "Geocoder error. IOException.");
            }
            Toast.makeText(this, "Location Data Updated", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(LOG_TAG, "onConnected() No Location Detected");
            Toast.makeText(this, R.string.no_location_detected, Toast.LENGTH_LONG).show();
        }
    }


    // MainActivity template menu override methods
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
