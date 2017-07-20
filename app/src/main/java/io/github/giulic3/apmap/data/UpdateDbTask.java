package io.github.giulic3.apmap.data;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;

import io.github.giulic3.apmap.helpers.MathHelper;
import io.github.giulic3.apmap.models.AccessPoint;
import io.github.giulic3.apmap.services.ApService;

public class UpdateDbTask extends AsyncTask<ArrayList<AccessPoint>, Void, Integer> {

    private static final int SCAN_LIMIT = 3; // trying to perform trilateration every 3 scans

    private Location scanningLocation; // device location when ap scanning started
    private DatabaseHelper dbHelper;
    Context mContext;

    ArrayList<AccessPoint> apList;
    private ArrayList<String> updatedApBssid; // aps (=bssids) that got lat/lon/coverage with last db update

    public UpdateDbTask(Context context, DatabaseHelper dbHelper, Location scanningLocation) {
        this.mContext = context;
        this.dbHelper = dbHelper;
        this.scanningLocation = scanningLocation;
    }

    // Called before starting the new thread, used for variable initialization
    protected void onPreExecute(){
        updatedApBssid = new ArrayList<String>();

    }


    protected Integer doInBackground(ArrayList<AccessPoint> ... aps) {

        updateDatabaseWithScanResults(aps[0]);

        // perform trilateration only every x scans
        if (ApService.SCAN_COUNTER >= SCAN_LIMIT) {

            Cursor cursor = dbHelper.getInputSetForTrilateration();

            if (cursor.moveToFirst()) {
                //get the first bssid
                String currentBssid = cursor.getString(
                        cursor.getColumnIndexOrThrow(Database.Table1.COLUMN_NAME_BSSID));

                int j = 0;
                boolean gettingTrilateration = true;
                LatLng[] latLngs = new LatLng[3];
                double[] distances = new double[3];
                do {
                    // if it differs, means we can start with trilateration for a new bssid, at the beginning is always true
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

                            // then update position in db (only if trilateration went well)
                            if (!Double.isNaN(res.latitude) && !Double.isNaN(res.longitude)) {

                                double newLat = MathHelper.truncateDouble(res.latitude);
                                double newLon = MathHelper.truncateDouble(res.longitude);
                                // update coverage
                                Cursor covCursor = dbHelper.searchScanResultsForCoverage(Database.Table2.TABLE_NAME, currentBssid);
                                double coverageRadius = MathHelper.determineCoverage(covCursor, newLat, newLon);
                                // patch value in case the math fails
                                if (coverageRadius > 100)
                                    coverageRadius = 30;

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

            ApService.SCAN_COUNTER = 0;
            cursor.close();
        }

        apList = aps[0];
        return aps[0].size();

    }


    private void updateDatabaseWithScanResults(ArrayList<AccessPoint> aps) {

        // for each ap found
        for (int i = 0; i < aps.size(); i++) {
            // insert in db as scan object only if there wasn't already a scan for that bssid at those lat/lon
            if (scanningLocation != null) {

                double lat = MathHelper.truncateDouble(scanningLocation.getLatitude());
                double lon = MathHelper.truncateDouble(scanningLocation.getLongitude());
                boolean scanFound = dbHelper.searchBssidGivenLatLon(aps.get(i).getBssid(), lat, lon);
                // avoid duplicates in ScanResult table
                if (!scanFound) {

                    dbHelper.insertScanObject(aps.get(i).getBssid(),
                            aps.get(i).getTimestamp(),
                            lat,
                            lon,
                            aps.get(i).getLevel());

                }
                // avoid duplicates in AccessPointInfo table
                // insert in table only if it's the first scan for this ap
                boolean isFound = dbHelper.searchBssid(Database.Table1.TABLE_NAME, aps.get(i).getBssid());
                if (!isFound) {
                    dbHelper.insertAp(aps.get(i).getBssid(),
                            aps.get(i).getSsid(),
                            aps.get(i).getCapabilities(),
                            aps.get(i).getFrequency());
                }
            }
        }

    }
    protected void onProgressUpdate(Void... progress) { }

    protected void onPostExecute(Integer result) {

        // update map on the ui thread involving only scanned aps
        ArrayList<String> scanResultBssids = new ArrayList<>();
        ArrayList<String> scanResultSsids = new ArrayList<>();
        ArrayList<Integer> scanResultLevels = new ArrayList<>();

        for (int i = 0; i < apList.size(); i++) {
            scanResultBssids.add(apList.get(i).getBssid());
            scanResultSsids.add(apList.get(i).getSsid());
            scanResultLevels.add(apList.get(i).getLevel());
        }
        // also send the local broadcast to update the level in the textviews (with the scan results apList)
        Intent intent = new Intent("DatabaseUpdates");
        intent.putStringArrayListExtra("updatedApBssid", updatedApBssid);
        intent.putStringArrayListExtra("scanResultBssids", scanResultBssids); 
        intent.putStringArrayListExtra("scanResultSsids", scanResultSsids);
        intent.putIntegerArrayListExtra("scanResultLevels", scanResultLevels);

        // mContext refers to ApService that started the AsyncTask instance
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);

    }

}
