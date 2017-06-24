package io.github.giulic3.apmap.services;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;

import io.github.giulic3.apmap.activities.MainActivity;

import static com.google.android.gms.common.ConnectionResult.NETWORK_ERROR;
import static com.google.android.gms.common.ConnectionResult.SERVICE_MISSING;
import static com.google.android.gms.common.ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;
import static com.google.android.gms.location.LocationSettingsRequest.*;

public class LocationService extends Service implements LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Activity activityParameter;

    private Location mLastLocation;
    private LocationRequest mLocationRequest; //get locationRequest()

    private GoogleApiClient mGoogleApiClient;
    private static final String LOGSERVICE = "LocationService";

    // binder given to clients (=mainActivity)

    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        buildGoogleApiClient();
        Log.i(LOGSERVICE, "onCreate");

        initializeLocationRequest();

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOGSERVICE, "onStartCommand");

        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
        return START_STICKY;
    }



    // this returns IBinder object to Activity
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }

    // returns an instance of this service
    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }


    private void destroyConnection() {
        if (mGoogleApiClient != null)
            mGoogleApiClient.disconnect();
    }

    // starts as callback when the phone connects to a GoogleApiClient
    @Override
    public void onConnected(Bundle connectionHint) {

        Log.d("DEBUG", "onConnected()");

        //requestLocationSettings();
        // la checklocationpermission ha senso farla prima di lanciare il service
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        }
    }

    // starts as callback when connection between GoogleApi and phone is suspended
    @Override
    public void onConnectionSuspended(int i) {

        Log.d("DEBUG", "onConnectionSuspended()");
    }

    // starts as callback when phone can't connect to GoogleApiServices, e.g. no GoogleServices are available
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // TODO: ADD CASES (see ConnectionResult reference) and resolve activityParameter problem
        // il problema sarà testarlo su un real device adesso
        Log.d("DEBUG", "onConnectionFailed()");

        int errorCode = connectionResult.getErrorCode();
        switch (errorCode) {
            case SERVICE_MISSING: {
                GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
                googleApiAvailability.getErrorDialog(activityParameter, SERVICE_MISSING, 2401).show();
                // come usare da qua startActiviyForResult per risolvere il problema?
            }
            case NETWORK_ERROR: {
                // to be implemented
            }

            case SERVICE_VERSION_UPDATE_REQUIRED: {
                GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
                googleApiAvailability.getErrorDialog(activityParameter, SERVICE_VERSION_UPDATE_REQUIRED, 2402).show();
            }
            default: {

            }

        }
    }

    // this method starts as callback when the LocationListener listens for a change in the current location
    @Override
    public void onLocationChanged(Location location) {

        Log.d("DEBUG", "onLocationChanged()");
        // TODO: toast temporanei per avere un feedback, è ridicolo avere un service che aggiorna la ui
        Toast.makeText(this, "location :"+location.getLatitude()+" , "+location.getLongitude(), Toast.LENGTH_LONG).show();
        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());


        //TODO: need to find a way to update ui now?
    }


    public Location getLocation(){
        return mLastLocation;
    }


    // TODO define parameters
    private void initializeLocationRequest(){
        Log.d("DEBUG", "initializeLocationRequest()");


        // a locationRequest object must be prepared before asking for permission
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // these determines how often onLocationChanged() will be called,
        // time is in ms, aka 30 seconds */ //TODO: set time according to common sense
        mLocationRequest.setInterval(30000);
        // 30 seconds
        mLocationRequest.setFastestInterval(30000);


    }

    private void stopLocationUpdate() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

    }

    protected synchronized void buildGoogleApiClient() {
        Log.d("DEBUG", "buildGoogleApiClient()");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
    }


}

