package io.github.giulic3.apmap.data;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;

import io.github.giulic3.apmap.helpers.MathHelper;
import io.github.giulic3.apmap.models.AccessPoint;
import io.github.giulic3.apmap.services.ApService;

// TODO: reorder or split methods
public class UpdateDbTask extends AsyncTask<ArrayList<AccessPoint>, Void, Integer> {

    private Location scanningLocation;     // device location when ap scanning started
    private DatabaseHelper dbHelper;
    Context mContext;

    ArrayList<AccessPoint> apList;
    private static final int SCAN_LIMIT = 50;
    private ArrayList<String> updatedApBssid; // bssids of the aps that received lat/lon/coverage with last db update

    public UpdateDbTask(Context context, DatabaseHelper dbHelper, Location scanningLocation) {
        this.mContext = context;
        this.dbHelper = dbHelper;
        this.scanningLocation = scanningLocation;
    }
    // executed before starting the new thread, used for variable initialization
    protected void onPreExecute(){
        updatedApBssid = new ArrayList<String>();

    }

    // all the stuff to be done in background
    protected Integer doInBackground(ArrayList<AccessPoint> ... aps) { //TODO refactor method

        Log.d("DEBUG", "UpdateDbTask: doInBackground()");

        dbHelper.printAll(Database.Table2.TABLE_NAME);

        // for each ap found (note that the apList is in aps[0])
        for (int i = 0; i < aps[0].size(); i++) {
            // insert in db as scan_fab object only if there wasn't already a scan_fab for that bssid at those lat/lon
            // look for a scanresult in scan_fab table at a given lat lon
            if (scanningLocation != null) { // TODO: transform in method...
                double lat = Math.floor(scanningLocation.getLatitude() * 10000) / 10000;
                double lon = Math.floor(scanningLocation.getLongitude() * 10000) / 10000;

                boolean scanFound = dbHelper.searchBssidGivenLatLon(aps[0].get(i).getBssid(), lat, lon);
                // used to avoid doubles that mess with trilateration algorithm
                if (!scanFound) {
                    Log.d("DEBUG", "UpdateDbTask in scanFound");

                    dbHelper.insertScanObject(aps[0].get(i).getBssid(),
                            aps[0].get(i).getTimestamp(),
                            lat,
                            lon,
                            aps[0].get(i).getLevel());

                }

                // insert in table AccessPointInfoEntry (only if it's the first scan for this ap)
                boolean isFound = dbHelper.searchBssid(Database.Table1.TABLE_NAME, aps[0].get(i).getBssid());
                if (!isFound) {
                    dbHelper.insertAp(aps[0].get(i).getBssid(),
                            aps[0].get(i).getSsid(),
                            aps[0].get(i).getCapabilities(),
                            aps[0].get(i).getFrequency());
                }
        }
        }

        // perform approximation only every tot scans
        if (ApService.SCAN_COUNTER >= SCAN_LIMIT) {
            // i could move everything inside databasehelper TODO
            Cursor cursor = dbHelper.getInputSetForTrilateration();

            if (cursor.moveToFirst()) {
                String currentBssid = cursor.getString(
                        cursor.getColumnIndexOrThrow(Database.Table1.COLUMN_NAME_BSSID));
                  //get the first bssid

                int j = 0;
                boolean gettingTrilateration = true; // dev'essere true all'inizio
                // assegno il bssid, prendo le prime 3 misure che trovo, costruisco latlng + distances
                // dopo aver approssimato scalo al prossimo bssid
                // e continuo
                LatLng[] latLngs = new LatLng[3]; // works only with three points
                double[] distances = new double[3];
                do {
                    // if it differs, it means i can start with trilateration for a new bssid, at the beginning is always true
                    if (!cursor.getString(cursor.getColumnIndexOrThrow(Database.Table1.COLUMN_NAME_BSSID)).equals(currentBssid)) {
                        gettingTrilateration = true;
                        currentBssid = cursor.getString(
                                cursor.getColumnIndexOrThrow(Database.Table1.COLUMN_NAME_BSSID));
                    }

                    // this variable is used to mean we are in the middle of taking 3 measures for a certain bssid
                    if (gettingTrilateration) {
                        // retrieving information from db
                        double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(Database.Table2.COLUMN_NAME_SCAN_LATITUDE));
                        double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(Database.Table2.COLUMN_NAME_SCAN_LONGITUDE));
                        int level = cursor.getInt(cursor.getColumnIndexOrThrow(Database.Table2.COLUMN_NAME_LEVEL));
                        int frequency = cursor.getInt(cursor.getColumnIndexOrThrow(Database.Table1.COLUMN_NAME_FREQUENCY));

                        latLngs[j] = new LatLng(lat, lon);
                        distances[j] = MathHelper.levelToDistance(level, frequency); //ds in km

                        if (j == 2) {
                            LatLng res = MathHelper.getLocationByTrilateration(latLngs[0], distances[0],
                                    latLngs[1], distances[1], latLngs[2], distances[2]);
                            Log.d("DEBUG", "UpdateDbTask: latitude " + res.latitude + "\n" +
                                    "longitude " + res.longitude);
                            // then update position in db (only if trilateration went well)
                            if (!Double.isNaN(res.latitude) && !Double.isNaN(res.longitude)) {
                                // testing when it is better to approximate? also before coverage?
                                double newLat = Math.floor(res.latitude * 10000) / 10000;
                                double newLon = Math.floor(res.longitude * 10000) / 10000;
                                // update coverage
                                Cursor covCursor = dbHelper.searchScanResultsForCoverage(Database.Table2.TABLE_NAME, currentBssid);
                                double coverageRadius = MathHelper.determineCoverage(covCursor, currentBssid, newLat, newLon);
                                // patch value in case the math fails
                                if (coverageRadius > 100)
                                    coverageRadius = 30;
                                // approximate again before saving (see if it works) // TODO test
                                //double newLat = Math.floor(res.latitude * 100000) / 100000;
                                //double newLon = Math.floor(res.longitude * 100000) / 100000;
                                Log.d("DEBUG", "UpdateDbTask: newLat and newLon: "+String.valueOf(newLat)+" "+String.valueOf(newLon));
                                dbHelper.updateAp(currentBssid, null, null, newLat, newLon, coverageRadius);
                                // add to list  of updated bssid
                                updatedApBssid.add(currentBssid);

                            }
                            // then reset j and bool
                            j = 0;
                            gettingTrilateration = false;
                        } else {
                            j++;
                        }
                    } // end of if (gettingTrilateration)
                } // end of do block
                while (cursor.moveToNext());
            }

            //ApService.SCAN_COUNTER = 0; // TODO: uncomment when done testing
            cursor.close();
        }
        //dbHelper.close();

        Log.d("DEBUG", "UpdateDbTask: doInBackground() ended");
        apList = aps[0];
        return aps[0].size();

    }

    protected void onProgressUpdate(Void... progress) {

    }

    //TODO: e quelli che erano già sulla mappa? casino (può succedere se c'è un cambio di ssid ad es.)
    protected void onPostExecute(Integer result) {

        Log.d("DEBUG", "UpdateDbTask: onPostExecute()");
        // update map on the ui thread involving only scanned aps
        ArrayList<String> scanResultBssids = new ArrayList<>();
        ArrayList<String> scanResultSsids = new ArrayList<>();
        ArrayList<Integer> scanResultLevels = new ArrayList<>();

        for (int i = 0; i < apList.size(); i++) {
            scanResultBssids.add(apList.get(i).getBssid());
            scanResultSsids.add(apList.get(i).getSsid());
            scanResultLevels.add(apList.get(i).getLevel());
        }
        // send also the local broadcast to update the level in the textviews (with the scan results apList)
        Intent intent = new Intent("DatabaseUpdates");
        intent.putStringArrayListExtra("updatedApBssid", updatedApBssid);
        intent.putStringArrayListExtra("scanResultBssids", scanResultBssids); //not needed anymore
        intent.putStringArrayListExtra("scanResultSsids", scanResultSsids);
        intent.putIntegerArrayListExtra("scanResultLevels", scanResultLevels);

        // mContext refers to ApService that started the asynctask instance
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);

    }

}
