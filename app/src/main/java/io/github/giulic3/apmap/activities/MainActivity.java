package io.github.giulic3.apmap.activities;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;

import android.os.IBinder;
import android.renderscript.ScriptGroup;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import io.github.giulic3.apmap.data.Database;
import io.github.giulic3.apmap.data.DatabaseHelper;
import io.github.giulic3.apmap.services.ApService;
import io.github.giulic3.apmap.services.LocationService;

import static com.google.android.gms.common.ConnectionResult.SERVICE_MISSING;
import static com.google.android.gms.common.ConnectionResult.NETWORK_ERROR;
import static com.google.android.gms.common.ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;

// this activity has the following responsibilities used as a View/Controller:
// - inflates layout(s)
// - starts and communicates with services
// - handles events (scan)
// - handle permissions (can do that only in activities or fragments)
// - setup (custom) map


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Location mLastKnownLocation;
    private BottomSheetBehavior mBottomSheetBehavior;
    private Button mButton;
    private LocationService mLocationService;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    boolean isBound = false;
    boolean isFirstUpdate = true; // true if locationChanged for the first time (patch)

    // when to bind or unbind:
    // If you need to interact with the service only while your activity is visible, you should bind
    // during onStart() and unbind during onStop().
    // If you want your activity to receive responses even while it is stopped in the background,
    // then you can bind during onCreate() and unbind during onDestroy().
    private ServiceConnection mLocationConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mLocationService = ((LocationService.LocalBinder)service).getService();
            mLocationRequest = ((LocationService.LocalBinder)service).getLocationRequest();
            mGoogleApiClient = ((LocationService.LocalBinder)service).getGoogleApiClient();
            isBound = true;
            requestLocationSettings();
            Toast.makeText(MainActivity.this, "onServiceConnected()", Toast.LENGTH_LONG).show();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLocationService = null;
            isBound = false;

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d("DEBUG", "MainActivity: onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setup MapFragment
        if (mMap == null) {
            MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }

        // starts service only if location is active
        if (checkLocationPermission()) {

            bindService(new Intent(this, LocationService.class), mLocationConnection, Context.BIND_AUTO_CREATE);
            startService(new Intent(this, LocationService.class));
        }
        startService(new Intent(this, ApService.class));

        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(
                mLocationReceiver, new IntentFilter("GPSLocationUpdates"));
       // LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(
          //      mApReceiver, new IntentFilter("AccessPointsUpdates"));


        View bottomSheet = findViewById(R.id.ap_bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        bottomSheet.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d("DEBUG", "bottomsheet listener");

                if(mBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
                else {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

    }

    @Override
    protected void onStart() {

        Log.d("DEBUG", "MainActivity: onStart()");

        super.onStart();
    }

    @Override
    protected void onStop() {

        Log.d("DEBUG", "MainActivity: onStop()");

        super.onStop();
        // unbind from the service
        if (isBound) {
            unbindService(mLocationConnection);
            isBound = false;
        }

        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mLocationReceiver);
       // LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(WifiReceiver);
    }


    @Override
    protected void onResume() {

        Log.d("DEBUG", "MainActivity: onResume()");

        super.onResume();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // if permission is currently disabled (permissions could have been disabled in settings)
            if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) && (mMap != null)) {
                mMap.setMyLocationEnabled(true);
            }

        }
    }

    // this method contains all the commands to customize the map
    private void setUpMap() {

        Log.d("DEBUG", "MainActivity: setUpMap()");
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) && (mMap != null)) {
            mMap.setMyLocationEnabled(true);
        }

    }
    // this method extracts data from db and fill the map with aps and relative info
    private void populateMap() {

    }

    // callback interface for when the map is ready to be used
    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.d("DEBUG", "MainActivity: onMapReady()");

        mMap = googleMap;
        setUpMap();

    }

    private BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("Status");
            Bundle b = intent.getBundleExtra("Location");
            mLastKnownLocation = (Location) b.getParcelable("Location");

            if ((mLastKnownLocation != null) && (isFirstUpdate)) {
                LatLng latLng = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());

                // move camera to current position and focus
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14), 2000, null);
                isFirstUpdate = false;
            }
        }
    };

    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    // this method creates a Location Settings Request specifying all kinds of requests that will be asked
//TODO: boilerplate
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
    //TODO: boilerplate
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(intent);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        // aggiorno l'oggetto locationRequest e lo rimando al service?
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

// mechanism to request location permissions at runtime
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    // if access_fine_location is granted, automatically it is granted coarse_loc too
    public boolean checkLocationPermission() {

        Log.d("DEBUG", "MainActivity: checkLocationPermission()");

        // checking permissions at runtime only for api level greater or equal than 23
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // if permission is currently disabled
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // ask permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);

                return false;
            } else {
                return true;
            }
        }
            // means permission was granted during installation
        else{
                Log.d("DEBUG", "MainActivity: api<23 in checkPermission()");
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

                    // permission was granted, so I can do the location-related task
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        mMap.setMyLocationEnabled(true);

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
