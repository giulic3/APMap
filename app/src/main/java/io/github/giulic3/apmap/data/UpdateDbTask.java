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
import java.util.List;

import io.github.giulic3.apmap.activities.MainActivity;
import io.github.giulic3.apmap.services.ApService;


public class UpdateDbTask extends AsyncTask<List<AccessPoint>, Void, Integer> {

    // device location when ap scanning started
    Location scanningLocation; // default modifier is 'package-private'
    DatabaseHelper dbHelper;
    Context mContext;

    List<AccessPoint> apList;
    AccessPoint apProva;
    private static final int SCAN_LIMIT = 50;
    private ArrayList<String> updatedApBssid; // contiene i bssid degli apinfoentry che ora hanno lat/lon/coverage
    // usata per aggiornar i marker sulla mappa

    // my own constructor
    public UpdateDbTask(Context context, DatabaseHelper dbHelper, Location scanningLocation) {
        this.mContext = context;
        this.dbHelper = dbHelper;
        this.scanningLocation = scanningLocation;
    }
    // executed before starting the new thread
    protected void onPreExecute(){

    }

    // all the stuff to be done in background
    protected Integer doInBackground(List<AccessPoint> ... aps) { //TODO refactor method

        Log.d("DEBUG", "UpdateDbTask: doInBackground()");
        dbHelper.printAll(Database.Table2.TABLE_NAME);
        updatedApBssid = new ArrayList<String>();

        //TEMPORANEO, TOGLIERE
        apProva = aps[0].get(0); //il primo ap della scansione

        // for each ap found (note that the apList is in aps[0])
        for (int i = 0; i < aps[0].size(); i++) {
            // insert in db as scan object only if there wasn't already a scan for that bssid at those lat/lon
            // look for a scanresult in scan table at a given lat lon
            // first truncate double to second decimal cifra perché il db me le salva troncate!
            // TODO: use 10000 instead of 1000 (and see if db saves 4 digits after float) if trilateration is not accurate
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

            // insert in db as accesspointinfo (only if it's the first time)
            boolean isFound = dbHelper.searchBssid(Database.Table1.TABLE_NAME, aps[0].get(i).getBssid());
            if (!isFound) {
                dbHelper.insertAp(aps[0].get(i).getBssid(),
                        aps[0].get(i).getSsid(),
                        aps[0].get(i).getCapabilities(),
                        aps[0].get(i).getFrequency());
            }
        }

        // printing all scanobject entries
        //dbHelper.printAll(Database.Table2.TABLE_NAME);
        // printing all accesspointinfo entries
        //dbHelper.printAll(Database.Table1.TABLE_NAME);

        apList = aps[0];

        // perform approximation every 100 scan, anche meno volendo
        if (ApService.SCAN_COUNTER >= SCAN_LIMIT) {
            // i could move everything inside databasehelper TODO
            Cursor cursor = dbHelper.getInputSetForTrilateration();

            if (cursor.moveToFirst()) {
                String currentBssid = cursor.getString(
                        cursor.getColumnIndexOrThrow(Database.Table1.COLUMN_NAME_BSSID));
                  //get the first bssid

                int j = 0;
                // assegno il bssid, prendo le prime 3 misure che trovo, costruisco latlng + distances
                // dopo aver approssimato scalo al prossimo bssid
                // e continuo
                LatLng[] latLngs = new LatLng[3]; // works only with three points
                double[] distances = new double[3];
                do {

                    // retrieving information from db
                    double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(Database.Table2.COLUMN_NAME_SCAN_LATITUDE));
                    double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(Database.Table2.COLUMN_NAME_SCAN_LONGITUDE));
                    int level = cursor.getInt(cursor.getColumnIndexOrThrow(Database.Table2.COLUMN_NAME_LEVEL));
                    int frequency = cursor.getInt(cursor.getColumnIndexOrThrow(Database.Table1.COLUMN_NAME_FREQUENCY));

                    latLngs[j] = new LatLng(lat, lon);
                    distances[j] = levelToDistance(level, frequency);

                    if (j == 2)  {
                        LatLng res = getLocationByTrilateration(latLngs[0], distances[0], latLngs[1], distances[1], latLngs[2], distances[2]);
                        Log.d("DEBUG", "UpdateDbTask: latitude "+res.latitude+"\n" +
                                "longitude "+res.longitude);
                        // then update position in db (only if trilateration went well)
                        if (!Double.isNaN(res.latitude) && !Double.isNaN(res.longitude)) {
                            // update coverage
                            double coverageRadius = determineCoverage(currentBssid, res.latitude, res.longitude);
                            if (coverageRadius > 400 ) // PATCH TODO
                                coverageRadius = 50; // metto valore standard perché se lo lascio null mi ritrovo una nullpointerex

                            //HARDCODED
                                // quando vado a riempire mCircles nella mainactivity
                                dbHelper.updateAp(currentBssid, null, null, res.latitude, res.longitude, coverageRadius);
                                //IL PROBLEMA È CHE DOPO NON LA AGGIORNERÀ MAI PIÙ LA COVERAGE, QUINDI NON VA BENE!
                                updatedApBssid.add(currentBssid); // add to list  of updated bssid

                        }
                        // then go to next bssid
                        while (cursor.getString(cursor.getColumnIndexOrThrow(
                                Database.Table1.COLUMN_NAME_BSSID)).equals(currentBssid)) {
                            cursor.moveToNext();
                        }

                        currentBssid = cursor.getString(cursor.getColumnIndexOrThrow(
                                Database.Table1.COLUMN_NAME_BSSID));

                        // horrible patch but should work
                        cursor.moveToPrevious();
                        // then reset j
                        j = 0;
                    }
                    else {
                        j++;
                    }

                }
                while (cursor.moveToNext());
            }

            // TODO bad habit to assign a public field like this
            ApService.SCAN_COUNTER = 0;
            cursor.close();
        }
        // è importante chiuderlo?
        //dbHelper.close();

        Log.d("DEBUG", "UpdateDbTask: doInBackground() ended");
        return aps[0].size(); // con nessuna utilità apparente

    }

    protected void onProgressUpdate(Void... progress) {

    }

    //TODO: e quelli che erano già sulla mappa? casino (può succedere se c'è un cambio di ssid ad es. o se si prevede
    // un miglioramento dell'approssimazione della posizione
    protected void onPostExecute(Integer result) {
        Log.d("DEBUG", "UpdateDbTask: onPostExecute()");


        //PROVA TEMPORANEA
        /*
        double lat = Math.floor(scanningLocation.getLatitude() * 1000) / 1000;
        double lon = Math.floor(scanningLocation.getLongitude() * 1000) / 1000;

        boolean scanFound = dbHelper.searchBssidGivenLatLon(apProva.getBssid(), lat, lon);
        // used to avoid doubles that mess with trilateration algorithm
        if (!scanFound) {
            Log.d("DEBUG", "UpdateDbTask in scanFound");

            dbHelper.insertScanObject(apProva.getBssid(),
                    apProva.getTimestamp(),
                    lat,
                    lon,
                    apProva.getLevel());

        }
        */
        // update map on the ui thread involving only scanned aps
        // mContext refers to ApService that started the asynctask instance
        //Intent intent = new Intent(mContext, MainActivity.class);
        Intent intent = new Intent("DatabaseUpdates");
        intent.putStringArrayListExtra("updatedApBssid", updatedApBssid);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);

    }

// EFFICIENTE MUOVENDOSI DI POCO, NON SO CON GROSSE VARIAZIONI DI LAT/LON, mettere scansione veloce
// taken from stackoverflow, must give credit to author, distances in km?
    // works with only 3 points
    public LatLng getLocationByTrilateration(LatLng location1, double distance1,
                                                 LatLng location2, double distance2,
                                                 LatLng location3, double distance3){

        Log.d("DEBUG", "UpdateDbTask: getLocationByTrilateration()");
        //DECLARE VARIABLES

        double[] P1   = new double[2];
        double[] P2   = new double[2];
        double[] P3   = new double[2];
        double[] ex   = new double[2];
        double[] ey   = new double[2];
        double[] p3p1 = new double[2];
        double jval  = 0;
        double temp  = 0;
        double ival  = 0;
        double p3p1i = 0;
        double triptx;
        double tripty;
        double xval;
        double yval;
        double t1;
        double t2;
        double t3;
        double t;
        double exx;
        double d;
        double eyy;

        //TRANSlaTE POINTS TO VECTORS
        //POINT 1
        P1[0] = location1.latitude;
        P1[1] = location1.longitude;
        //POINT 2
        P2[0] = location2.latitude;
        P2[1] = location2.longitude;
        //POINT 3
        P3[0] = location3.latitude;
        P3[1] = location3.longitude;

        //TRANSFORM THE METERS VALUE FOR THE MAP UNIT
        //DISTANCE BETWEEN POINT 1 AND MY LOCATION
        distance1 = (distance1 / 100000);
        //DISTANCE BETWEEN POINT 2 AND MY LOCATION
        distance2 = (distance2 / 100000);
        //DISTANCE BETWEEN POINT 3 AND MY LOCATION
        distance3 = (distance3 / 100000);

        for (int i = 0; i < P1.length; i++) {
            t1   = P2[i];
            t2   = P1[i];
            t    = t1 - t2;
            temp += (t*t);
        }
        d = Math.sqrt(temp);
        for (int i = 0; i < P1.length; i++) {
            t1    = P2[i];
            t2    = P1[i];
            exx   = (t1 - t2)/(Math.sqrt(temp));
            ex[i] = exx;
        }
        for (int i = 0; i < P3.length; i++) {
            t1      = P3[i];
            t2      = P1[i];
            t3      = t1 - t2;
            p3p1[i] = t3;
        }
        for (int i = 0; i < ex.length; i++) {
            t1 = ex[i];
            t2 = p3p1[i];
            ival += (t1*t2);
        }
        for (int  i = 0; i < P3.length; i++) {
            t1 = P3[i];
            t2 = P1[i];
            t3 = ex[i] * ival;
            t  = t1 - t2 -t3;
            p3p1i += (t*t);
        }
        for (int i = 0; i < P3.length; i++) {
            t1 = P3[i];
            t2 = P1[i];
            t3 = ex[i] * ival;
            eyy = (t1 - t2 - t3)/Math.sqrt(p3p1i);
            ey[i] = eyy;
        }
        for (int i = 0; i < ey.length; i++) {
            t1 = ey[i];
            t2 = p3p1[i];
            jval += (t1*t2);
        }
        xval = (Math.pow(distance1, 2) - Math.pow(distance2, 2) + Math.pow(d, 2))/(2*d);
        yval = ((Math.pow(distance1, 2) - Math.pow(distance3, 2) + Math.pow(ival, 2) + Math.pow(jval, 2))/(2*jval)) - ((ival/jval)*xval);

        t1 = location1.latitude;
        t2 = ex[0] * xval;
        t3 = ey[0] * yval;
        triptx = t1 + t2 + t3;

        t1 = location1.longitude;
        t2 = ex[1] * xval;
        t3 = ey[1] * yval;
        tripty = t1 + t2 + t3;


        return new LatLng(triptx,tripty);

    }
    // takes level in dbm, frequency in mhZ and returns distance in km using an approximation of
    // free-space path loss formula
    private double levelToDistance(int level, int frequency) { // be sure of minus sign

        double distanceInMetres = Math.pow(10, ((27.55 - (20*Math.log10(frequency)) - level)/ 20));
        double distanceInKilometres = distanceInMetres / 1000;
        return distanceInKilometres;
    }
    // static? can't do better?
    public static double convertToDistanceUsingDoubles(double accessPointLatitude, double accessPointLongitude,
                                                 double scanLatitude, double scanLongitude ) {


        Log.d("DEBUG", "UpdateDbTask: scanLatitude: "+scanLatitude+" scanLongitude: "+scanLongitude);
        Log.d("DEBUG", "UpdateDbTask: convertToDistanceUsingDoubles()");
        double earthRadius = 6371000; // metres
        double φ1 = Math.toRadians(accessPointLatitude);
        double φ2 = Math.toRadians(scanLatitude);
        double Δφ = Math.toRadians(scanLatitude - accessPointLatitude);
        double Δλ = Math.toRadians(scanLongitude - accessPointLongitude);

        double a = Math.sin(Δφ/2) * Math.sin(Δφ/2) +
                Math.cos(φ1) * Math.cos(φ2) *
                        Math.sin(Δλ/2) * Math.sin(Δλ/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        double distanceInMetres = earthRadius * c;
        return distanceInMetres;
    }
    // given a set of scan results for a certain bssid, this method returns the average among
    // all distances
    // then approximates coverage as a circle, returning its radius
    // now using patch per evitare di aggiornare con valori troppo sfasati, finché non si è capito il problema TODO
    private double determineCoverage(String bssid, double accessPointLatitude, double accessPointLongitude) {

       Cursor cursor = dbHelper.searchScanResultsForCoverage(Database.Table2.TABLE_NAME, bssid);
        int length = cursor.getCount();
        double[] scanLatitudes = new double[length];
        double[] scanLongitudes = new double[length];
        double[] distances = new double[length];

        if (cursor.moveToFirst()) { //IMPORTANTE, ALLA FINE DI UNA QUERY IL CURSOR È POSIZIONATO A -1
            for (int i = 0; i < length; i++) { // come fa length a essere 15? al max 3 boh

                scanLatitudes[i] = cursor.getDouble(cursor.getColumnIndexOrThrow(Database.Table2.COLUMN_NAME_SCAN_LATITUDE));
                scanLongitudes[i] = cursor.getDouble(cursor.getColumnIndexOrThrow(Database.Table2.COLUMN_NAME_SCAN_LONGITUDE));

                distances[i] = convertToDistanceUsingDoubles(accessPointLatitude, accessPointLongitude,
                        scanLatitudes[i], scanLongitudes[i]);
            }
        }

        // calculate average or max, just decide
        double coverageRadius = max(distances);

        return coverageRadius;
    }

    // dunno if better using average or max
    private double average(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;

    }
    // as above, returns -1 if values is empty, i'm using it with distances so i know they must be > 0
    private double max(double[] values) {
        double max = -1;
        for (double v : values)
            if (v > max)  max = v ;

        return max;

    }
}
