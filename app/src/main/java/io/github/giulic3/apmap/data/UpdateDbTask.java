package io.github.giulic3.apmap.data;

import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import io.github.giulic3.apmap.data.AccessPoint;
import io.github.giulic3.apmap.services.ApService;


public class UpdateDbTask extends AsyncTask<List<AccessPoint>, Void, Integer> {

    // device location when ap scanning started
    Location scanningLocation;
    DatabaseHelper dbHelper;
    List<AccessPoint> apList;
    private static final int SCAN_LIMIT = 100;

    // my own constructor
    public UpdateDbTask(DatabaseHelper dbHelper, Location scanningLocation) {
        this.dbHelper = dbHelper;
        this.scanningLocation = scanningLocation;
    }
    // executed before starting the new thread
    protected void onPreExecute(){

    }

    // all the stuff to be done in background
    protected Integer doInBackground(List<AccessPoint> ... aps) {


        Log.d("DEBUG", "UpdateDbTask: doInBackground");
        // for each ap found (note that the apList is in aps[0])
        for (int i = 0; i < aps[0].size(); i++) {
            // insert in db as scan object
            dbHelper.insertScanObject(aps[0].get(i).getBssid(),
                    aps[0].get(i).getTimestamp(),
                    scanningLocation.getLatitude(),
                    scanningLocation.getLongitude(),
                    aps[0].get(i).getLevel());


            // TODO change bool definition, it's counterintuitive
            // insert in db as accesspointinfo (only if it's the first time)
            boolean canInsert = dbHelper.searchBssid(Database.Table1.TABLE_NAME, aps[0].get(i).getBssid());
            if (!canInsert) {
                dbHelper.insertAp(aps[0].get(i).getBssid(),
                        aps[0].get(i).getSsid(),
                        aps[0].get(i).getCapabilities(),
                        aps[0].get(i).getFrequency());
            }
            //TODO: terminare
        }

        // printing all scanobject entries
        //dbHelper.printAll(Database.Table2.TABLE_NAME);
        // printing all accesspointinfo entries
        dbHelper.printAll(Database.Table1.TABLE_NAME);

        apList = aps[0]; // che succede ad aps[0] quando il metodo termina?

        // perform approximation every 100 scan
        if (ApService.SCAN_COUNTER >= SCAN_LIMIT) {

            Cursor cursor = dbHelper.getInputSetForTriangulation();
            updateAccessPointPosition();

            // TODO bad habit to assign a public field like this
            ApService.SCAN_COUNTER = 0;
        }

        dbHelper.close();

        return aps[0].size(); // con nessuna utilità apparente

    }

    protected void onProgressUpdate(Void... progress) {

    }

    protected void onPostExecute(Void... result) {
        Log.d("DEBUG", "UpdateDbTask: onPostExecute");
        // update map on the ui thread involving only scanned aps
        refreshMap();
    }

    // update on the map infos relative to given list of ap
    private void refreshMap(){
        // TODO:
        // find marker by bssid
        // move marker, update info on bottomsheet or list
    }



    private void updateAccessPointPosition(){

        Log.d("DEBUG", "UpdateDbTask: updateAccessPointPosition()");
    }

    // given an array of n objects (lat+lon+level) that corresponds to a certain bssid (ap)
    private void performTriangulation(ArrayList<LatLng> latLngs) {

    }
}
