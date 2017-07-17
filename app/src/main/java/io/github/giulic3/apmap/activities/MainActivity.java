package io.github.giulic3.apmap.activities;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;

import android.os.IBinder;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import io.github.giulic3.apmap.R;
import io.github.giulic3.apmap.data.AccessPointInfoEntry;
import io.github.giulic3.apmap.data.CustomMap;
import io.github.giulic3.apmap.data.Database;
import io.github.giulic3.apmap.data.DatabaseHelper;
import io.github.giulic3.apmap.fragments.ScanResultFragment;
import io.github.giulic3.apmap.helpers.CustomAdapter;
import io.github.giulic3.apmap.helpers.DisplayValueHelper;
import io.github.giulic3.apmap.helpers.VisualizationHelper;
import io.github.giulic3.apmap.services.ApService;
import io.github.giulic3.apmap.services.LocationService;

import static com.google.android.gms.common.ConnectionResult.SERVICE_MISSING;
import static com.google.android.gms.common.ConnectionResult.NETWORK_ERROR;
import static com.google.android.gms.common.ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;

// this activity has the following responsibilities
// - inflates layout(s)
// - starts and communicates with services
// - handles events (scan_fab)
// - handles permissions
// - setup (custom) map

// TODO reorder or split methods
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
GoogleMap.OnInfoWindowClickListener{

    private GoogleMap mMap;
    private Location mLastKnownLocation;
    private BottomSheetBehavior mBottomSheetBehavior;
    private LocationService mLocationService;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    boolean isBound = false;
    boolean isFirstUpdate = true; // true if locationChanged for the first time (patch)
    private DatabaseHelper mDbHelper;
    private FloatingActionButton mButton;
    private List<AccessPointInfoEntry> apInfoList;
    private ArrayList<Marker> mMarkerArray; //used to save all the markers on map
    private ArrayList<Circle> mCircles; //used to save all circles associated to markers
    private VisualizationHelper mVisualizationHelper;
    private ArrayList<String> scanResultSsids;
    private ArrayList<Integer> scanResultLevels;
    private DisplayValueHelper mDisplayValueHelper;
    private FloatingActionButton button; //temporary

    private ServiceConnection mLocationConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mLocationService = ((LocationService.LocalBinder)service).getService();
            mLocationRequest = ((LocationService.LocalBinder)service).getLocationRequest();
            mGoogleApiClient = ((LocationService.LocalBinder)service).getGoogleApiClient();
            isBound = true;
            requestLocationSettings();
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

        // starts location service only if location is active
        if (checkLocationPermission()) {

            bindService(new Intent(this, LocationService.class), mLocationConnection, Context.BIND_AUTO_CREATE);
            startService(new Intent(this, LocationService.class));
        }
        // starts apservice
        startService(new Intent(this, ApService.class));

        // register for broadcasts
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(
                mLocationReceiver, new IntentFilter("GPSLocationUpdates"));
       // LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(
          //      mApReceiver, new IntentFilter("AccessPointsUpdates"));
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mDatabaseUpdatesReceiver,
                new IntentFilter("DatabaseUpdates"));

        // get reference to dbhelper object
        mDbHelper = new DatabaseHelper(MainActivity.this);

        View bottomSheet = findViewById(R.id.ap_bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setPeekHeight(0); // key line



        // instantiates object that handles marker visualization methods on map
        mVisualizationHelper = new VisualizationHelper();
        mDisplayValueHelper = new DisplayValueHelper();


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
        mButton = (FloatingActionButton) findViewById(R.id.scan_fab_id);
        if (mButton != null) {
            mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO: pass through the intent the scanresults obtained here from mDatabaseReceiver
                    // the intent extra is always the same! the first one passed BUG
                    Intent intent = new Intent(MainActivity.this, ScanResultsActivity.class);
                    intent.putStringArrayListExtra("scanResultSsids", scanResultSsids);
                    intent.putIntegerArrayListExtra("scanResultLevels", scanResultLevels);
                    startActivity(intent);
                }
            });
        }

        // temporary: setting db button

        FloatingActionButton button = (FloatingActionButton) findViewById(R.id.button_db);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    Intent dbmanager = new Intent(MainActivity.this, AndroidDatabaseManager.class);
                    startActivity(dbmanager);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMap.clear();

        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mLocationReceiver);
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mDatabaseUpdatesReceiver);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.visualization_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.show_all:
                mVisualizationHelper.showAll(mMarkerArray);
                return true;
            case R.id.show_open:
                mVisualizationHelper.showOnlyOpen(mMarkerArray);
                return true;
            case R.id.show_closed:
                mVisualizationHelper.showOnlyClosed(mMarkerArray);
                return true;
            case R.id.show_range:
                mVisualizationHelper.showOnlyRange(mMarkerArray, mLastKnownLocation.getLatitude(),
                        mLastKnownLocation.getLongitude()); //change location accordingly
                return true;
            case R.id.hide_coverage:
                mVisualizationHelper.hideCoverage(mCircles);
                return true;
            case R.id.show_coverage:
                mVisualizationHelper.showCoverage(mCircles);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // TODO: vuoto così è inutile, si può spostare il contenuto altrove
    // this method contains all the commands to customize the map
    private void setUpMap() {

        Log.d("DEBUG", "MainActivity: setUpMap()");
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) && (mMap != null)) {
            mMap.setMyLocationEnabled(true);
        }

        populateMap();

    }

    // this method extracts data from db and fill the map with aps markers, associating each marker
    // to an accesspointinfo object
    private void populateMap() { //TODO: refactor method, too long

        Log.d("DEBUG", "MainActivity: populateMap()");
        apInfoList = new ArrayList<AccessPointInfoEntry>();
        mMarkerArray = new ArrayList<Marker>();
        mCircles = new ArrayList<Circle>();

        Cursor cursor = mDbHelper.getAll(Database.Table1.TABLE_NAME);
        mMap.setOnMarkerClickListener(this);
        // Set a listener for info window events.
        mMap.setOnInfoWindowClickListener(this);

        if (cursor.moveToFirst()) {
            do {
                // type string: can be null
                String latitude = cursor.getString(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_ESTIMATED_LATITUDE));
                String longitude = cursor.getString(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_ESTIMATED_LONGITUDE));
                String coverageRadius = cursor.getString(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_COVERAGE_RADIUS));

                if (latitude != null && longitude != null) {


                    addMarker(cursor, Double.parseDouble(latitude), Double.parseDouble(longitude),
                            Double.parseDouble(coverageRadius));
                }

            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private void addMarker(Cursor cursor, Double latitude, Double longitude, Double coverageRadius){

        Log.d("DEBUG", "MainActivity: addMarker()");
        // TODO: can use a method for marker color setting
        // info needed to determine marker color
        String capabilities = cursor.getString(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_CAPABILITIES));
        String securityType = getAccessPointSecurityType(capabilities);

        // setting marker color
        float markerColor;
        if (securityType.equals("open")) markerColor = BitmapDescriptorFactory.HUE_GREEN;
        else markerColor = BitmapDescriptorFactory.HUE_RED;

        Marker marker = mMap.addMarker(new MarkerOptions()
                .title(cursor.getString(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_SSID)))
                .position(new LatLng(latitude, longitude))
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

        // setting circle coverage and circle color
        int circleColor = getCircleColor(cursor.getString(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_CAPABILITIES)));
        // adding circle
        Circle circle = mMap.addCircle(new CircleOptions()
                .center(new LatLng(latitude, longitude))
                .radius(coverageRadius)
                .strokeColor(Color.TRANSPARENT)
                .fillColor(circleColor)
                .zIndex(1.0f)); // color depends on capabilities
        // green ones have higher z-index(?)
        // prepare object to associate to map marker
        // no lat/lon, no need to associate object, will be done when refreshing map
        apInfoList.add(new AccessPointInfoEntry(
                cursor.getString(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_BSSID)),
                cursor.getString(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_SSID)),
                cursor.getString(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_CAPABILITIES)),
                cursor.getInt(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_FREQUENCY)), //following two can be replaced
                cursor.getDouble(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_ESTIMATED_LATITUDE)),
                cursor.getDouble(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_ESTIMATED_LONGITUDE)),
                cursor.getDouble(cursor.getColumnIndex((Database.Table1.COLUMN_NAME_COVERAGE_RADIUS)))));

        marker.setTag(apInfoList.get(apInfoList.size() - 1));
        // adding marker to array: also for circles?
        mMarkerArray.add(marker);
        mCircles.add(circle);


    }
// not working
    private int getCircleColor(String capabilities) {
        // given capabilties of an ap, decide if the network is open or closed
        // return red if close, else green
        if (getAccessPointSecurityType(capabilities).equals("closed"))
            return Color.parseColor("#66FF0000"); // USE CONSTANTS TODO

        else { //it's open
            return Color.parseColor("#6614EE91");
        }
    }
    // return open or closed according to network security
    public static String getAccessPointSecurityType(String capabilities) {
        if (capabilities.contains("WEP") || capabilities.contains("WPA") ||
                capabilities.contains("WPA2") || capabilities.contains("ESS"))
            return "closed";
        else
            return "open";
    }

    /*
    // formula inversa per risalire al livello
    private double distanceToLevel(Location currentLocation, int frequency) { // be sure of minus sign
        double distanceInMetres =
        double distanceInMetres = Math.pow(10, ((27.55 - (20*Math.log10(frequency)) - level)/ 20));
        double distanceInKilometres = distanceInMetres / 1000;
        return distanceInKilometres;
    }

    private int convertLevelToHumanReadableValue() {}
    */

    // on (generic) marker click callback MAYBE A RECYCLERVIEW OR A LISTVIEW?
    // questo modo di fare le cose fa schifo
    @Override
    public boolean onMarkerClick(Marker marker){
        Log.d("DEBUG", "MainActivity: onMarkerClick()");

        // where to put all these variables?
        TextView bssidTv = (TextView) findViewById(R.id.bssid);
        TextView ssidTv = (TextView) findViewById(R.id.ssid);
        TextView capabilitiesTv = (TextView) findViewById(R.id.capabilities);
        TextView frequencyTv = (TextView) findViewById(R.id.frequency);
        //TextView levelTv = (TextView) findViewById(R.id.level);
        TextView estimatedLatitudeTv = (TextView) findViewById(R.id.estimated_latitude);
        TextView estimatedLongitudeTv = (TextView) findViewById(R.id.estimated_longitude);
        TextView coverageRadiusTv = (TextView) findViewById(R.id.coverage_radius);

        AccessPointInfoEntry apInfoEntry = (AccessPointInfoEntry) marker.getTag();
        bssidTv.setText("BSSID: " + apInfoEntry.getBssid());
        ssidTv.setText("SSID: " + apInfoEntry.getSsid());
        capabilitiesTv.setText("CAPABILITIES: " + mDisplayValueHelper.getReadableSecurityType(
                apInfoEntry.getCapabilities()));
        frequencyTv.setText("FREQUENCY: " + String.valueOf(apInfoEntry.getFrequency()));
        // levelTv.setText(); level must be set in other ways //TODO
        estimatedLatitudeTv.setText("LATITUDE: " + mDisplayValueHelper.formatCoordinate(apInfoEntry.getEstimatedLatitude()));
        estimatedLongitudeTv.setText("LONGITUDE: " + mDisplayValueHelper.formatCoordinate(apInfoEntry.getEstimatedLongitude()));
        coverageRadiusTv.setText("COVERAGE RADIUS: " + mDisplayValueHelper.formatCoordinate(apInfoEntry.getCoverageRadius()));

        marker.showInfoWindow();

        if (mBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }

        return true;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        marker.hideInfoWindow();
    }

    // callback interface for when the map is ready to be used
    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.d("DEBUG", "MainActivity: onMapReady()");

        mMap = googleMap;


        setUpMap();
    }
    // BROADCAST RECEIVERS
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

    private BroadcastReceiver mDatabaseUpdatesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d("DEBUG", "MainActivity: mDatabaseUpdatesReceiver onReceive()");
            ArrayList<String> updatedApBssid = intent.getStringArrayListExtra("updatedApBssid");
            Log.d("DEBUG", "MainActivity: updatedApBssid size: "+updatedApBssid.size());

            Toast toast = Toast.makeText(getApplicationContext(),
                    "updatedApBssid size: "+updatedApBssid.size(), Toast.LENGTH_SHORT);
            toast.show();
            // maybe it doesn't do anything because the array is always empty
            for (int i = 0; i < updatedApBssid.size(); i++) {

                if ((apInfoList != null) && (mMarkerArray != null) && (mCircles != null)) {

                    Cursor cursor = mDbHelper.getBssid(Database.Table1.TABLE_NAME, updatedApBssid.get(i));
                    //save lat, lon and everything else
                    if (cursor.moveToFirst()) {
                        double lat = cursor.getDouble(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_ESTIMATED_LATITUDE));
                        double lon = cursor.getDouble(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_ESTIMATED_LONGITUDE));
                        double radius = cursor.getDouble(cursor.getColumnIndex((Database.Table1.COLUMN_NAME_COVERAGE_RADIUS)));
                        addMarker(cursor, lat, lon, radius);
                        Toast toast2 = Toast.makeText(getApplicationContext(),
                                "new marker added", Toast.LENGTH_SHORT);
                        toast2.show();
                    }
                }
            }

            //temporary
            /*
            mMap.clear();
            populateMap();
            */
            // TODO:
            scanResultSsids = intent.getStringArrayListExtra("scanResultSsids");
            scanResultLevels = intent.getIntegerArrayListExtra("scanResultLevels");

        }
    };



    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    // this method creates a Location Settings Request specifying all kinds of requests that will be asked
    //TODO: boilerplate
     public void requestLocationSettings(){

         Log.d("DEBUG", "MainActivity: requestLocationSettings()");

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
        Log.d("DEBUG", "MainActivity: onActivityResult()");
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
        else {
                Log.d("DEBUG", "MainActivity: api<23 in checkPermission()");
                return true;
        }

    }

    // this callback handles the user answers to the location request
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        Log.d("DEBUG", "MainActivity: onRequestPermissionsResul()");
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
