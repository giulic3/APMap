package io.github.giulic3.apmap;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener{

    private GoogleMap mMap;
    private static final LatLng BOLOGNA_POINT = new LatLng(44.496781, 11.356387);

    private LocationManager locationManager;

    private static final long MIN_TIME = 10;
    private static final float MIN_DISTANCE = 100;

    private double currentLatitude;
    private double currentLongitude;

    private Geocoder geocoder;

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

    // TODO: fill in
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initializeMap();

    }

    @Override
    public void onLocationChanged(Location location) {

        Toast t= Toast.makeText(this,"New GPS Sample Available", Toast.LENGTH_SHORT);
        t.show();

        //latitude.setText(location.getLatitude()+"");
        //longitude.setText(location.getLongitude()+"");

        currentLatitude=location.getLatitude();
        currentLongitude=location.getLongitude();


        try {
            List<Address> addressList = geocoder.getFromLocation(currentLatitude, currentLongitude, 1);
            if ((addressList!=null) && (addressList.size()>0)) {
                Address currentAddress=addressList.get(0);
                //!countryName.setText(currentAddress.getCountryName());
            }
        }
        catch(IOException ex) {

        }

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

    // se il permesso è già stato accettato, la preferenza è salvata e posso andare subito alla
    // localizzazione
    @Override
    protected void onResume() {
        super.onResume();
        if (checkLocationPermission()) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER))
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);

            }
        }
    }



    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    // run time permission checking only form marshmallow  on

    public boolean checkLocationPermission() {

        // for api level greater than 23 (marshmallow) - with runtime permissions, for api level
        // 22 and lower checking runtime permissions doesn't make sense
        // perché tanto se non si accetta tutto all'inizio l'app non viene installata
        if (android.os.Build.VERSION.SDK_INT >= 23) {

            // se la richiesta di permessi non viene accolta
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
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_LOCATION);
                }
                return false;
            } else {
                return true;
            }

        }
        // significa che il permesso è stato accettato in fase di installazione
        else return true;
    }

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

                }
                return;
            }

        }
    }

}
