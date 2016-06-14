package com.lipata.testlocationdatafromdevice;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

/**
 *  A terribly structured, "quick and dirty" -- very dirty -- test project that gets device location and relative address.
 *  This data can be used to feed remote APIs, etc that provide data based on a user's location.
 *
 *  As an exercise to learn RxJava, Geocoder is implemented asynchronously using the Observable/Subscriber RxJava pattern.
 */

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    static private final String LOG_TAG = MainActivity.class.getSimpleName();

    // App permissions cases
    public static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 0;

    // Google Play Api parameters

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

        /**
         * The fastest rate for active location updates. Exact. Updates will never be more frequent
         * than this value.
         */
        public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
                UPDATE_INTERVAL_IN_MILLISECONDS / 2;

        /**
         * Provides the entry point to Google Play services.
         */
        protected GoogleApiClient mGoogleApiClient;

        /**
         * Stores parameters for requests to the FusedLocationProviderApi.
         */
        protected LocationRequest mLocationRequest;

        /**
         * Tracks the status of the location updates request. Value changes when the user presses the
         * Start Updates and Stop Updates buttons.
         */
        protected Boolean mRequestingLocationUpdates;
        /**
         * Time when the location was updated represented as a String.
         */
        protected String mLastUpdateTime;

        /**
         * Represents a geographical location.
         */
        protected Location mLastLocation;
        protected String mLatitudeLabel;
        protected String mLongitudeLabel;

    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    // UI
    protected TextView mTextView_Latitude;
    protected TextView mTextView_Longitude;
    protected TextView mTextView_Other;

    // Activity lifecycle

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mLatitudeLabel = getResources().getString(R.string.latitude_label);
        mLongitudeLabel = getResources().getString(R.string.longitude_label);
        mTextView_Latitude = (TextView) findViewById((R.id.latitude_text));
        mTextView_Longitude = (TextView) findViewById((R.id.longitude_text));
        mTextView_Other = (TextView) findViewById((R.id.otherlocationdata_text));

        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);

        // Kick off the process of building a GoogleApiClient and requesting the LocationServices
        // API.
        buildGoogleApiClient();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearUI();
                if(mGoogleApiClient.isConnected()) {
                    Toast.makeText(getApplicationContext(), "Getting latest location data...", Toast.LENGTH_SHORT).show();
                    startLocationUpdates();
                } else {
                    Toast.makeText(getApplicationContext(), "ERROR: Google Play API not connected", Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    @Override public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume()");
        if(!mGoogleApiClient.isConnected() || mLastLocation==null){
            mGoogleApiClient.connect();
        }

    }

    @Override protected void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause()");
        if(mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override protected void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop()");
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
            mGoogleApiClient.disconnect();
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    // Override methods for Google Play Services
    @Override public void onConnected(Bundle connectionHint) {
        Log.d(LOG_TAG, "Google Play Api: onConnected()");

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation == null) {
            startLocationUpdates();
        } else {
            updateLatLongUI();
        }
    }

    /**
     * Callback that fires when the location changes.
     */
    @Override public void onLocationChanged(Location location) {
        mLastLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        // Call Geocoder via RxJava
        getAddressObservable().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Address>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(LOG_TAG, e.getMessage(), e);
                    }

                    @Override
                    public void onNext(Address address) {
                        updateAddressUI(address);
                    }
                });

        updateLatLongUI();
        stopLocationUpdates();
    }

    @Override public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(LOG_TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(LOG_TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    // Helper methods

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(LOG_TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        createLocationRequest();
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    Address getAddress() throws IOException {
        Log.d(LOG_TAG, "getAddress()");
        Geocoder geocoder = new Geocoder(this);
        return geocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1).get(0);
    }

    Observable<Address> getAddressObservable(){

        return Observable.defer(new Func0<Observable<Address>>() {
            @Override
            public Observable<Address> call() {
                try {
                    return Observable.just(getAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
    }

    // UI methods
    private void updateLatLongUI() {
        if (mLastLocation != null) {

            // Clear OtherLocationData textview
            mTextView_Other.setText("");

            // Update textviews with data calls
            mTextView_Latitude.setText(String.format("%s: %f", mLatitudeLabel,
                    mLastLocation.getLatitude()));
            mTextView_Longitude.setText(String.format("%s: %f", mLongitudeLabel,
                    mLastLocation.getLongitude()));
            mTextView_Other.append("\nAccuracy: " + mLastLocation.getAccuracy() + " meters");


            double altitude = mLastLocation.getAltitude();
            if(altitude!=0){
                mTextView_Other.append("\nAltitude: " + mLastLocation.getAltitude() + " meters above the WGS 84 reference ellipsoid.");
            } else mTextView_Other.append("\nAltitude: Not available");

            float speed = mLastLocation.getSpeed();
            if(speed!=0){
                mTextView_Other.append("\nSpeed: " + mLastLocation.getAltitude() + " meters/second");
            } else mTextView_Other.append("\nSpeed: Not available");

            Toast.makeText(this, "Location Data Updated", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(LOG_TAG, "UpdateUI() mLastLocation=null");
            mTextView_Other.setText("Location not available");
        }

    }

    private void updateAddressUI(Address address) {
        mTextView_Other.append("\n\nADDRESS LOOKUP USING RXJAVA");
        if(address != null){
            try {
                for (int i = 0; i < 3; i++) {
                    mTextView_Other.append("\n" + address.getAddressLine(i));
                }
            } catch (IllegalArgumentException e){
                Log.e(LOG_TAG, "address.getAddressLine error");}
        } else {
            mTextView_Other.setText("Address not available");
        }
    }

    private void clearUI() {
        mTextView_Latitude.setText("");
        mTextView_Longitude.setText("");
        mTextView_Other.setText("");
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

    // Retain state

    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mLastLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Updates fields based on data stored in the bundle.
     *
     * @param savedInstanceState The activity state saved in the Bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);

                //DO SOMETHING TO REPLACE THIS: setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mLastLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            updateLatLongUI();
        }
    }



}
