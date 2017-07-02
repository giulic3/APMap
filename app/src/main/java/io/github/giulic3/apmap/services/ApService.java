package io.github.giulic3.apmap.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.github.giulic3.apmap.data.AccessPoint;
import io.github.giulic3.apmap.data.Database;
import io.github.giulic3.apmap.data.DatabaseHelper;

public class ApService extends Service {

    private static final long THREAD_SLEEP = 60000;
    private Looper mServiceLooper;
    //private ServiceHandler mServiceHandler;
    private WifiManager mWifiManager;
    private WifiReceiver mWifiReceiver;
    private Location mLastKnownLocation;
    private DatabaseHelper mDbHelper;
    // instantiate database

    // A Handler allows you to send and process Message and Runnable objects associated with a thread's MessageQueue.
    // Handler that receives message from the thread (i dati sugli ap, da salvare nel database)

    // non penso che serva. ho bisogno ddi un handler che prenda i dati della scansione, dalla onReceive faccio un
    // sendmsg, boh?
    /*
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            try {
                Thread.sleep(10000); //10 secondi
                //do work
                Log.d("DEBUG", "ApService: handleMessage()");
                mWifiManager.startScan();


            } catch (InterruptedException e) {
                // Restore interrupt status.
                Thread.currentThread().interrupt();
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }

    }
*/
    // called by the system when this service is first created
    @Override
    public void onCreate(){

        Log.d("DEBUG", "ApService: onCreate()");
        /*
        // Start up a separate thread with background priority
       /HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        // the looper is  used for threads to run a message loop
        mServiceLooper = thread.getLooper();
        // associate creating thread to Handler
        mServiceHandler = new ServiceHandler(mServiceLooper);
        */

    }


    // called by the system to notify a service is no longer used
    // should clean up any resources it holds (e.g. threads)
    @Override
    public void onDestroy() {

    }

    // executes when someone calls startService()
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        Log.d("DEBUG", "ApService: onStartCommand()");
        // prova
        mDbHelper = new DatabaseHelper(getApplicationContext());

        // setup broadcast receiver
        mWifiReceiver = new WifiReceiver();
        // iscrivo il mio receiver ad un broadcast con evento di tipo specificato dall'intent filter passato
        // così l'onreceive parte sul thread separato ? vedi android monitor
        //registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION), null, mServiceHandler);
        registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        if(!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }

        new Thread(new Runnable(){
            public void run() {
                // TODO Auto-generated method stub
                while(true)
                {
                    try {
                        //mServiceHandler.sendEmptyMessage(0);
                        // increase thread_sleep because onreceive is much slower than thread
                        Log.d("DEBUG", "ApService: in thread startScan()");
                        mWifiManager.startScan();

                        Thread.sleep(THREAD_SLEEP);


                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }

            }
        }).start();

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
       // Message msg = mServiceHandler.obtainMessage();
        //msg.arg1 = startId;
        //mServiceHandler.sendMessage(msg);

        // automatically restarts service if killed
        return START_STICKY;
    }

    // executes when MainActivity asks to bind with this service
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // con un broadcast receiver l'app viene notificata dal sistema ogni qualvolta si verifica
    // un determinato evento (per il quale il receiver si è registrato)
    class WifiReceiver extends BroadcastReceiver {
        private List<ScanResult> wifiList;

        // executes when scan results are available
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            List<AccessPoint> apList = new ArrayList<AccessPoint>();

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

            //mWifiManager.startScan();
            //new UpdateDbTask.java(mLastKnownLocation).execute(apList);
            long ok = mDbHelper.insertAp(wifiList.get(0).BSSID, wifiList.get(0).SSID,
                    wifiList.get(0).capabilities, "ehiehiehi", null, null, null, null);
            Log.d("DEBUG", "ApService: ap inserted "+ok);
            // inserts twice if it's already there????
            updateDatabase();

        }
    }

    private void updateDatabase(){

        // TODO: temporarily update db without asynctask
        //DatabaseHelper mDbHelper = new DatabaseHelper(ApService.this);


        // uso una select per vedere il contenuto del database
        SQLiteDatabase dbtry = mDbHelper.getReadableDatabase();
        Cursor cursor = dbtry.query(Database.Table1.TABLE_NAME, null, null, null,
                null, null, null);
        //non entra mai
        // return false (cursor is empty?)
        if (cursor != null && cursor.getCount()>0) Log.d("DEBUG", "sticazzi");
        /*
        if (c.moveToFirst()) {
            do {
                //assign values
                String column1 = c.getString(0);
                String column2 = c.getString(1);
                String column3 = c.getString(2);
                Log.d("DEBUG", column1 +" "+ column2 +" "+ column3);

            } while (c.moveToNext());
        }
        */

        while(cursor.moveToNext()) {
            String bssid = cursor.getString(
                    cursor.getColumnIndexOrThrow(Database.Table1.COLUMN_NAME_BSSID));
            Log.d("DEBUG", "sticazzi: bssid");
        }

        cursor.close();
        //dbtry.close();
    }
}


