package io.github.giulic3.apmap;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/*
*
*
* */

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener,
        ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private static final LatLng BOLOGNA_POINT = new LatLng(44.496781, 11.356387);
    private LatLng currentLocation = new LatLng(0,0);
    private LocationManager locationManager;

    private static final long MIN_TIME = 10;
    private static final float MIN_DISTANCE = 100;

    private double currentLatitude;
    private double currentLongitude;

    private Location mLastLocation;
    private double mLastLatitude;
    private double mLastLongitude;

    private Geocoder geocoder;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }


        initializeMap();

    }


    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }



    // se il permesso è già stato accettato, la preferenza è salvata e posso andare subito alla
    // localizzazione
    @Override
    protected void onResume() {
        super.onResume();

        /* ad ogni ricaricamento dell'activity, controllo le permission (che sono modificabili dai settings)*/
        if (checkLocationPermission()) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER))
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);

            }
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {

        if (checkLocationPermission()) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (mLastLocation != null) {
                mLastLatitude = mLastLocation.getLatitude();
                mLastLongitude = mLastLocation.getLongitude();
                //TODO: FATTO UN PO' A CASO
                LatLng mLastPosition = new LatLng(mLastLatitude,mLastLongitude);
                mMap.addMarker(new MarkerOptions().position(mLastPosition).title("Posizione attuale"));

            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void initializeMap() {

        geocoder = new Geocoder(this);

        // Get instance of LocationManager system service
        locationManager=(LocationManager) getSystemService(LOCATION_SERVICE);
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);

        if (mMap == null) {
            MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this); // this refers to onMapReady? why?
        }
    }

    // TODO: fill in mettendo dei setting onesti
    // used for map settings (when displayed at first)
    // es. voglio mostrare la mappa con la mia locazione
    private void setUpMap() {

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(BOLOGNA_POINT)
                .zoom(15)
                .bearing(90)
                .tilt(30)
                .build();

        // CameraUpdate != CameraPosition
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
        mMap.moveCamera(cameraUpdate);

        mMap.addMarker(new MarkerOptions().position(BOLOGNA_POINT).title("Dipartimento Di Informatica DISI"));


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setUpMap();
    }




    @Override
    public void onLocationChanged(Location location) {

        Toast t= Toast.makeText(this,"New GPS Sample Available", Toast.LENGTH_SHORT);
        t.show();

        // aggiungo un marker ogni volta che cambio location (da emulatore posso "mandarla" altr.
        // non sarebbe fattibile)


        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        //how to? change locationcurrentLocation =
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(currentLocation)
                .zoom(15)
                .bearing(90)
                .tilt(30)
                .build();

        // CameraUpdate != CameraPosition
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
        mMap.moveCamera(cameraUpdate);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }



// MECCANISMO PER RICHIEDERE PERMESSI DI LOCALIZZAZIONE A RUNTIME
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    // run time permission checking only form marshmallow  on

    // if access_fine_location is granted, automatically it is granted coarse_loc too
    public boolean checkLocationPermission() {

        // for api level greater than 23 (marshmallow) - with runtime permissions, for api level
        // 22 and lower checking runtime permissions doesn't make sense
        // perché tanto se non si accetta tutto all'inizio l'app non viene installata
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // il controllo posso spostarlo prima di chiamare la checkLocationPermission

            // se il permesso è attualmente disabilitato
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                //mostriamo un dialog con le spiegazioni dovute
                // shouldShow... ritorna true se precedentemente l'utente ha selezionato
                // "nega permessi"
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {

                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.title_location_permission)
                            .setMessage(R.string.text_location_permission)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Prompt the user once explanation has been shown
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                            MY_PERMISSIONS_REQUEST_LOCATION);
                                }
                            })
                            .create()
                            .show();


                } else {
                    //vado qua se è la prima volta che richiedo il permesso
                    //faccio la richiesta senza spiegazioni
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_LOCATION);
                }
                return false;
            } else {
                return true;
            }

        }
        // significa che il permesso è stato accettato in fase di installazione (sono su una versione
        //più vecchia)
        else return true;
    }

    // callback per gestire la risposta dell'utente alla richiesta
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                        //locationManager.requestLocationUpdates(provider, 400, 1, this);
                        if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER))
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);

                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    // TODO: l'app non visualizzerà più la propria posizione ma continuerà a funzionare
                    // avendo un database di ap e usando degli indirizzi per navigare la mappa
                }
                return;
            }

        }
    }

}
