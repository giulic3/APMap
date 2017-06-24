package io.github.giulic3.apmap.activities;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;

import android.os.IBinder;
import android.renderscript.ScriptGroup;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.model.LatLng;

import io.github.giulic3.apmap.R;
import io.github.giulic3.apmap.services.LocationService;

import static com.google.android.gms.common.ConnectionResult.SERVICE_MISSING;
import static com.google.android.gms.common.ConnectionResult.NETWORK_ERROR;
import static com.google.android.gms.common.ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;

// this activity has the following responsibilities used as a View/Controller:
// - inflates layout(s)
// - starts and communicates with services
// - handles events (scan)
// - handle permissions (can do that only in activities or fragments)
// - setup map


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    //private BottomSheetBehavior mBottomSheetBehavior1;

    // TODO: REFACTOR
    private LocationService mLocationService;

    private ServiceConnection mLocationConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mLocationService = ((LocationService.LocalBinder)service).getService();
            Toast.makeText(MainActivity.this, "onServiceConnected()", Toast.LENGTH_LONG).show();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLocationService = null;

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d("DEBUG", "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setup mapfragment
        if (mMap == null) {
            MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }

        // starts service only if location is active
        if (checkLocationPermission()) {

            bindService(new Intent(this, LocationService.class), mLocationConnection, Context.BIND_AUTO_CREATE);
            startService(new Intent(this, LocationService.class));
        }
    }


    @Override
    protected void onStart() {

        Log.d("DEBUG", "onStart()");

        super.onStart();
    }

    @Override
    protected void onStop() {

        Log.d("DEBUG", "onStop()");

        super.onStop();
    }


    @Override
    protected void onResume() {

        Log.d("DEBUG", "onResume()");

        super.onResume();
        // ad ogni ricaricamento dell'activity, controllo le permission (che sono modificabili dai settings)
        // la prima volta che chiamo onResume() è un problema perché non so se verrà chiamata prima questa o la onConnected

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // if permission is currently disabled
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) && (mMap != null)) {
                mMap.setMyLocationEnabled(true);
            }

        }
    }



    /* this method contains all the commands to customize the map */
    private void setUpMap() {

        Log.d("DEBUG", "setUpMap()");

        /*
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(BOLOGNA_POINT)
                .zoom(15)
                .bearing(0)
                .tilt(0)
                .build();

        // CameraUpdate != CameraPosition
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
        mMap.moveCamera(cameraUpdate);
        */


    }

    /* callback interface for when the map is ready to be used */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.d("DEBUG", "onMapReady()");

        mMap = googleMap;
        setUpMap();

    }


    // this method starts as callback when the LocationListener listens for a change in the current location
    /*
    @Override
    public void onLocationChanged(Location location) {

            Log.d("DEBUG", "onLocationChanged()");

            Toast.makeText(this, "location :"+location.getLatitude()+" , "+location.getLongitude(), Toast.LENGTH_LONG).show();
            mLastLocation = location;

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            // move camera to current position and focus
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

/*
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    if (marker.getTitle().equals("CurrentPosition")) {

                        Log.d("DEBUG", "onMarkerClick()");

                        View bottomSheet = findViewById(R.id.ap_bottom_sheet);
                        mBottomSheetBehavior1 = BottomSheetBehavior.from(bottomSheet);

                        if (mBottomSheetBehavior1.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                            Log.d("DEBUG", "expand bottom sheet");
                            mBottomSheetBehavior1.setState(BottomSheetBehavior.STATE_EXPANDED);
                        } else {
                            mBottomSheetBehavior1.setState(BottomSheetBehavior.STATE_COLLAPSED);
                            Log.d("DEBUG", "collapse bottom sheet");

                        }

                        return true;
                    }
                    return false;
                }
            });
*/
//    }

    /*
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    // this method creates a Location Settings Request specifying all kinds of requests that will be asked

public void requestLocationSettings(){

    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequest); //currently only high priority, give chance to choose

    PendingResult<LocationSettingsResult> result =
            LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

    result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
        @Override
        public void onResult(LocationSettingsResult result) {
            final Status status = result.getStatus();
            final LocationSettingsStates locationSettingsStates = result.getLocationSettingsStates();
            switch (status.getStatusCode()) {
                case LocationSettingsStatusCodes.SUCCESS:
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                 //...
                    break;
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    // Location settings are not satisfied. But could be fixed by showing the user
                    // a dialog.
                    initializeLocationRequests();
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        status.startResolutionForResult(
                                MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException e) {
                        // Ignore the error.
                    }
                    break;
                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    // Location settings are not satisfied. However, we have no way to fix the
                    // settings so we won't show the dialog.
                    //...
                    break;
            }
        }
    });
}
*/
    /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(intent);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        initializeLocationRequests();
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        break;
                    default:
                        break;
                }
                break;
        }
    }
*/





//TODO: ma installando tramite apk non vengono richiesti i permessi di installazione per api < 23?
    // solo installando da play store? e io come lo testo?

// MECCANISMO PER RICHIEDERE PERMESSI DI LOCALIZZAZIONE A RUNTIME
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    // if access_fine_location is granted, automatically it is granted coarse_loc too
    public boolean checkLocationPermission() {

        Log.d("DEBUG", "checkLocationPermission()");

        // checking permissions at runtime only for api level greater or equal than 23
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            /* if permission is currently disabled */
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                /* this method returns true if the user has previously selected "deny permission",(or has disabled from settings)
                * in this case we show an explanation dialog */
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {

                    // show explanation to the user *asynchronously*  After the user
                    // sees the explanation, try again to request the permission.
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.title_location_permission)
                            .setMessage(R.string.text_location_permission)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                            MY_PERMISSIONS_REQUEST_LOCATION);
                                }
                            })
                            .create()
                            .show();

                } else {

                    // this is the first time requesting the permission, I ask without explanations
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_LOCATION);
                }
                return false;
            } else {
                return true;
            }

        }
        // means permission was granted during installation

        else {
            Log.d("DEBUG", "api<23 in checkPermission()");
            return true;
        }
    }

    // this callback handles the user answers to the location request
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // if request is cancelled, the result arrays are empty
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    /* permission was granted, so I can do the location-related task */
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        /*
                        if (mGoogleApiClient != null) {

                            // this adds the button on the top right of the map and the focus functionality
                            mMap.setMyLocationEnabled(true);
                            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
                        }
                        */
                        // start service when permission is granted
                        bindService(new Intent(this, LocationService.class), mLocationConnection, Context.BIND_AUTO_CREATE);
                        startService(new Intent(this, LocationService.class));

                    }

                } else {

                    // permission denied, disabling the functionality that depends on this permission.
                    // TODO: l'app non visualizzerà più la propria posizione ma continuerà a funzionare
                    // avendo un database di ap e usando degli indirizzi per navigare la mappa

                }
                return;
            }

        }
    }

}
