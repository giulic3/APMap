package io.github.giulic3.apmap.activities;

import android.Manifest;
import android.app.Activity;
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
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

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

import java.util.ArrayList;
import java.util.List;

import io.github.giulic3.apmap.R;
import io.github.giulic3.apmap.data.Database;
import io.github.giulic3.apmap.data.DatabaseHelper;
import io.github.giulic3.apmap.helpers.DisplayValueHelper;
import io.github.giulic3.apmap.helpers.VisualizationHelper;
import io.github.giulic3.apmap.models.AccessPointInfoEntry;
import io.github.giulic3.apmap.services.ApService;
import io.github.giulic3.apmap.services.LocationService;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener{

    // map and location related
    private GoogleMap mMap;
    private Location mLastKnownLocation;
    private LocationService mLocationService;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    // bool
    boolean isBound = false;
    boolean isFirstUpdate = true; // true if location changed for the first time (patch)
    // helpers
    private DatabaseHelper mDbHelper;
    private VisualizationHelper mVisualizationHelper;
    private DisplayValueHelper mDisplayValueHelper;
    // support variables
    private List<AccessPointInfoEntry> apInfoList;
    private ArrayList<Marker> mMarkerArray; //used to save all the markers on map
    private ArrayList<Circle> mCircles; //used to save all the circles associated to markers
    private ArrayList<String> scanResultSsids;
    private ArrayList<String> scanResultBssids;
    private ArrayList<Integer> scanResultLevels;
    // views
    private BottomSheetBehavior mBottomSheetBehavior;
    private FloatingActionButton mButton;


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
    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

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
        // starts ApService
        startService(new Intent(this, ApService.class));

        // register broadcast receivers
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(
                mLocationReceiver, new IntentFilter("GPSLocationUpdates"));
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mDatabaseUpdatesReceiver,
                new IntentFilter("DatabaseUpdates"));

        // get references to helpers
        mDbHelper = new DatabaseHelper(MainActivity.this);
        mVisualizationHelper = new VisualizationHelper();
        mDisplayValueHelper = new DisplayValueHelper();
        // set bottom sheet behaviour
        View bottomSheet = findViewById(R.id.ap_bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setPeekHeight(0);

    }
    /** Called when the activity is about to become visible. */
    protected void onStart() {

        super.onStart();
    }
    /** Called when the activity is no longer visible. */
    @Override
    protected void onStop() {

        super.onStop();
        // unbind from the service
        if (isBound) {
            unbindService(mLocationConnection);
            isBound = false;
        }

    }

    /** Called when the activity has become visible. */
    @Override
    protected void onResume() {

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
                    // pass through the intent the scan results obtained from mDatabaseReceiver
                    Intent intent = new Intent(MainActivity.this, ScanResultsActivity.class);
                    intent.putStringArrayListExtra("scanResultSsids", scanResultSsids);
                    intent.putStringArrayListExtra("scanResultBssids", scanResultBssids);
                    intent.putIntegerArrayListExtra("scanResultLevels", scanResultLevels);
                    startActivity(intent);
                }
            });
        }

    }
    /** Called just before the activity is destroyed. */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMap.clear();

        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mLocationReceiver);
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mDatabaseUpdatesReceiver);

    }
    /** Called (only) the first time the options menu is displayed.*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.visualization_options_menu, menu);
        return true;
    }
    /**  Called whenever an item in the options menu is selected. */
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

    /** This method extracts data from db and fill the map with aps markers, associating each marker
    with an AccessPointInfoEntry object **/
    private void populateMap() {

        apInfoList = new ArrayList<AccessPointInfoEntry>();
        mMarkerArray = new ArrayList<Marker>();
        mCircles = new ArrayList<Circle>();

        Cursor cursor = mDbHelper.getAll(Database.Table1.TABLE_NAME);
        mMap.setOnMarkerClickListener(this);
        // set a listener for info window events.
        mMap.setOnInfoWindowClickListener(this);

        if (cursor.moveToFirst()) {
            do {
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

        // info needed to determine marker color
        String capabilities = cursor.getString(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_CAPABILITIES));
        String securityType = getAccessPointSecurityType(capabilities);

        // setting marker color according to capabilities
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
                .zIndex(1.0f));

        // prepare object to associate with map marker
        apInfoList.add(new AccessPointInfoEntry(
                cursor.getString(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_BSSID)),
                cursor.getString(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_SSID)),
                cursor.getString(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_CAPABILITIES)),
                cursor.getInt(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_FREQUENCY)), //following two can be replaced
                cursor.getDouble(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_ESTIMATED_LATITUDE)),
                cursor.getDouble(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_ESTIMATED_LONGITUDE)),
                cursor.getDouble(cursor.getColumnIndex((Database.Table1.COLUMN_NAME_COVERAGE_RADIUS)))));

        marker.setTag(apInfoList.get(apInfoList.size() - 1));

        mMarkerArray.add(marker);
        mCircles.add(circle);


    }

    // return circle color according to network security, red if closed, else green
    private int getCircleColor(String capabilities) {
        if (getAccessPointSecurityType(capabilities).equals("closed"))
            return Color.parseColor("#66FF0000");

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

    /** Called whenever a marker is clicked*/
    @Override
    public boolean onMarkerClick(Marker marker){

        TextView bssidTv = (TextView) findViewById(R.id.bssid);
        TextView ssidTv = (TextView) findViewById(R.id.ssid);
        TextView capabilitiesTv = (TextView) findViewById(R.id.capabilities);
        TextView frequencyTv = (TextView) findViewById(R.id.frequency);
        //TextView levelTv = (TextView) findViewById(R.id.level);
        TextView estimatedLatitudeTv = (TextView) findViewById(R.id.estimated_latitude);
        TextView estimatedLongitudeTv = (TextView) findViewById(R.id.estimated_longitude);
        TextView coverageRadiusTv = (TextView) findViewById(R.id.coverage_radius);

        AccessPointInfoEntry apInfoEntry = (AccessPointInfoEntry) marker.getTag();
        bssidTv.setText(getString(R.string.bssid_tag, apInfoEntry.getBssid()));
        ssidTv.setText(getString(R.string.ssid_tag, apInfoEntry.getSsid()));
        capabilitiesTv.setText(getString(R.string.capabilities_tag, mDisplayValueHelper.getReadableSecurityType(
                apInfoEntry.getCapabilities())));
        frequencyTv.setText(getString(R.string.frequency_tag, String.valueOf(apInfoEntry.getFrequency())));
        estimatedLatitudeTv.setText(getString(R.string.latitude_tag,
                mDisplayValueHelper.formatCoordinate(apInfoEntry.getEstimatedLatitude())));
        estimatedLongitudeTv.setText(getString(R.string.longitude_tag,
                mDisplayValueHelper.formatCoordinate(apInfoEntry.getEstimatedLongitude())));
        coverageRadiusTv.setText(getString(R.string.coverage_radius_tag,
                mDisplayValueHelper.formatCoordinate(apInfoEntry.getCoverageRadius())));

        marker.showInfoWindow();

        // slide bottom sheet up
        if (mBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }

        return true;
    }
    /** Called whenever a marker info window is clicked */
    @Override
    public void onInfoWindowClick(Marker marker) {
        marker.hideInfoWindow();
    }

    /** Called when the map is ready to be used */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) && (mMap != null)) {
            mMap.setMyLocationEnabled(true);
        }

        populateMap();
    }

    // broadcast receivers

    private BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // get extra data included in the Intent
            String message = intent.getStringExtra("Status");
            Bundle b = intent.getBundleExtra("Location");
            mLastKnownLocation = (Location) b.getParcelable("Location");

            if ((mLastKnownLocation != null) && (isFirstUpdate)) {
                LatLng latLng = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());

                // move camera to current position and focus
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14), 2000, null);
                // so that the camera moves only at the first update
                isFirstUpdate = false;
            }
        }
    };

    private BroadcastReceiver mDatabaseUpdatesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            ArrayList<String> updatedApBssid = intent.getStringArrayListExtra("updatedApBssid");

            for (int i = 0; i < updatedApBssid.size(); i++) {

                if ((apInfoList != null) && (mMarkerArray != null) && (mCircles != null)) {

                    Cursor cursor = mDbHelper.getBssid(Database.Table1.TABLE_NAME, updatedApBssid.get(i));
                    //save lat, lon and everything else
                    if (cursor.moveToFirst()) {
                        double lat = cursor.getDouble(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_ESTIMATED_LATITUDE));
                        double lon = cursor.getDouble(cursor.getColumnIndex(Database.Table1.COLUMN_NAME_ESTIMATED_LONGITUDE));
                        double radius = cursor.getDouble(cursor.getColumnIndex((Database.Table1.COLUMN_NAME_COVERAGE_RADIUS)));
                        addMarker(cursor, lat, lon, radius);
                    }
                }
            }

            scanResultSsids = intent.getStringArrayListExtra("scanResultSsids");
            scanResultBssids = intent.getStringArrayListExtra("scanResultBssids");
            scanResultLevels = intent.getIntegerArrayListExtra("scanResultLevels");

        }
    };

    // mechanism to request location settings

    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    // this method creates a Location Settings Request specifying all kinds of requests that will be asked
     public void requestLocationSettings(){

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest); //currently only high priority

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        final LocationSettingsStates states = LocationSettingsStates.fromIntent(intent);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
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

    /** This method checks if permission was granted, if not, it requests location permession at runtime */
    public boolean checkLocationPermission() {

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
            // means permission was granted during installation, api < 23
        else {
                return true;
        }

    }

    /** This callback handles the user's answer to the location permission request */
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

                        // bind and start service when permission is granted
                        bindService(new Intent(this, LocationService.class), mLocationConnection, Context.BIND_AUTO_CREATE);
                        startService(new Intent(this, LocationService.class));
                    }

                } else {

                    // permission denied, disabling the functionality that depends on this permission.

                }
                return;
            }
        }
    }

}
