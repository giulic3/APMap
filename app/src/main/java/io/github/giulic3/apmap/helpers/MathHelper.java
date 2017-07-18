package io.github.giulic3.apmap.helpers;

import android.database.Cursor;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import io.github.giulic3.apmap.data.Database;

public class MathHelper {


    // taken from stackoverflow, must give credit to author, distances in km
    // works with only 3 points TODO: provare cambiando conversione
    public static LatLng getLocationByTrilateration(LatLng location1, double distance1,
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
    public static double levelToDistance(int level, int frequency) { // be sure of minus sign

        double distanceInMetres = Math.pow(10, ((27.55 - (20*Math.log10(frequency)) - level)/ 20));
        double distanceInKilometres = distanceInMetres / 1000;
        return distanceInKilometres;
    }
    // Haversine formula: given two pair of coordinates (lat/lon) returns the distance between them in metres
    public static double convertToDistanceUsingDoubles(double lat1, double lon1,
                                                       double lat2, double lon2 ) {



        double earthRadius = 6371000; // metres
        double φ1 = Math.toRadians(lat1);
        double φ2 = Math.toRadians(lat2);
        double Δφ = Math.toRadians(lat2 - lat1);
        double Δλ = Math.toRadians(lon2 - lon1);

        double a = Math.sin(Δφ/2) * Math.sin(Δφ/2) + Math.cos(φ1) * Math.cos(φ2) *
                        Math.sin(Δλ/2) * Math.sin(Δλ/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        double distanceInMetres = earthRadius * c;
        return distanceInMetres; //metres
    }
    // given a set of scan results for a certain bssid, this method returns the max among
    // all distances then approximates coverage as a circle, returning its radius
    public static double determineCoverage(Cursor cursor, String bssid, double accessPointLatitude, double accessPointLongitude) {

        int length = cursor.getCount();
        double[] scanLatitudes = new double[length];
        double[] scanLongitudes = new double[length];
        double[] distances = new double[length];

        if (cursor.moveToFirst()) {
            for (int i = 0; i < length; i++) {

                scanLatitudes[i] = cursor.getDouble(cursor.getColumnIndexOrThrow(Database.Table2.COLUMN_NAME_SCAN_LATITUDE));
                scanLongitudes[i] = cursor.getDouble(cursor.getColumnIndexOrThrow(Database.Table2.COLUMN_NAME_SCAN_LONGITUDE));

                distances[i] = convertToDistanceUsingDoubles(accessPointLatitude, accessPointLongitude,
                        scanLatitudes[i], scanLongitudes[i]);
            }
        }

        double coverageRadius = max(distances);

        return coverageRadius;
    }

    // returns -1 if values is empty, i'm using it with distances so i know they must be > 0
    public static double max(double[] values) {
        double max = -1;
        for (double v : values)
            if (v > max)  max = v ;

        return max;

    }
}
