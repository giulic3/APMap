package io.github.giulic3.apmap.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.util.List;

public class ApService extends Service {

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private WifiManager mWifiManager;
    //private Scan mScan;
    private WifiReceiver mWifiReceiver;
    //private List<AccessPoint> apList; // TOOD: da definire AccessPoint come una struct privata

    public ApService() {
    }

    // A Handler allows you to send and process Message and Runnable objects associated with a thread's MessageQueue.
    // Handler that receives message from the thread (i dati sugli ap, da salvare nel database)
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            try {
                Thread.sleep(10000); //10 secondi
                // do work

            } catch (InterruptedException e) {
                // Restore interrupt status.
                Thread.currentThread().interrupt();
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }

    }

    // called by the system when this service is first created
    @Override
    public void onCreate(){
        // Start up a separate thread with background priority
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        // the looper is  used for threads to run a message loop
        mServiceLooper = thread.getLooper();
        // associate creating thread to Handler
        mServiceHandler = new ServiceHandler(mServiceLooper);



    }


    // called by the system to notify a service is no longer used
    // should clean up any resources it holds (e.g. threads)
    @Override
    public void onDestroy() {

    }

    /* executes when someone calls startService() */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        // setup broadcast receiver
        // TODO: per ora in OnStartCommand, non so se sia meglio in onCreate, ma a logica no perché se android
        // mi killa il service io ho bisogno che quando mi riparte venga nuovamente registrato il broadcast receiver
        mWifiReceiver = new WifiReceiver();
        // iscrivo il mio receiver ad un broadcast con evento di tipo specificato dall'intent filter passato
        // così l'onreceive parte sul thread separato
        registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION), null, mServiceHandler);
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // la scansione basta lanciarla una volta, il broadcast receiver entra nell'onReceive
        // ogni volta che si ha un nuovo risultato per la scansione
        // quindi non c'è bisogno di lanciarla nel thread in un loop
        mWifiManager.startScan();


        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);


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
    //
    class WifiReceiver extends BroadcastReceiver {
        private List<ScanResult> wifiList;

        // executes when scan results are available (foreach scan ? or only the first time?)
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            wifiList = mWifiManager.getScanResults();
            // a chi spedisco questi dati? devo usarli per aggiornare PRIMA il database e poi la mappa
            // per rendere più accurata la posizione
            for (int i = 0; i < wifiList.size(); i++) {
                Toast toast = Toast.makeText(getApplicationContext(), "SSID: " + wifiList.get(i).SSID + "\n" +
                                "BSSID: " + wifiList.get(i).BSSID + "\n" +
                                "capabilities: " + wifiList.get(i).capabilities + "\n" +
                                "frequency: " + wifiList.get(i).frequency + "\n" +
                                "level: " + wifiList.get(i).level + "\n" +
                                "timestamp: " + wifiList.get(i).timestamp
                        , Toast.LENGTH_LONG);
                toast.show();
            }
        }

    }
}


