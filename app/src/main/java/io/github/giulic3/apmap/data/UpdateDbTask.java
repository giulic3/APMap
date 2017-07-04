package io.github.giulic3.apmap.data;

import android.location.Location;
import android.os.AsyncTask;

import java.util.List;

import io.github.giulic3.apmap.data.AccessPoint;


public class UpdateDbTask extends AsyncTask<List<AccessPoint>, Void, Void> {

    // device location when ap scanning started
    Location scanningLocation;
    DatabaseHelper mDbHelper;

    // my own constructor
    public UpdateDbTask(Location scanningLocation) {
        this.scanningLocation = scanningLocation;
    }

    protected List<AccessPoint> doInBackground(List<AccessPoint> ... aps) {


    }

    protected void onProgressUpdate(Void... progress) {

    }

    protected void onPostExecute(Void... result) {

    }

}
