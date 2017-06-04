package io.github.giulic3.apmap;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;

import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/*
*
*
* */

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener,
        ConnectionCallbacks, OnConnectionFailedListener {

    private GoogleMap mMap;
    private static final LatLng BOLOGNA_POINT = new LatLng(44.496781, 11.356387);


    private Location mLastLocation;
    private double mLastLatitude;
    private double mLastLongitude;
    private LatLng mLastPosition;


    private GoogleApiClient mGoogleApiClient;
    private LocationRequest locationRequest;


    private BottomSheetBehavior mBottomSheetBehavior1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d("DEBUG", "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getLocation();
        if (mMap == null) {
            MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }


    }


    private void getLocation(){

        Log.d("DEBUG", "getLocation()");



        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

    }

    protected void onStart() {

        Log.d("DEBUG", "onStart()");
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {

        Log.d("DEBUG", "onStop()");

        if (mGoogleApiClient != null)
            mGoogleApiClient.disconnect();
        super.onStop();
    }


    @Override
    protected void onResume() {

        Log.d("DEBUG", "onResume()");

        super.onResume();

        /* ad ogni ricaricamento dell'activity, controllo le permission (che sono modificabili dai settings)*/
        /*
        if (checkLocationPermission()) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                if (mGoogleApiClient != null)
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);

            }
        }
        */
    }


    /* starts as callback when the phone connects to a GoogleApiClient */
    @Override
    public void onConnected(Bundle connectionHint) {

        Log.d("DEBUG", "onConnected()");

        /* a locationRequest object must be prepared before asking for permission */
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        /* these determines how often onLocationChanged() will be called,
         time is in ms, aka 30 seconds */ //TODO: set time according to common sense
        locationRequest.setInterval(30000);
        /* 30 seconds*/
        locationRequest.setFastestInterval(30000);


        if (checkLocationPermission()) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);

        }
    }

    /* starts as callback when connection between GoogleApi and phone is suspended */
    @Override
    public void onConnectionSuspended(int i) {

        Log.d("DEBUG", "onConnectionSuspended()");
    }

    /* starts as callback when phone can't connect to GoogleApiServices, for example, no GoogleServices?*/
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.d("DEBUG", "onConnectionFailed()");

    }

    // TODO: fill in mettendo dei setting onesti
    // used for map settings (when displayed at first)
    // es. voglio mostrare la mappa con la mia locazione

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
        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mMap.setMyLocationEnabled(true);
            }
            else {
                checkLocationPermission();
            }
        }
        else {
            mMap.setMyLocationEnabled(true);
        }
        setUpMap();
    }


    /* this method starts as callback when the LocationListener listens for a change in the current location */
    @Override
    public void onLocationChanged(Location location) {


            Log.d("DEBUG", "onLocationChanged()");
            Toast.makeText(this, "location :"+location.getLatitude()+" , "+location.getLongitude(), Toast.LENGTH_LONG).show();
            mLastLocation = location;

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            mMap.addMarker(new MarkerOptions().position(latLng).title("CurrentPosition")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))); //marker del colore che si vuole

            // move camera to current position and focus
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));


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

    }






// MECCANISMO PER RICHIEDERE PERMESSI DI LOCALIZZAZIONE A RUNTIME
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    // run time permission checking only form marshmallow  on

    // if access_fine_location is granted, automatically it is granted coarse_loc too
    public boolean checkLocationPermission() {

        Log.d("DEBUG", "checkLocationPermission()");
        // for api level greater than 23 (marshmallow) - with runtime permissions, for api level
        // 22 and lower checking runtime permissions doesn't make sense
        // perché tanto se non si accetta tutto all'inizio l'app non viene installata
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            /* if permission is currently disabled */
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                /* this method returns true if the user has previously selected "deny permission",
                * in this case we show an explanation dialog */
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {

                    /* Show an explanation to the user *asynchronously*  After the user
                     sees the explanation, try again to request the permission.*/
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

                    /* this is the first time requesting the permission, I ask without explanations */
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_LOCATION);
                }
                return false;
            } else {
                return true;
            }

        }
        /* means permission was granted during installation */
        else return true;
    }

    /* this callback handles the user answers to the location request */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                /* if request is cancelled, the result arrays are empty */
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    /* permission was granted, so I can do the location-related task */
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient != null) //anche locationRequest dev'essere valido!
                        {
                            mMap.setMyLocationEnabled(true);
                            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
                        }

                    }

                } else {

                    /* permission denied, disabling the functionality that depends on this permission.
                    // TODO: l'app non visualizzerà più la propria posizione ma continuerà a funzionare
                    // avendo un database di ap e usando degli indirizzi per navigare la mappa
                    */
                }
                return;
            }

        }
    }

}
