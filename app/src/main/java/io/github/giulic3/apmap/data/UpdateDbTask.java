package io.github.giulic3.apmap.data;

import android.location.Location;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import io.github.giulic3.apmap.data.AccessPoint;


public class UpdateDbTask extends AsyncTask<List<AccessPoint>, Void, Integer> {

    // device location when ap scanning started
    Location scanningLocation;
    DatabaseHelper dbHelper;

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

        // for each ap found (note that the apList is in aps[0])
        for (int i = 0; i < aps[0].size(); i++) {
            // insert in db as scan object
            dbHelper.insertScanObject(aps[0].get(i).getBssid(),
                            aps[0].get(i).getTimestamp(),
                            scanningLocation.getLatitude(),
                            scanningLocation.getLongitude(),
                            aps[0].get(i).getLevel());

            // insert in db as accesspointinfo (only if it's the first time)
            dbHelper.insertAp(aps[0].get(i).getBssid(),
                    aps[0].get(i).getSsid(),
                    aps[0].get(i).getCapabilities(),
                    aps[0].get(i).getFrequency());

            //TODO: terminare
        }

        int i = 0; return i;

    }

    protected void onProgressUpdate(Void... progress) {

    }

    protected void onPostExecute(Void... result) {

    }

}
