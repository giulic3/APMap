package io.github.giulic3.apmap.data;

import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

            if (cursor.moveToFirst()) {
                cursor.getString(0);  //get the first bssid
                LatLng[] latLngs = new LatLng[3]; // works only with three points
                double[] distances = new double[3]; // TODO: where to put these
                int i = 0;
                // assegno il bssid, prendo le prime 3 misure che trovo, costruisco latlng + distances
                // dopo aver approssimato scalo al prossimo bssid
                // e continuo
                do {
                    double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(Database.Table2.COLUMN_NAME_SCAN_LATITUDE));
                    double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(Database.Table2.COLUMN_NAME_SCAN_LONGITUDE));
                    latLngs[i] = new LatLng(lat, lon);
                    int level = cursor.getInt(cursor.getColumnIndexOrThrow(Database.Table2.COLUMN_NAME_LEVEL));
                    int frequency = cursor.getInt(cursor.getColumnIndexOrThrow(Database.Table1.COLUMN_NAME_FREQUENCY));
                    distances[i] = levelToDistance(level, frequency); // TODO: completare
                    if (i == 2)  {
                        LatLng res = getLocationByTrilateration(latLngs[0], distances[0], latLngs[1], distances[1], latLngs[2], distances[2]);

                        // then update position in db

                        // then reset i
                        i = 0;
                    }

                }
                while (cursor.moveToNext());
            }


            //TEMPORARY, FOR TESTING
            /*
            LatLng[] latLngs = new LatLng[]{new LatLng(37.418436,-121.963477),
                    new LatLng(37.417243,-121.961889 ), new LatLng(37.418692,-121.960194)};
            double[] distances = new double[]{0.265710701754, 0.234592423446, 0.0548954278262 }; //distances in km
            */
            LatLng res = getLocationByTrilateration(latLngs[0], distances[0], latLngs[1], distances[1], latLngs[2], distances[2]);
            Log.d("DEBUG", "UpdateDbTask: latitude "+res.latitude+"\n" +
                    "longitude "+res.longitude);
            // should be 37.4191023738 -121.960579208
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
/*
    // given an array of n objects (lat+lon+level in metres) that corresponds to a certain bssid (ap)
    private LatLng performTriangulation(LatLng[] latLngs, double[] distances) {

        double[][] positions = new double[latLngs.length][3]; // tante righe quante le misure, e tre colonne
        // le 3 dimensioni x,y,z

        double earthRadius = 6371; // in km
        // convert latlng to cartesian points and fill in positions array
        // also convert distances (in m/km) to absolutes
        for (int i = 0; i < latLngs.length - 1 ; i++) {

            double lat = latLngs[i].latitude;
            double lon = latLngs[i].longitude;

            double x = earthRadius * (Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(lon)));
            double y = earthRadius * (Math.cos(Math.toRadians(lat)) * Math.sin(Math.toRadians(lon)));
            double z = earthRadius * (Math.sin(Math.toRadians(lat)));
            positions[i][0] = x;
            positions[i][1] = y;
            positions[i][2] = z;
        }

        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(
                new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
        LeastSquaresOptimizer.Optimum optimum = solver.solve();

        // the answer
        double[] centroid = optimum.getPoint().toArray();

        double resultX = centroid[0];
        double resultY = centroid[1];
        double resultZ = centroid[2];
        LatLng result = new LatLng(Math.asin(resultZ / earthRadius),
                                 Math.atan2(resultY, resultX));

        return result;

    }
*/
    // works with only 3 points
    public LatLng getLocationByTrilateration(
            LatLng location1, double distance1,
            LatLng location2, double distance2,
            LatLng location3, double distance3){

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

        //TRANSALTE POINTS TO VECTORS
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
}
