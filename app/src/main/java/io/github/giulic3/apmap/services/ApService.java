package io.github.giulic3.apmap.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.github.giulic3.apmap.models.AccessPoint;
import io.github.giulic3.apmap.data.DatabaseHelper;
import io.github.giulic3.apmap.data.UpdateDbTask;

public class ApService extends Service {

    // used in conjunction with lastknownlocation to determine if can perform scansion
    private static final long THREAD_SLEEP = 60000; // set 60000
    private WifiManager mWifiManager;
    private WifiReceiver mWifiReceiver;
    //private Location previousLocation;
    private Location mLastKnownLocation;
    private DatabaseHelper mDbHelper;
     // used to count number of scan performed, every 100 scans, will
    // attempt to perform triangulation again TODO: set to 0 when done testing
     public static int SCAN_COUNTER = 100;

    /** Called by the system when this service is first created */
    @Override
    public void onCreate(){

        Log.d("DEBUG", "ApService: onCreate()");

        LocalBroadcastManager.getInstance(ApService.this).registerReceiver(
                mLocationReceiver, new IntentFilter("GPSLocationUpdates"));
    }


    // called by the system to notify a service is no longer used
    // should clean up any resources it holds (e.g. threads)
    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(ApService.this).unregisterReceiver(mLocationReceiver);

    }

    // executes when someone calls startService()
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        Log.d("DEBUG", "ApService: onStartCommand()");
        // prova
        mDbHelper = new DatabaseHelper(getApplicationContext());

        // setup broadcast receiver
        mWifiReceiver = new WifiReceiver();

        registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        if(!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }

        new Thread(new Runnable(){
            public void run() {

                while(true)
                {
                    try {
                        Log.d("DEBUG", "ApService: in thread startScan()");

                        mWifiManager.startScan();
                        SCAN_COUNTER++;
                        Thread.sleep(THREAD_SLEEP);


                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }

            }
        }).start();

        // automatically restarts service if killed
        return START_STICKY;
    }

    // executes when MainActivity asks to bind with this service
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    class WifiReceiver extends BroadcastReceiver {
        private List<ScanResult> wifiList;

        // executes when scan_fab results are available
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            ArrayList<AccessPoint> apList = new ArrayList<AccessPoint>();

            Log.d("DEBUG", "ApService: onReceive()");
            wifiList = mWifiManager.getScanResults();

            for (int i = 0; i < wifiList.size(); i++) {

                apList.add(new AccessPoint(wifiList.get(i).BSSID, wifiList.get(i).SSID,
                        wifiList.get(i).capabilities, wifiList.get(i).frequency,
                        wifiList.get(i).level,
                        wifiList.get(i).timestamp));


                Log.i("INFO", "BSSID: " + wifiList.get(i).BSSID + "\n" +
                        "SSID: " + wifiList.get(i).SSID + "\n" +
                        "capabilities: " + wifiList.get(i).capabilities + "\n" +
                        "frequency: " + wifiList.get(i).frequency + "\n" +
                        "level: " + wifiList.get(i).level + "\n" +
                        "timestamp: " + wifiList.get(i).timestamp );


            }

            DatabaseHelper mDbHelper = new DatabaseHelper(ApService.this);
            Context mContext = getApplicationContext();
            new UpdateDbTask(mContext, mDbHelper, mLastKnownLocation).execute(apList);

            Toast toast = Toast.makeText(getApplicationContext(), "Database updated", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    // broadcast receiver to get location
    private BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String message = intent.getStringExtra("Status");
            Bundle b = intent.getBundleExtra("Location");
            mLastKnownLocation = (Location) b.getParcelable("Location");
        }
    };

}


